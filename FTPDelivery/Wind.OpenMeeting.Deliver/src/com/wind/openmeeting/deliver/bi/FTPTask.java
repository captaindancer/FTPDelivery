package com.wind.openmeeting.deliver.bi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.SocketException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import com.wind.openmeeting.deliver.beans.DBConfig;
import com.wind.openmeeting.deliver.beans.FTPAccount;
import com.wind.openmeeting.deliver.utils.DBPasswordCenterAgency;
import com.wind.openmeeting.deliver.utils.FTPUtils;
import com.wind.openmeeting.deliver.utils.JDBCUtils;
import com.wind.openmeeting.task.BaseTaskManager;
import com.wind.openmeeting.task.IBuilder;
import com.wind.openmeetings.servlet.SchedualServlet;
import com.windin.component.InfoNotice;
import com.windin.ocean.common.sql.DBCon;

public class FTPTask extends BaseService {
	private static final Logger LOGGER = Logger.getLogger(FTPTask.class);
	// private FTPAccount[] ftpAccounts;
	private static ConcurrentHashMap<String, FTPAccount> ftpAccountMap=new ConcurrentHashMap<String, FTPAccount>();
	private static final String DBCONSTR = "res:///com/wind/openmeeting/deliver/res/derby.properties";
	private static final String MSSQLDBCONSTR = "res:///com/wind/openmeeting/deliver/res/mssql.properties";

	private static DBCon derbyDBManager = null;
	private static Connection derbyConnection = null;

	private static DBCon mssqlDBManager = null;
	private static Connection mssqlConnection = null;

	private static final String STATUSSQL = "select VideoID,IPAddress,FTPUserID,FTPPassword,Directory,FileName,VideoGUID from ut_videostatus where Status=0 and TransferStatus=0";
	private static final String STATUSUPDATE = "update ut_videostatus set Status=?,TransferStatus=? where VideoID=? and IPAddress=?";
	private static final String COUNTSQL = "select Count from ut_videostatus where VideoID=? and IPAddress=?";
	private static final String COUNTUPDATE = "update ut_videostatus set Count=?,TransferStatus=? where VideoID=? and IPAddress=?";
	private static final String COUNTSTATISTICS = "select VideoID,count(Status) from ut_videostatus where WriteBack=0 and Status=1 group by VideoID";
	private static final String WRITEBACK = "update dbo.Tb_CollegeVideo set DistributeStatus=1 where VideoID=?";
	private static final String WRITELOG = "insert into dbo.Tb_CollegeVideoReleaseLog(VideoID,LogTitle,LogContent,ReleaseStatus,ReleaseIP,CreateDT) values(?,?,?,?,?,?)";
	private static final String FILENAME_RELEASEIP = "select FileName,ReleaseIP,VideoGUID from dbo.Tb_CollegeVideo where VideoID=?";
	private static final String DERBYWRITEBACK = "update ut_videostatus set WriteBack=1 where VideoID=?";
	private static final String TRANSFERSTATUS = "update ut_videostatus set TransferStatus=? where VideoID=? and IPAddress=?";
	private static final String FTPACCOUNT="select * from dbo.Tb_CollegeVideoServer";
	
	private static final String SETALLTRANSFER="update ut_videostatus set TransferStatus=0";
	
	private String filePath;
	private String copyPath;

	private int speedLimit;
	
	private DBConfig dbConfig;
	
	public int getSpeedLimit() {
		return speedLimit;
	}

	public void setSpeedLimit(int speedLimit) {
		this.speedLimit = speedLimit;
	}

	public String getCopyPath() {
		return copyPath;
	}

	public void setCopyPath(String copyPath) {
		this.copyPath = copyPath;
	}

	private static String fileSeparator;

	private ExecutorService executorService ;

	private int threadNumber;
	
	public int getThreadNumber() {
		return threadNumber;
	}

	public void setThreadNumber(int threadNumber) {
		this.threadNumber = threadNumber;
	}

	private static SimpleDateFormat dateFormat = new SimpleDateFormat(
			"yyyy年MM月dd日 HH:mm:ss");
	private String TaskPlan;
	private InfoNotice noticeObj;

	static {
		fileSeparator = System.getProperties().getProperty("file.separator");
	}

	static {
		try {
			derbyDBManager = DBCon.getInstance(DBCONSTR);
			mssqlDBManager = DBCon.getInstance(MSSQLDBCONSTR);
		} catch (Exception e) {
			throw new ExceptionInInitializerError("无法加载数据库驱动!");
		}
	}
	
