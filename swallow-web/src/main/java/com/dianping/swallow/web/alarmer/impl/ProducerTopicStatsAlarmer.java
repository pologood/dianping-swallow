package com.dianping.swallow.web.alarmer.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dianping.swallow.common.internal.action.SwallowAction;
import com.dianping.swallow.common.internal.action.SwallowActionWrapper;
import com.dianping.swallow.common.internal.action.impl.CatActionWrapper;
import com.dianping.swallow.common.internal.exception.SwallowException;
import com.dianping.swallow.web.model.alarm.ProducerBaseAlarmSetting;
import com.dianping.swallow.web.model.alarm.QPSAlarmSetting;
import com.dianping.swallow.web.model.alarm.TopicAlarmSetting;
import com.dianping.swallow.web.model.stats.ProducerTopicStatsData;
import com.dianping.swallow.web.monitor.MonitorDataListener;
import com.dianping.swallow.web.monitor.ProducerDataRetriever;
import com.dianping.swallow.web.monitor.wapper.ProducerStatsDataWapper;
import com.dianping.swallow.web.service.ProducerServerAlarmSettingService;
import com.dianping.swallow.web.service.ProducerTopicStatsDataService;
import com.dianping.swallow.web.service.TopicAlarmSettingService;

/**
 * 
 * @author qiyin
 *
 * 2015年8月3日 下午6:07:16
 */
@Component
public class ProducerTopicStatsAlarmer extends AbstractStatsAlarmer implements MonitorDataListener {

	@Autowired
	private ProducerDataRetriever producerDataRetriever;

	@Autowired
	private ProducerStatsDataWapper producerStatsDataWapper;

	@Autowired
	private ProducerTopicStatsDataService topicStatsDataService;

	@Autowired
	private TopicAlarmSettingService topicAlarmSettingService;

	@Autowired
	private ProducerServerAlarmSettingService serverAlarmSettingService;

	@Override
	public void achieveMonitorData() {
		dataCount.incrementAndGet();
	}

	@Override
	public void doInitialize() throws Exception {
		super.doInitialize();
		producerDataRetriever.registerListener(this);
	}

	@Override
	public void doAlarm() {
		if (dataCount.get() <= 0) {
			return;
		}
		dataCount.decrementAndGet();
		final List<ProducerTopicStatsData> topicStatsDatas = producerStatsDataWapper.getTopicStatsDatas(lastTimeKey
				.get());
		SwallowActionWrapper catWrapper = new CatActionWrapper(getClass().getSimpleName(), "doAlarm");
		catWrapper.doAction(new SwallowAction() {
			@Override
			public void doAction() throws SwallowException {
				topicAlarm(topicStatsDatas);
			}
		});
	}

	private void topicAlarm(List<ProducerTopicStatsData> topicStatsDatas) {
		if (topicStatsDatas == null) {
			return;
		}
		TopicAlarmSetting topicAlarmSetting = topicAlarmSettingService.findDefault();
		if (topicAlarmSetting == null || topicAlarmSetting.getProducerAlarmSetting() == null) {
			return;
		}

		ProducerBaseAlarmSetting producerAlarmSetting = topicAlarmSetting.getProducerAlarmSetting();
		QPSAlarmSetting qps = producerAlarmSetting.getQpsAlarmSetting();
		long delay = producerAlarmSetting.getDelay();
		List<String> whiteList = serverAlarmSettingService.getTopicWhiteList();
		for (ProducerTopicStatsData topicStatsData : topicStatsDatas) {
			if (whiteList == null || !whiteList.contains(topicStatsData.getTopicName())) {
				qpsAlarm(topicStatsData, qps);
				topicStatsData.checkDelay(delay);
			}
		}
	}

	public boolean qpsAlarm(ProducerTopicStatsData topicStatsData, QPSAlarmSetting qps) {
		if (qps != null) {
			if (!topicStatsData.checkQpsPeak(qps.getPeak())) {
				return false;
			}
			if (!topicStatsData.checkQpsValley(qps.getValley())) {
				return false;
			}
			long preQps = getExpectQps(topicStatsData.getTopicName(), topicStatsData.getTimeKey());
			if (!topicStatsData.checkQpsFlu(qps.getFluctuationBase(), preQps, qps.getFluctuation())) {
				return false;
			}
		}
		return true;
	}

	public long getExpectQps(String topicName, long timeKey) {
		long preDayTimeKey = getPreDayKey(timeKey);
		long startKey = preDayTimeKey - getTimeSection();
		long endKey = preDayTimeKey + getTimeSection();
		List<ProducerTopicStatsData> topicStatsDatas = topicStatsDataService.findSectionData(topicName, startKey,
				endKey);
		int dataCount = 0;
		long sumQps = 0L;
		if (topicStatsDatas != null) {
			for (ProducerTopicStatsData topicStatsData : topicStatsDatas) {
				if (topicStatsData.getQps() > 0L) {
					sumQps += topicStatsData.getQps();
					dataCount++;
				}
			}
			if (dataCount > 0) {
				return sumQps / dataCount;
			}
		}
		return 0L;
	}
}