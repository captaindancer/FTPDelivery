package com.wind.openmeetings.deliver.test;

import java.io.IOException;
import java.net.SocketException;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.junit.Test;

public class TestFTPDirectory {

	private String hostname="10.100.1.204:20";
	private String username="x2hadoop";
	private String password="Wind2013";
	
	@Test
	public void test() throws SocketException, IOException {
		FTPClient ftpClient=new FTPClient();
		ftpClient.connect(hostname);
		ftpClient.login(username, password);
		System.out.println(ftpClient.listFiles().length);
		for(FTPFile file:ftpClient.listFiles()){
			System.out.println(file.getName());
		}
		System.out.println("****************");
		ftpClient.changeWorkingDirectory("");
		System.out.println(ftpClient.listFiles().length);
		for(FTPFile file:ftpClient.listFiles()){
			System.out.println(file.getName());
		}
		ftpClient.disconnect();
	}

}
