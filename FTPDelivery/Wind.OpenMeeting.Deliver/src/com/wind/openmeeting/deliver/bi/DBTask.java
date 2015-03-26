package com.wind.openmeeting.deliver.bi;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import com.wind.openmeeting.deliver.beans.DBConfig;
import com.wind.openmeeting.deliver.beans.FTPAccount;
import com.wind.openmeeting.deliver.utils.DBPasswordCenterAgency;
import com.wind.openmeeting.deliver.utils.JDBCUtils;
import com.wind.openmeeting.task.BaseTaskManager;
import com.wind.openmeeting.task.IBuilder;
import com.windin.ocean.common.sql.DBCon;

public class DBTask extends BaseService{

	private static Logger logger=Logger.getLogger(DBTask.class);
	
	private static final String MSSQLDBCONSTR="res:///com/wind/openmeeting/deliver/res/mssql.properties";
	private static final String DERBYDBCONSTR = "res:///com/wind/openmeeting/deliver/res/derby.properties";
	
	private static DBCon derbyDBManager = null;
	private static DBCon mssqlDBManager = null;
	
	private static final String INSERTSQL="insert into ut_videostatus(VideoID,IPAddress,FTPUserID,FTPPassword,Directory,FileName,VideoGUID) values(?,?,?,?,?,?,?)";
	private static final String SRCSQL="select * from dbo.Tb_CollegeVideo where DistributeStatus=0";
	private static final String CHECKSQL="select * from ut_videostatus where VideoID=?";
	
	private static final String CONSISTENCE="update ut_videostatus set TransferStatus=0 where Status=0";
	
	private static final String FTPACCOUNT="select * from dbo.Tb_CollegeVideoServer";
	private static ConcurrentHashMap<String, FTPAccount> ftpAccountMap=new ConcurrentHashMap<String, FTPAccount>();

	private DBConfig dbConfig;
	
	public DBConfig getDbConfig() {
		return dbConfig;
	}

	public void setDbConfig(DBConfig dbConfig) {
		this.dbConfig = dbConfig;
	}

	static {
		try {
			derbyDBManager = DBCon.getInstance(DERBYDBCONSTR);
			mssqlDBManager = DBCon.getInstance(MSSQLDBCONSTR);
		} catch (Exception e) {
			throw new ExceptionInInitializerError("无法加载数据库驱动!");
		}
	}
	
	
	//加载类文件时做数据一致性校验,为了解决上次突然退出而导致传输状态没有被设置为0的情况
	static{
//		logger.info("文件传输呀:"+new Date());
		Connection dbConn=null;
		PreparedStatement preparedStatement=null;
		try {
			dbConn=derbyDBManager.getConnection();
			preparedStatement=dbConn.prepareStatement(CONSISTENCE);
			preparedStatement.executeUpdate();
		} catch (Exception e) {
			throw new RuntimeException("校验传输状态标志位时发生问题!");
		}finally{
			JDBCUtils.free(null, preparedStatement, dbConn);
		}
	}
	
	
	private String TaskPlan;
	
	public String getTaskPlan() {
		return TaskPlan;
	}

	public void setTaskPlan(String taskPlan) {
		TaskPlan = taskPlan;
	}


