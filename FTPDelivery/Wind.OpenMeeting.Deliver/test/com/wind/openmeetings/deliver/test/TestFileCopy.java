package com.wind.openmeetings.deliver.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import org.junit.Test;

public class TestFileCopy {

	@Test
	public void test(){
		String srcPath="/home/liufeng/windcollege/zeus.flv";
		String fileName="zeus.flv";
		String desPath="/home/liufeng/copy/";
		File file=new File(srcPath);
		if(file.exists()){
			FileChannel channel=null;
			FileInputStream inputStream=null;
			try {
				inputStream=new FileInputStream(file);
				channel=inputStream.getChannel();
				long position=0;
				long size=file.length();
				FileChannel outChannel=null;
				FileOutputStream outputStream=null;
				try {
					File newFile=new File(desPath+fileName);
					newFile.createNewFile();
					outputStream=new FileOutputStream(newFile);
					outChannel=outputStream.getChannel();
					while(size>0){
						long bytes=channel.transferTo(position, size, outChannel);
						position+=bytes;
						size-=bytes;
					}
				} catch (FileNotFoundException e) {
					throw new RuntimeException("无法创建新文件"+fileName);
				} catch (IOException e) {
					throw new RuntimeException("文件复制出现错误");
				}finally{
					try {
						outChannel.close();
						outputStream.close();
					} catch (IOException e) {
						throw new RuntimeException("无法关闭通道");
					}
				}
			} catch (FileNotFoundException e) {
				throw new RuntimeException("指定的源文件不存在");
			} finally{
				try {
					channel.close();
					inputStream.close();
					file.delete();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

}
