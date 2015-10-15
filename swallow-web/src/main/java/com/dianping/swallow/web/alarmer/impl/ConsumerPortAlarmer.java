package com.dianping.swallow.web.alarmer.impl;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.plexus.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dianping.swallow.web.container.ResourceContainer;
import com.dianping.swallow.web.container.ResourceContainer.ConsumerServerResourcePair;
import com.dianping.swallow.web.model.event.EventType;
import com.dianping.swallow.web.model.event.ServerEvent;
import com.dianping.swallow.web.model.event.ServerType;
import com.dianping.swallow.web.model.resource.ConsumerServerResource;
import com.dianping.swallow.web.service.IPCollectorService;
import com.dianping.swallow.web.util.NetUtil;

/**
 * 
 * @author qiyin
 *
 *         2015年9月17日 下午8:24:37
 */
@Component
public class ConsumerPortAlarmer extends AbstractServiceAlarmer {

	private Map<String, Boolean> isSlaveIps = new ConcurrentHashMap<String, Boolean>();

	private static final String KEY_SPLIT = "&";

	@Autowired
	private IPCollectorService ipCollectorService;

	@Autowired
	private ResourceContainer resourceContainer;

	@Override
	public void doInitialize() throws Exception {
		super.doInitialize();
		alarmInterval = 30;
		alarmDelay = 30;
	}

	@Override
	public void doAlarm() {
		checkPort();
	}

	public boolean checkPort() {
		List<ConsumerServerResourcePair> cServerReourcePairs = resourceContainer.findConsumerServerResourcePairs();

		if (cServerReourcePairs == null || cServerReourcePairs.size() == 0) {
			logger.error("[checkPort] cannot find consumermaster or consumerslave reources.");
			return false;
		}

		for (ConsumerServerResourcePair cServerReourcePair : cServerReourcePairs) {
			ConsumerServerResource cMasterResource = cServerReourcePair.getMasterResource();
			ConsumerServerResource cSlaveReource = cServerReourcePair.getSlaveResource();
			if (StringUtils.isBlank(cMasterResource.getIp()) || !cMasterResource.isAlarm()) {
				continue;
			}
			alarmPort(cMasterResource, cSlaveReource);
		}
		return true;
	}

	private boolean alarmPort(ConsumerServerResource cMasterResource, ConsumerServerResource cSlaveReource) {
		String slaveIp = cSlaveReource.getIp();
		String masterIp = cMasterResource.getIp();
		boolean usingMaster = checkPort(masterIp, cMasterResource.getPort());
		boolean usingSlave = checkPort(slaveIp, cSlaveReource.getPort());

		String key = masterIp + KEY_SPLIT + slaveIp;

		if (!usingMaster && usingSlave) {
			isSlaveIps.put(masterIp, true);
			report(masterIp, slaveIp, ServerType.SLAVEPORT_OPENED);
			lastCheckStatus.put(key, false);
			return false;
		} else if (usingMaster && usingSlave) {
			isSlaveIps.put(masterIp, false);
			report(masterIp, slaveIp, ServerType.BOTHPORT_OPENED);
			lastCheckStatus.put(key, false);
			return false;
		} else if (!usingMaster && !usingSlave) {
			isSlaveIps.put(masterIp, false);
			report(masterIp, slaveIp, ServerType.BOTHPORT_UNOPENED);
			lastCheckStatus.put(key, false);
			return false;
		} else {
			isSlaveIps.put(masterIp, false);
			if (lastCheckStatus.containsKey(key) && !lastCheckStatus.get(key).booleanValue()) {
				report(masterIp, slaveIp, ServerType.PORT_OPENED_OK);
				lastCheckStatus.put(key, true);
			}
		}
		return true;
	}

	private void report(String masterIp, String slaveIp, ServerType serverType) {
		ServerEvent serverEvent = eventFactory.createServerEvent();
		serverEvent.setIp(masterIp).setSlaveIp(slaveIp).setServerType(serverType).setEventType(EventType.CONSUMER)
				.setCreateTime(new Date());
		eventReporter.report(serverEvent);
	}

	public boolean isSlaveOpen(String ip) {
		if (isSlaveIps.containsKey(ip)) {
			return isSlaveIps.get(ip);
		}
		return false;
	}

	private boolean checkPort(String ip, int port) {
		boolean usingPort = NetUtil.isPortOpen(ip, port);
		if (!usingPort) {
			threadSleep();
			usingPort = NetUtil.isPortOpen(ip, port);
		}
		return usingPort;
	}

}
