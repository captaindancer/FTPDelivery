package com.wind.openmeetings.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.wind.openmeeting.deliver.beans.FTPAccount;
import com.wind.openmeeting.deliver.beans.VideoJSON;
import com.wind.openmeeting.deliver.bi.FTPTask;
import com.wind.openmeeting.deliver.utils.DBPasswordCenterAgency;
import com.wind.openmeeting.deliver.utils.JDBCUtils;
import com.windin.ocean.common.sql.DBCon;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class SchedualServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	
	private static final String MSSQLDBCONSTR = "res:///com/wind/openmeeting/deliver/res/mssql.properties";
	private static final String QUERYSCHEDUAL="select VideoID,ShowName,FileName,VideoGUID from dbo.Tb_CollegeVideo where DistributeStatus=?";
	private static final String FTPACCOUNT="select * from dbo.Tb_CollegeVideoServer";
	private static final String VIDEOINFO="select FileName,VideoGUID,ReleaseIP from dbo.Tb_CollegeVideo where VideoID=?";
	private static final String WRITEBACK="update dbo.Tb_CollegeVideo set DistributeStatus=? where VideoID=?";
	private static final String ALLRESULT="select FileName,ReleaseIP from Tb_CollegeVideo where VideoID=?";
	private static final String SUCCESSRESULT="select count(*) from Tb_CollegeVideoReleaseLog where VideoID=? and ReleaseIP=? and ReleaseStatus like '%成功%'";
	private static final String CALLBACKVAD="select count(ReleaseIP) from Tb_CollegeVideoReleaseLog where VideoID=?";
	
	private static ConcurrentHashMap<String, FTPAccount> ftpAccountMap=new ConcurrentHashMap<String, FTPAccount>();
	
	private static DBCon mssqlDBManager=null;
	
	static{
		try {
			mssqlDBManager = DBCon.getInstance(MSSQLDBCONSTR);
		} catch (Exception e) {
			throw new ExceptionInInitializerError("无法加载数据库驱动!");
		}
	}
	
    private static String fileSeparator;
	
    private String dbPassword;
    
	static{
		fileSeparator=System.getProperties().getProperty("file.separator");
	}
	
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String file=getServletConfig().getInitParameter("userConfig");
		Map<String, String> userMap = XmlUtil.readUserList(file);
		HttpSession session = request.getSession();
		String userName=(String) session.getAttribute("name");
		String password=(String) session.getAttribute("password");
		/*System.out.println("username:"+userName);
		System.out.println("password:"+password);*/
		if(userName!=null){
			if(userMap.get(userName).equals(password)){
				passwordServiceInit();
				response.setContentType("application/json");
				response.setCharacterEncoding("GBK");
				PrintWriter out = response.getWriter();
				String keywords = request.getParameter("keywords");
//				System.out.println(keywords);
				if(keywords != null && ! keywords.equalsIgnoreCase("")) {
					schedualTask(keywords);
				}
				JSONArray rows  = showDataTable();
				JSONObject json = new JSONObject(); 
				json.put("rows", rows);
				out.write(json.toString());
//				session.invalidate();
				out.flush();  
				out.close();
			}
		}
	}
	
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		this.doPost(request, response);
	}
	
	public void schedualTask(String keywords) {
//		System.out.println(keywords);
		Connection connection=null;
		PreparedStatement preparedStatement=null;
		ResultSet resultSet=null;
		try {
			connection=mssqlDBManager.getConnection();
			preparedStatement=connection.prepareStatement(FTPACCOUNT);
			resultSet=preparedStatement.executeQuery();
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
		}finally{
			JDBCUtils.free(resultSet, preparedStatement, connection);
		}
		
		if(keywords.contains(",")){
			String[] videoIDs=keywords.split(",");
			for(String videoID_String:videoIDs){
				int videoID=Integer.parseInt(videoID_String.trim());
				Connection transferConnection=null;
				PreparedStatement transferStatement=null;
				try {
					transferConnection=mssqlDBManager.getConnection();
					transferStatement=transferConnection.prepareStatement(WRITEBACK);
					transferStatement.setInt(1, 2);
					transferStatement.setInt(2, videoID);
					System.out.println("影响了"+videoID+":"+transferStatement.executeUpdate());;
				} catch (Exception e) {
					e.printStackTrace();
				}finally{
					JDBCUtils.free(null, transferStatement, transferConnection);
				}
				doTaskSchedual(videoID);
				writeBack(videoID);
			}
			
		}else{
			int videoID=Integer.parseInt(keywords);
			Connection transferConnection=null;
			PreparedStatement transferStatement=null;
			try {
				transferConnection=mssqlDBManager.getConnection();
				transferStatement=transferConnection.prepareStatement(WRITEBACK);
				transferStatement.setInt(1, 2);
				transferStatement.setInt(2, videoID);
//				System.out.println("影响了"+videoID+":"+transferStatement.executeUpdate());;
			} catch (Exception e) {
				e.printStackTrace();
			}finally{
				JDBCUtils.free(null, transferStatement, transferConnection);
			}
			doTaskSchedual(videoID);
//			writeBack(videoID);
		}
	}
	
	private void doTaskSchedual(int videoID){
		int threadNumber=Integer.parseInt(getServletConfig().getInitParameter("threadNumber"));
		
		ExecutorService executorService = Executors.newFixedThreadPool(threadNumber);
		
		String uploadPath=getServletConfig().getInitParameter("uploadPath");
		String fileName=null;
		String releaseIPs=null;
		Connection connection=null;
		PreparedStatement preparedStatement=null;
		ResultSet resultSet=null;
		FTPTask ftpTask=new FTPTask();
		try {
			connection=mssqlDBManager.getConnection();
			preparedStatement=connection.prepareStatement(VIDEOINFO);
			preparedStatement.setInt(1, videoID);
			resultSet=preparedStatement.executeQuery();
			String videoGUID=null;
			if(resultSet.next()){
				fileName=resultSet.getString("FileName");
				videoGUID=resultSet.getString("VideoGUID");
				releaseIPs=resultSet.getString("ReleaseIP");
			}
			if(!uploadPath.contains(fileSeparator)){
				throw new RuntimeException("配置文件中文件路径格式错误");
			}
			if(uploadPath.endsWith(fileSeparator)){
				uploadPath+=fileName;
			}else{
				uploadPath=uploadPath+fileSeparator+fileName;
			}
			String[] releaseIP=releaseIPs.split(",");
			int speedLimit=Integer.parseInt(getServletConfig().getInitParameter("speedLimit"));
			for(String ip:releaseIP){
				executorService.execute(ftpTask.new FTPThread(videoID, uploadPath, ftpAccountMap.get(ip), videoGUID, ftpTask,speedLimit,0,dbPassword,this));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			JDBCUtils.free(resultSet, preparedStatement, connection);
		}
	}
	
	public void writeBack(int videoID){
		Connection connection=null;
		PreparedStatement preparedStatement=null;
		PreparedStatement allResult=null;
		PreparedStatement successResult=null;
		ResultSet allResultSet=null;
		ResultSet successResultSet=null;
		String releaseIPs=null;
		int successCount=0;
		FTPTask ftpTask=new FTPTask();
		String fileName=null;
		PreparedStatement callbackVadStatement=null;
		ResultSet callbackVadResultSet=null;
		try {
			connection=mssqlDBManager.getConnection();
			preparedStatement=connection.prepareStatement(WRITEBACK);
			preparedStatement.setInt(2, videoID);
			allResult=connection.prepareStatement(ALLRESULT);
			allResult.setInt(1, videoID);
			successResult=connection.prepareStatement(SUCCESSRESULT);
			allResultSet=allResult.executeQuery();
			if(allResultSet.next()){
				releaseIPs=allResultSet.getString("ReleaseIP");
				fileName=allResultSet.getString("FileName");
//				System.out.println(releaseIPs);
			}
			
			callbackVadStatement=connection.prepareStatement(CALLBACKVAD);
			callbackVadStatement.setInt(1, videoID);
			callbackVadResultSet=callbackVadStatement.executeQuery();
			int callbackCount=0;
			if(callbackVadResultSet.next()){
				callbackCount=callbackVadResultSet.getInt(1);
			}
			int flag=0;
			if(releaseIPs!=null){
				String[] releaseIP=releaseIPs.split(",");
//				System.out.println(releaseIP.length);
				for(String ip:releaseIP){
					successResult.setInt(1, videoID);
					successResult.setString(2, ip.trim());
					successResultSet=successResult.executeQuery();
					if(successResultSet.next()){
						successCount=successResultSet.getInt(1);
					}
					if(successCount>=1){
						flag++;
					}
				}
				if(flag==releaseIP.length){
					preparedStatement.setInt(1, 1);
					preparedStatement.executeUpdate();
					ftpTask.copyFile(getServletConfig().getInitParameter("uploadPath"), fileName, getServletConfig().getInitParameter("copyPath"));
				}else{
					if((callbackCount%releaseIP.length)==0){
						preparedStatement.setInt(1, 0);
						preparedStatement.executeUpdate();
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			JDBCUtils.free(allResultSet, allResult, null);
			JDBCUtils.free(successResultSet, successResult, null);
			JDBCUtils.free(callbackVadResultSet, callbackVadStatement, null);
			JDBCUtils.free(null, preparedStatement, connection);
		}
	}
	
	private void passwordServiceInit(){
		/*String dbSource=getServletConfig().getInitParameter("dbSource");
		String userID=getServletConfig().getInitParameter("userID");
		dbPassword=DBPasswordCenterAgency.GetPassword(dbSource, userID);
		mssqlDBManager.resetPassword(dbPassword);*/
		dbPassword=null;
		Runtime runtime=Runtime.getRuntime();
		BufferedReader reader=null;
		Process process=null;
		String passwordService=getServletConfig().getInitParameter("passwordService");
		try {
			process=runtime.exec(passwordService);
			reader=new BufferedReader(new InputStreamReader(process.getInputStream()));
			dbPassword=reader.readLine();
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			try {
				reader.close();
				process.getOutputStream().close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		mssqlDBManager.resetPassword(dbPassword);
	}
	
	@SuppressWarnings("finally")
	public JSONArray showDataTable() {
		JSONArray rows = null;
		List<VideoJSON>	videoList = new ArrayList<VideoJSON>();
		
		Connection connection=null;
		PreparedStatement preparedStatement=null;
		ResultSet resultSet=null;
		
		try {
			connection=mssqlDBManager.getConnection();
			preparedStatement=connection.prepareStatement(QUERYSCHEDUAL);
			preparedStatement.setInt(1, 0);
			resultSet=preparedStatement.executeQuery();
			while(resultSet.next()){
				VideoJSON json=new VideoJSON();
				json.setVideoID(resultSet.getInt("VideoID"));
				json.setShowName(resultSet.getString("ShowName"));
				json.setFileName(resultSet.getString("FileName"));
				json.setVideoGUID(resultSet.getString("VideoGUID"));
				videoList.add(json);
			}
			rows=JSONArray.fromObject(videoList);
		} catch (Exception e1) {
			e1.printStackTrace();
		}finally{
			JDBCUtils.free(resultSet, preparedStatement, connection);
			return rows;
		}
		
	} 
}
