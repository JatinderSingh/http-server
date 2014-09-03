
/*
 * 
 * This file is part of Http-Server
 * Copyright (C) 2013  Jatinder
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this library; If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package singh.jatinder.server;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.AppendableCharSequence;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import singh.jatinder.server.exception.DefectiveRequest;
import singh.jatinder.server.statistics.ICollector;

import com.stumbleupon.async.Callback;
import com.stumbleupon.async.Deferred;

/**
 * @author Jatinder
 *
 * Servers link to handle Netty's messages and parse http request/responses
 *
 */
public abstract class RequestHandler extends ChannelInboundHandlerAdapter implements ICollector {
	
	private static ServerInit initializer;
	private static final Logger LOG = LoggerFactory.getLogger(RequestHandler.class);
	protected static final AtomicLong totalHttpRequests = new AtomicLong();
	protected static final AtomicLong totalExceptions = new AtomicLong();
	protected static final AtomicLong activeHttpRequests = new AtomicLong();
	private static final String normalLogTemplate = "Request on uri {} of type {} was processed in {} ms";
	private static final String exceptionLogTemplate = "Request on uri {} of type {} threw Exception in {} ms";
	private static final String unexpectedExceptionLogTemplate = "Unexpected request : {} from channel {} ";
	
	  @Override
	  public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
		final long start = System.nanoTime();
		if (msg instanceof FullHttpRequest) {
			final FullHttpRequest request = (FullHttpRequest) msg;
			totalHttpRequests.incrementAndGet();
			activeHttpRequests.incrementAndGet();
			Deferred<FullHttpResponse> response = process(ctx, request);
			response.addCallbacks(new Callback<Object, FullHttpResponse>() {
				public Object call(FullHttpResponse response) throws Exception {
					sendResponse(ctx, response, request);
					LOG.trace(normalLogTemplate, request.getUri(), request.getMethod(), (System.nanoTime() - start) / 1000000 );
					activeHttpRequests.decrementAndGet();
					return null;
				}
			}, new Callback<Object, Exception>() {
				public Object call(Exception arg) throws Exception {
					totalExceptions.incrementAndGet();
					FullHttpResponse response = new DefaultFullHttpResponse(request.getProtocolVersion(), HttpResponseStatus.INTERNAL_SERVER_ERROR);
					response.headers().add(HttpHeaders.Names.CONTENT_TYPE, "text/html; charset=UTF-8");
					// FIXME response.setContent(arg.getMessage().getBytes(arg0));
					sendResponse(ctx, response, request);
					LOG.debug(exceptionLogTemplate, request.getUri(), request.getMethod(), (System.nanoTime() - start) / 1000000 );
					LOG.error(arg.getLocalizedMessage(), arg);
					activeHttpRequests.decrementAndGet();
					return null;
				}
			});
		} else {
			LOG.error(unexpectedExceptionLogTemplate, msg, ctx.channel());
			totalExceptions.incrementAndGet();
			ctx.channel().close();
		}

	  }

	void setInitializer(ServerInit initialiser) {
		if (null == this.initializer) {
			this.initializer = initialiser;
		}
	}
  
	private void sendResponse(final ChannelHandlerContext ctx, final FullHttpResponse response, final HttpRequest request) {
		ReferenceCountUtil.release(request);
		if (!ctx.channel().isActive()) {
			return;
		}
		final boolean keepalive = HttpHeaders.isKeepAlive(request);
		if (keepalive) {
			HttpHeaders.setContentLength(response, response.content().readableBytes());
			response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
		}
		final ChannelFuture future = ctx.write(response);
		ctx.flush();
		if (!keepalive) {
			future.addListener(ChannelFutureListener.CLOSE);
		}
	}

	protected void doShutdown() {
		LOG.info("shutdown requested ");
		// Netty gets stuck in an infinite loop if we shut it down from within a
		// NIO thread. So do this from a newly created thread.
		final class ShutdownNetty extends Thread {
			ShutdownNetty() {
				super("ShutdownNetty");
			}
			public void run() {
				initializer.shutDownGracefully();
			}
		}
		new ShutdownNetty().start();
	}
	
	/**
	 * Should always call request.sendReply(String reply) once during whole method
	 * @param msg
	 */
	protected abstract Deferred<FullHttpResponse> process(ChannelHandlerContext context, FullHttpRequest request);
	
	protected String getEndPoint(final HttpRequest request) {
		final AppendableCharSequence uri = request.getUri();
		if (uri.length() < 1) {
			throw new DefectiveRequest("Empty query");
		}
		if (uri.charAt(0) != '/') {
			throw new DefectiveRequest("Query doesn't start with a slash: <code>" + uri + "</code>");
		}
		int questionmark = -1;
		int slash = -1;
		boolean foundAll = false;
		for(int i=1; i<uri.length() && !foundAll; i++) {
			switch (uri.charAt(i)) {
				case '?' :
					questionmark = i;
				break;
				case '/' :
					slash = i;
				break;
				default :
					if (i>1 && (questionmark > 1 || slash > 1)) {
						foundAll = true;
					}
				break;
			}
		}
		int pos = uri.length(); // Will be set to where the first path segment ends.
		if (questionmark > 0) {
			if (slash > 0) {
				pos = (questionmark < slash ? questionmark // Request: /foo?bar/quux
						: slash); // Request: /foo/bar?quux
			} else {
				pos = questionmark; // Request: /foo?bar
			}
		} else {
			pos = (slash > 0 ? slash // Request: /foo/bar
					: uri.length()); // Request: /foo
		}
		return uri.substring(1, pos);
	}
	
	public Map<String, Number> getStatistics() {
		Map<String, Number> stats = new HashMap<String, Number>();
		stats.put("totalHttpRequests", totalHttpRequests.get());
		stats.put("totalExceptions", totalExceptions.get());
		stats.put("activeHttpRequests", activeHttpRequests.get());
		return stats;
	}
	
    public boolean isDisplayed() {
        return true;
    }
}
