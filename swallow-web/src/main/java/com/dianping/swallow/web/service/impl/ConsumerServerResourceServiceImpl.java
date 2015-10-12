package com.dianping.swallow.web.service.impl;

import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.dianping.lion.client.ConfigCache;
import com.dianping.lion.client.LionException;
import com.dianping.swallow.web.common.Pair;
import com.dianping.swallow.web.dao.ConsumerServerResourceDao;
import com.dianping.swallow.web.model.alarm.QPSAlarmSetting;
import com.dianping.swallow.web.model.resource.ConsumerServerResource;
import com.dianping.swallow.web.model.resource.ServerResource;
import com.dianping.swallow.web.model.resource.ServerType;
import com.dianping.swallow.web.service.AbstractSwallowService;
import com.dianping.swallow.web.service.ConsumerServerResourceService;
import com.dianping.swallow.web.service.ConsumerServerStatsDataService;
import com.dianping.swallow.web.service.TopicResourceService;
import com.dianping.swallow.web.util.ResponseStatus;

/**
 * @author mingdongli
 *
 *         2015年8月10日下午6:25:07
 */
@Service("consumerServerResourceService")
public class ConsumerServerResourceServiceImpl extends AbstractSwallowService implements ConsumerServerResourceService {

	private static final String BLANK_STRING = "";

	public static final String SWALLOW_CONSUMER_SERVER_URI = "swallow.consumer.consumerServerURI";

	@Autowired
	private ConsumerServerResourceDao consumerServerResourceDao;

	@Resource(name = "topicResourceService")
	private TopicResourceService topicResourceService;

	@Resource(name = "consumerServerStatsDataService")
	private ConsumerServerStatsDataService consumerServerStatsDataService;

	private ConfigCache configCache;

	private String consumerServerLionConfig;
	
	@Override
	protected void doInitialize() throws Exception {
		try {
			configCache = ConfigCache.getInstance();

			consumerServerLionConfig = configCache.getProperty(SWALLOW_CONSUMER_SERVER_URI);
			configCache.addChange(this);
			logger.info("Init configCache successfully.");
		} catch (LionException e) {
			logger.error("Erroe when init lion config", e);
		}
	}

	@Override
	public boolean insert(ConsumerServerResource consumerServerResource) {

		return consumerServerResourceDao.insert(consumerServerResource);
	}

	@Override
	public boolean update(ConsumerServerResource consumerServerResource) {

		return consumerServerResourceDao.update(consumerServerResource);
	}

	@Override
	public int remove(String ip) {

		return consumerServerResourceDao.remove(ip);
	}

	@Override
	public Pair<Long, List<ConsumerServerResource>> findConsumerServerResourcePage(int offset, int limit) {

		return consumerServerResourceDao.findConsumerServerResourcePage(offset, limit);
	}

	@Override
	public ServerResource findByIp(String ip) {

		return consumerServerResourceDao.findByIp(ip);
	}

	@Override
	public ServerResource findByHostname(String hostname) {

		return consumerServerResourceDao.findByHostname(hostname);
	}

	@Override
	public List<ConsumerServerResource> findByGroupId(long groupId) {

		return consumerServerResourceDao.findByGroupId(groupId);
	}

	@Override
	public ServerResource findDefault() {

		return consumerServerResourceDao.findDefault();
	}

	@Override
	public List<ConsumerServerResource> findAll() {

		return consumerServerResourceDao.findAll();
	}

	@Override
	public ConsumerServerResource buildConsumerServerResource(String ip, String hostName, int port, int groupId,
			ServerType serverType) {
		ConsumerServerResource serverResource = new ConsumerServerResource();
		serverResource.setIp(ip);
		serverResource.setAlarm(true);
		serverResource.setHostname(hostName);
		serverResource.setPort(port);
		serverResource.setGroupId(groupId);
		serverResource.setType(serverType);
		serverResource.setCreateTime(new Date());
		serverResource.setUpdateTime(new Date());
		ConsumerServerResource defaultResource = (ConsumerServerResource) findDefault();
		if (defaultResource == null) {
			serverResource.setAlarm(false);
			serverResource.setSendAlarmSetting(new QPSAlarmSetting());
			serverResource.setAckAlarmSetting(new QPSAlarmSetting());
		} else {
			serverResource.setSendAlarmSetting(defaultResource.getSendAlarmSetting());
			serverResource.setAckAlarmSetting(defaultResource.getAckAlarmSetting());
		}
		return serverResource;
	}

