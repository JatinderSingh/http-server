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
package io.maelstorm.server;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.maelstorm.netty.AppendableCharSequenceAddon;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultMessageSizeEstimator;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

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
	private static final int NUM_WORKER_THREADS=Math.max(Runtime.getRuntime().availableProcessors()-1, 2);
	
	public void start(String configFile, RequestHandler handler) throws Exception {
		LOG.info("Starting.");
		AppendableCharSequenceAddon.configure();
		try {
			System.in.close(); // Release a FD we don't need.
		} catch (Exception e) {
			LOG.warn("Failed to close stdin", e);
		}
		Properties prop = getProperties(configFile);
		requestDistributor = handler;
		requestDistributor.setInitializer(this);
		String os = System.getProperty("os.name").toLowerCase(Locale.UK).trim();
		if (os.startsWith("linux")) {
            bossGroup = (null==bossGroup) ? new EpollEventLoopGroup(1):bossGroup;
            workerGroup = (null==workerGroup) ? new EpollEventLoopGroup(Integer.parseInt((String)prop.getOrDefault("threads", NUM_WORKER_THREADS))):workerGroup;
        } else {
            bossGroup = (null==bossGroup) ? new NioEventLoopGroup(1):bossGroup;
            workerGroup = (null==workerGroup) ? new NioEventLoopGroup(Integer.parseInt((String)prop.getOrDefault("threads", NUM_WORKER_THREADS))):workerGroup;
        }
		
		String[] servers = prop.getProperty("servers").split(",");
		for (String server: servers) {
		
    		try {
    			controller = new ServerBootstrap();
    			controller.group(bossGroup, workerGroup);
    			if (os.startsWith("linux")) {
    			    controller.channel(EpollServerSocketChannel.class);
    			    controller.option(EpollChannelOption.TCP_CORK, true);
    			} else {
    			    controller.channel(NioServerSocketChannel.class);
    			}
    			controller.childHandler(new PipelineFactory(handler, getPipelineConfig(prop, server)));
    			controller.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
    			controller.option(ChannelOption.RCVBUF_ALLOCATOR, AdaptiveRecvByteBufAllocator.DEFAULT);
    			controller.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR, DefaultMessageSizeEstimator.DEFAULT);
    			controller.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, getInt(prop, server, "connectTimeoutMillis"));
    			controller.option(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(1 * 1024, 64 *1024));
    			controller.option(ChannelOption.SO_KEEPALIVE, getBoolean(prop, server, "SOKeepalive"));
    			controller.option(ChannelOption.SO_REUSEADDR, true);
    			controller.option(ChannelOption.TCP_NODELAY, true);
    			controller.option(ChannelOption.SO_LINGER, 0);
    			controller.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
    			controller.childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(1 * 1024, 64 *1024));
    			controller.childOption(ChannelOption.SO_KEEPALIVE, true);
    			controller.childOption(ChannelOption.SO_REUSEADDR, true);
    			controller.childOption(ChannelOption.TCP_NODELAY, true);
    			controller.childOption(ChannelOption.SO_LINGER, 0);
    			controller.childOption(ChannelOption.SO_RCVBUF, 6291456);
    			
    			final InetSocketAddress addr = new InetSocketAddress(getInt(prop, server, "port"));
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
	}
	
    private boolean getBoolean(Properties prop, String server, String type) throws Exception {
        String bool = prop.getProperty(server+"."+type);
        if (null!=bool && !bool.isEmpty()) {
            return Boolean.parseBoolean(bool);
        }
        else throw new Exception(type+" Keepalive not defined for server "+ server);
    }

    private int getInt(Properties prop, String server, String type) throws Exception {
        String port = prop.getProperty(server+"."+type);
        if (null!=port && !port.isEmpty()) {
            return Integer.parseInt(port);
        }
        else throw new Exception(type+" not defined for server "+ server);
    }
    
    private Map<String, String> getPipelineConfig(Properties prop, String server) throws Exception {
        Map<String, String> configs = new HashMap<String, String>();
        configs.put("connectTimeoutMillis", Integer.toString(getInt(prop, server, "connectTimeoutMillis")));
        configs.put("idleTimeoutSeconds", Integer.toString(getInt(prop, server, "idleTimeoutSeconds")));
        configs.put("maxInitialLineLength", Integer.toString(getInt(prop, server, "maxInitialLineLength")));
        configs.put("maxHeaderSize", Integer.toString(getInt(prop, server, "maxHeaderSize")));
        configs.put("maxChunkSize", Integer.toString(getInt(prop, server, "maxChunkSize")));
        configs.put("maxContentLength", Integer.toString(getInt(prop, server, "maxContentLength")));
        configs.put("chunkedSupported", Boolean.toString(getBoolean(prop, server, "chunkedSupported")));
        boolean isSSL = getBoolean(prop, server, "isSSL");
        configs.put("isSSL", Boolean.toString(isSSL));
        if (isSSL) {
            String keyCertChainFile = prop.getProperty(server+".keyCertChainFile");
            if (null!=keyCertChainFile && !keyCertChainFile.isEmpty())
                configs.put("keyCertChainFile", keyCertChainFile);
            else
                throw new Exception("keyStore not defined for server "+ server);
            String pkeyFile = prop.getProperty(server+".pkeyFile");
            if (null!=pkeyFile && !pkeyFile.isEmpty())
                configs.put("pkeyFile", pkeyFile);
            else
                throw new Exception("pkeyFile not defined for server "+ server);
        }
        return configs;
    }

    private Properties getProperties(String configFile) throws Exception {
        Properties properties = new Properties();
        InputStream is = this.getClass().getResourceAsStream((configFile==null)?"/config.properties":configFile.isEmpty()?"/config.properties":configFile);
        properties.load(is);
        return properties;
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
