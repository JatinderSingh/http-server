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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
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
public abstract class RequestHandler extends SimpleChannelUpstreamHandler implements ICollector {
	
	private static final Logger LOG = LoggerFactory.getLogger(RequestHandler.class);
	protected static final AtomicLong totalHttpRequests = new AtomicLong();
	protected static final AtomicLong totalExceptions = new AtomicLong();
	protected static final AtomicLong activeHttpRequests = new AtomicLong();
	
	  @Override
	  public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent msgevent) {
		final long start = System.nanoTime();
		if (msgevent.getMessage() instanceof HttpRequest) {
			final HttpRequest request = (HttpRequest) msgevent.getMessage();
			totalHttpRequests.incrementAndGet();
			activeHttpRequests.incrementAndGet();
			Deferred<HttpResponse> response = process(ctx, request);
			response.addCallbacks(new Callback<Object, HttpResponse>() {
				public Object call(HttpResponse response) throws Exception {
					sendResponse(response, msgevent);
					LOG.info("Request on uri {} of type {} was processed in {} ms", new Object[] { request.getUri(), request.getMethod(), (System.nanoTime() - start) / 1000000 });
					activeHttpRequests.decrementAndGet();
					return null;
				}
			}, new Callback<Object, Exception>() {
				public Object call(Exception arg) throws Exception {
					totalExceptions.incrementAndGet();
					HttpResponse response = new DefaultHttpResponse(request.getProtocolVersion(), HttpResponseStatus.INTERNAL_SERVER_ERROR);
					response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/html; charset=UTF-8");
					// FIXME response.setContent(arg.getMessage().getBytes(arg0));
					sendResponse(response, msgevent);
					LOG.info("Request on uri {} of type {} threw Exception in {} ms", new Object[] { request.getUri(), request.getMethod(), (System.nanoTime() - start) / 1000000 });
					LOG.error(arg.getLocalizedMessage(), arg);
					activeHttpRequests.decrementAndGet();
					return null;
				}
			});
		} else {
			LOG.error("Unexpected request : {} from channel {} ", msgevent.getMessage(), msgevent.getChannel());
			totalExceptions.incrementAndGet();
		}
	  }
	  
	private void sendResponse(final HttpResponse response, final MessageEvent request) {
		if (!request.getChannel().isConnected()) {
			return;
		}
		final boolean keepalive = HttpHeaders.isKeepAlive((HttpRequest)request.getMessage());
		if (keepalive) {
			HttpHeaders.setContentLength(response, response.getContent().readableBytes());
		}
		final ChannelFuture future = request.getChannel().write(response);
		if (!keepalive) {
			future.addListener(ChannelFutureListener.CLOSE);
		}
	}

	protected void doShutdown(final Channel channel) {
		LOG.info("shutdown requested from channel {} ", channel);
		ConnectionManager.closeAllConnections();
		// Netty gets stuck in an infinite loop if we shut it down from within a
		// NIO thread. So do this from a newly created thread.
		final class ShutdownNetty extends Thread {
			ShutdownNetty() {
				super("ShutdownNetty");
			}
			public void run() {
				channel.getFactory().releaseExternalResources();
			}
		}
		new ShutdownNetty().start();
	}
	
	/**
	 * Should always call request.sendReply(String reply) once during whole method
	 * @param msg
	 */
	protected abstract Deferred<HttpResponse> process(ChannelHandlerContext context, HttpRequest request);
	
	protected String getEndPoint(final HttpRequest request) {
		final String uri = request.getUri();
		if (uri.length() < 1) {
			throw new DefectiveRequest("Empty query");
		}
		if (uri.charAt(0) != '/') {
			throw new DefectiveRequest("Query doesn't start with a slash: <code>" + uri + "</code>");
		}
		final int questionmark = uri.indexOf('?', 1);
		final int slash = uri.indexOf('/', 1);
		int pos; // Will be set to where the first path segment ends.
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
}
