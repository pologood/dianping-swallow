package com.dianping.swallow.web.monitor.collector;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dianping.swallow.common.internal.action.SwallowAction;
import com.dianping.swallow.common.internal.action.SwallowActionWrapper;
import com.dianping.swallow.common.internal.action.impl.CatActionWrapper;
import com.dianping.swallow.common.internal.exception.SwallowException;
import com.dianping.swallow.web.model.resource.ConsumerIdResource;
import com.dianping.swallow.web.model.resource.IpInfo;
import com.dianping.swallow.web.model.stats.ConsumerIpGroupStatsData;
import com.dianping.swallow.web.model.stats.ConsumerIpStatsData;
import com.dianping.swallow.web.monitor.MonitorDataListener;
import com.dianping.swallow.web.monitor.wapper.ConsumerStatsDataWapper;
import com.dianping.swallow.web.service.ConsumerIdResourceService;
import com.dianping.swallow.web.util.ThreadFactoryUtils;

/**
 * @author mingdongli
 *
 *         2015年8月31日下午8:14:56
 */
@Component
public class ConsumerIdResourceCollector extends AbstractResourceCollector implements MonitorDataListener {

	@Autowired
	private ConsumerStatsDataWapper cStatsDataWapper;

	@Autowired
	private ConsumerIdResourceService consumerIdResourceService;

	private static final String FACTORY_NAME = "ResourceCollector-ConsumerIdIpMonitor";

	private ExecutorService executor = null;

	private ActiveIpContainer<ConsumerIdKey> activeIpManager = new ActiveIpContainer<ConsumerIdKey>();

	@Override
	protected void doInitialize() throws Exception {
		super.doInitialize();
		collectorName = getClass().getSimpleName();
		collectorInterval = 20;
		collectorDelay = 1;
		executor = Executors.newSingleThreadExecutor(ThreadFactoryUtils.getThreadFactory(FACTORY_NAME));
	}

	@Override
	public void achieveMonitorData() {
		executor.submit(new Runnable() {
			@Override
			public void run() {
				SwallowActionWrapper catWrapper = new CatActionWrapper(CAT_TYPE, collectorName + "-IpMonitor");
				catWrapper.doAction(new SwallowAction() {
					@Override
					public void doAction() throws SwallowException {
						doIpDataMonitor();
					}
				});
			}
		});
	}

	private void doIpDataMonitor() {
		List<ConsumerIpGroupStatsData> ipGroupStatsDatas = cStatsDataWapper.getIpGroupStatsDatas(-1, false);
		if (ipGroupStatsDatas == null || ipGroupStatsDatas.isEmpty()) {
			return;
		}
		for (ConsumerIpGroupStatsData ipGroupStatsData : ipGroupStatsDatas) {
			if (ipGroupStatsData == null) {
				continue;
			}
			List<ConsumerIpStatsData> ipStatsDatas = ipGroupStatsData.getConsumerIpStatsDatas();
			if (ipStatsDatas == null || ipStatsDatas.isEmpty()) {
				continue;
			}
			for (ConsumerIpStatsData ipStatsData : ipStatsDatas) {
				activeIpManager.putActiveIpData(
						new ConsumerIdKey(ipStatsData.getTopicName(), ipStatsData.getConsumerId()),
						ipStatsData.getIp(), ipStatsData.hasStatsData());
			}
		}
	}

	@Override
	public void doCollector() {
		logger.info("[doCollector] start collect consumerIdResource.");
		doConsumerIdCollector();
	}

	private void doConsumerIdCollector() {
		Set<String> topicNames = cStatsDataWapper.getTopics(false);
		if (topicNames == null || topicNames.isEmpty()) {
			return;
		}
		for (String topicName : topicNames) {
			Set<String> consumerIds = cStatsDataWapper.getConsumerIds(topicName, false);
			if (consumerIds == null || consumerIds.isEmpty()) {
				continue;
			}
			for (String consumerId : consumerIds) {
				updateConsumerIdResource(topicName, consumerId);
			}
		}

	}

