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
	
	public void start(int port, RequestHandler handler, boolean enableKeepAlive) {
		LOG.info("Starting.");
		try {
			System.in.close(); // Release a FD we don't need.
		} catch (Exception e) {
			LOG.warn("Failed to close stdin", e);
		}
		
		final NioServerSocketChannelFactory factory = new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool(), 100);
		try {
			controller = new ServerBootstrap(factory);
			controller.setPipelineFactory(new PipelineFactory(handler));
			controller.setOption("child.tcpNoDelay", true);
			controller.setOption("child.keepAlive", enableKeepAlive);
			controller.setOption("reuseAddress", true);
			final InetSocketAddress addr = new InetSocketAddress(port);
			controller.bind(addr);
			LOG.info("Server Ready to serve on " + addr);
		} catch (Throwable t) {
			factory.releaseExternalResources();
			throw new RuntimeException("Initialization failed", t);
		}
	}
	
	public void stop() {
		LOG.info("Stop Requested");
		ConnectionManager.closeAllConnections();
		if (null!=controller) {
			controller.releaseExternalResources();
		}
	}
}
