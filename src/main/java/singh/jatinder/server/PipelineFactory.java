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

import static org.jboss.netty.channel.Channels.pipeline;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;

import singh.jatinder.server.statistics.StatisticsEndPoint;

/**
 * @author Jatinder
 * 
 * Netty Internal pipeline setup for Http server
 */
public class PipelineFactory implements ChannelPipelineFactory {

	private final ConnectionManager connmgr = new ConnectionManager();

	/** Stateless handler for RPCs. */
	private final RequestHandler handler;

	/**
	 * Constructor.
	 */
	public PipelineFactory(RequestHandler handler) {
		this.handler = handler;
		StatisticsEndPoint.registerStatisticalCollector("ConnectionManager", connmgr);
		StatisticsEndPoint.registerStatisticalCollector("RequestHandler", handler);
	}

	//@Override
	public ChannelPipeline getPipeline() throws Exception {
		final ChannelPipeline pipeline = pipeline();
		pipeline.addLast("connmgr", connmgr);
		pipeline.addLast("decoder", new HttpRequestDecoder());
		pipeline.addLast("encoder", new HttpResponseEncoder());
		pipeline.addLast("aggregator", new HttpChunkAggregator(10000));
		pipeline.addLast("handler", handler);
		return pipeline;
	}
}