	private void updateConsumerIdResource(String topicName, String consumerId) {
		try {
			if (StringUtils.isBlank(topicName) && StringUtils.isBlank(consumerId)) {
				return;
			}
			ConsumerIdResource consumerIdResource = consumerIdResourceService.findByConsumerIdAndTopic(topicName,
					consumerId);
			if (consumerIdResource == null) {

				consumerIdResource = consumerIdResourceService.buildConsumerIdResource(topicName, consumerId);
				Set<String> consumerIdIps = cStatsDataWapper.getConsumerIdIps(topicName, consumerId, false);
				if (consumerIdIps != null && !consumerIdIps.isEmpty()) {
					List<IpInfo> ipInfos = new ArrayList<IpInfo>();
					for (String consumerIdIp : consumerIdIps) {
						ipInfos.add(new IpInfo(consumerIdIp, true, true));
					}
					consumerIdResource.setConsumerIpInfos(ipInfos);
				}
				consumerIdResourceService.insert(consumerIdResource);
			} else {
				updateConsumerIdIpInfos(consumerIdResource);
				consumerIdResourceService.update(consumerIdResource);
			}
		} catch (Exception e) {
			logger.error("[doConsumerIdCollector] collect consumerId resource error.", e);
		}
	}

	private void updateConsumerIdIpInfos(ConsumerIdResource consumerIdResource) {
		String topicName = consumerIdResource.getTopic();
		String consumerId = consumerIdResource.getConsumerId();
		Set<String> activeIps = activeIpManager.getActiveIps(new ConsumerIdKey(topicName, consumerId));
		List<IpInfo> ipInfos = consumerIdResource.getConsumerIpInfos();
		if ((activeIps == null || activeIps.isEmpty()) && (ipInfos == null || ipInfos.isEmpty())) {
			return;
		}
		if (ipInfos == null || ipInfos.isEmpty()) {
			ipInfos = new ArrayList<IpInfo>();
		} else if (activeIps == null || activeIps.isEmpty()) {
			for (IpInfo ipInfo : ipInfos) {
				ipInfo.setActive(false);
			}
		} else {
			for (IpInfo ipInfo : ipInfos) {
				ipInfo.setActive(false);
			}
			for (String activeIp : activeIps) {
				if (StringUtils.isBlank(activeIp)) {
					continue;
				}
				boolean isIpExist = false;
				for (IpInfo ipInfo : ipInfos) {
					if (activeIp.equals(ipInfo.getIp())) {
						ipInfo.setActive(true);
						isIpExist = true;
					}
				}
				if (!isIpExist) {
					ipInfos.add(new IpInfo(activeIp, true, true));
				}
			}
		}
		consumerIdResource.setConsumerIpInfos(ipInfos);
	}

	@Override
	public int getCollectorDelay() {
		return collectorDelay;
	}

	@Override
	public int getCollectorInterval() {
		return collectorInterval;
	}

	@Override
	protected void doDispose() throws Exception {
		super.doDispose();
		if (executor != null && !executor.isShutdown()) {
			executor.shutdown();
		}
	}

	public static class ConsumerIdKey {

		private String topicName;

		private String consumerId;

		public ConsumerIdKey() {

		}

		public ConsumerIdKey(String topicName, String consumerId) {
			this.topicName = topicName;
			this.consumerId = consumerId;
		}

		public String getTopicName() {
			return topicName;
		}

		public void setTopicName(String topicName) {
			this.topicName = topicName;
		}

		public String getConsumerId() {
			return consumerId;
		}

		public void setConsumerId(String consumerId) {
			this.consumerId = consumerId;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((topicName == null) ? 0 : topicName.hashCode());
			result = prime * result + ((consumerId == null) ? 0 : consumerId.hashCode());
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
			ConsumerIdKey other = (ConsumerIdKey) obj;
			if (topicName == null) {
				if (other.topicName != null)
					return false;
			} else if (!topicName.equals(other.topicName))
				return false;
			if (consumerId == null) {
				if (other.consumerId != null)
					return false;
			} else if (!consumerId.equals(other.consumerId))
				return false;
			return true;
		}

	}

}
