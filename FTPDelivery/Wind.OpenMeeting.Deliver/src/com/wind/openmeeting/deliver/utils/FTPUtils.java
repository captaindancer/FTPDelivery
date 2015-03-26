package com.wind.openmeeting.deliver.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.log4j.Logger;

import com.wind.openmeeting.deliver.beans.FTPAccount;

public class FTPUtils {
	
	private static Logger log = Logger.getLogger(FTPUtils.class);
	
	/**
	 * 上传本地文件到远程FTP服务器中
	 * @param localPath 本地文件的绝对路径
	 * @param hostname ftp服务器的地址,如果不是默认端口21,需要加上端口号
	 * @param username ftp登录用户名
	 * @param password ftp登录密码
	 * @param directory 上传的ftp服务器目录
	 * @param filename 上传到ftp服务器的文件名
	 * @return
	 * 0 表示上传失败
	 * 1 表示直接上传成功
	 * 2 表示断点续传成功
	 * 3 表示文件已经存在
	 * 4 ftp服务器无法连接
	 * 5 目录没有权限
	 * @throws SocketException
	 * @throws IOException
	 */
	public static int uploadFile(String localPath,String hostname,String username,String password,String directory,String filename) throws SocketException, IOException{
		int returnFlag=0;
		FTPClient ftpClient=null;
		ftpClient=new FTPClient();
		try {
			ftpClient.connect(hostname);
			ftpClient.login(username, password);
//		System.out.println(ftpClient.getReplyString());
			int reply=ftpClient.getReplyCode();
			if(!FTPReply.isPositiveCompletion(reply)){
				ftpClient.disconnect();
				log.error("["+hostname+"]ftp服务器无法连接!");
				return 4;
			}else{
				log.info("["+hostname+"] "+ftpClient.getReplyString());
			}
			if(!changeDirectory(ftpClient, directory,hostname)){
				log.error("["+hostname+"]目录没有权限!");
				ftpClient.disconnect();
				return 5;
			}
			returnFlag=offsetRestart(localPath, ftpClient, directory, filename,hostname);
		}catch(Exception e){
			return 4;
		}finally{
			/*ftpClient.setBufferSize(1024);
			ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
			ftpClient.storeFile(filename, inputStream);*/
			ftpClient.disconnect();
		}
		return returnFlag;
	}
	
	/**
	 * 上传本地文件到远程FTP服务器中
	 * @param localPath 本地文件的绝对路径
	 * @param ftpAccount  远程服务器的账户信息
	 * HostName如果不是默认的21端口,需要加上端口号
	 * Directory目录为null或者""时,changeWorkingDirectory方法保持当前的目录不变
	 * @return
	 * @throws SocketException
	 * @throws IOException
	 */
	public static int uploadFile(String localPath,FTPAccount ftpAccount,String filename) throws SocketException, IOException{
		return uploadFile(localPath, ftpAccount.getHostname(), ftpAccount.getUsername(), ftpAccount.getPassword(), ftpAccount.getDirectory(),filename);
	}
	
	private static boolean changeDirectory(FTPClient ftpClient,String directory,String hostname) throws IOException{
		if(directory!=null && !ftpClient.changeWorkingDirectory(directory)){
			int start=0;
			int end=0;
			if(directory.startsWith("/")){
				start=1;
			}
			end=directory.indexOf("/", start);
			while(true){
				String subDirectory=directory.substring(start, end);
				if(!ftpClient.changeWorkingDirectory(subDirectory)){
					if(ftpClient.makeDirectory(subDirectory)){
						ftpClient.changeWorkingDirectory(subDirectory);
					}else{
						log.error("["+hostname+"]创建目录失败");
						return false;
					}
				}
				start=end+1;
				end=directory.indexOf("/",start);
				
				if(end<=start){
					break;
				}
			}
		}
		return true;
	}
	/**
	 * 文件通过ftp上传到服务器
	 * @param localPath 本地文件的地址
	 * @param ftpClient ftp上传服务的ftpClient
	 * @param directory 上传的ftp服务器目录
	 * @param filename 上传到ftp服务器的文件名
	 * @param hostname ftp服务器的地址,如果不是默认端口21,需要加上端口号
	 * @return
	 * 0 表示上传失败
	 * 1 表示直接上传成功
	 * 2 表示断点续传成功
	 * 3 表示文件已经存在
	 * @throws IOException
	 */
	private static int offsetRestart(String localPath,FTPClient ftpClient,String directory,String filename,String hostname) throws IOException{
		
		
		ftpClient.setBufferSize(1024);
		ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
		
//		ftpClient.deleteFile(filename);
		
		FTPFile[] ftpFiles=ftpClient.listFiles();
		int index=0;
		boolean flag=false;
		for(;index<ftpFiles.length;index++){
			if(ftpFiles[index].getName().equals(filename)){
				flag=true;
				break;
			}
		}
//		FTPFile[] files=ftpClient.listFiles("/home/x2hadoop/liufeng/ftp/zeus.flv");
//		FTPFile[] files=ftpClient.listFiles(pathname);
//		System.out.println("files:"+files.length);
		if(flag==true){
//			System.out.println(ftpFiles[index].getName());
//			System.out.println(flag+" "+index);
//			System.out.println("大小:"+ftpFiles[index].getSize());
			long remoteSize=ftpFiles[index].getSize();
			File localFile=new File(localPath);
			if(remoteSize==localFile.length()){
				log.warn("["+hostname+"]当前上传文件已存在!");
				return 3;
			}
			InputStream inputStream=new FileInputStream(localFile);
			if(remoteSize<localFile.length()){
				if(inputStream.skip(remoteSize)==remoteSize){
					log.info("["+hostname+"]断点续传中!");
					ftpClient.setRestartOffset(remoteSize);
					if(ftpClient.storeFile(filename, inputStream)){
						inputStream.close();
//						System.out.println(localFile.length());
						for(FTPFile file:ftpClient.listFiles()){
							if(file.getName().equals(filename)){
//								System.out.println(file.getSize());
//								System.out.println(localFile.length());
								if(file.getSize()==localFile.length()){
									log.info("["+hostname+"]完成断点续传!");
									return 2;
								}
							}
						}
					}
				}
			}
			//可能存在问题:当服务器文件大小大于本地文件的时候,删除服务端文件
			if(remoteSize>localFile.length()){
				ftpClient.deleteFile(filename);
				log.info("["+hostname+"]服务器文件大于本地文件,删除服务器文件,重新上传!");
				if(ftpClient.storeFile(filename, inputStream)){
					log.info("["+hostname+"]上传成功!");
					inputStream.close();
					return 1;
				}else{
					return 0;
				}
			}
			
			if(ftpClient.deleteFile(filename)){
				log.info("["+hostname+"]断点续传没有成功,删除服务器文件,重新上传!");
			}
			inputStream=new FileInputStream(localFile);
			if(ftpClient.storeFile(filename, inputStream)){
				log.info("["+hostname+"]上传成功!");
				inputStream.close();
				return 1;
			}
		}else{
			log.info("["+hostname+"]开始上传!");
			InputStream inputStream=new FileInputStream(localPath);
			if(ftpClient.storeFile(filename, inputStream)){
				log.info("["+hostname+"]上传成功");
				inputStream.close();
				return 1;
			}
		}
		
		return 0;
	}
}
