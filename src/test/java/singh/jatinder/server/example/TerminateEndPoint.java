/**
 * 
 */
package singh.jatinder.server.example;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

import com.stumbleupon.async.Deferred;

import singh.jatinder.server.ShutDownEndpoint;

/**
 * @author Jatinder
 *
 */
public class TerminateEndPoint extends ShutDownEndpoint {

	@Override
	public void shutDownGracefully() {
		// close all application resources
	}
}
