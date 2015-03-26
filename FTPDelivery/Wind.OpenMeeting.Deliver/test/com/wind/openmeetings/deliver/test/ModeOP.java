package com.wind.openmeetings.deliver.test;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Test;

public class ModeOP {

	@Test
	public void test() {
		System.out.println(5%3);
		System.out.println(3%3);
		System.out.println(6%3);
		if((9%3)==0){
			System.out.println("haha");
		}
		System.out.println("videoID"+"\t"+"videoGUID");
		
		Date currentTime=new Date();
		SimpleDateFormat formatter=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String timeString=formatter.format(currentTime);
		System.out.println(timeString);
	}

}
