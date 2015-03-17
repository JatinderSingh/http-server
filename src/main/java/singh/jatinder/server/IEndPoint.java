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

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpResponse;
import singh.jatinder.netty.HttpObjectAggregator.AggregatedFullHttpRequest;

import com.stumbleupon.async.Deferred;

/**
 * @author Jatinder
 *
 * Marker interface for all uri end points linked with server
 * 
 */
public interface IEndPoint {
	Deferred<FullHttpResponse> process(ChannelHandlerContext context, AggregatedFullHttpRequest request);
}
