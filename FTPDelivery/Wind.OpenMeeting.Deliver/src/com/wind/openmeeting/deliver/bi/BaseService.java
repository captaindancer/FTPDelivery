package com.wind.openmeeting.deliver.bi;

import com.wind.openmeeting.deliver.EagleParamter;
import com.windin.component.AbstractLifeCycle;
import com.windin.component.InfoNotice;

public abstract class BaseService extends AbstractLifeCycle{
	protected EagleParamter eagleObj;
	protected InfoNotice     noticeObj;

	public void setEagleObj(EagleParamter eagleObj) {
		this.eagleObj = eagleObj;
	}

	public void setNoticeObj(InfoNotice noticeObj) {
		this.noticeObj = noticeObj;
	}
}
