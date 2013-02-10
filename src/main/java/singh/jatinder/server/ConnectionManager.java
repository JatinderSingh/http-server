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

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.WriteCompletionEvent;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import singh.jatinder.server.statistics.ICollector;

/**
 * @author Jatinder
 * 
 * Connection manager aggregating all connection related data
 *
 */
public class ConnectionManager extends SimpleChannelHandler implements ICollector {
	private static final Logger LOG = LoggerFactory.getLogger(ConnectionManager.class);

	  private static final AtomicLong totalConnections = new AtomicLong();
	  private static final AtomicLong activeConnections = new AtomicLong();
	  private static final AtomicLong exceptionsCount = new AtomicLong();
	  private static final AtomicLong totalRequests = new AtomicLong();
	  private static final AtomicLong totalResponses = new AtomicLong();
	  private static final AtomicLong activeBoundedChannels = new AtomicLong();
	  private static final AtomicLong activeChannels = new AtomicLong();

	  private static final DefaultChannelGroup channels =
	    new DefaultChannelGroup("all");

	  static void closeAllConnections() {
	    channels.close().awaitUninterruptibly();
	  }

	  /** Constructor. */
	  public ConnectionManager() {
	  }

	  @Override
	  public void channelOpen(final ChannelHandlerContext ctx,
	                          final ChannelStateEvent e) {
	    channels.add(e.getChannel());
	    totalConnections.incrementAndGet();
	    activeConnections.incrementAndGet();
	  }

	  @Override
	  public void handleUpstream(final ChannelHandlerContext ctx,
	                             final ChannelEvent e) throws Exception {
	    if (e instanceof ChannelStateEvent) {
	      LOG.debug(e.toString());
	    }
	    super.handleUpstream(ctx, e);
	  }

	  @Override
	  public void exceptionCaught(final ChannelHandlerContext ctx,
	                              final ExceptionEvent e) {
	    final Throwable cause = e.getCause();
	    final Channel chan = ctx.getChannel();
	    if (cause instanceof ClosedChannelException) {
	      LOG.warn("Attempt to write to closed channel " + chan);
	    } else if (cause instanceof IOException
	               && "Connection reset by peer".equals(cause.getMessage())) {
	    	/**
	    	 * Only possible way in java until Some other way is exposed by jvm 
	    	 */
	    	activeConnections.decrementAndGet();
	    } else {
	      LOG.error("Unexpected exception from downstream for " + chan, cause);
	      e.getChannel().close();
	    }
	    exceptionsCount.incrementAndGet();
	  }
	  
	  @Override
	  public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		  activeConnections.decrementAndGet();
		  super.channelClosed(ctx, e);
	   }

	public Map<String, Number> getStatistics() {
		Map<String, Number> stats = new HashMap<String, Number>();
		stats.put("totalConnections", totalConnections.get());
		stats.put("activeConnections", activeConnections.get());
		stats.put("exceptionsCount", exceptionsCount.get());
		stats.put("totalRequests", totalRequests.get());
		stats.put("totalResponses", totalResponses.get());
		stats.put("activeBoundedChannels", activeBoundedChannels.get());
		stats.put("activeChannels", activeChannels.get());
		stats.put("underProcessRequests", totalRequests.get()-totalResponses.get());
		return stats;
	}

    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
    	totalRequests.incrementAndGet();
    	super.messageReceived(ctx, e);
    }

    public void channelBound(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
    	activeBoundedChannels.incrementAndGet();
    	super.channelBound(ctx, e);
    }

    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
    	activeChannels.incrementAndGet();
        super.channelConnected(ctx, e);
    }

    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
    	activeChannels.decrementAndGet();
        super.channelDisconnected(ctx, e);
    }

    public void channelUnbound(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
    	activeBoundedChannels.decrementAndGet();
        super.channelUnbound(ctx, e);
    }

    public void writeComplete(ChannelHandlerContext ctx, WriteCompletionEvent e) throws Exception {
    	totalResponses.incrementAndGet();
        super.writeComplete(ctx, e);
    }

    public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e)
            throws Exception {
    	if (e instanceof ChannelStateEvent) {
  	      LOG.debug(e.toString());
  	    }
  	    super.handleDownstream(ctx, e);
    }
}
