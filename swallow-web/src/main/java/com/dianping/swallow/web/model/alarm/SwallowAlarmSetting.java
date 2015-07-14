package com.dianping.swallow.web.model.alarm;

import java.util.List;

public class SwallowAlarmSetting extends BaseAlarmSetting{
	
	private String swallowId;
	
	private List<String> producerWhiteList;
	
	private List<String> consumerWhiteList;

	public List<String> getProducerWhiteList() {
		return producerWhiteList;
	}

	public void setProducerWhiteList(List<String> producerWhiteList) {
		this.producerWhiteList = producerWhiteList;
	}

	public List<String> getConsumerWhiteList() {
		return consumerWhiteList;
	}

	public void setConsumerWhiteList(List<String> consumerWhiteList) {
		this.consumerWhiteList = consumerWhiteList;
	}

	public String getSwallowId() {
		return swallowId;
	}

	public void setSwallowId(String swallowId) {
		this.swallowId = swallowId;
	}

}