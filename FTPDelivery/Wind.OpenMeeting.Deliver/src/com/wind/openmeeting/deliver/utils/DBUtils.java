package com.wind.openmeeting.deliver.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.wind.openmeeting.deliver.beans.FTPAccount;

public class DBUtils {
	
	public int insertVideo(Connection connect,int videoID,FTPAccount ftpAccount) throws SQLException{
		String insertStatement="insert into ut_videostatus(VideoID,IPAddress,ftpUserID,ftpPassword,uri) values(?,?,?,?,?)";
		PreparedStatement preparedStatement=connect.prepareStatement(insertStatement);
		if(ftpAccount!=null){
			preparedStatement.setInt(1, videoID);
			preparedStatement.setString(2, ftpAccount.getHostname());
			preparedStatement.setString(3, ftpAccount.getUsername());
			preparedStatement.setString(4, ftpAccount.getPassword());
			preparedStatement.setString(5, ftpAccount.getDirectory());
		}
		int exeValue=preparedStatement.executeUpdate();
		JDBCUtils.free(null, preparedStatement, null);
		return exeValue;
	}
	
}
