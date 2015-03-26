package com.wind.openmeetings.deliver.test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.Test;

import com.wind.openmeeting.deliver.utils.JDBCUtils;
import com.windin.ocean.common.sql.DBCon;

public class TestMSSQLConnection {
	
	private static final String MSSQLDBCONSTR="res:///com/wind/openmeeting/deliver/res/mssql.properties";
	private static final String RELEASEIP="select ReleaseIP from dbo.Tb_CollegeVideo where VideoID=?";

	@Test
	public void test() throws Exception {
		DBCon mssqlDBManager=DBCon.getInstance(MSSQLDBCONSTR);
		String password="hadoop";
		mssqlDBManager.resetPassword(password);
		Connection connection=mssqlDBManager.getConnection();
		PreparedStatement preparedStatement=connection.prepareStatement(RELEASEIP);
		preparedStatement.setInt(1, 2655);
		ResultSet resultSet=preparedStatement.executeQuery();
		if(resultSet.next())
			System.out.println(resultSet.getString("ReleaseIP"));
		JDBCUtils.free(resultSet, preparedStatement, connection);
	}

}
