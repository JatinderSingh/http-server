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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import singh.jatinder.server.statistics.StatisticsEndPoint;

import com.stumbleupon.async.Deferred;

/**
 * @author Jatinder
 *
 * Class to Register all user defined endpoints to server.
 * 
 */
public class RequestDistributor extends RequestHandler {

	private static final Logger LOG = LoggerFactory.getLogger(RequestDistributor.class);
	private static final Map<String, IEndPoint> endPoints = new ConcurrentHashMap<String, IEndPoint>();
	
	static {
		endPoints.put("stats", new StatisticsEndPoint());
	}
	
	protected Deferred<HttpResponse> process(final ChannelHandlerContext context, final HttpRequest request) {
		LOG.debug("Http Request ? from thread ?", request, Thread.currentThread());
		Deferred<HttpResponse> def = null;
		final IEndPoint endPoint = endPoints.get(getEndPoint(request));
		if (null != endPoint) {
			def = endPoint.process(context, request);
		} else {
			def =  new Deferred<HttpResponse>();
			def.callback(ResponseUtils.PAGE_NOT_FOUND);
		}
		return def;
	}
	
	public void addEndPoint(String uri, IEndPoint endpoint) {
		endPoints.put(uri, endpoint);
	}
}