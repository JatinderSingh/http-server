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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;

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
		requestDistributor.setInitializer(this);
		EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        
		try {
			controller = new ServerBootstrap();
			controller.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).childHandler(new PipelineFactory(handler));
			controller.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
			controller.option(ChannelOption.RCVBUF_ALLOCATOR, AdaptiveRecvByteBufAllocator.DEFAULT);
			controller.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000);
			controller.option(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 64 * 1024);
			controller.option(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 1 * 1024);
			controller.option(ChannelOption.SO_KEEPALIVE, true);
			controller.option(ChannelOption.SO_REUSEADDR, true);
			controller.option(ChannelOption.TCP_NODELAY, true);
			controller.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
			controller.childOption(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 64 * 1024);
			controller.childOption(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 10);
			controller.childOption(ChannelOption.SO_KEEPALIVE, true);
			controller.childOption(ChannelOption.SO_REUSEADDR, true);
			controller.childOption(ChannelOption.TCP_NODELAY, true);
			controller.childOption(ChannelOption.SO_KEEPALIVE, true);
			controller.childOption(ChannelOption.SO_REUSEADDR, true);
			controller.childOption(ChannelOption.TCP_NODELAY, true);
			
			final InetSocketAddress addr = new InetSocketAddress(port);
			controller.bind(addr).sync();
			LOG.info("Server Ready to serve on " + addr);
		} catch (Throwable t) {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
			throw new RuntimeException("Initialization failed", t);
		}
	}
	
	public RequestDistributor getRequestDistributor() {
		return (RequestDistributor)requestDistributor;
	}
	
	public void shutDownGracefully() {
		LOG.info("Stop Requested");
		ConnectionManager.closeAllConnections();
		if (null!=controller) {
			controller.group().shutdownGracefully().awaitUninterruptibly();
			controller.childGroup().shutdownGracefully().awaitUninterruptibly();
		}
	}
}
