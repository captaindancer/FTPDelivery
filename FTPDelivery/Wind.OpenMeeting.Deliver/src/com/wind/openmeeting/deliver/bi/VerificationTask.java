package com.wind.openmeeting.deliver.bi;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Hashtable;

import org.apache.log4j.Logger;

import com.wind.openmeeting.deliver.utils.JDBCUtils;
import com.wind.openmeeting.task.BaseTaskManager;
import com.wind.openmeeting.task.IBuilder;
import com.windin.ocean.common.sql.DBCon;

public class VerificationTask extends BaseService {
	private static final String DBCONSTR = "res:///com/wind/openmeeting/deliver/res/derby.properties";
	private static final String MSSQLDBCONSTR = "res:///com/wind/openmeeting/deliver/res/mssql.properties";

	private static final Logger logger=Logger.getLogger(VerificationTask.class);
	
	private static DBCon derbyDBManager = null;
	private static DBCon mssqlDBManager = null;
	
	private static final String DISTRIBUTESTATUS="select VideoID from dbo.Tb_CollegeVideo where DistributeStatus=0";
	private static final String UPDATESTATUS="update ut_videostatus set Status=0 and WriteBack=0 where VideoID=? and WriteBack=1";
	
	static {
		try {
			derbyDBManager = DBCon.getInstance(DBCONSTR);
			mssqlDBManager = DBCon.getInstance(MSSQLDBCONSTR);
		} catch (Exception e) {
			throw new ExceptionInInitializerError("无法加载数据库驱动!");
		}
	}
	
	private String TaskPlan;
	
	public String getTaskPlan() {
		return TaskPlan;
	}

	public void setTaskPlan(String taskPlan) {
		TaskPlan = taskPlan;
	}
	
	public void verficateDB(){
		Connection mssqlConnection=null;
		Connection derbyConnection=null;
		PreparedStatement mssqlStatement=null;
		PreparedStatement derbyUpdateStatement=null;
		ResultSet resultSet=null;
		try {
			mssqlConnection=mssqlDBManager.getConnection();
			mssqlStatement=mssqlConnection.prepareStatement(DISTRIBUTESTATUS);
			resultSet=mssqlStatement.executeQuery();
			derbyConnection=derbyDBManager.getConnection();
			derbyUpdateStatement=derbyConnection.prepareStatement(UPDATESTATUS);
			while(resultSet.next()){
				int videoID=resultSet.getInt("VideoID");
				derbyUpdateStatement.setInt(1, videoID);
				derbyUpdateStatement.executeUpdate();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			JDBCUtils.free(resultSet, mssqlStatement, mssqlConnection);
			JDBCUtils.free(null,derbyUpdateStatement,derbyConnection);
		}
	}
	
	private VerificationTaskManager verificationTaskManager;

	protected void doStart() throws Exception {
		super.doStart();
//		getDBData();
		verificationTaskManager=new VerificationTaskManager();
	}


	protected void doStop() throws Exception {
		super.doStop();
	}
	
	class VerificationTaskBuilder implements IBuilder{

		@Override
		public String getTaskID() {
			return "VerificationTask";
		}

		@Override
		public void build() {
			StringBuffer errInfo = new StringBuffer();
			build(errInfo);
		}

		@Override
		public void build(StringBuffer errInfo) {
			logger.info("verification task run");
			verficateDB();
			logger.info("verification task over");
		}
		
	}
	
	class VerificationTaskManager extends BaseTaskManager{

		@Override
		protected Hashtable<IBuilder, String> refrashTaskDeclare() {
			Hashtable<IBuilder, String> taskDeclares = new Hashtable<IBuilder, String>();
			taskDeclares.put(new VerificationTaskBuilder(), getTaskPlan());
			return taskDeclares;
		}
		
	}
}