	@Override
	public ConsumerServerResource buildConsumerServerResource(String ip, int groupId) {
		ConsumerServerResource serverResource = new ConsumerServerResource();
		serverResource.setIp(ip);
		serverResource.setAlarm(true);
		serverResource.setCreateTime(new Date());
		serverResource.setUpdateTime(new Date());
		serverResource.setGroupId(groupId);
		ConsumerServerResource defaultResource = (ConsumerServerResource) findDefault();
		if (defaultResource == null) {
			serverResource.setAlarm(false);
			serverResource.setSendAlarmSetting(new QPSAlarmSetting());
			serverResource.setAckAlarmSetting(new QPSAlarmSetting());
		} else {
			serverResource.setSendAlarmSetting(defaultResource.getSendAlarmSetting());
			serverResource.setAckAlarmSetting(defaultResource.getAckAlarmSetting());
		}
		return serverResource;
	}

	@Override
	public Pair<String, ResponseStatus> loadIdleConsumerServer() {

		ConsumerServerResource consumerServerResource = consumerServerResourceDao.loadIdleConsumerServer();

		if (consumerServerResource == null) {
			return new Pair<String, ResponseStatus>(BLANK_STRING, ResponseStatus.NOCONSUMERSERVER);
		}
		long groupId = consumerServerResource.getGroupId();
		List<ConsumerServerResource> consumerServerResourceList = consumerServerResourceDao.findByGroupId(groupId);
		if (consumerServerResourceList.size() < 2) {
			return new Pair<String, ResponseStatus>(StringUtils.EMPTY, ResponseStatus.NOCONSUMERSERVER);
		}
		String masterIp = consumerServerResource.getIp();
		int masterPort = consumerServerResource.getPort();
		if (!validateIpPort(masterIp, masterPort)) {
			return new Pair<String, ResponseStatus>(StringUtils.EMPTY, ResponseStatus.NOCONSUMERSERVER);
		}
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(masterIp).append(":").append(masterPort);
		for (ConsumerServerResource csr : consumerServerResourceList) {
			ServerType serverType = csr.getType();

			if (ServerType.MASTER == serverType) {
				if (!masterIp.equals(csr.getIp())) {
					return new Pair<String, ResponseStatus>(StringUtils.EMPTY, ResponseStatus.NOCONSUMERSERVER);
				}
			} else {
				String slaveIp = csr.getIp();
				int slavePort = csr.getPort();

				if (!validateIpPort(slaveIp, slavePort)) {
					return new Pair<String, ResponseStatus>(StringUtils.EMPTY, ResponseStatus.NOCONSUMERSERVER);
				} else {
					stringBuilder.append(",").append(slaveIp).append(":").append(slavePort);
				}

			}
		}

		return new Pair<String, ResponseStatus>(stringBuilder.toString(), ResponseStatus.SUCCESS);
	}

	@Override
	public String loadConsumerServerLionConfig() {

		return consumerServerLionConfig;
	}

	@Override
	public synchronized void setConsumerServerLionConfig(String consumerServerLionConfig) {

		this.consumerServerLionConfig = consumerServerLionConfig;
	}

	@Override
	public void onChange(String key, String value) {

		if (key != null && key.equals(SWALLOW_CONSUMER_SERVER_URI)) {
			if (logger.isInfoEnabled()) {
				logger.info("[onChange][" + SWALLOW_CONSUMER_SERVER_URI + "]" + value);
			}
			this.consumerServerLionConfig = value.trim();
		} else {
			if (logger.isInfoEnabled()) {
				logger.info("not match");
			}
		}

	}

	private boolean validateIpPort(String masterIp, int masterPort) {

		return StringUtils.isNotBlank(masterIp) && masterPort > 0;
	}

	@Override
	public int getNextGroupId() {
		return consumerServerResourceDao.getMaxGroupId() + 1;
	}
}
