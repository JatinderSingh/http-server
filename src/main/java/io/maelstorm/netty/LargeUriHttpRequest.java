/**
 * 
 */
package io.maelstorm.netty;

import io.netty.handler.codec.http.DefaultHttpMessage;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.internal.AppendableCharSequence;

/**
 * @author Jatinder
 *
 */
public class LargeUriHttpRequest extends DefaultHttpMessage implements HttpRequest {

    private static final int HASH_CODE_PRIME = 31;
    private HttpMethod method;
    private AppendableCharSequence uri;

    /**
     * Creates a new instance.
     *
     * @param httpVersion the HTTP version of the request
     * @param method      the HTTP getMethod of the request
     * @param uri         the URI or path of the request
     */
    public LargeUriHttpRequest(HttpVersion httpVersion, HttpMethod method, AppendableCharSequence uri) {
        this(httpVersion, method, uri, true);
    }

    /**
     * Creates a new instance.
     *
     * @param httpVersion       the HTTP version of the request
     * @param method            the HTTP getMethod of the request
     * @param uri               the URI or path of the request
     * @param validateHeaders   validate the header names and values when adding them to the {@link HttpHeaders}
     */
    public LargeUriHttpRequest(HttpVersion httpVersion, HttpMethod method, AppendableCharSequence uri, boolean validateHeaders) {
        super(httpVersion, validateHeaders, false);
        if (method == null) {
            throw new NullPointerException("method");
        }
        if (uri == null) {
            throw new NullPointerException("uri");
        }
        this.method = method;
        this.uri = uri;
    }

    @Override
    @Deprecated
    public HttpMethod getMethod() {
        return method();
    }

    @Override
    public HttpMethod method() {
        return method;
    }

    @Override
    @Deprecated
    public String getUri() {
        return uri();
    }

    @Override
    public String uri() {
        return uri.toString();
    }
    
    public AppendableCharSequence geturi() {
        return uri;
    }

    @Override
    public HttpRequest setMethod(HttpMethod method) {
        if (method == null) {
            throw new NullPointerException("method");
        }
        this.method = method;
        return this;
    }

    @Override
    public HttpRequest setUri(String uri) {
        if (uri == null) {
            throw new NullPointerException("uri");
        }
        this.uri = new AppendableCharSequence(uri.length());
        this.uri.append(uri);
        return this;
    }

    @Override
    public HttpRequest setProtocolVersion(HttpVersion version) {
        super.setProtocolVersion(version);
        return this;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = HASH_CODE_PRIME * result + method.hashCode();
        result = HASH_CODE_PRIME * result + uri.hashCode();
        result = HASH_CODE_PRIME * result + super.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof LargeUriHttpRequest)) {
            return false;
        }

        LargeUriHttpRequest other = (LargeUriHttpRequest) o;

        return method().equals(other.method()) &&
               uri().equalsIgnoreCase(other.uri()) &&
               super.equals(o);
    }

    @Override
    public String toString() {
        return method + " " + uri.toString();
    }
}
