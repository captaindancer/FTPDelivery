package com.wind.openmeeting.task;

import java.util.Calendar;
import java.util.List;
import java.util.Vector;

 
public class TimeSchema {
	private List<Integer> _minutePolicy = new Vector<Integer>();
	private List<Integer> _hourPolicy = new Vector<Integer>();
	private List<Integer> _dayPolicy = new Vector<Integer>();
	private List<Integer> _monthPolicy = new Vector<Integer>();
	private List<Integer> _weekPolicy = new Vector<Integer>();
	private String _schemaStr;
	
	public TimeSchema(String schema) throws TimeSchemaInvalidException{
		_schemaStr = schema;
		splitSchema(_schemaStr);
	}

	public String get_schemaStr() {
		return _schemaStr;
	}
	
	private void splitSchema(String schema) throws TimeSchemaInvalidException{
		String[] timeShards = schema.split("\\s{1,}"); 
		if (timeShards.length!=5) throw new TimeSchemaInvalidException("格式不正确，必须有5个定义部分，不同部分中使用空格分割");
		String minuteStr = timeShards[0];
		String hourStr = timeShards[1];
		String dayStr = timeShards[2];
		String monthStr = timeShards[3];
		String weekStr = timeShards[4];
		checkPartFormat(minuteStr,"分钟",0,59,_minutePolicy);
		checkPartFormat(hourStr,"小时",0,23,_hourPolicy);
		checkPartFormat(dayStr,"日期",1,31,_dayPolicy);
		checkPartFormat(monthStr,"月份",0,12,_monthPolicy);
		checkPartFormat(weekStr,"星期",0,7,_weekPolicy);
	}
	
	public boolean taskTimeisActive(Calendar date){
		if (!_monthPolicy.contains(date.get(Calendar.MONTH))) return false;
		if (!_dayPolicy.contains(date.get(Calendar.DAY_OF_MONTH))) return false;
		if (!_weekPolicy.contains(date.get(Calendar.DAY_OF_WEEK))) return false;
		if (!_hourPolicy.contains(date.get(Calendar.HOUR_OF_DAY))) return false;
		if (!_minutePolicy.contains(date.get(Calendar.MINUTE))) return false;
		return true;
	}
	
	private void addData(int begin,int end,List<Integer> policy){
		for (int i=begin;i<=end;i++){
			policy.add(i);
		}
	}
	private void checkPartFormat(String source,String sourceType,int begin,int end,List<Integer> policy) throws TimeSchemaInvalidException{
		String[] temp;
		if ("*".equalsIgnoreCase(source)){
			addData(begin,end,policy);
			return;
		}
		temp = source.split("/|-|,");
		if (temp.length==1){
			//简单时间点类型
			try{
				policy.add(Integer.parseInt(temp[0]));
			}catch(NumberFormatException e){
				throw new TimeSchemaInvalidException(sourceType+"当前使用格式不正确，应该如0这种简单类型");
			}
		}
		if (source.startsWith("*/")){
			temp = source.split("/");
			if (temp.length != 2) throw new TimeSchemaInvalidException(sourceType+"当前使用的格式不正确，应该如*/1类型");
			try{
				int step = Integer.parseInt(temp[1]);
				for(int i=begin;i<=end;){
					i=i+step;
					policy.add(i);
				}
				return;
			}catch(Exception e){
				throw new TimeSchemaInvalidException(sourceType+"当前使用的格式不正确，应该如*/1类型");
			}
		}
		temp=source.split("-");
		if (temp.length==2){
			//简单时间范围
			try{
				int lbegin = Integer.parseInt(temp[0]);
				int lend = Integer.parseInt(temp[1]);
				if (lend<=lbegin) throw new TimeSchemaInvalidException(sourceType+"时间范围相等");
				if (lbegin<begin || lend>end) throw new TimeSchemaInvalidException(sourceType+"时间范围超出边界");
				addData(lbegin,lend,policy);
				return;
			}catch(Exception e){
				throw new TimeSchemaInvalidException(sourceType+"当前使用的格式不正确，应该如1-2类型");
			}
		}
		temp = source.split(",");
		if (temp.length!=0){
			try{
				for (int i=0;i<temp.length;i++){
					policy.add(Integer.parseInt(temp[i]));
				}
				return;
			}catch(Exception e){
				throw new TimeSchemaInvalidException(sourceType+"当前使用格式不正确，应该如1,2,4类型");
			}
		}
		throw new TimeSchemaInvalidException(sourceType+"当前使用格式不能识别，当前的格式："+source);
	}
	
}
