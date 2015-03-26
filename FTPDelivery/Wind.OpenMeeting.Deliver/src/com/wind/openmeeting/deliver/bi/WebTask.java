package com.wind.openmeeting.deliver.bi;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;

public class WebTask extends BaseService{

	private static final Logger LOGGER = Logger.getLogger(WebTask.class);
	
	private String baseDirectory;

	private Server server;
	public String getBaseDirectory() {
		return baseDirectory;
	}

	public void setBaseDirectory(String baseDirectory) {
		this.baseDirectory = baseDirectory;
	}
	
	private int port;
	
	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	private static String fileSeparator;
	
	static{
		fileSeparator=System.getProperties().getProperty("file.separator");
	}
	
	private void initContainer() throws Exception{
		server=new Server(port);
		
		WebAppContext context=new WebAppContext();
//		String base="/home/liufeng/jetty/WebContent";
		if(baseDirectory.endsWith(fileSeparator)){
			context.setDescriptor(baseDirectory+"WEB-INF"+fileSeparator+"web.xml");
		}else{
			context.setDescriptor(baseDirectory+fileSeparator+"WEB-INF"+fileSeparator+"web.xml");
		}
		context.setResourceBase(baseDirectory);
		context.setContextPath("/");
		context.setParentLoaderPriority(true);
		
		server.setHandler(context);
		
		server.start();		
	}

	@Override
	protected void doStart() throws Exception {
		super.doStart();
		initContainer();
	}

	@Override
	protected void doStop() throws Exception {
		super.doStop();
	}
	
}