	static{
		Connection setAllTransferConnection =null;
		PreparedStatement setAllTransferStatement=null;
		try {
			setAllTransferConnection=derbyDBManager.getConnection();
			DatabaseMetaData metaData=setAllTransferConnection.getMetaData();
			String[] names={"TABLE"};
			ResultSet result=metaData.getTables(null, null, null, names);
			List<String> tableNameList=new ArrayList<String>();
			while(result.next()){
				tableNameList.add(result.getString("TABLE_NAME"));
			}
			if(tableNameList.contains("UT_VIDEOSTATUS")){
				setAllTransferStatement = setAllTransferConnection.prepareStatement(SETALLTRANSFER);
				setAllTransferStatement.executeUpdate();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			JDBCUtils.free(null, setAllTransferStatement, setAllTransferConnection);
		}

	}
	
	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getTaskPlan() {
		return TaskPlan;
	}

	public void setTaskPlan(String taskPlan) {
		TaskPlan = taskPlan;
	}

	public InfoNotice getNoticeObj() {
		return noticeObj;
	}

	public void setNoticeObj(InfoNotice noticeObj) {
		this.noticeObj = noticeObj;
	}

	public String getReleaseIP(FTPAccount ftpAccount) {
		for (Entry<String, FTPAccount> entry : ftpAccountMap.entrySet()) {
			if (entry.getValue().equals(ftpAccount)) {
				return entry.getKey();
			}
		}
		return null;
	}

	class FTPTaskBuilder implements IBuilder {

		@Override
		public String getTaskID() {
			return "FTPTask";
		}

		@Override
		public void build() {
			StringBuffer errInfo = new StringBuffer();
			build(errInfo);
		}

		@Override
		public void build(StringBuffer errInfo) {
			LOGGER.info("ftp task run");
			writeBack();
			String dbPassword=DBPasswordCenterAgency.GetPassword(dbConfig.getDbSource(), dbConfig.getUserID());
			taskDistribution(dbPassword);
		}

	}

	class FTPTaskManager extends BaseTaskManager {

		@Override
		protected Hashtable<IBuilder, String> refrashTaskDeclare() {
			Hashtable<IBuilder, String> taskDeclares = new Hashtable<IBuilder, String>();
			taskDeclares.put(new FTPTaskBuilder(), getTaskPlan());
			return taskDeclares;
		}

	}

	private FTPTaskManager ftpTaskManager;

	protected void doStart() throws Exception {
		super.doStart();
		// LOGGER.info(ftpAccountMap);
		ftpTaskManager = new FTPTaskManager();
	}

	public void taskDistribution(String dbPassword) {
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		executorService=Executors.newFixedThreadPool(threadNumber);
		try {
			derbyConnection = derbyDBManager.getConnection();
			preparedStatement = derbyConnection.prepareStatement(STATUSSQL);
			resultSet = preparedStatement.executeQuery();
			int videoID;
			String fileName;
			String videoGUID;
			while (resultSet.next()) {
				FTPAccount ftpAccount = new FTPAccount();
				String fileUploadPath = filePath;
				videoID = resultSet.getInt("VideoID");
				ftpAccount.setHostname(resultSet.getString("IPAddress"));
				ftpAccount.setUsername(resultSet.getString("FTPUserID"));
				ftpAccount.setPassword(resultSet.getString("FTPPassword"));
				ftpAccount.setDirectory(resultSet.getString("Directory"));
				// LOGGER.info(videoID+":"+ftpAccount);
				fileName = resultSet.getString("FileName");
				videoGUID = resultSet.getString("VideoGUID");
				if (!fileUploadPath.contains(fileSeparator)) {
					throw new RuntimeException("配置文件中文件路径格式错误");
				}
				if (fileUploadPath.endsWith(fileSeparator)) {
					fileUploadPath += fileName;
				} else {
					fileUploadPath = fileUploadPath + fileSeparator + fileName;
				}
				// System.out.println("系统分割符:"+fileSeparator);
				// LOGGER.info(videoID+":"+fileUploadPath+":"+ftpAccount+":"+videoGUID);
				PreparedStatement transferstatus = derbyConnection
						.prepareStatement(TRANSFERSTATUS);
				transferstatus.setInt(1, 1);
				transferstatus.setInt(2, videoID);
				transferstatus.setString(3, ftpAccount.getHostname());
				transferstatus.executeUpdate();
				JDBCUtils.free(null, transferstatus, null);
				executorService.execute(new FTPThread(videoID, fileUploadPath,
								ftpAccount, videoGUID, this,speedLimit,1,dbPassword,null));
				// new Thread(new
				// FTPThread(videoID,fileUploadPath,ftpAccount,videoGUID,this)).start();
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException();
		} finally {
			JDBCUtils.free(resultSet, preparedStatement, derbyConnection);
		}

	}
	
	public long getCreateTime(String fileUploadPath){
		String time=null;
		long createTime=0;
		SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd  HH:mm");
		InputStream inputStream=null;
		BufferedReader reader=null;
		Process proc=null;
		try {
			proc=Runtime.getRuntime().exec("cmd /C dir "+fileUploadPath+"/tc");
			inputStream=proc.getInputStream();
			reader=new BufferedReader(new InputStreamReader(inputStream));
			String line;
			while((line=reader.readLine())!=null){
				if(line.endsWith(".wmv")||line.endsWith(".flv")||line.endsWith(".avi")||line.endsWith(".swf")){
					time=line.substring(0,17);
					break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			try {
				reader.close();
				proc.getOutputStream().close();
			} catch (IOException e) {
				throw new RuntimeException("文件流无法关闭!");
			}
		}
		try {
//			System.out.println("创建时间:"+time);
			Date date=format.parse(time);
			createTime=date.getTime();
		} catch (ParseException e) {
			throw new RuntimeException("日期解析异常");
		}
		return createTime;
	}

	public void writeBack() {
		String password=DBPasswordCenterAgency.GetPassword(dbConfig.getDbSource(), dbConfig.getUserID());
		mssqlDBManager.resetPassword(password);
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		PreparedStatement mssqlStatement = null;
		PreparedStatement releaseIPStatement = null;
		ResultSet releaseIPResultSet = null;
		PreparedStatement derbyWriteBack = null;
		// LOGGER.info("进入try-catch块");
		try {
			derbyConnection = derbyDBManager.getConnection();
			preparedStatement = derbyConnection
					.prepareStatement(COUNTSTATISTICS);
			resultSet = preparedStatement.executeQuery();
			mssqlConnection = mssqlDBManager.getConnection();
			mssqlStatement = mssqlConnection.prepareStatement(WRITEBACK);
			releaseIPStatement = mssqlConnection
					.prepareStatement(FILENAME_RELEASEIP);
			// LOGGER.info("进入循环");
			while (resultSet.next()) {
				int videoID = resultSet.getInt("VideoID");
				int count = resultSet.getInt(2);
				mssqlConnection.setAutoCommit(false);
				releaseIPStatement.setInt(1, videoID);
				releaseIPResultSet = releaseIPStatement.executeQuery();
				String releaseIPs = null;
				//备份时的文件名是GUID
				String fileName = null;
				if (releaseIPResultSet.next()) {
					fileName = releaseIPResultSet.getString("FileName");
					releaseIPs = releaseIPResultSet.getString("ReleaseIP");
				}
				if (releaseIPs != null && count == releaseIPs.split(",").length) {
					// 回写操作
					mssqlStatement.setInt(1, videoID);
					mssqlStatement.executeUpdate();
					derbyWriteBack = derbyConnection
							.prepareStatement(DERBYWRITEBACK);
					derbyWriteBack.setInt(1, videoID);
					LOGGER.info("derby回写标记改变:" + derbyWriteBack.executeUpdate());
					LOGGER.info(videoID + " mssql回写操作已完成!");
					if (fileName != null){
//						fileName=System.currentTimeMillis()+fileName;
						copyFile(filePath, fileName, copyPath);
					}
					LOGGER.info(videoID + "文件备份完成");
				}
				mssqlConnection.commit();
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException();
		} finally {
			JDBCUtils.free(resultSet, preparedStatement, derbyConnection);
			JDBCUtils.free(null, mssqlStatement, mssqlConnection);
			JDBCUtils.free(releaseIPResultSet, releaseIPStatement, null);
		}
	}

	/*public void copyFile(String srcPath, String fileName, String desPath) {
		if (srcPath.endsWith(fileSeparator)) {
			srcPath += fileName;
		} else {
			srcPath = srcPath + fileSeparator + fileName;
		}
		File file = new File(srcPath);
		if (file.exists()) {
			FileChannel channel = null;
			FileInputStream inputStream = null;
			try {
				inputStream = new FileInputStream(file);
				channel = inputStream.getChannel();
				long position = 0;
				long size = file.length();
				FileChannel outChannel = null;
				FileOutputStream outputStream = null;
				try {
					if (desPath.endsWith(fileSeparator)) {
						desPath += fileName;
					} else {
						desPath = desPath + fileSeparator + fileName;
					}
					File newFile = new File(desPath);
					newFile.createNewFile();
					outputStream = new FileOutputStream(newFile);
					outChannel = outputStream.getChannel();
					while (size > 0) {
						long bytes = channel.transferTo(position, size,
								outChannel);
						position += bytes;
						size -= bytes;
					}
				} catch (FileNotFoundException e) {
					throw new RuntimeException("无法创建新文件" + fileName);
				} catch (IOException e) {
					throw new RuntimeException("文件复制出现错误");
				} finally {
					try {
						outChannel.close();
						outputStream.close();
					} catch (IOException e) {
						throw new RuntimeException("无法关闭通道");
					}
				}
			} catch (FileNotFoundException e) {
				throw new RuntimeException("指定的源文件不存在");
			} finally {
				try {
					channel.close();
					inputStream.close();
					if (file.delete()) {
						LOGGER.info(srcPath + "删除完成");
					} else {
						LOGGER.info(srcPath + "未被删除");
					}
				} catch (IOException e) {
					e.printStackTrace();
					throw new RuntimeException();
				}
			}
		}
	}*/

	public void copyFile(String srcPath, String fileName, String desPath){
		if (srcPath.endsWith(fileSeparator)) {
			srcPath += fileName;
		} else {
			srcPath = srcPath + fileSeparator + fileName;
		}
		File file = new File(srcPath);
		if(file.exists()){
			InputStream inputStream=null;
			OutputStream outputStream=null;
			int reads=0;
			try {
				inputStream=new FileInputStream(file);
				if (desPath.endsWith(fileSeparator)) {
					fileName=System.currentTimeMillis()+fileName;
					desPath += fileName;
				} else {
					fileName=System.currentTimeMillis()+fileName;
					desPath = desPath + fileSeparator + fileName;
				}
				File newFile = new File(desPath);
				newFile.createNewFile();
				outputStream = new FileOutputStream(newFile);
				byte[] buffer=new byte[1024*4];
				while((reads=inputStream.read(buffer))!=-1){
					outputStream.write(buffer, 0, reads);
				}
			} catch (FileNotFoundException e) {
				throw new RuntimeException("找不到文件" + fileName);
			} catch (IOException e) {
				throw new RuntimeException("无法创建新文件"+fileName);
			}finally{
				try {
					outputStream.close();
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (file.delete()) {
					LOGGER.info("["+srcPath + "] 删除完成");
				} else {
					LOGGER.info("["+srcPath + "] 未被删除");
				}
			}
		}
	}
	
	public void writeLogSuccess(int videoID, FTPAccount ftpAccount, String filename,
			String beginTime, String endTime, String filePath,
			Timestamp createDT,String dbPassword) {
//		String password=DBPasswordCenterAgency.GetPassword(dbConfig.getDbSource(), dbConfig.getUserID());
		mssqlDBManager.resetPassword(dbPassword);
		{
			Connection dbConnection=null;
			PreparedStatement localFtpAccountStatement=null;
			ResultSet resultSet=null;
			try {
				dbConnection=mssqlDBManager.getConnection();
				localFtpAccountStatement=dbConnection.prepareStatement(FTPACCOUNT);
				resultSet=localFtpAccountStatement.executeQuery();
				AtomicInteger serverID=new AtomicInteger(2);
				while(resultSet.next()){
					FTPAccount localFtpAccount=new FTPAccount();
					localFtpAccount.setHostname(resultSet.getString("IPAddress"));
					localFtpAccount.setUsername(resultSet.getString("LoginName"));
					localFtpAccount.setPassword(resultSet.getString("Password"));
					localFtpAccount.setDirectory(null);
					ftpAccountMap.put(serverID.toString(), localFtpAccount);
					serverID.getAndIncrement();
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException("无法读取SQL Server数据库");
			}finally{
				JDBCUtils.free(resultSet, localFtpAccountStatement, dbConnection);
			}
		}
		String releaseIP = getReleaseIP(ftpAccount);
		if (releaseIP == null) {
			throw new RuntimeException("配置文件IP地址与FTP帐号关联错误!");
		}
		PreparedStatement preparedStatement = null;
		Connection localmssqlConnection = null;
		try {
			localmssqlConnection = mssqlDBManager.getConnection();
			preparedStatement = localmssqlConnection.prepareStatement(WRITELOG);
			preparedStatement.setInt(1, videoID);
			preparedStatement.setString(2,
					filename + "发送" + ftpAccount.getHostname() + "服务器成功");
			preparedStatement.setString(3, "系统从" + beginTime + "到" + endTime
					+ "将" + filePath + "成功发送至" + ftpAccount.getHostname()
					+ "服务器");
			preparedStatement.setString(4, "发送成功");
			preparedStatement.setString(5, releaseIP);
			preparedStatement.setTimestamp(6, createDT);

			preparedStatement.executeUpdate();
			LOGGER.info(ftpAccount.getHostname() + "上传日志发送成功!");
		} catch (Exception e) {
			throw new RuntimeException(ftpAccount.getHostname()
					+ "无法创建与SQL Server数据库连接!");
		} finally {
			JDBCUtils.free(null, preparedStatement, localmssqlConnection);
		}
	}

	public void writeLogFail(int videoID, FTPAccount ftpAccount, String filename,
			String beginTime, String endTime, String filePath,
			Timestamp createDT,String dbPassword) {
//		String password=DBPasswordCenterAgency.GetPassword(dbConfig.getDbSource(), dbConfig.getUserID());
		mssqlDBManager.resetPassword(dbPassword);
		{
			Connection dbConnection=null;
			PreparedStatement localFtpAccountStatement=null;
			ResultSet resultSet=null;
			try {
				dbConnection=mssqlDBManager.getConnection();
				localFtpAccountStatement=dbConnection.prepareStatement(FTPACCOUNT);
				resultSet=localFtpAccountStatement.executeQuery();
				AtomicInteger serverID=new AtomicInteger(2);
				while(resultSet.next()){
					FTPAccount localFtpAccount=new FTPAccount();
					localFtpAccount.setHostname(resultSet.getString("IPAddress"));
					localFtpAccount.setUsername(resultSet.getString("LoginName"));
					localFtpAccount.setPassword(resultSet.getString("Password"));
					localFtpAccount.setDirectory(null);
					ftpAccountMap.put(serverID.toString(), localFtpAccount);
					serverID.getAndIncrement();
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException("无法读取SQL Server数据库");
			}finally{
				JDBCUtils.free(resultSet, localFtpAccountStatement, dbConnection);
			}
		}
		String releaseIP = getReleaseIP(ftpAccount);
		if (releaseIP == null) {
			throw new RuntimeException("配置文件IP地址与FTP帐号关联错误!");
		}
		PreparedStatement preparedStatement = null;
		Connection localmssqlConnection = null;
		try {
			localmssqlConnection = mssqlDBManager.getConnection();
			preparedStatement = localmssqlConnection.prepareStatement(WRITELOG);
			preparedStatement.setInt(1, videoID);
			preparedStatement.setString(2,
					filename + "发送" + ftpAccount.getHostname() + "服务器失败");
			preparedStatement.setString(3, "系统从" + beginTime + "到" + endTime
					+ "将" + filePath + "失败发送至" + ftpAccount.getHostname()
					+ "服务器");
			preparedStatement.setString(4, "发送失败");
			preparedStatement.setString(5, releaseIP);
			preparedStatement.setTimestamp(6, createDT);

			preparedStatement.executeUpdate();
			LOGGER.info(ftpAccount.getHostname() + "上传日志发送成功!");
		} catch (Exception e) {
			throw new RuntimeException(ftpAccount.getHostname()
					+ "无法创建与SQL Server数据库连接!");
		} finally {
			JDBCUtils.free(null, preparedStatement, localmssqlConnection);
		}
	}
	
	protected void doStop() throws Exception {
		super.doStop();
	}

	public class FTPThread implements Runnable {

		private int videoID;
		private String localThreadPath;
		private FTPAccount ftpAccount;
		private String filename;
		private FTPTask ftpTask;
		private int flagOut;
		private int speed;
		private String dbPassword;
		private SchedualServlet schedualServlet;

		public FTPThread() {

		}

		public FTPThread(int videoID, String localThreadPath,
				FTPAccount ftpAccount, String filename, FTPTask ftptask,int speed,int flagOut,String dbPassword,SchedualServlet schedualServlet) {
			this.videoID = videoID;
			this.localThreadPath = localThreadPath;
			this.ftpAccount = ftpAccount;
			this.filename = filename;
			this.ftpTask = ftptask;
			this.flagOut=flagOut;
			this.speed=speed;
			this.dbPassword=dbPassword;
			this.schedualServlet=schedualServlet;
		}

		@Override
		public void run() {
			File file = new File(localThreadPath);
			PreparedStatement preparedStatement = null;
			ResultSet resultSet = null;
			Connection localDerbyConnection = null;
			try {
				if(flagOut!=0){
					localDerbyConnection = derbyDBManager.getConnection();
				}
				if (!file.exists()) {
					LOGGER.error("传输到" + ftpAccount.getHostname() + ": ["
							+ localThreadPath + "] 此文件不存在!请核查!");
					Date endDate = new Date();
					String endTime = dateFormat.format(endDate);
					Timestamp createDT = new Timestamp(endDate.getTime());
					LOGGER.error(ftpAccount.getHostname()+ "["+filename+"]上传文件不存在,记录写到日志文件中");
					ftpTask.writeLogFail(videoID, ftpAccount,filename, endTime, endTime,
							localThreadPath, createDT,dbPassword);
					// System.out.println(Thread.currentThread().getName()+"传输到"+ftpAccount.getHostname()+":"+localPath+"此文件不存在!请核查!");
				} else {
					//判断当前文件是否传完,(currentTime-创建时间)/1000=间隔时间,length/间隔时间<1024
					long currentTime=System.currentTimeMillis();
					long createTime=getCreateTime(localThreadPath);
					if(createTime==0){
						LOGGER.error("文件读取失败");
					}else{
						double transferTime=1.0*(currentTime-createTime)/1000;
						/*System.out.println("传输速度:"+(file.length()/transferTime));
						System.out.println(speedLimit);*/
						//速度控制的单位是字节数
						if((file.length()/transferTime)<speed){
							if(flagOut!=0){
								try {
									// System.out.println(Thread.currentThread().getName());
									// System.out.println(derbyConnection);
									preparedStatement = localDerbyConnection
											.prepareStatement(COUNTSQL);
									preparedStatement.setInt(1, videoID);
									preparedStatement
											.setString(2, ftpAccount.getHostname());
									resultSet = preparedStatement.executeQuery();

									if (resultSet.next()) {
										int count = resultSet.getInt(1);
										Date beginDate = new Date();
										String beginTime = dateFormat.format(beginDate);
										if (count < 10) {
											switch (FTPUtils.uploadFile(localThreadPath,
													ftpAccount, filename)) {
											case 1: {
												Date endDate = new Date();
												String endTime = dateFormat.format(endDate);
												preparedStatement = localDerbyConnection
														.prepareStatement(STATUSUPDATE);
												preparedStatement.setInt(1, 1);
												preparedStatement.setInt(2, 0);
												preparedStatement.setInt(3, videoID);
												preparedStatement.setString(4,ftpAccount.getHostname());
												preparedStatement.executeUpdate();
												Timestamp createDT = new Timestamp(
														beginDate.getTime());
												LOGGER.info(ftpAccount.getHostname()+"["+filename+"]开始将结果写到日志文件中");
												ftpTask.writeLogSuccess(videoID, ftpAccount,
														filename, beginTime, endTime,
														localThreadPath, createDT,dbPassword);
												break;
											}
											case 2: {
												Date endDate = new Date();
												String endTime = dateFormat.format(endDate);
												preparedStatement = localDerbyConnection
														.prepareStatement(STATUSUPDATE);
												preparedStatement.setInt(1, 1);
												preparedStatement.setInt(2, 0);
												preparedStatement.setInt(3, videoID);
												preparedStatement.setString(4,ftpAccount.getHostname());
												preparedStatement.executeUpdate();
												Timestamp createDT = new Timestamp(
														beginDate.getTime());
												LOGGER.info(ftpAccount.getHostname()+"["+filename+"]断点续传开始将结果写到日志文件中");
												ftpTask.writeLogSuccess(videoID, ftpAccount,
														filename, beginTime, endTime,
														localThreadPath, createDT,dbPassword);
												break;
											}
											case 3: {
												Date endDate = new Date();
												String endTime = dateFormat.format(endDate);
												preparedStatement = localDerbyConnection
														.prepareStatement(STATUSUPDATE);
												preparedStatement.setInt(1, 1);
												preparedStatement.setInt(2, 0);
												preparedStatement.setInt(3, videoID);
												preparedStatement.setString(4,ftpAccount.getHostname());
												preparedStatement.executeUpdate();
												Timestamp createDT = new Timestamp(
														beginDate.getTime());
												LOGGER.info(ftpAccount.getHostname()+"["+filename+"]文件已存在不将结果写到日志文件中");
												ftpTask.writeLogSuccess(videoID, ftpAccount,
														filename, beginTime, endTime,
														localThreadPath, createDT,dbPassword);
												break;
											}
											case 0:{
												Date endDate = new Date();
												String endTime = dateFormat.format(endDate);
												count = count + 1;
												preparedStatement = localDerbyConnection
														.prepareStatement(COUNTUPDATE);
												preparedStatement.setInt(1, count);
												preparedStatement.setInt(2, 0);
												preparedStatement.setInt(3, videoID);
												preparedStatement.setString(4,ftpAccount.getHostname());
												preparedStatement.executeUpdate();
												Timestamp createDT = new Timestamp(
														beginDate.getTime());
												LOGGER.error(ftpAccount.getHostname()+"["+filename+"] 文件上传不成功!计数器增加1");
												ftpTask.writeLogFail(videoID, ftpAccount,filename, beginTime, endTime,
														localThreadPath, createDT,dbPassword);
												break;
											}
											case 4:{
												Date endDate = new Date();
												String endTime = dateFormat.format(endDate);
//												LOGGER.error("获取的值:"+count);
												count = count + 1;
//												LOGGER.error("修改后的值:"+count);
												preparedStatement = localDerbyConnection
														.prepareStatement(COUNTUPDATE);
												preparedStatement.setInt(1, count);
												preparedStatement.setInt(2, 0);
												preparedStatement.setInt(3, videoID);
												preparedStatement.setString(4,ftpAccount.getHostname());
												preparedStatement.executeUpdate();
												Timestamp createDT = new Timestamp(
														beginDate.getTime());
												LOGGER.error(ftpAccount.getHostname()+"["+filename+"] ftp服务器没有连接!文件上传不成功!计数器增加1");
												ftpTask.writeLogFail(videoID, ftpAccount,filename, beginTime, endTime,
														localThreadPath, createDT,dbPassword);
												break;
											}
											case 5: {
												Date endDate = new Date();
												String endTime = dateFormat.format(endDate);
												count = count + 1;
												preparedStatement = localDerbyConnection
														.prepareStatement(COUNTUPDATE);
												preparedStatement.setInt(1, count);
												preparedStatement.setInt(2, 0);
												preparedStatement.setInt(3, videoID);
												preparedStatement.setString(4,ftpAccount.getHostname());
												preparedStatement.executeUpdate();
												Timestamp createDT = new Timestamp(
														beginDate.getTime());
												LOGGER.error(ftpAccount.getHostname()+"["+filename+"] 上传目录没有权限!文件上传不成功!计数器增加1");
												ftpTask.writeLogFail(videoID, ftpAccount,filename, beginTime, endTime,
														localThreadPath, createDT,dbPassword);
											}
											}
										} else {
//											LOGGER.error("进入的值:"+count);
											LOGGER.error(ftpAccount.getHostname() + "邮件预警!");
											Date mailCurrentTime=new Date();
											SimpleDateFormat formatter=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
											String timeString=formatter.format(mailCurrentTime);
											String windEmaiMessage=videoID+"\t"+filename+"\t"+timeString+"\t";
											noticeObj.NoticeMailInfo("邮件预警", windEmaiMessage+"发送到"
													+ ftpAccount.getHostname() + " "
													+ localThreadPath + "失败!");
											LOGGER.error(ftpAccount.getHostname() +"["+filename+"]邮件已发送");
											preparedStatement = localDerbyConnection
													.prepareStatement(COUNTUPDATE);
											preparedStatement.setInt(1, 0);
											preparedStatement.setInt(2, 0);
											preparedStatement.setInt(3, videoID);
											preparedStatement.setString(4,ftpAccount.getHostname());
											preparedStatement.executeUpdate();
										}
									}
								} catch (SocketException e) {
									throw new RuntimeException(ftpAccount.getHostname()
											+ "socket连接出现问题");
								} catch (IOException e) {
									throw new RuntimeException(ftpAccount.getHostname()
											+ "文件无法读取");
								} catch (Exception e) {
									throw new ExceptionInInitializerError(
											ftpAccount.getHostname() + "无法创建derby数据库连接");
								} finally {
									JDBCUtils.free(resultSet, preparedStatement, null);
								}
							}else{
								Date beginDate = new Date();
								String beginTime = dateFormat.format(beginDate);
								switch (FTPUtils.uploadFile(localThreadPath,ftpAccount, filename)){
								case 1:{
									Date endDate = new Date();
									String endTime = dateFormat.format(endDate);
									Timestamp createDT = new Timestamp(beginDate.getTime());
									LOGGER.info(ftpAccount.getHostname()+"["+filename+"] 开始将结果写到日志文件中");
									ftpTask.writeLogSuccess(videoID, ftpAccount,filename, beginTime, endTime,
											localThreadPath, createDT,dbPassword);
									break;
								}
								case 2:{
									Date endDate = new Date();
									String endTime = dateFormat.format(endDate);
									Timestamp createDT = new Timestamp(beginDate.getTime());
									LOGGER.info(ftpAccount.getHostname()+"["+filename+"] 断点续传开始将结果写到日志文件中");
									ftpTask.writeLogSuccess(videoID, ftpAccount,filename, beginTime, endTime,
											localThreadPath, createDT,dbPassword);
									break;
								}
								case 3:{
									Date endDate = new Date();
									String endTime = dateFormat.format(endDate);
									Timestamp createDT = new Timestamp(beginDate.getTime());
									LOGGER.info(ftpAccount.getHostname()+"["+filename+"] 文件已存在将结果写到日志文件中");
									ftpTask.writeLogSuccess(videoID, ftpAccount,filename, beginTime, endTime,
											localThreadPath, createDT,dbPassword);
									break;
								}
								case 0:{
									Date endDate = new Date();
									String endTime = dateFormat.format(endDate);
									Timestamp createDT = new Timestamp(beginDate.getTime());
//									LOGGER.info(ftpAccount.getHostname()+ "文件已存在将结果写到日志文件中");
									ftpTask.writeLogFail(videoID, ftpAccount,filename, beginTime, endTime,
											localThreadPath, createDT,dbPassword);
									LOGGER.error(ftpAccount.getHostname()+"["+filename+"] 文件上传不成功!");
									break;
								}
								case 4:{
									Date endDate = new Date();
									String endTime = dateFormat.format(endDate);
									Timestamp createDT = new Timestamp(beginDate.getTime());
//									LOGGER.info(ftpAccount.getHostname()+ "文件已存在将结果写到日志文件中");
									ftpTask.writeLogFail(videoID, ftpAccount,filename, beginTime, endTime,
											localThreadPath, createDT,dbPassword);
									LOGGER.error(ftpAccount.getHostname()+"["+filename+"] [ftp服务器无法连接]文件上传不成功!");
									break;
								}
								case 5:{
									Date endDate = new Date();
									String endTime = dateFormat.format(endDate);
									Timestamp createDT = new Timestamp(beginDate.getTime());
//									LOGGER.info(ftpAccount.getHostname()+ "文件已存在将结果写到日志文件中");
									ftpTask.writeLogFail(videoID, ftpAccount,filename, beginTime, endTime,
											localThreadPath, createDT,dbPassword);
									LOGGER.error(ftpAccount.getHostname()+"["+filename+"] [目录没有权限]文件上传不成功!");
								}
								}
							}
						}else{
							LOGGER.info(localThreadPath+"目录中的文件未传输完,还无法上传视频!");
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if(flagOut!=0){
					PreparedStatement transferstatus = null;
					try {
						transferstatus = localDerbyConnection.prepareStatement(TRANSFERSTATUS);
						transferstatus.setInt(1, 0);
						transferstatus.setInt(2, videoID);
						transferstatus.setString(3, ftpAccount.getHostname());
						transferstatus.executeUpdate();
					} catch (SQLException e) {
						e.printStackTrace();
					} finally {
						JDBCUtils.free(null, transferstatus, localDerbyConnection);
					}
				}else{
					//回调检查状态
					schedualServlet.writeBack(videoID);
				}
			}
		}
	}

	public DBConfig getDbConfig() {
		return dbConfig;
	}

	public void setDbConfig(DBConfig dbConfig) {
		this.dbConfig = dbConfig;
	}
}
