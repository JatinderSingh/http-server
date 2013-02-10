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
package singh.jatinder.server.example;

import singh.jatinder.server.RequestDistributor;
import singh.jatinder.server.ServerInit;

/**
 * @author Jatinder
 *
 * BootStrap class for Hello World example...
 *
 */
public class HelloServer {
	
	public static void main(String[] args) {
		RequestDistributor distr = new RequestDistributor();
		distr.addEndPoint("hello", new HelloEndpoint());
		new ServerInit().start(9000, distr, true);
	}
}
