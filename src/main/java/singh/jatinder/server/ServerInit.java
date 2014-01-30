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

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.AdaptiveReceiveBufferSizePredictorFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jatinder
 *
 * Bootstrap class for server
 *
 */
public class ServerInit {
	private static final Logger LOG = LoggerFactory.getLogger(ServerInit.class);
	private ServerBootstrap controller;
	private volatile RequestHandler requestDistributor;
	
	public void start(int port, RequestHandler handler, boolean enableKeepAlive) {
		LOG.info("Starting.");
		try {
			System.in.close(); // Release a FD we don't need.
		} catch (Exception e) {
			LOG.warn("Failed to close stdin", e);
		}
		
		requestDistributor = handler;
		
		final NioServerSocketChannelFactory factory = new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
		try {
			controller = new ServerBootstrap(factory);
			controller.setPipelineFactory(new PipelineFactory(handler));
			controller.setOption("child.tcpNoDelay", true);
			controller.setOption("child.keepAlive", enableKeepAlive);
			controller.setOption("reuseAddress", true);
			// better to have an receive buffer predictor 
			controller.setOption("receiveBufferSizePredictorFactory", new AdaptiveReceiveBufferSizePredictorFactory());  

			//if the server is sending 1000 messages per sec, optimum write buffer water marks will
			//prevent unnecessary throttling, Check NioSocketChannelConfig doc   
			controller.setOption("writeBufferLowWaterMark", 1 * 1024);
			controller.setOption("writeBufferHighWaterMark", 64 * 1024);
			final InetSocketAddress addr = new InetSocketAddress(port);
			controller.bind(addr);
			LOG.info("Server Ready to serve on " + addr);
		} catch (Throwable t) {
			factory.releaseExternalResources();
			throw new RuntimeException("Initialization failed", t);
		}
	}
	
	public RequestDistributor getRequestDistributor() {
		return (RequestDistributor)requestDistributor;
	}
	
	public void stop() {
		LOG.info("Stop Requested");
		ConnectionManager.closeAllConnections();
		if (null!=controller) {
			controller.releaseExternalResources();
		}
	}
}
