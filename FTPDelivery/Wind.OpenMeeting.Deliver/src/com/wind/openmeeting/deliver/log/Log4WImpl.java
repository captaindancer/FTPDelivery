package com.wind.openmeeting.deliver.log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

import org.apache.log4j.Logger;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.wind.eagle.log4w.ILog4W;
import com.wind.eagle.log4w.IProcessInfoListener;
import com.wind.eagle.log4w.declare.ProcessInfo;

public class Log4WImpl  implements ILog4W{
	private IProcessInfoListener perfdataListener = null;
	private Logger log = Logger.getLogger(Log4WImpl.class);

	private interface Log4WLibrary extends Library {
		Log4WLibrary instance = (Log4WLibrary) Native.loadLibrary(
				"Log4W", Log4WLibrary.class);
		int Initialize(String serviceName);
		int UnInitialize();
		int SendInfo(int messageid,byte messageType,byte[] messageHead,byte[] messageBody,String performaceData);
		int RegisterCallback(IPerformaceCallback callback);
		
	}
	private interface IPerformaceCallback extends Callback{
		public  void callback(PerformaceInfo  info);
	}
	
	private class PerformaceCallbackImpl implements IPerformaceCallback{


		public void callback(PerformaceInfo info) {
			ProcessInfo pinfo = new ProcessInfo();
			perfdataListener.fillAppPerfData(pinfo);
			info.VMUsedMemory = pinfo.getUsedVMMemory();
			info.TotalRequest=0xffffffff;
			info.TotalTraffic=0xffffffff;
			info.AvgRequest = 0xffffffff;
			info.AvgTraffic =0xffffffff;
			info.Reserve01=0xffffffff;
			info.Reserve02=0xffffffff;
			info.Reserve03=0xffffffff;
			info.Reserve04=0xffffffff;
			info.Reserve05=0xffffffff;
			info.Reserve06=0xffffffff;
			info.Reserve07=0xffffffff;
			info.Reserve08=0xffffffff;
			info.Reserve09=0xffffffff;
			info.Reserve10=0xffffffff;
			
		}
		
	}
	
	private IPerformaceCallback intf;
	@Override
	public void setListener(IProcessInfoListener listener) {
		if (null ==perfdataListener){
			perfdataListener = listener;
			intf = new PerformaceCallbackImpl();
			Log4WLibrary.instance.RegisterCallback(intf);
		}
		
	}
	
	
	
	public Log4WImpl() {
		Log4WLibrary.instance.Initialize("");
	}

	@Override
	public void disConnect() {
		Log4WLibrary.instance.UnInitialize();		
	}

	@Override
	public void sendDebug(String head, String body) {
		sendInfo(90,'D',head,body);
		
	}
	private void appendErrInfo(Exception e,StringBuffer errInfo){
		StringWriter lcStringWriter = new StringWriter();
		PrintWriter lcPrintWriter = new PrintWriter(lcStringWriter);
		try {
			e.printStackTrace(lcPrintWriter);
			lcPrintWriter.flush();
			errInfo.append(lcStringWriter.getBuffer());
		} finally {
			lcPrintWriter.close();
		}		
	}
	

	@Override
	public void sendDebug(String head, Exception e) {
		StringBuffer errInfo = new StringBuffer();
		appendErrInfo(e,errInfo);
		sendDebug(head,errInfo.toString());
		
	}

	private  static String changeCharset(String str,String oldSet,String newSet){
		try {
			byte[] tmp = str.getBytes(oldSet);
			return new String(tmp,newSet);
		} catch (UnsupportedEncodingException e) {
			return str;
		}
		
	}
	private static String toGBK(String str){
		try {
			byte[] tmp = str.getBytes("UTF-8");
			return new String(tmp,"GBK");
		} catch (UnsupportedEncodingException e) {
			return str;
		}
		
	}
	private static byte[] convert2GBKBytes(String str) throws UnsupportedEncodingException{
		byte[] tmp = str.getBytes("GBK");
		byte[] result = new byte[tmp.length+1];
		System.arraycopy(tmp, 0, result, 0, tmp.length);
		result[tmp.length]=0;
		return result;
		
	}
	@Override
	public void sendInfo(int messageid, char messageType, String msgHead, String msgBody) {
		if (messageid<=0 || messageid>=10000) return;
		try{
			if (msgHead.getBytes("UTF-8").length>490) return;
			if (msgBody.getBytes("UTF-8").length>4095) return;
		}catch(UnsupportedEncodingException e1){
			return;
		}
		try{
			Log4WLibrary.instance.SendInfo(messageid, (byte)messageType, convert2GBKBytes(msgHead),
					convert2GBKBytes(msgBody), "");
		}catch(Exception e){
			log.error("发生信息到Eagle失败",e);
		}
	}
	
	public static void main(String[] args) throws Exception{
		
		ILog4W log4w = new Log4WImpl();
		for(int i=0;i<2;i++){
		  log4w.sendInfo(500, 'I', "test测试", "this is a 测试消息");
		  Thread.sleep(1000);
		}
		log4w.disConnect();
		
		System.out.println("over");
		System.exit(0);
	}

}
