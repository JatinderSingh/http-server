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
package singh.jatinder.server.statistics;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

import singh.jatinder.server.IEndPoint;
import singh.jatinder.server.ResponseUtils;

import com.stumbleupon.async.Deferred;

/**
 * @author Jatinder
 *
 * Statistical data display endpoint
 * All collectors should register here.
 */
public class StatisticsEndPoint implements IEndPoint {
	
	private static final Map<String, ICollector> statistics = new ConcurrentHashMap<String, ICollector>();
	
	public static void registerStatisticalCollector(String name, ICollector collector) {
		statistics.put(name, collector);
	}
	
	public Deferred<HttpResponse> process(ChannelHandlerContext context, HttpRequest request) {
		StringBuilder buffer = new StringBuilder();
		for(Entry<String, ICollector> entry : statistics.entrySet()) {
			buffer.append(entry.getKey()).append("<br>").append(mapToBuffer(entry.getValue().getStatistics())).append("<br>").append("<br>");
		}
		HttpResponse response = new DefaultHttpResponse(request.getProtocolVersion(), HttpResponseStatus.OK);
		response.setContent(ResponseUtils.makePage(null, "Stats", "Stats", buffer));
		Deferred<HttpResponse> deferred = new Deferred<HttpResponse>();
		deferred.callback(response);
		return deferred;
	}
	
	private StringBuffer mapToBuffer(Map<String, Number> data) {
		StringBuffer buffer = new StringBuffer();
		for (Entry<String, Number> entry : data.entrySet()) {
			buffer.append('\t').append(entry.getKey()).append(':').append(entry.getValue()).append("<br>");
		}
		return buffer;
	}
}
