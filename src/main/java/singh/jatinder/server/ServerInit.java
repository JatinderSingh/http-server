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
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;
import java.util.Locale;

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
	private EventLoopGroup bossGroup;
	private EventLoopGroup workerGroup;
	
	public void start(int port, RequestHandler handler, boolean enableKeepAlive) {
		LOG.info("Starting.");
		try {
			System.in.close(); // Release a FD we don't need.
		} catch (Exception e) {
			LOG.warn("Failed to close stdin", e);
		}
		
		requestDistributor = handler;
		requestDistributor.setInitializer(this);
		String os = System.getProperty("os.name").toLowerCase(Locale.UK).trim();
		if (os.startsWith("linux")) {
            bossGroup = (null==bossGroup) ? new EpollEventLoopGroup():bossGroup;
            workerGroup = (null==workerGroup) ? new EpollEventLoopGroup():workerGroup;
        } else {
            bossGroup = (null==bossGroup) ? new NioEventLoopGroup():bossGroup;
            workerGroup = (null==workerGroup) ? new NioEventLoopGroup():workerGroup;
        }
		        
		try {
			controller = new ServerBootstrap();
			controller.group(bossGroup, workerGroup);
			if (os.startsWith("linux")) {
			    controller.channel(EpollServerSocketChannel.class);
			    controller.option(EpollChannelOption.TCP_CORK, true);
			} else {
			    controller.channel(NioServerSocketChannel.class);
			}
			controller.childHandler(new PipelineFactory(handler));
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
			controller.childOption(ChannelOption.SO_BACKLOG, 10);
			
			final InetSocketAddress addr = new InetSocketAddress(port);
			ChannelFuture future = controller.bind(addr).sync();
			if (future.isSuccess())
				LOG.info("Server Ready to serve on " + addr);
			else 
				throw new Exception("Address already in use");
		} catch (Throwable t) {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
			throw new RuntimeException("Initialization failed", t);
		}
	}
	
	protected EventLoopGroup getBossGroup() {
        return bossGroup;
    }
	
	protected EventLoopGroup getWorkerGroup() {
        return workerGroup;
    }
	
	public RequestDistributor getRequestDistributor() {
		return (RequestDistributor)requestDistributor;
	}
	
	public void shutDownGracefully() {
		LOG.info("Stop Requested");
		if (null!=controller) {
			controller.group().shutdownGracefully().awaitUninterruptibly();
			controller.childGroup().shutdownGracefully().awaitUninterruptibly();
		}
	}
}
