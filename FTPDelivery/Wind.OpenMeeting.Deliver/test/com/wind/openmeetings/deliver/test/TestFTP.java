package com.wind.openmeetings.deliver.test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;

import org.apache.commons.net.ftp.FTPClient;
import org.junit.Test;

import com.wind.openmeeting.deliver.utils.FTPUtils;

public class TestFTP {

	private String hostname="10.100.1.204";
	private String username="x2hadoop";
	private String password="Wind2013";
	
	private boolean uploadFile(InputStream inputStream,String hostname,String username,String password,String directory,String filename) throws SocketException, IOException{
		FTPClient ftpClient=new FTPClient();
		ftpClient.connect(hostname);
		ftpClient.login(username, password);
		ftpClient.changeWorkingDirectory(directory);
		ftpClient.setBufferSize(1024);
		ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
		ftpClient.storeFile(filename, inputStream);
		inputStream.close();
		ftpClient.disconnect();
		return true;
	}
	
	@Test
	public void test() throws SocketException, IOException {
		String pathname="/home/liufeng/windcollege/zeus.flv";
		File uploadFile=new File(pathname);
		long fileSize=uploadFile.length()/1024;
//		System.out.println(fileSize/1024);
		long star_time=System.currentTimeMillis();
		
//		FileInputStream inputStream=new FileInputStream(uploadFile);
		String directory="liufeng/ftp";
		String filename=pathname.substring(pathname.lastIndexOf("/")+1);
//		uploadFile(inputStream, hostname, username, password, directory, filename)
		if(FTPUtils.uploadFile(pathname, hostname, username, password, directory,"c387cafd-26eb-4511-9324-17e518f301b5.wmv")!=0){
			System.out.println("文件传输成功!");
			long end_time=System.currentTimeMillis();
			long lasts=(end_time-star_time)/1000;
			System.out.println("传输时间:"+lasts+"s");
			System.out.println("文件大小:"+fileSize+"KB");
			if(lasts!=0){
				System.out.println("传输速率:"+fileSize/lasts+"KBps");
			}else{
				System.out.println("传输速率:"+fileSize+"KBps");
			}
		}
	}

}
