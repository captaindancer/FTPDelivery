package com.wind.openmeeting.task;

public interface IBuilder {
	String getTaskID();
	void build();
	void build(StringBuffer errInfo);
}
