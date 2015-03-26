package com.wind.openmeetings.deliver.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.Test;

public class TestCopy {

	@Test
	public void test() throws IOException {
		String srcPath="/home/liufeng/windcollege/zeus.flv";
		String fileName="zeus.flv";
		String desPath="/home/liufeng/copy/";
		File file=new File(srcPath);
		InputStream inputStream=null;
		OutputStream outputStream=null;
		int reads=0;
		if(file.exists()){
			try {
				inputStream=new FileInputStream(file);
				outputStream=new FileOutputStream(desPath+fileName);
				byte[] buffer=new byte[1024*4];
				while((reads=inputStream.read(buffer))!=-1){
					outputStream.write(buffer, 0, reads);
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}finally{
				outputStream.close();
				inputStream.close();
			}
		}
	}

}
