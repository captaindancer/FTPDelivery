package com.wind.openmeeting.deliver.log;

import com.wind.eagle.log4w.declare.ProcessInfo;
import com.wind.eagle.log4w.impl.AbstractPerfDataListener;

public class PerfDataListener extends  AbstractPerfDataListener{

	public String convertPerfName(String innerName) {
		return super.convertPerfName(innerName);
	}

	public void fillAppPerfData(ProcessInfo perfData) {
		super.fillAppPerfData(perfData);
	}

}
