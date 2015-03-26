package com.wind.openmeeting.deliver.beans;

public class VideoJSON implements Cloneable {
	
	private int videoID;
	private String showName;
	private String fileName;
	private String videoGUID;
	public int getVideoID() {
		return videoID;
	}
	public void setVideoID(int videoID) {
		this.videoID = videoID;
	}
	public String getShowName() {
		return showName;
	}
	public void setShowName(String showName) {
		this.showName = showName;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getVideoGUID() {
		return videoGUID;
	}
	public void setVideoGUID(String videoGUID) {
		this.videoGUID = videoGUID;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((fileName == null) ? 0 : fileName.hashCode());
		result = prime * result
				+ ((showName == null) ? 0 : showName.hashCode());
		result = prime * result
				+ ((videoGUID == null) ? 0 : videoGUID.hashCode());
		result = prime * result + videoID;
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		VideoJSON other = (VideoJSON) obj;
		if (fileName == null) {
			if (other.fileName != null)
				return false;
		} else if (!fileName.equals(other.fileName))
			return false;
		if (showName == null) {
			if (other.showName != null)
				return false;
		} else if (!showName.equals(other.showName))
			return false;
		if (videoGUID == null) {
			if (other.videoGUID != null)
				return false;
		} else if (!videoGUID.equals(other.videoGUID))
			return false;
		if (videoID != other.videoID)
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return "VideoJSON [videoID=" + videoID + ", showName=" + showName
				+ ", fileName=" + fileName + ", videoGUID=" + videoGUID + "]";
	}
	@Override
	protected Object clone() throws CloneNotSupportedException {
		return (VideoJSON)super.clone();
	}
	
}
