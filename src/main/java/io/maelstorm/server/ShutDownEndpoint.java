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
package io.maelstorm.server;

import com.stumbleupon.async.Deferred;

import io.maelstorm.netty.HttpObjectAggregator.AggregatedFullHttpRequest;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * @author Jatinder
 *
 * Endpoint to shutdown server
 *
 */
public abstract class ShutDownEndpoint extends RequestHandler implements IEndPoint {
	
	private static final String resp = "Accepted ShutDown Request";
	public Deferred<FullHttpResponse> process(ChannelHandlerContext context, AggregatedFullHttpRequest request) {
		ByteBuf buffer = context.alloc().buffer(resp.length());
		buffer.writeBytes(resp.getBytes());
		FullHttpResponse response = new DefaultFullHttpResponse(request.getProtocolVersion(), HttpResponseStatus.OK, buffer);
		response.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN);
		response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
		Deferred<FullHttpResponse> deferred = new Deferred<FullHttpResponse>();
		deferred.callback(response);
		shutDownGracefully();
		super.doShutdown();
		return deferred;
	}
	
	public abstract void shutDownGracefully();
}
