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

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.traffic.AbstractTrafficShapingHandler;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import io.netty.util.concurrent.GlobalEventExecutor;
import singh.jatinder.server.statistics.StatisticsEndPoint;

/**
 * @author Jatinder
 * 
 * Netty Internal pipeline setup for Http server
 */
public class PipelineFactory extends ChannelInitializer<SocketChannel> {

	private final AbstractTrafficShapingHandler trafficShapingHandler = new GlobalTrafficShapingHandler(GlobalEventExecutor.INSTANCE, 10l);
	private final ConnectionManager connmgr = new ConnectionManager(trafficShapingHandler);
	//private final IdleStateHandler idleState = new IdleStateHandler(5, 5, 10);

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

	@Override
	public void initChannel(SocketChannel ch) throws Exception {
		final ChannelPipeline pipeline = ch.pipeline();
		pipeline.addLast("traffic-handler", trafficShapingHandler);
		pipeline.addLast("idleStateHandler", new IdleStateHandler(5, 5, 10));
		pipeline.addLast("connmgr", connmgr);

		//pipeline.addLast("codec", new HttpServerCodec());
		pipeline.addLast("decoder", new HttpRequestDecoder());
		pipeline.addLast("aggregator", new HttpObjectAggregator(10));
		pipeline.addLast("encoder", new HttpResponseEncoder());
		
		pipeline.addLast("handler", handler);
	}
}
