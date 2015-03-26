package com.wind.openmeetings.deliver.test;

import java.io.File;
import java.util.Date;

import org.junit.Test;

public class TestFileTime {

	@Test
	public void test() {
		String firPath="/home/liufeng/windcollege/zeus.flv";
		String secPath="/home/liufeng/copy/zeus.flv";
		File srcFile=new File(firPath);
		File copyFile=new File(secPath);
		System.out.println(srcFile.lastModified());
		System.out.println(new Date(srcFile.lastModified()));
		System.out.println(copyFile.lastModified());
		System.out.println(new Date(copyFile.lastModified()));
		long current_time=System.currentTimeMillis();
		System.out.println(current_time);
		System.out.println(current_time-srcFile.lastModified());
		System.out.println((current_time-srcFile.lastModified())/1000/3600);
	}

}
