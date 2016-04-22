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

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateHandler;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.Map;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLEngine;

import io.maelstorm.netty.HttpObjectAggregator;
import io.maelstorm.netty.HttpRequestDecoder;
import io.maelstorm.server.statistics.StatisticsEndPoint;

/**
 * @author Jatinder
 * 
 * Netty Internal pipeline setup for Http server
 */
public class PipelineFactory extends ChannelInitializer<SocketChannel> {

	private static final ConnectionManager connmgr = new ConnectionManager();

	/** Stateless handler for RPCs. */
	private final RequestHandler handler;
	private final int idleTimeoutSeconds, maxInitialLineLength, maxHeaderSize, maxChunkSize, maxContentLength; 
	private final boolean chunkedSupported;
	private final SslContext sslContext;

	/**
	 * Constructor.
	 * @throws Exception 
	 */
	public PipelineFactory(RequestHandler handler, Map<String, String> configs) throws Exception {
		this.handler = handler;
		StatisticsEndPoint.registerStatisticalCollector("ConnectionManager", connmgr);
		StatisticsEndPoint.registerStatisticalCollector("RequestHandler", handler);
		idleTimeoutSeconds = Integer.parseInt(configs.get("idleTimeoutSeconds"));
		maxInitialLineLength = Integer.parseInt(configs.get("maxInitialLineLength"));
		maxHeaderSize = Integer.parseInt(configs.get("maxHeaderSize"));
		maxChunkSize = Integer.parseInt(configs.get("maxChunkSize"));
		maxContentLength = Integer.parseInt(configs.get("maxContentLength"));
		chunkedSupported = Boolean.parseBoolean(configs.get("chunkedSupported"));
		boolean sslEnabled = Boolean.parseBoolean(configs.get("isSSL"));
		if (sslEnabled) {
            sslContext = SslContextBuilder.forServer(new File(configs.get("keyCertChainFile")), new File(configs.get("pkeyFile"))).build();
		} else {
			sslContext = null;
		}
	}

	@Override
	public void initChannel(SocketChannel ch) throws Exception {
		final ChannelPipeline pipeline = ch.pipeline();
		pipeline.addLast("idleStateHandler", new IdleStateHandler(idleTimeoutSeconds, idleTimeoutSeconds, idleTimeoutSeconds));
		pipeline.addLast("connmgr", connmgr);
		if (null!=sslContext) {
	        pipeline.addLast("sslHandler", sslContext.newHandler(ch.alloc()));
		}
		pipeline.addLast("decoder", new HttpRequestDecoder(maxInitialLineLength, maxHeaderSize, maxChunkSize, chunkedSupported));
		pipeline.addLast("aggregator", new HttpObjectAggregator(maxContentLength));
		pipeline.addLast("encoder", new HttpResponseEncoder());
		pipeline.addLast("handler", handler);
	}
}
