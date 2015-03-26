package com.wind.openmeeting.deliver.db;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.windin.ocean.common.sql.DBAccess;
import com.windin.ocean.common.sql.DBCon;


public class DBInit {
	private Logger log= Logger.getLogger(DBInit.class);
	private static final String DBCONSTR="res:///com/wind/openmeeting/deliver/res/derby.properties";
	//private static final String DBCONSTR="file:///home/liufeng/Wind.OpenMeeting/Wind.OpenMeeting.res/src/com/wind/openmeeting/deliver/res/derby.properties";
	
	
	/*private final String ut_deploytaskSql="create table ut_deploytask ("
		+" deployid	   varchar(40) not null, "
		+" deployname	varchar(500),"
		+" rootpath		varchar(500),"
		+" createdate	timestamp,"
		+" runflag		int,"
		+" backuped    int default 0,"
		+" rollbackfile  varchar(2048) default '' ,"
		+" retrytime	int	default 0,"
		+" primary key (deployid) )";
	private final String ut_deploydeclareSql="create table ut_deploydeclare	("
			//+"	   vid                  int not null generated always as identity (start with 1,increment by 1),"
			+"	   deployid             varchar(40) not null,"
			+"	   vname                varchar(50) not null,"
			+"	   value                varchar(2048),"
			+"	   primary key (deployid,vname)	)";
	
	private final String ut_taskdetailSql="	create table ut_taskdetail	( "
			//+"	   taskid               int not null generated always as identity (start with 1,increment by 1),"
			+"	   deployid             varchar(40) not null,"
			+"	   taskname             varchar(40) not null,"
			+"	   type                 varchar(40) not null,"
			+"	   needrun              int ,"
			+"	   needbackup           int,"
			+"	   runonce              int,"
			+"     addon               varchar(1024),"
			+"	   errinfo              varchar(4096),"
			+"	   runflag              int ,"
			+"	   commandid			int ,"
			+"	   remark               varchar(1024),"
			+"	   primary key (deployid,taskname,type)	)";*/
	
	private final String ut_videostatusSql="create table ut_videostatus("
			+"id int not null generated always as identity (start with 1,increment by 1),"
			+"VideoID int not null,"
			+"IPAddress varchar(25) not null,"
			+"FTPUserID varchar(50) not null,"
			+"FTPPassword varchar(128) not null,"
			+"Directory varchar(1024),"
			+"FileName varchar(300) not null,"
			+"VideoGUID varchar(100) not null,"
			+"WriteBack int default 0,"
			+"TransferStatus int default 0,"
			+"Status int default 0,"
			+"Count int default 0,"
			+"primary key (id) )";
	
	public static String getDbconstr() {
		return DBCONSTR;
	}


	public DBInit() throws Exception{
		setDBSystemDir();
		init();
	}
	
	
	public DBInit(String dbRootPath) throws Exception{
		System.setProperty("derby.system.home", dbRootPath);
		File fileSystemDir = new File(dbRootPath);
		if (!fileSystemDir.exists())	fileSystemDir.mkdir();		
		init();
	}
	private void setDBSystemDir() {
		// decide on the db system directory
		// String userHomeDir = System.getProperty("user.home", ".");
		String userHomeDir = System.getProperty("user.dir", ".");
		String systemDir = userHomeDir + System.getProperty("file.separator")+ "db";
		System.setProperty("derby.system.home", systemDir);

		// create the db system directory
		File fileSystemDir = new File(systemDir);
		if (!fileSystemDir.exists())	fileSystemDir.mkdir();
	}
	
	private void init() throws Exception{
		String[] names ={"TABLE"};
		DBCon dbManager = DBCon.getInstance(DBCONSTR);
		//DBCon dbManager = DBCon.getInstance("res:///com/wind/deploy/res/am.properties");
		Connection conn = dbManager.getConnection();		
		DatabaseMetaData metadata = conn.getMetaData();
		ResultSet result =metadata.getTables(null,null,null,names);
		List<String> tableNameList = new ArrayList<String>();
		while(result.next()){
			tableNameList.add(result.getString("TABLE_NAME"));
		}
		/*if (!tableExists(tableNameList, "ut_deploytask"))  DBAccess.updateData(DBCONSTR, ut_deploytaskSql);
		if (!tableExists(tableNameList, "ut_deploydeclare"))  DBAccess.updateData(DBCONSTR, ut_deploydeclareSql);
		if (!tableExists(tableNameList, "ut_taskdetail"))  DBAccess.updateData(DBCONSTR, ut_taskdetailSql);*/
		if(!tableExists(tableNameList, "ut_videostatus")) 
			DBAccess.updateData(DBCONSTR, ut_videostatusSql);
		printTableNames(tableNameList);
	}
	
	private boolean tableExists(List<String> tableNames,String tableName){
		for (String name : tableNames){
			if (tableName.equalsIgnoreCase(name)) return true;
		}
		return false;
	}
	
	private void printTableNames(List<String> tableNames){
		for(String tableName : tableNames) log.info("嵌入式数据库中存在数据表："+tableName);
	}
}
