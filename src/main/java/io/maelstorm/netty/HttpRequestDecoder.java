/**
 * 
 */
package io.maelstorm.netty;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.util.ByteProcessor;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpConstants;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpExpectationFailedEvent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectDecoder;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.internal.AppendableCharSequence;

/**
 * @author Jatinder
 *
 */
public class HttpRequestDecoder extends io.netty.handler.codec.http.HttpRequestDecoder {
    private static final String EMPTY_VALUE = "";

    private final int maxChunkSize;
    private final boolean chunkedSupported;
    protected final boolean validateHeaders;
    private final HeaderParser headerParser;
    private final LineParser lineParser;

    private HttpMessage message;
    private long chunkSize;
    private long contentLength = Long.MIN_VALUE;
    private volatile boolean resetRequested;

    // These will be updated by splitHeader(...)
    private CharSequence name;
    private CharSequence value;
    

    private LastHttpContent trailer;
    private AppendableCharSequence method = new AppendableCharSequence(4);
    private AppendableCharSequence version = new AppendableCharSequence(8);
    private AppendableCharSequence uri = new AppendableCharSequence(8);
    
    /**
     * The internal state of {@link HttpObjectDecoder}.
     * <em>Internal use only</em>.
     */
    private enum State {
        SKIP_CONTROL_CHARS,
        READ_INITIAL,
        READ_HEADER,
        READ_VARIABLE_LENGTH_CONTENT,
        READ_FIXED_LENGTH_CONTENT,
        READ_CHUNK_SIZE,
        READ_CHUNKED_CONTENT,
        READ_CHUNK_DELIMITER,
        READ_CHUNK_FOOTER,
        BAD_MESSAGE,
        UPGRADED
    }
    
    private State currentState = State.SKIP_CONTROL_CHARS;
    
    /**
     * Creates a new instance with the default
     * {@code maxInitialLineLength (4096}}, {@code maxHeaderSize (8192)}, and
     * {@code maxChunkSize (8192)}.
     */
    protected HttpRequestDecoder() {
        this(4096, 8192, 8192, true);
    }

    /**
     * Creates a new instance with the specified parameters.
     */
    public HttpRequestDecoder(
            int maxInitialLineLength, int maxHeaderSize, int maxChunkSize, boolean chunkedSupported) {
        this(maxInitialLineLength, maxHeaderSize, maxChunkSize, chunkedSupported, true);
    }

    protected HttpRequestDecoder(
            int maxInitialLineLength, int maxHeaderSize, int maxChunkSize,
            boolean chunkedSupported, boolean validateHeaders) {
        this(maxInitialLineLength, maxHeaderSize, maxChunkSize, chunkedSupported, validateHeaders, 128);
    }
    
