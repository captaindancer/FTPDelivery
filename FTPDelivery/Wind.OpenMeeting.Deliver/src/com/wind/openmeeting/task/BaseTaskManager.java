package com.wind.openmeeting.task;

import java.util.Calendar;
import java.util.Hashtable;
import java.util.Map.Entry;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public abstract class BaseTaskManager {
	private final int _refrashInterval = 1000 * 60 * 5;
	private final int _checkInterval = 1000* 60;
	private Hashtable<TimeSchema,IBuilder> _tasks = null;
	private Hashtable<String,Thread> _runingTaskList = new Hashtable<String,Thread>();
	private Thread _checkTaskThread;
	private Thread _refrashTaskThread;
	private boolean terminated = false;
	
	private static Object _lockObj = new Object();
	
	private Logger loger = LogManager.getLogger("BaseTaskManager");
	
	public BaseTaskManager(){
		_refrashTaskThread = new RefrashDeclareThread();
		_refrashTaskThread.start();
		_checkTaskThread = new CheckTaskThread();
		_checkTaskThread.start();
	}
	
	public boolean isTerminated() {
		return terminated;
	}

	public void setTerminated(boolean terminated) {
		this.terminated = terminated;
		if (terminated){
			synchronized(_refrashTaskThread){
				_refrashTaskThread.notify();
			}
			synchronized(_checkTaskThread){
				_checkTaskThread.notify();
			}
		}
	}


	/**
	 * 构建或者刷新计划任务列表
	 * @return 计划任务列表，IBuilder：表示执行计划任务的实现，String：为计划任务调度字符串，按照Linux下crontab的格式
	 */
	protected abstract Hashtable<IBuilder,String> refrashTaskDeclare();

	private class RefrashDeclareThread extends Thread{
		
		@Override
		public void run() {
			while(!terminated){
				Hashtable<IBuilder,String> taskDeclares = refrashTaskDeclare();
				convertTaskDeclare(taskDeclares);
				try {
					synchronized(this){
						this.wait(_refrashInterval);
						//System.out.println("刷新线程等待退出");
					}
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}	
		private void convertTaskDeclare(Hashtable<IBuilder,String> taskDeclareList) {
			Hashtable<TimeSchema,IBuilder> tempTasks = new Hashtable<TimeSchema,IBuilder>();
			for(Entry<IBuilder,String> entry :taskDeclareList.entrySet()){
				try {
					tempTasks.put(new TimeSchema(entry.getValue()), entry.getKey());
				} catch (TimeSchemaInvalidException e) {
					loger.error("计划任务时间调度格式不正确。",e);
				}
			}
			_tasks = tempTasks;
		}
	}
	
	
	
	private class CheckTaskThread extends Thread{
		
		@Override
		public void run() {
			while(!terminated){
				Calendar begin = Calendar.getInstance();
				if (_tasks !=null){
					for(Entry<TimeSchema,IBuilder> task : _tasks.entrySet()){
						String taskID = task.getValue().getTaskID();
						loger.debug(task.getKey().get_schemaStr());
						if (task.getKey().taskTimeisActive(Calendar.getInstance())){
							loger.debug(taskID+" in schedule time");
							synchronized(_lockObj){
								if (!_runingTaskList.containsKey(taskID)){
									loger.debug(taskID+" not in running threads");
									Thread taskThread= new TaskRunThread(task.getValue());
									_runingTaskList.put(taskID, taskThread);
									taskThread.start();								
								}
							}
						}
					}
				}
				Calendar end = Calendar.getInstance();
				long interval = end.getTimeInMillis() - begin.getTimeInMillis();
				try {
					synchronized(this){
						this.wait( _checkInterval -interval);
						//System.out.println("任务检查线程等待退出");
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}			
		}		
	}
	
	private class TaskRunThread extends Thread{
		private IBuilder intf ;
		public TaskRunThread(IBuilder intf){
			this.intf = intf;
		}
		@Override
		public void run() {
			if (null==intf){
				loger.debug("IBuilder is null,quit the thread.");
				loger.error("任务执行线程参数不能转换到IBuilder类型的接口，或者传递的任务执行接口为null");
			}else{
				loger.debug("task thread begin run");
				StringBuffer errInfo = new StringBuffer();
				intf.build(errInfo);
				if (errInfo.length()>0){
					loger.error(errInfo.toString());
				}
			}
			synchronized(_lockObj){
				_runingTaskList.remove(intf.getTaskID());
			}
		}		
	}
}
