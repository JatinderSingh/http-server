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

import java.nio.charset.Charset;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

import com.stumbleupon.async.Deferred;

import singh.jatinder.server.IEndPoint;

/**
 * @author Jatinder
 *
 * Endpoint to shutdown server
 *
 */
public abstract class ShutDownEndpoint extends RequestHandler implements IEndPoint {

	public Deferred<HttpResponse> process(ChannelHandlerContext context, HttpRequest request) {
		HttpResponse response = new DefaultHttpResponse(request.getProtocolVersion(), HttpResponseStatus.OK);
		response.setHeader("content-type", "text/html");
		response.setContent(ChannelBuffers.copiedBuffer("Accepted ShutDown Request", Charset.defaultCharset()));
		Deferred<HttpResponse> deferred = new Deferred<HttpResponse>();
		deferred.callback(response);
		shutdown();
		super.doShutdown(context.getChannel());
		return deferred;
	}

	public abstract void shutdown();
}
