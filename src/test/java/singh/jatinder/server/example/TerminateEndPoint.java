/**
 * 
 */
package singh.jatinder.server.example;

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
