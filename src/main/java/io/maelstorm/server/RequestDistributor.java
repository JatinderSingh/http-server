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

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.maelstorm.netty.HttpObjectAggregator.AggregatedFullHttpRequest;
import io.maelstorm.server.statistics.StatisticsEndPoint;

import com.stumbleupon.async.Deferred;

/**
 * @author Jatinder
 *
 * Class to Register all user defined endpoints to server.
 * 
 */
@Sharable
public class RequestDistributor extends RequestHandler {

	private static final Logger LOG = LoggerFactory.getLogger(RequestDistributor.class);
	private static final Map<String, IEndPoint> endPoints = new ConcurrentHashMap<String, IEndPoint>();
	
	static {
		endPoints.put("stats", new StatisticsEndPoint());
	}
	
	protected Deferred<FullHttpResponse> process(final ChannelHandlerContext context, final AggregatedFullHttpRequest request) {
		Deferred<FullHttpResponse> def = null;
		final IEndPoint endPoint = endPoints.get(getEndPoint(request.geturi()));
		if (null != endPoint) {
			try {
				def = endPoint.process(context, request);
			} catch (Exception e) {
				def =  new Deferred<FullHttpResponse>();
				FullHttpResponse response = new DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.INTERNAL_SERVER_ERROR, ResponseUtils.makePage(null, "INTERNAL ERROR", "Error 500", new StringBuilder(e.toString())));
				def.callback(response);
			}
		} else {
			def =  new Deferred<FullHttpResponse>();
			def.callback(ResponseUtils.PAGE_NOT_FOUND);
		}
		return def;
	}
	
	public void addEndPoint(String uri, IEndPoint endpoint) {
		endPoints.put(uri.intern(), endpoint);
	}
}