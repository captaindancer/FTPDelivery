package com.wind.openmeetings.servlet;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;

public class JettyWebApplication {

	public static void main(String[] args) throws Exception {
		Server server=new Server(8080);
		
		WebAppContext context=new WebAppContext();
		String base="/home/liufeng/jetty/WebContent";
		context.setDescriptor(base+"/WEB-INF/web.xml");
		context.setResourceBase(base);
		context.setContextPath("/");
		context.setParentLoaderPriority(true);
		
		server.setHandler(context);
		
		server.start();
		server.join();
	}

}
