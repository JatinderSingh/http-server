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
package singh.jatinder.server.example;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.ReferenceCountUtil;
import singh.jatinder.server.IEndPoint;

import com.stumbleupon.async.Deferred;

/**
 * @author Jatinder
 *
 * Endpoint for Hello world Example
 *
 */
public class HelloEndpoint implements IEndPoint {

	byte[] response = "Hello".getBytes();
	
	public Deferred<FullHttpResponse> process(ChannelHandlerContext context, FullHttpRequest request) {
		ReferenceCountUtil.release(request);
		ByteBuf buffer = context.alloc().buffer();
		buffer.writeBytes(response);
		FullHttpResponse response = new DefaultFullHttpResponse(request.getProtocolVersion(), HttpResponseStatus.OK, buffer);
		response.headers().add(HttpHeaders.Names.CONTENT_TYPE, "text/html");
		Deferred<FullHttpResponse> deferred = new Deferred<FullHttpResponse>();
		deferred.callback(response);
		return deferred;
	} 
}
