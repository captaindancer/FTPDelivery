package com.wind.openmeetings.deliver.test;

import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Test;

import com.wind.openmeeting.deliver.beans.FTPAccount;

public class TestFTPAcount {

	@Test
	public void test() {
		FTPAccount ftpAccount1=new FTPAccount();
		FTPAccount ftpAccount2=new FTPAccount();
		ftpAccount1.setHostname("1");
		ftpAccount1.setPassword("2");
		ftpAccount1.setDirectory("3");
		ftpAccount1.setUsername("4");
		ftpAccount2.setHostname("1");
		ftpAccount2.setPassword("2");
		ftpAccount2.setDirectory("3");
		ftpAccount2.setUsername("4");
		System.out.println(ftpAccount1);
		System.out.println(ftpAccount2);
		System.out.println(ftpAccount1.equals(ftpAccount2));
		SimpleDateFormat dateFormat=new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss");
		System.out.println(dateFormat.format(new Date()));
		Date currentDate=new Date();
		System.out.println(currentDate);
		java.sql.Date sqlDate=new java.sql.Date(currentDate.getTime());
		System.out.println(sqlDate);
		Time sqlTime=new Time(currentDate.getTime());
		System.out.println(sqlTime);
		Timestamp sqlTimestamp=new Timestamp(currentDate.getTime());
		System.out.println(sqlTimestamp);
	}

}
