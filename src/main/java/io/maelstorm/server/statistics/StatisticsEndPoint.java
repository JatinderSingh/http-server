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
package io.maelstorm.server.statistics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.stumbleupon.async.Deferred;

import io.maelstorm.netty.HttpObjectAggregator.AggregatedFullHttpRequest;
import io.maelstorm.server.IEndPoint;
import io.maelstorm.server.ResponseUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.internal.AppendableCharSequence;

/**
 * @author Jatinder
 *
 * Statistical data display endpoint
 * All collectors should register here.
 */
public class StatisticsEndPoint implements IEndPoint {
	
	private static final Map<String, ICollector> statistics = Collections.synchronizedMap(new LinkedHashMap<String, ICollector>());
	private static final char splitter = '/';
	private static final FullHttpResponse NotFound =  new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
	
	static {
		NotFound.headers().add(HttpHeaderNames.CONTENT_LENGTH, 0);
		NotFound.headers().add(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
	}
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
    		ByteBuf content = ResponseUtils.makePage(null, "Stats", "Stats", buffer);
    		response = new DefaultFullHttpResponse(request.getProtocolVersion(), HttpResponseStatus.OK, content);
    		response.headers().add(HttpHeaderNames.CONTENT_TYPE, "text/html");
     		response.headers().add(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
     		response.headers().add(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
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
	                if (null != statistics.get(key1)) {
	                	buffer.append(key1).append("<br>").append("<table>").append(mapToBuffer(statistics.get(key1).getStatistics(), true)).append("</table>").append("<br>").append("<br>");
	                }
	                ByteBuf content = ResponseUtils.makePage(null, "Stats", "Stats", buffer);
	                response = new DefaultFullHttpResponse(request.getProtocolVersion(), HttpResponseStatus.OK, content);
	                response.headers().add(HttpHeaderNames.CONTENT_TYPE, "text/html");
	         		response.headers().add(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
	         		response.headers().add(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
	                break;
	            case 2:
	                key1 = uri.substring(splitterLocations.get(0)+1, splitterLocations.get(1));
	                if (null != statistics.get(key1)) {
		                String key2 = uri.substring(splitterLocations.get(1)+1, uri.length());
		                buffer.append(statistics.get(key1).getStatistics().get(key2));
	                }
	                content = ResponseUtils.getChannelBuffer(buffer);
	                response = new DefaultFullHttpResponse(request.getProtocolVersion(), HttpResponseStatus.OK, content);
	                response.headers().add(HttpHeaderNames.CONTENT_TYPE, "text/html");
	         		response.headers().add(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
	         		response.headers().add(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
	                break;
	            default:
	            	NotFound.retain();
	                response = NotFound;
	        }
	    }
	    deferred.callback(response);
        return deferred;
	}
	
	private StringBuffer mapToBuffer(Map<String, Number> data, boolean isDisplayed) {
		StringBuffer buffer = new StringBuffer(data.size()*30);
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
