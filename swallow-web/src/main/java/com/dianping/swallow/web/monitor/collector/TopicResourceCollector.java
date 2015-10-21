package com.dianping.swallow.web.monitor.collector;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dianping.swallow.common.internal.action.SwallowAction;
import com.dianping.swallow.common.internal.action.SwallowActionWrapper;
import com.dianping.swallow.common.internal.action.impl.CatActionWrapper;
import com.dianping.swallow.common.internal.exception.SwallowException;
import com.dianping.swallow.web.model.resource.IpInfo;
import com.dianping.swallow.web.model.resource.TopicResource;
import com.dianping.swallow.web.model.stats.ProducerIpStatsData;
import com.dianping.swallow.web.monitor.MonitorDataListener;
import com.dianping.swallow.web.monitor.ProducerDataRetriever;
import com.dianping.swallow.web.monitor.wapper.ProducerStatsDataWapper;
import com.dianping.swallow.web.service.TopicResourceService;
import com.dianping.swallow.web.util.ThreadFactoryUtils;

/**
 * 
 * @author qiyin
 *
 *         2015年9月30日 上午11:24:59
 */
@Component
public class TopicResourceCollector extends AbstractResourceCollector implements MonitorDataListener {

	@Autowired
	private TopicResourceService topicResourceService;

	@Autowired
	private ProducerStatsDataWapper pStatsDataWapper;

	@Autowired
	private ProducerDataRetriever producerDataRetriever;

	private static final String FACTORY_NAME = "ResourceCollector-TopicIpMonitor";

	private ExecutorService executor = null;

	private IpStatusMonitor<String> activeIpManager = new IpStatusMonitor<String>();

	@Override
	protected void doInitialize() throws Exception {
		super.doInitialize();
		collectorName = getClass().getSimpleName();
		collectorInterval = 20;
		collectorDelay = 3;
		producerDataRetriever.registerListener(this);
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
		Set<String> topicNames = pStatsDataWapper.getTopics(false);
		for (String topicName : topicNames) {
			List<ProducerIpStatsData> ipStatsDatas = pStatsDataWapper.getIpStatsDatas(topicName, -1, false);
			if (ipStatsDatas == null || ipStatsDatas.isEmpty()) {
				continue;
			}
			for (ProducerIpStatsData ipStatsData : ipStatsDatas) {
				activeIpManager.putActiveIpData(ipStatsData.getTopicName(), ipStatsData.getIp(),
						ipStatsData.hasStatsData());
			}
		}
	}

	@Override
	public void doCollector() {
		logger.info("[doCollector] start collect topicResource.");
		doTopicCollector();
	}

	private void doTopicCollector() {
		Set<String> topicNames = pStatsDataWapper.getTopics(false);
		if (topicNames != null && !topicNames.isEmpty()) {
			for (String topicName : topicNames) {
				try {
					updateTopicIpInfos(topicName);
				} catch (Exception e) {
					logger.error("[doTopicCollector] update topicIpInfos error.", e);
				}
			}
		}
	}

	private void updateTopicIpInfos(String topicName) {
		TopicResource topicResource = topicResourceService.findByTopic(topicName);
		Set<String> topicIps = pStatsDataWapper.getTopicIps(topicName, false);
		if (topicResource != null) {
			List<IpInfo> ipInfos = topicResource.getProducerIpInfos();
			ipInfos = activeIpManager.getRelatedIpInfo(topicName, ipInfos, topicIps);
			topicResource.setProducerIpInfos(ipInfos);
			topicResourceService.update(topicResource);
			logger.info("[updateTopicIpInfos] topicResource {}", topicResourceService.toString());
		}
	}

	@Override
	protected void doDispose() throws Exception {
		super.doDispose();
		if (executor != null && !executor.isShutdown()) {
			executor.shutdown();
		}
	}

	@Override
	public int getCollectorDelay() {
		return collectorDelay;
	}

	@Override
	public int getCollectorInterval() {
		return collectorInterval;
	}

}