    /**
     * Creates a new instance with the specified parameters.
     */
    public HttpRequestDecoder(
            int maxInitialLineLength, int maxHeaderSize, int maxChunkSize,
            boolean chunkedSupported, boolean validateHeaders, int initialBufferSize) {

        if (maxInitialLineLength <= 0) {
            throw new IllegalArgumentException(
                    "maxInitialLineLength must be a positive integer: " +
                     maxInitialLineLength);
        }
        if (maxHeaderSize <= 0) {
            throw new IllegalArgumentException(
                    "maxHeaderSize must be a positive integer: " +
                    maxHeaderSize);
        }
        if (maxChunkSize <= 0) {
            throw new IllegalArgumentException(
                    "maxChunkSize must be a positive integer: " +
                    maxChunkSize);
        }
        this.maxChunkSize = maxChunkSize;
        this.chunkedSupported = chunkedSupported;
        this.validateHeaders = validateHeaders;
        AppendableCharSequence seq = new AppendableCharSequence(initialBufferSize);
        lineParser = new LineParser(seq, maxInitialLineLength);
        headerParser = new HeaderParser(seq, maxHeaderSize);
    }
    
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) throws Exception {
        if (resetRequested) {
            resetNow();
        }

        switch (currentState) {
        case SKIP_CONTROL_CHARS: {
            if (!skipControlCharacters(buffer)) {
                return;
            }
            currentState = State.READ_INITIAL;
        }
        case READ_INITIAL: try {
            AppendableCharSequence line = lineParser.parse(buffer);
            if (line == null) {
                return;
            }
            int aStart = findNonWhitespace(line, 0);
            int aEnd = findWhitespace(line, aStart);

            int bStart = findNonWhitespace(line, aEnd);
            int bEnd = findWhitespace(line, bStart);

            int cStart = findNonWhitespace(line, bEnd);
            int cEnd = findEndOfString(line);

            //String[] initialLine = splitInitialLine(line);
            if (cStart>=cEnd) {
                // Invalid initial line - ignore.
                currentState = State.SKIP_CONTROL_CHARS;
                return;
            }
// uncommment to fix
            
            AppendableCharSequenceAddon.invokeSubsequence(line, aStart, aEnd, method);
            AppendableCharSequenceAddon.invokeSubsequence(line, bStart, bEnd, uri);
            AppendableCharSequenceAddon.invokeSubsequence(line, cStart, cEnd, version);
            message = createMessage(method, version, uri);
            currentState = State.READ_HEADER;
            // fall-through
        } catch (Exception e) {
            out.add(invalidMessage(buffer, e));
            return;
        }
        case READ_HEADER: try {
            State nextState = readHeaders(buffer);
            if (nextState == null) {
                return;
            }
            currentState = nextState;
            switch (nextState) {
            case SKIP_CONTROL_CHARS:
                // fast-path
                // No content is expected.
                out.add(message);
                out.add(LastHttpContent.EMPTY_LAST_CONTENT);
                resetNow();
                return;
            case READ_CHUNK_SIZE:
                if (!chunkedSupported) {
                    throw new IllegalArgumentException("Chunked messages not supported");
                }
                // Chunked encoding - generate HttpMessage first.  HttpChunks will follow.
                out.add(message);
                return;
            default:
                long contentLength = contentLength();
                if (contentLength == 0 || contentLength == -1 && isDecodingRequest()) {
                    out.add(message);
                    out.add(LastHttpContent.EMPTY_LAST_CONTENT);
                    resetNow();
                    return;
                }

                assert nextState == State.READ_FIXED_LENGTH_CONTENT ||
                        nextState == State.READ_VARIABLE_LENGTH_CONTENT;

                out.add(message);

                if (nextState == State.READ_FIXED_LENGTH_CONTENT) {
                    // chunkSize will be decreased as the READ_FIXED_LENGTH_CONTENT state reads data chunk by chunk.
                    chunkSize = contentLength;
                }

                // We return here, this forces decode to be called again where we will decode the content
                return;
            }
        } catch (Exception e) {
            out.add(invalidMessage(buffer, e));
            return;
        }
        case READ_VARIABLE_LENGTH_CONTENT: {
            // Keep reading data as a chunk until the end of connection is reached.
            int toRead = Math.min(buffer.readableBytes(), maxChunkSize);
            if (toRead > 0) {
                ByteBuf content = buffer.readSlice(toRead).retain();
                out.add(new DefaultHttpContent(content));
            }
            return;
        }
        case READ_FIXED_LENGTH_CONTENT: {
            int readLimit = buffer.readableBytes();

            // Check if the buffer is readable first as we use the readable byte count
            // to create the HttpChunk. This is needed as otherwise we may end up with
            // create a HttpChunk instance that contains an empty buffer and so is
            // handled like it is the last HttpChunk.
            //
            // See https://github.com/netty/netty/issues/433
            if (readLimit == 0) {
                return;
            }

            int toRead = Math.min(readLimit, maxChunkSize);
            if (toRead > chunkSize) {
                toRead = (int) chunkSize;
            }
            ByteBuf content = buffer.readSlice(toRead).retain();
            chunkSize -= toRead;

            if (chunkSize == 0) {
                // Read all content.
                out.add(new DefaultLastHttpContent(content, validateHeaders));
                resetNow();
            } else {
                out.add(new DefaultHttpContent(content));
            }
            return;
        }
        /**
         * everything else after this point takes care of reading chunked content. basically, read chunk size,
         * read chunk, read and ignore the CRLF and repeat until 0
         */
        case READ_CHUNK_SIZE: try {
            AppendableCharSequence line = lineParser.parse(buffer);
            if (line == null) {
                return;
            }
            int chunkSize = getChunkSize(line.toString());
            this.chunkSize = chunkSize;
            if (chunkSize == 0) {
                currentState = State.READ_CHUNK_FOOTER;
                return;
            }
            currentState = State.READ_CHUNKED_CONTENT;
            // fall-through
        } catch (Exception e) {
            out.add(invalidChunk(buffer, e));
            return;
        }
        case READ_CHUNKED_CONTENT: {
            assert chunkSize <= Integer.MAX_VALUE;
            int toRead = Math.min((int) chunkSize, maxChunkSize);
            toRead = Math.min(toRead, buffer.readableBytes());
            if (toRead == 0) {
                return;
            }
            HttpContent chunk = new DefaultHttpContent(buffer.readSlice(toRead).retain());
            chunkSize -= toRead;

            out.add(chunk);

            if (chunkSize != 0) {
                return;
            }
            currentState = State.READ_CHUNK_DELIMITER;
            // fall-through
        }
        case READ_CHUNK_DELIMITER: {
            final int wIdx = buffer.writerIndex();
            int rIdx = buffer.readerIndex();
            while (wIdx > rIdx) {
                byte next = buffer.getByte(rIdx++);
                if (next == HttpConstants.LF) {
                    currentState = State.READ_CHUNK_SIZE;
                    break;
                }
            }
            buffer.readerIndex(rIdx);
            return;
        }
        case READ_CHUNK_FOOTER: try {
            LastHttpContent trailer = readTrailingHeaders(buffer);
            if (trailer == null) {
                return;
            }
            out.add(trailer);
            resetNow();
            return;
        } catch (Exception e) {
            out.add(invalidChunk(buffer, e));
            return;
        }
        case BAD_MESSAGE: {
            // Keep discarding until disconnection.
            buffer.skipBytes(buffer.readableBytes());
            break;
        }
        case UPGRADED: {
            int readableBytes = buffer.readableBytes();
            if (readableBytes > 0) {
                // Keep on consuming as otherwise we may trigger an DecoderException,
                // other handler will replace this codec with the upgraded protocol codec to
                // take the traffic over at some point then.
                // See https://github.com/netty/netty/issues/2173
                out.add(buffer.readBytes(readableBytes));
            }
            break;
        }
        }
    }
    
    @Override
    protected void decodeLast(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        super.decodeLast(ctx, in, out);

        // Handle the last unfinished message.
        if (message != null) {
            boolean chunked = HttpUtil.isTransferEncodingChunked(message);
            if (currentState == State.READ_VARIABLE_LENGTH_CONTENT && !in.isReadable() && !chunked) {
                // End of connection.
                out.add(LastHttpContent.EMPTY_LAST_CONTENT);
                reset();
                return;
            }
            // Check if the closure of the connection signifies the end of the content.
            boolean prematureClosure;
            if (isDecodingRequest() || chunked) {
                // The last request did not wait for a response.
                prematureClosure = true;
            } else {
                // Compare the length of the received content and the 'Content-Length' header.
                // If the 'Content-Length' header is absent, the length of the content is determined by the end of the
                // connection, so it is perfectly fine.
                prematureClosure = contentLength() > 0;
            }
            resetNow();

            if (!prematureClosure) {
                out.add(LastHttpContent.EMPTY_LAST_CONTENT);
            }
        }
    }
    
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof HttpExpectationFailedEvent) {
            switch (currentState) {
            case READ_FIXED_LENGTH_CONTENT:
            case READ_VARIABLE_LENGTH_CONTENT:
            case READ_CHUNK_SIZE:
                reset();
                break;
            default:
                break;
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    protected boolean isContentAlwaysEmpty(HttpMessage msg) {
        if (msg instanceof HttpResponse) {
            HttpResponse res = (HttpResponse) msg;
            int code = res.status().code();

            // Correctly handle return codes of 1xx.
            //
            // See:
            //     - http://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html Section 4.4
            //     - https://github.com/netty/netty/issues/222
            if (code >= 100 && code < 200) {
                // One exception: Hixie 76 websocket handshake response
                return !(code == 101 && !res.headers().contains(HttpHeaderNames.SEC_WEBSOCKET_ACCEPT)
                         && res.headers().contains(HttpHeaderNames.UPGRADE, HttpHeaderValues.WEBSOCKET, true));
            }

            switch (code) {
            case 204: case 205: case 304:
                return true;
            }
        }
        return false;
    }

    /**
     * Resets the state of the decoder so that it is ready to decode a new message.
     * This method is useful for handling a rejected request with {@code Expect: 100-continue} header.
     */
    public void reset() {
        resetRequested = true;
    }
    
    protected HttpMessage createMessage(AppendableCharSequence method, AppendableCharSequence version, AppendableCharSequence uri) throws Exception {
        return new LargeUriHttpRequest(getHttpVersion(version), getMethod(method), uri, validateHeaders);
    }
    
    protected HttpMessage createInvalidMessage() {
        return new LargeUriHttpRequest(HttpVersion.HTTP_1_0, HttpMethod.GET, new AppendableCharSequence(12).append("/bad-request"), validateHeaders);
    }

    protected boolean isDecodingRequest() {
        return true;
    }
    
    private static HttpVersion getHttpVersion(AppendableCharSequence version) {
        if ('H' == version.charAtUnsafe(0) && 'T' == version.charAtUnsafe(1) && 'T' == version.charAtUnsafe(2) && 'P' == version.charAtUnsafe(3) && '/' == version.charAtUnsafe(4) && '1' == version.charAtUnsafe(5) && '.' == version.charAtUnsafe(6)){
            if ('0' == version.charAtUnsafe(7))
                return HttpVersion.HTTP_1_0;
            if ('1' == version.charAtUnsafe(7))
                return HttpVersion.HTTP_1_1;
        }
        return null;
    }
    
    private static HttpMethod getMethod(AppendableCharSequence method) {
        switch (method.charAtUnsafe(0)){
            case 'O' :
                if ('P' == method.charAtUnsafe(1) && 'T' == method.charAtUnsafe(2) && 'I' == method.charAtUnsafe(3) && 'O' == method.charAtUnsafe(4) && 'N' == method.charAtUnsafe(5) && 'S' == method.charAtUnsafe(6))
                    return HttpMethod.OPTIONS;
            case 'H' :
                if ('E' == method.charAtUnsafe(1) && 'A' == method.charAtUnsafe(2) && 'D' == method.charAtUnsafe(3))
                    return HttpMethod.HEAD;
            case 'G' :
                if ('E' == method.charAtUnsafe(1) && 'T' == method.charAtUnsafe(2))
                    return HttpMethod.GET;
            case 'T' :
                if ('R' == method.charAtUnsafe(1) && 'A' == method.charAtUnsafe(2) && 'C' == method.charAtUnsafe(3) && 'E' == method.charAtUnsafe(4))
                    return HttpMethod.TRACE;
            case 'D' :
                if ('E' == method.charAtUnsafe(1) && 'L' == method.charAtUnsafe(2) && 'E' == method.charAtUnsafe(3) && 'T' == method.charAtUnsafe(4) && 'E' == method.charAtUnsafe(5))
                    return HttpMethod.DELETE;
            case 'C' :
                if ('O' == method.charAtUnsafe(1) && 'N' == method.charAtUnsafe(2) && 'N' == method.charAtUnsafe(3) && 'E' == method.charAtUnsafe(4) && 'C' == method.charAtUnsafe(5) && 'T' == method.charAtUnsafe(6))
                    return HttpMethod.CONNECT;
            case 'P' :
                if ('U' == method.charAtUnsafe(1) && 'T' == method.charAtUnsafe(2))
                    return HttpMethod.PUT;
                if ('O' == method.charAtUnsafe(1) && 'S' == method.charAtUnsafe(2) && 'T' == method.charAtUnsafe(3))
                    return HttpMethod.POST;
                if ('A' == method.charAtUnsafe(1) && 'T' == method.charAtUnsafe(2) && 'C' == method.charAtUnsafe(3) && 'H' == method.charAtUnsafe(4))
                    return HttpMethod.PATCH;
        }
        return HttpMethod.valueOf(method.toString());
    }
    
    private void resetNow() {
        HttpMessage message = this.message;
        this.message = null;
        name = null;
        value = null;
        contentLength = Long.MIN_VALUE;
        lineParser.reset();
        headerParser.reset();
        trailer = null;
        if (!isDecodingRequest()) {
            HttpResponse res = (HttpResponse) message;
            if (res != null && res.status().code() == 101) {
                currentState = State.UPGRADED;
                return;
            }
        }

        currentState = State.SKIP_CONTROL_CHARS;
    }

    private HttpMessage invalidMessage(ByteBuf in, Exception cause) {
        currentState = State.BAD_MESSAGE;

        // Advance the readerIndex so that ByteToMessageDecoder does not complain
        // when we produced an invalid message without consuming anything.
        in.skipBytes(in.readableBytes());

        if (message != null) {
            message.setDecoderResult(DecoderResult.failure(cause));
        } else {
            message = createInvalidMessage();
            message.setDecoderResult(DecoderResult.failure(cause));
        }

        HttpMessage ret = message;
        message = null;
        return ret;
    }

    private HttpContent invalidChunk(ByteBuf in, Exception cause) {
        currentState = State.BAD_MESSAGE;

        // Advance the readerIndex so that ByteToMessageDecoder does not complain
        // when we produced an invalid message without consuming anything.
        in.skipBytes(in.readableBytes());

        HttpContent chunk = new DefaultLastHttpContent(Unpooled.EMPTY_BUFFER);
        chunk.setDecoderResult(DecoderResult.failure(cause));
        message = null;
        trailer = null;
        return chunk;
    }

    private static boolean skipControlCharacters(ByteBuf buffer) {
        boolean skiped = false;
        final int wIdx = buffer.writerIndex();
        int rIdx = buffer.readerIndex();
        while (wIdx > rIdx) {
            int c = buffer.getUnsignedByte(rIdx++);
            if (!Character.isISOControl(c) && !Character.isWhitespace(c)) {
                rIdx--;
                skiped = true;
                break;
            }
        }
        buffer.readerIndex(rIdx);
        return skiped;
    }

    private State readHeaders(ByteBuf buffer) {
        final HttpMessage message = this.message;
        final HttpHeaders headers = message.headers();

        AppendableCharSequence line = headerParser.parse(buffer);
        if (line == null) {
            return null;
        }
        if (line.length() > 0) {
            do {
                char firstChar = line.charAt(0);
                if (name != null && (firstChar == ' ' || firstChar == '\t')) {
                    StringBuilder buf = new StringBuilder(value.length() + line.length() + 1);
                    buf.append(value)
                       .append(' ')
                       .append(line.toString().trim());
                    value = buf.toString();
                } else {
                    if (name != null) {
                        headers.add(name, value);
                    }
                    splitHeader(line);
                }

                line = headerParser.parse(buffer);
                if (line == null) {
                    return null;
                }
            } while (line.length() > 0);
        }

        // Add the last header.
        if (name != null) {
            headers.add(name, value);
        }
        // reset name and value fields
        name = null;
        value = null;

        State nextState;

        if (isContentAlwaysEmpty(message)) {
            HttpUtil.setTransferEncodingChunked(message, false);
            nextState = State.SKIP_CONTROL_CHARS;
        } else if (HttpUtil.isTransferEncodingChunked(message)) {
            nextState = State.READ_CHUNK_SIZE;
        } else if (contentLength() >= 0) {
            nextState = State.READ_FIXED_LENGTH_CONTENT;
        } else {
            nextState = State.READ_VARIABLE_LENGTH_CONTENT;
        }
        return nextState;
    }

    private long contentLength() {
        if (contentLength == Long.MIN_VALUE) {
            contentLength = HttpUtil.getContentLength(message, -1);
        }
        return contentLength;
    }

    private LastHttpContent readTrailingHeaders(ByteBuf buffer) {
        AppendableCharSequence line = headerParser.parse(buffer);
        if (line == null) {
            return null;
        }
        CharSequence lastHeader = null;
        if (line.length() > 0) {
            LastHttpContent trailer = this.trailer;
            if (trailer == null) {
                trailer = this.trailer = new DefaultLastHttpContent(Unpooled.EMPTY_BUFFER, validateHeaders);
            }
            do {
                char firstChar = line.charAt(0);
                if (lastHeader != null && (firstChar == ' ' || firstChar == '\t')) {
                    List<String> current = trailer.trailingHeaders().getAll(lastHeader);
                    if (!current.isEmpty()) {
                        int lastPos = current.size() - 1;
                        String lineTrimmed = line.toString().trim();
                        CharSequence currentLastPos = current.get(lastPos);
                        StringBuilder b = new StringBuilder(currentLastPos.length() + lineTrimmed.length());
                        b.append(currentLastPos)
                         .append(lineTrimmed);
                        current.set(lastPos, b.toString());
                    } else {
                        // Content-Length, Transfer-Encoding, or Trailer
                    }
                } else {
                    splitHeader(line);
                    CharSequence headerName = name;
                    if (!HttpHeaderNames.CONTENT_LENGTH.contentEqualsIgnoreCase(headerName) &&
                        !HttpHeaderNames.TRANSFER_ENCODING.contentEqualsIgnoreCase(headerName) &&
                        !HttpHeaderNames.TRAILER.contentEqualsIgnoreCase(headerName)) {
                        trailer.trailingHeaders().add(headerName, value);
                    }
                    lastHeader = name;
                    // reset name and value fields
                    name = null;
                    value = null;
                }

                line = headerParser.parse(buffer);
                if (line == null) {
                    return null;
                }
            } while (line.length() > 0);

            this.trailer = null;
            return trailer;
        }

        return LastHttpContent.EMPTY_LAST_CONTENT;
    }

    private static int getChunkSize(String hex) {
        hex = hex.trim();
        for (int i = 0; i < hex.length(); i ++) {
            char c = hex.charAt(i);
            if (c == ';' || Character.isWhitespace(c) || Character.isISOControl(c)) {
                hex = hex.substring(0, i);
                break;
            }
        }

        return Integer.parseInt(hex, 16);
    }

    private void splitHeader(AppendableCharSequence sb) {
        final int length = sb.length();
        int nameStart;
        int nameEnd;
        int colonEnd;
        int valueStart;
        int valueEnd;

        nameStart = findNonWhitespace(sb, 0);
        for (nameEnd = nameStart; nameEnd < length; nameEnd ++) {
            char ch = sb.charAt(nameEnd);
            if (ch == ':' || Character.isWhitespace(ch)) {
                break;
            }
        }

        for (colonEnd = nameEnd; colonEnd < length; colonEnd ++) {
            if (sb.charAt(colonEnd) == ':') {
                colonEnd ++;
                break;
            }
        }

        name = sb.substring(nameStart, nameEnd);
        valueStart = findNonWhitespace(sb, colonEnd);
        if (valueStart == length) {
            value = EMPTY_VALUE;
        } else {
            valueEnd = findEndOfString(sb);
            value = sb.substring(valueStart, valueEnd);
        }
    }

    private static int findNonWhitespace(CharSequence sb, int offset) {
        int result;
        for (result = offset; result < sb.length(); result ++) {
            if (!Character.isWhitespace(sb.charAt(result))) {
                break;
            }
        }
        return result;
    }

    private static int findWhitespace(CharSequence sb, int offset) {
        int result;
        for (result = offset; result < sb.length(); result ++) {
            if (Character.isWhitespace(sb.charAt(result))) {
                break;
            }
        }
        return result;
    }

    private static int findEndOfString(CharSequence sb) {
        int result;
        for (result = sb.length(); result > 0; result --) {
            if (!Character.isWhitespace(sb.charAt(result - 1))) {
                break;
            }
        }
        return result;
    }

    private static class HeaderParser implements ByteProcessor {
        private final AppendableCharSequence seq;
        private final int maxLength;
        private int size;

        HeaderParser(AppendableCharSequence seq, int maxLength) {
            this.seq = seq;
            this.maxLength = maxLength;
        }

        public AppendableCharSequence parse(ByteBuf buffer) {
            final int oldSize = size;
            seq.reset();
            int i = buffer.forEachByte(this);
            if (i == -1) {
                size = oldSize;
                return null;
            }
            buffer.readerIndex(i + 1);
            return seq;
        }

        public void reset() {
            size = 0;
        }

        @Override
        public boolean process(byte value) throws Exception {
            char nextByte = (char) value;
            if (nextByte == HttpConstants.CR) {
                return true;
            }
            if (nextByte == HttpConstants.LF) {
                return false;
            }

            if (++ size > maxLength) {
                // TODO: Respond with Bad Request and discard the traffic
                //    or close the connection.
                //       No need to notify the upstream handlers - just log.
                //       If decoding a response, just throw an exception.
                throw newException(maxLength);
            }

            seq.append(nextByte);
            return true;
        }

        protected TooLongFrameException newException(int maxLength) {
            return new TooLongFrameException("HTTP header is larger than " + maxLength + " bytes.");
        }
    }

    static final class LineParser extends HeaderParser {

        LineParser(AppendableCharSequence seq, int maxLength) {
            super(seq, maxLength);
        }

        @Override
        public AppendableCharSequence parse(ByteBuf buffer) {
            reset();
            return super.parse(buffer);
        }

        @Override
        protected TooLongFrameException newException(int maxLength) {
            return new TooLongFrameException("An HTTP line is larger than " + maxLength + " bytes.");
        }
    }
}
