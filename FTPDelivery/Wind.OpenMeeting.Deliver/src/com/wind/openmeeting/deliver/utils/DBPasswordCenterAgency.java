package com.wind.openmeeting.deliver.utils;

import java.util.Hashtable;

import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;

/**
 * @author liufeng E-mail:fliu.Dancer@wind.com.cn
 * @version Time:Nov 17, 2014 2:36:09 PM
 * @Description
 */
public class DBPasswordCenterAgency {

	private static Hashtable hashTable = new Hashtable();

	public static interface IGetDBPasswordCenter extends Library {
		IGetDBPasswordCenter INSTANCE = (IGetDBPasswordCenter) Native.loadLibrary("CryptoCPP", IGetDBPasswordCenter.class);

		int GetCredential(String dbSource, String userID, Memory memory, int length);
	}

	public static String GetPassword(String dbSource, String userID) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(dbSource).append('\\').append(userID);
		String key = stringBuilder.toString();
		if (hashTable.containsKey(key)) {
			return hashTable.get(key).toString();
		}
		return GetLastedPassword(dbSource, userID);
	}

	public static String GetLastedPassword(String dbSource, String userID) {
		Memory memory = new Memory(50);
		int length = 50;
		int result = IGetDBPasswordCenter.INSTANCE.GetCredential(dbSource, userID, memory, length);
		int i = 0;
		while (i < 3) {
			if (result <= 0) {
				result = IGetDBPasswordCenter.INSTANCE.GetCredential(dbSource, userID, memory, length);
			}

			if (result > memory.getSize()) {
				memory.clear();
				memory = new Memory(result + 5);
				length = result + 5;
				result = IGetDBPasswordCenter.INSTANCE.GetCredential(dbSource, userID, memory, length);
			}

			if (result > 0) {
				StringBuilder stringBuilder = new StringBuilder();
				stringBuilder.append(dbSource).append('\\').append(userID);
				String key = stringBuilder.toString();
				String password = memory.getString(0);
				hashTable.put(key, password);
				return password;
			}
			i++;
		}
		return "";
	}

	public static void main(String[] args) {
		System.out.println(System.getProperty("java.library.path"));
		String passwd = DBPasswordCenterAgency.GetLastedPassword("10.100.6.53", "hadoop");
		System.out.println(passwd);
		if (passwd != null && passwd.length() != 0) {
			System.out.println("获取动态密码成功");
		} else {
			System.out.println("获取动态密码失败");
		}
	}

}
