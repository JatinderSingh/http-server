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
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateHandler;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.Map;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import singh.jatinder.netty.HttpObjectAggregator;
import singh.jatinder.netty.HttpRequestDecoder;
import singh.jatinder.server.statistics.StatisticsEndPoint;

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
	private final SSLContext serverContext;

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
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(new FileInputStream(configs.get("keystore")), configs.get("password").toCharArray());
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, configs.get("password").toCharArray());
            serverContext = SSLContext.getInstance("TLS");
            serverContext.init(kmf.getKeyManagers(), null, null);
		} else {
		    serverContext = null;
		}
	}

	@Override
	public void initChannel(SocketChannel ch) throws Exception {
		final ChannelPipeline pipeline = ch.pipeline();
		pipeline.addLast("idleStateHandler", new IdleStateHandler(idleTimeoutSeconds, idleTimeoutSeconds, idleTimeoutSeconds));
		pipeline.addLast("connmgr", connmgr);
		if (null!=serverContext) {
		    SSLEngine engine = serverContext.createSSLEngine();
	        engine.setUseClientMode(false);
	        pipeline.addLast("sslHandler", new SslHandler(engine));
		}
		pipeline.addLast("decoder", new HttpRequestDecoder(maxInitialLineLength, maxHeaderSize, maxChunkSize, chunkedSupported));
		pipeline.addLast("aggregator", new HttpObjectAggregator(maxContentLength));
		pipeline.addLast("encoder", new HttpResponseEncoder());
		pipeline.addLast("handler", handler);
	}
}