	public void scanDB(){
		String password=DBPasswordCenterAgency.GetPassword(dbConfig.getDbSource(), dbConfig.getUserID());
		mssqlDBManager.resetPassword(password);
		{
			Connection dbConnection=null;
			PreparedStatement ftpAccountStatement=null;
			ResultSet resultSet=null;
			try {
				dbConnection=mssqlDBManager.getConnection();
				ftpAccountStatement=dbConnection.prepareStatement(FTPACCOUNT);
				resultSet=ftpAccountStatement.executeQuery();
				AtomicInteger serverID=new AtomicInteger(2);
				while(resultSet.next()){
					FTPAccount ftpAccount=new FTPAccount();
					ftpAccount.setHostname(resultSet.getString("IPAddress"));
					ftpAccount.setUsername(resultSet.getString("LoginName"));
					ftpAccount.setPassword(resultSet.getString("Password"));
					ftpAccount.setDirectory(null);
					ftpAccountMap.put(serverID.toString(), ftpAccount);
					serverID.getAndIncrement();
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException("无法读取SQL Server数据库");
			}finally{
				JDBCUtils.free(resultSet, ftpAccountStatement, dbConnection);
			}
		}
		Connection dbSrcConn=null;
		Connection dbDestConn=null;
		PreparedStatement preparedStatement=null;
		ResultSet resultSet=null;
		PreparedStatement localStatement=null;
		ResultSet localResult=null;
//		System.out.println(ftpAccountMap);
		logger.info("进入数据库扫描");
		try {
			dbSrcConn=mssqlDBManager.getConnection();
//			DatabaseMetaData meta=dbSrcConn.getMetaData();
//			logger.info("sql server:"+meta.supportsTransactions());
			dbDestConn=derbyDBManager.getConnection();
//			DatabaseMetaData metaData=dbDestConn.getMetaData();
//			logger.info("derby:"+metaData.supportsTransactions());
			preparedStatement=dbSrcConn.prepareStatement(SRCSQL);
			resultSet=preparedStatement.executeQuery();
			localStatement=dbDestConn.prepareStatement(CHECKSQL);
			while(resultSet.next()){
				int videoID=resultSet.getInt("VideoID");
//				logger.info("分发状态为0:"+videoID);
				localStatement.setInt(1, videoID);
				localResult=localStatement.executeQuery();
				try {
					if(!localResult.next()){
						String releaseIPs=resultSet.getString("ReleaseIP");
						logger.info("准备插入:"+videoID+":"+releaseIPs);
						String filename=resultSet.getString("FileName");
						String videoGUID=resultSet.getString("VideoGUID");
						dbDestConn.setAutoCommit(false);
						PreparedStatement insertStatement=dbDestConn.prepareStatement(INSERTSQL);
//						logger.info(releaseIPs.trim().split(",").length);
						if(filename!=null && videoGUID!=null){
							for(String ip:releaseIPs.trim().split(",")){
								FTPAccount ftpAccount=ftpAccountMap.get(ip);
//								logger.info("打印ftpAccount对象"+ftpAccount);
								insertStatement.setInt(1, videoID);
								insertStatement.setString(2, ftpAccount.getHostname());
								insertStatement.setString(3, ftpAccount.getUsername());
								insertStatement.setString(4, ftpAccount.getPassword());
								insertStatement.setString(5, ftpAccount.getDirectory());
								insertStatement.setString(6, filename);
								insertStatement.setString(7, videoGUID);
								insertStatement.addBatch();
//								logger.info("批量插入!");
							}
							logger.info("成功插入了:"+insertStatement.executeBatch().length);
							dbDestConn.commit();
						}
					}
				} catch (Exception e) {
					dbDestConn.rollback();
				}
			}
		} catch (Exception e) {
			throw new RuntimeException();
		} finally{
			JDBCUtils.free(resultSet,preparedStatement, dbSrcConn);
			JDBCUtils.free(localResult, localStatement, dbDestConn);
		}
	}
	
	/*private void getDBData() throws Exception{
		DBCon dbManager = DBCon.getInstance(DBCONSTR);
		dbManager.resetPassword("hadoop");
		Connection dbConn = dbManager.getConnection();
		String sql="select * from dbo.Tb_CollegeVideo";
		PreparedStatement preparedStatement=dbConn.prepareStatement(sql);
		ResultSet resultSet=preparedStatement.executeQuery();
		while(resultSet.next()){
			System.out.println(resultSet.getString("ShowName"));
		}
		JDBCUtils.free(resultSet, preparedStatement, dbConn);
		CachedRowSet rowSet = new CachedRowSetImpl();
		rowSet.setCommand("select * from dbo.Tb_CollegeVideo" );
		rowSet.setPageSize(100);
		try{
			rowSet.execute(dbConn);
			while(rowSet.next()){
				System.out.println(rowSet.getString("showName"));
			}
		}finally{
			if (!dbConn.isClosed()) dbConn.close();
		}
	}*/
	
	private DBTaskManager dbTaskManager;

	protected void doStart() throws Exception {
		super.doStart();
//		getDBData();
//		logger.info("开始了");
		dbTaskManager=new DBTaskManager();
		
	}


	protected void doStop() throws Exception {
		super.doStop();
	}
	
	class DBTaskBuilder implements IBuilder{

		@Override
		public String getTaskID() {
			return "DBTask";
		}

		@Override
		public void build() {
			StringBuffer errInfo = new StringBuffer();
			build(errInfo);
		}

		@Override
		public void build(StringBuffer errInfo) {
			logger.info("db task run");
			//TODO
			scanDB();
		}
		
	}
	
	class DBTaskManager extends BaseTaskManager{

		@Override
		protected Hashtable<IBuilder, String> refrashTaskDeclare() {
			Hashtable<IBuilder, String> taskDeclares = new Hashtable<IBuilder, String>();
			taskDeclares.put(new DBTaskBuilder(), getTaskPlan());
			return taskDeclares;
		}
		
	}

}
