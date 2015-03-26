package com.wind.openmeetings.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class XmlUtil {

	@SuppressWarnings("finally")
	public static Map<String, String> readUserList(String file) {
		Map<String, String> userMap = new HashMap<String, String>();
		try {
			InputStream in = new FileInputStream(new File(file));
			SAXReader reader = new SAXReader();
			Document doc;
			doc = reader.read(in);
			Element root = doc.getRootElement();
			List<Element> eleList = root.elements("user");
			for (Element ele : eleList) {
				/*System.out.println(ele.attributeValue("name") + ":"
						+ ele.attributeValue("password"));*/
				String name = ele.attributeValue("name");
				String password = ele.attributeValue("password");
				if (name != null && !name.trim().equalsIgnoreCase("")
						&& password != null
						&& !password.trim().equalsIgnoreCase("")) {
					userMap.put(name, password);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			return userMap;
		}
	}

}
