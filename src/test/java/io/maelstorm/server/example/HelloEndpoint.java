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
package io.maelstorm.server.example;

import com.stumbleupon.async.Deferred;

import io.maelstorm.netty.HttpObjectAggregator.AggregatedFullHttpRequest;
import io.maelstorm.server.IEndPoint;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * @author Jatinder
 *
 * Endpoint for Hello world Example
 *
 */
public class HelloEndpoint implements IEndPoint {

	byte[] response = "Hello".getBytes();
	DefaultHttpHeaders headers = new DefaultHttpHeaders();
	
	public HelloEndpoint() {
		headers.add(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN);
	}
	
	public Deferred<FullHttpResponse> process(ChannelHandlerContext context, AggregatedFullHttpRequest request) {
		ByteBuf buffer = context.alloc().buffer();
		buffer.writeBytes(response);
		FullHttpResponse response = new DefaultFullHttpResponse(request.getProtocolVersion(), HttpResponseStatus.OK, buffer, headers, EmptyHttpHeaders.INSTANCE);
		Deferred<FullHttpResponse> deferred = new Deferred<FullHttpResponse>();
		deferred.callback(response);
		return deferred;
	} 
}
