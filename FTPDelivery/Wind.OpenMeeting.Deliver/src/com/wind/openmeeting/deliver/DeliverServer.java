package com.wind.openmeeting.deliver;

import org.apache.log4j.Logger;

import com.wind.eagle.log4w.ILog4W;
import com.wind.eagle.log4w.Log4W;
import com.wind.openmeeting.deliver.db.DBInit;
import com.wind.openmeeting.deliver.log.Log4WImpl;
import com.wind.openmeeting.deliver.log.PerfDataListener;
import com.windin.component.BaseService;

public class DeliverServer extends BaseService{
	private Logger log = Logger.getLogger(DeliverServer.class);
	
	private EagleParamter eagleParamter =null;
	
	


	public EagleParamter getEagleParamter() {
		return eagleParamter;
	}

	private boolean isLinux(){
		return  System.getProperty("os.name").equalsIgnoreCase("Linux");
	}
	public void setEagleParamter(EagleParamter eagleParamter) throws Exception {
		this.eagleParamter = eagleParamter;
		ILog4W log4w = null;
		if (isLinux()){
			log4w = Log4W.getInstance(eagleParamter.getServiceName(), 
					eagleParamter.getBaseSystemCode(),
					eagleParamter.getSubSystemCode(),
					eagleParamter.getServerIP(),
					eagleParamter.getServerPort(),
					eagleParamter.getUseNSCAServer());
		}else{
			log4w = new Log4WImpl();
		}
		if (log4w != null){
			log4w.setListener(new PerfDataListener());
			this.eagleParamter.setLog4w(log4w);
		}
	}
	
	@Override
	protected void doStart() throws Exception {
		// TODO Auto-generated method stub
		super.doStart();
	}

	@Override
	protected void doStop() throws Exception {
		if (null != eagleParamter){
			eagleParamter.disConnect();
		}
		super.doStop();

	}

	public void initDB(String rootPath) throws Exception{
		log.info("嵌入式数据库目录："+rootPath);
		//DBInit dbInitObj = 
		new DBInit(rootPath);
	}

}
