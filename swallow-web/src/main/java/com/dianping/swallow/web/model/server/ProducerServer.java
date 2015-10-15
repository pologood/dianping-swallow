package com.dianping.swallow.web.model.server;


import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dianping.swallow.web.service.HttpService.HttpResult;

public class ProducerServer extends Server {

	private String pigeonHealthUrl;

	public ProducerServer() {

	}
	
	public ProducerServer(String ip){
		this.setIp(ip);
		pigeonHealthUrl = StringUtils.replace(serverConfig.getPigeonHealthUrl(), "{ip}", ip);
	}

	@Override
	public void doAlarm() {
		
	}
	
	private boolean checkService(){
		HttpResult httpResult = requestUrl(pigeonHealthUrl);
		return httpResult.isSuccess();
	}
	

}
