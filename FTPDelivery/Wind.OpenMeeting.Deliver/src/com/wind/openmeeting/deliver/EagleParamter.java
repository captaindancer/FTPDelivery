package com.wind.openmeeting.deliver;

import java.io.UnsupportedEncodingException;

import org.apache.log4j.Logger;

import com.wind.eagle.log4w.ILog4W;

public class EagleParamter {
	private String serviceName;
	private String baseSystemCode;
	private String subSystemCode;
	private String serverIP;
	private int    serverPort;
	private int    useNSCAServer;
	
	private Logger log = Logger.getLogger(EagleParamter.class);
	
	private ILog4W log4w = null;
	
	public String getServiceName() {
		return serviceName;
	}
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}
	public String getBaseSystemCode() {
		return baseSystemCode;
	}
	public void setBaseSystemCode(String baseSystemCode) {
		this.baseSystemCode = baseSystemCode;
	}
	public String getSubSystemCode() {
		return subSystemCode;
	}
	public void setSubSystemCode(String subSystemCode) {
		this.subSystemCode = subSystemCode;
	}
	public String getServerIP() {
		return serverIP;
	}
	public void setServerIP(String serverIP) {
		this.serverIP = serverIP;
	}
	public int getServerPort() {
		return serverPort;
	}
	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}
	public int getUseNSCAServer() {
		return useNSCAServer;
	}
	public void setUseNSCAServer(int useNSCAServer) {
		this.useNSCAServer = useNSCAServer;
	}
	/*
	public ILog4W getLog4w() {
		return log4w;
	}
	*/
	public void setLog4w(ILog4W log4w) {
		this.log4w = log4w;
	}

	private final int MAXLEN = 1300;
	/**
	 * 限定字符串的长度，防止数据过大被拒绝发送到Eagle
	 * @param info
	 * @return
	 */
	private String fixDetailInfo(String info){
		int realLen = info.length();
		if (realLen==0) return info;
		realLen = realLen>MAXLEN?MAXLEN:realLen;
		try {
			if (info.getBytes("UTF8").length<4095) return info;
		} catch (UnsupportedEncodingException e) {
			return  info.substring(0, realLen);
		}
		return  info.substring(0, realLen);
	}
	
	public void sendInfo(int messageid, char messageType, String msgHead, String msgBody){
		try{
			if (null !=log4w){			
				log4w.sendInfo(messageid, messageType, msgHead, fixDetailInfo(msgBody));
			}
		}catch(Exception e){
			log.error("发生信息到Eagle失败",e);
		}
	}
	public void disConnect() {
		if (null !=log4w){
			log4w.disConnect();
		}
		
	}
	
}
