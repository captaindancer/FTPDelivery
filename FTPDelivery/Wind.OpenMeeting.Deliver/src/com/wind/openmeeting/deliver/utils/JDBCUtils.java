package com.wind.openmeeting.deliver.utils;


import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import com.microsoft.sqlserver.jdbc.SQLServerException;

/**
 * @author liufeng E-mail:fliu.Dancer@wind.com.cn
 * @version Time:Nov 17, 2014 5:10:56 PM
 * @Description
 */
public class JDBCUtils {
	private static String driverClassName;
	private static String ipAddress;
	private static String url;
	private static String username;
	private static String password;
	private static String portnumber;
	private static String databasename;
	private static Connection connection;
	private static SQLServerDataSource sqlDataSource=new SQLServerDataSource();

	/*static {
		try {
			InputStream inputStream = JDBCUtils.class.getResourceAsStream("/config.xml");
			SAXReader reader = new SAXReader();
			Document document = reader.read(inputStream);
			Element root = document.getRootElement();
			Element datasource = root.element("datasource");
			driverClassName = datasource.element("driverClassName").getText();
			ipAddress = datasource.element("ipAddress").getText();
			url = datasource.element("url").getText();
			username = datasource.element("username").getText();
			portnumber = datasource.element("portNumber").getText();
			databasename = datasource.element("databaseName").getText();
			// System.out.println(url);
			// password="password="+DBPasswordCenterAgency.GetLastedPassword(ipAddress,
			// username)+";";
		} catch (DocumentException e) {
			throw new ExceptionInInitializerError("can't load the configuration file!");
		}
	}*/

	public static Connection getConnection() {
//		sqlDataSource = new SQLServerDataSource();
		sqlDataSource.setUser(username);
		password = "hadoop";
		sqlDataSource.setPassword(password);
		sqlDataSource.setServerName(ipAddress);
		if (portnumber != null) {
			try {
				sqlDataSource.setPortNumber(Integer.parseInt(portnumber));
			} catch (NumberFormatException e) {
				throw new RuntimeException("the format of portnumber is wrong!");
			}
		} else {
			sqlDataSource.setPortNumber(1433);
		}
		sqlDataSource.setDatabaseName(databasename);
		try {
			return sqlDataSource.getConnection();
		} catch (SQLServerException e) {
			throw new RuntimeException("can't create the connection!");
		}
	}

	public static Connection getDefaultConnection() {
		try {
			Class.forName(driverClassName);
		} catch (ClassNotFoundException e) {
			throw new ExceptionInInitializerError("can't load the driver!");
		}
		try {
			StringBuffer buffer = new StringBuffer(url);
			buffer.append(ipAddress).append(":").append(portnumber).append(";databaseName=").append(databasename).append(";user=").append(username)
					.append(";password=").append(password).append(";");
			System.out.println(buffer.toString());
			connection = DriverManager.getConnection(buffer.toString());
		} catch (SQLException e) {
			throw new RuntimeException("can't create the connection!");
		}
		return connection;
	}

	public static Connection getConnection(String url) {
		try {
			connection = DriverManager.getConnection(url);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return connection;
	}

	public static Connection getConnection(String url, String username, String password) {
		try {
			connection = DriverManager.getConnection(url, username, password);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return connection;
	}

	public static void free(ResultSet resultSet, Statement statement, Connection connection) {
		try {
			if (resultSet != null) {
				resultSet.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (statement != null) {
					statement.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				try {
					if (connection != null)
						connection.close();
				} catch (SQLException e) {
					throw new RuntimeException("can't close the connection");
				}
			}
		}
	}
}
