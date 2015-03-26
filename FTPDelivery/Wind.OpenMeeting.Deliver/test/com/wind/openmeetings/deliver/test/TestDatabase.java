package com.wind.openmeetings.deliver.test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.junit.Test;

import com.windin.ocean.common.sql.DBCon;

public class TestDatabase {

	@Test
	public void test()  {
		Connection connection=null;
		try {
			DBCon dbManager=DBCon.getInstance("");
			dbManager.resetPassword("hadoop");
			System.out.println(dbManager);
			connection=dbManager.getConnection();
//			String sqlStatement="select * from dbo.Tb_CollegeVideo where VideoID=?";
			String sqlStatement="insert into dbo.Tb_CollegeVideo(ShowName,FileName,VideoType) values(?,?,?)";
			PreparedStatement preparedStatement=connection.prepareStatement(sqlStatement);
			connection.setAutoCommit(false);
			preparedStatement.setString(1, "test");
			preparedStatement.setString(2, "test.wmv");
			preparedStatement.setInt(3, 3);
			preparedStatement.executeUpdate();
//			ResultSet resultSet=preparedStatement.executeQuery();
//			if(resultSet.next()){
//				System.out.println(resultSet.getString("FileName"));
//				String releaseIP=resultSet.getString("ReleaseIP");
//				System.out.println(releaseIP);
//				for(String ip:releaseIP.split(",")){
//					System.out.println(ip);
//				}
//			}else{
//				System.out.println("No data!");
//			}
			connection.commit();
		} catch (SQLException e) {
			try {
				connection.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("out");
//		connection.commit();
	}

}
