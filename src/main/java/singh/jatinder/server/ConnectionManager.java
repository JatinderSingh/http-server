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

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import singh.jatinder.server.statistics.ICollector;

/**
 * @author Jatinder
 * 
 *         Connection manager aggregating all connection related data
 * 
 */
@Sharable
public class ConnectionManager extends ChannelInboundHandlerAdapter implements ICollector {
	private static final Logger LOG = LoggerFactory.getLogger(ConnectionManager.class);

	private static final AtomicLong totalConnections = new AtomicLong();
	private static final AtomicLong registeredChannels = new AtomicLong();
	private static final AtomicLong exceptionsCount = new AtomicLong();
	private static final AtomicLong totalRequests = new AtomicLong();
	private static final AtomicLong readCompleteEvents = new AtomicLong();
	private static final AtomicLong activeChannels = new AtomicLong();
	private static final AtomicLong idleEventConnectionClose = new AtomicLong();

	@Override
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
		totalConnections.incrementAndGet();
		registeredChannels.incrementAndGet();
		// channels.add(ctx.channel());
		super.channelRegistered(ctx);
	}

	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
		registeredChannels.decrementAndGet();
		// channels.remove(ctx.channel());
		super.channelUnregistered(ctx);
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		activeChannels.incrementAndGet();
		super.channelActive(ctx);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		activeChannels.decrementAndGet();
		super.channelInactive(ctx);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		totalRequests.incrementAndGet();
		super.channelRead(ctx, msg);
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		readCompleteEvents.incrementAndGet();
		super.channelReadComplete(ctx);
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof IdleStateEvent) {
			ctx.close();
			idleEventConnectionClose.incrementAndGet();
		}
		super.userEventTriggered(ctx, evt);
	}

	@Override
	public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
		super.channelWritabilityChanged(ctx);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		if (cause instanceof ClosedChannelException) {
			LOG.warn("Attempt to write to closed channel {}", ctx.channel());
		} else if (cause instanceof IOException) {
			/**
			 * Only possible way in java until Some other way is exposed by jvm
			 */
			registeredChannels.decrementAndGet();
			ctx.channel().close();
		} else {
			LOG.error("Unexpected exception from downstream for {} ", ctx.channel(), cause);
			registeredChannels.decrementAndGet();
			ctx.channel().close();
		}
		exceptionsCount.incrementAndGet();
	}

	/*
	 * @Override public void handleUpstream(final ChannelHandlerContext ctx,
	 * final ChannelEvent e) throws Exception { if (e instanceof
	 * ChannelStateEvent) { LOG.debug(e.toString()); } if (e instanceof
	 * IdleStateEvent) { LOG.debug(e.toString());
	 * e.getFuture().addListener(ChannelFutureListener.CLOSE);
	 * idleEventConnectionClose.incrementAndGet(); } super.handleUpstream(ctx,
	 * e); }
	 */

	public Map<String, Number> getStatistics() {
		Map<String, Number> stats = new HashMap<String, Number>();
		stats.put("idleEventConnectionClose", idleEventConnectionClose.get());
		stats.put("totalConnections", totalConnections.get());
		stats.put("activeConnections", registeredChannels.get());
		stats.put("exceptionsCount", exceptionsCount.get());
		stats.put("totalRequests", totalRequests.get());
		stats.put("activeChannels", activeChannels.get());
		stats.put("readCompleteEvents", readCompleteEvents.get());
		return stats;
	}

    public boolean isDisplayed() {
        return true;
    }

}
