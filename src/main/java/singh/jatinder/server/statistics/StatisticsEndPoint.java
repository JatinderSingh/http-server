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

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.internal.AppendableCharSequence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import singh.jatinder.netty.HttpObjectAggregator.AggregatedFullHttpRequest;
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
	
	private static final Map<String, ICollector> statistics = Collections.synchronizedMap(new LinkedHashMap<String, ICollector>());
	private static final char splitter = '/';
	
	public static void registerStatisticalCollector(String name, ICollector collector) {
		statistics.put(name, collector);
	}
	
	public Deferred<FullHttpResponse> process(ChannelHandlerContext context, AggregatedFullHttpRequest request) {
	    AppendableCharSequence uri = request.geturi();
	    StringBuilder buffer = new StringBuilder();
	    Deferred<FullHttpResponse> deferred = new Deferred<FullHttpResponse>();
	    FullHttpResponse response;
	    if (uri.length() == 6) {// uri = "/stats"
    		for(Entry<String, ICollector> entry : statistics.entrySet()) {
    		    buffer.append(entry.getKey()).append("<br>").append("<table>").append(mapToBuffer(entry.getValue().getStatistics(), entry.getValue().isDisplayed())).append("</table>").append("<br>").append("<br>");
    		}
    		response = new DefaultFullHttpResponse(request.getProtocolVersion(), HttpResponseStatus.OK, ResponseUtils.makePage(null, "Stats", "Stats", buffer));
	    } else {
	        ArrayList<Integer> splitterLocations = new ArrayList<Integer>(3);
	        for (int i=6; i<uri.length(); i++) {
	            if (splitter == uri.charAt(i)) {
	                splitterLocations.add(i);
	            }
	        }
	        String key1;
	        switch (splitterLocations.size()) {
	            case 1:
	                key1 = uri.substring(splitterLocations.get(0)+1, uri.length()); 
	                buffer.append(key1).append("<br>").append("<table>").append(mapToBuffer(statistics.get(key1).getStatistics(), true)).append("</table>").append("<br>").append("<br>");
	                response = new DefaultFullHttpResponse(request.getProtocolVersion(), HttpResponseStatus.OK, ResponseUtils.makePage(null, "Stats", "Stats", buffer));
	                break;
	            case 2:
	                key1 = uri.substring(splitterLocations.get(0)+1, splitterLocations.get(1)); 
	                String key2 = uri.substring(splitterLocations.get(1)+1, uri.length());
	                buffer.append(statistics.get(key1).getStatistics().get(key2));
	                response = new DefaultFullHttpResponse(request.getProtocolVersion(), HttpResponseStatus.OK, ResponseUtils.getChannelBuffer(buffer));
	                break;
	            default:
	                response = new DefaultFullHttpResponse(request.getProtocolVersion(), HttpResponseStatus.NOT_FOUND);
	        }
	    }
	    deferred.callback(response);
        return deferred;
	}
	
	private StringBuffer mapToBuffer(Map<String, Number> data, boolean isDisplayed) {
		StringBuffer buffer = new StringBuffer();
		if (null!=data)
    		for (Entry<String, Number> entry : data.entrySet()) {
    		    if (isDisplayed) {
        		    buffer.append("<tr>");
        		    buffer.append("<td>").append(entry.getKey().toString()).append(':').append(entry.getValue()).append("</td>");
    		    }
    		}
		return buffer;
	}
}
