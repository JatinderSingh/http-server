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
package io.maelstorm.server.example;

import io.maelstorm.server.RequestDistributor;
import io.maelstorm.server.ServerInit;

/**
 * @author Jatinder
 *
 * BootStrap class for Hello World example...
 *
 */
public class HelloServer {
	
	public static void main(String[] args) throws Exception {
		RequestDistributor distr = new RequestDistributor();
		distr.addEndPoint("hello", new HelloEndpoint());
		distr.addEndPoint("diediedie", new TerminateEndPoint());
		new ServerInit().start(null, distr);
	}
}
