package com.dianping.swallow.web.model;

import org.springframework.data.annotation.Id;


/**
 * @author mingdongli
 *
 * 2015年6月24日下午1:43:19
 */
public class MessageDump {
	
	@Id
	private String _id;
	private String topic;
	private String name;
	private String time;
	private String startdt;
	private String stopdt;
	private String filename;
	private boolean finished;
	private String desc;
	
	public MessageDump(){
		
	}
	
	public String get_id() {
		return _id;
	}
	
	public MessageDump set_id(String _id) {
		this._id = _id;
		return this;
	}
	
	public String getTopic() {
		return topic;
	}
	
	public MessageDump setTopic(String topic) {
		this.topic = topic;
		return this;
	}
	
	public String getName() {
		return name;
	}
	
	public MessageDump setName(String name) {
		this.name = name;
		return this;
	}
	
	public String getTime() {
		return time;
	}
	
	public MessageDump setTime(String time) {
		this.time = time;
		return this;
	}
	
	public String getStartdt() {
		return startdt;
	}
	
	public MessageDump setStartdt(String startdt) {
		this.startdt = startdt;
		return this;
	}
	
	public String getStopdt() {
		return stopdt;
	}
	
	public MessageDump setStopdt(String stopdt) {
		this.stopdt = stopdt;
		return this;
	}
	
	public String getFilename() {
		return filename;
	}

	public MessageDump setFilename(String filename) {
		this.filename = filename;
		return this;
	}
	
	public boolean isFinished() {
		return finished;
	}

	public MessageDump setFinished(boolean finished) {
		this.finished = finished;
		return this;
	}
	
	public String getDesc() {
		return desc;
	}

	public MessageDump setDesc(String desc) {
		this.desc = desc;
		return this;
	}

	@Override
	public String toString() {
		return "MessageDump [_id=" + _id + ", topic=" + topic + ", name=" + name + ", time=" + time + ", startdt="
				+ startdt + ", stopdt=" + stopdt + ", filename=" + filename + ", finished=" + finished + ", desc="
				+ desc + "]";
	}

}