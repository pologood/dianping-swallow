package com.dianping.swallow.web.monitor.impl;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.dianping.swallow.common.server.monitor.data.ConsumerMonitorData;
import com.dianping.swallow.common.server.monitor.data.MonitorData;
import com.dianping.swallow.common.server.monitor.data.structure.TotalMap;
import com.dianping.swallow.common.server.monitor.visitor.ConsumerMonitorVisitor;
import com.dianping.swallow.common.server.monitor.visitor.MonitorTopicVisitor;
import com.dianping.swallow.common.server.monitor.visitor.MonitorVisitorFactory;
import com.dianping.swallow.web.monitor.ConsumerDataRetriever;
import com.dianping.swallow.web.monitor.StatsData;
import com.dianping.swallow.web.monitor.StatsDataType;

/**
 * @author mengwenchao
 *
 * 2015年4月21日 上午11:04:09
 */
@Component
public class DefaultConsumerDataRetriever extends AbstractMonitorDataRetriever implements ConsumerDataRetriever{

	
	@Override
	public List<StatsData> getSendDelayForAllConsumerId(String topic, int intervalTimeSeconds, long start, long end) {
		
		return createStatsData(topic, intervalTimeSeconds, start, end, StatsDataType.SEND_DELAY);
	}

	@Override
	public List<StatsData> getAckDelayForAllConsumerId(String topic,
			int intervalTimeSeconds, long start, long end) {

		
		return createStatsData(topic, intervalTimeSeconds, start, end, StatsDataType.ACK_DELAY);
	}

	private List<StatsData> createStatsData(String topic, int intervalTimeSeconds, long start, long end, StatsDataType type) {
		
		List<StatsData> result = new LinkedList<StatsData>();
		
		NavigableMap<Long, MonitorData> data = getData(topic, start, end);
		
		long realStartTime = getRealStartTime(data, start, end);
		
		Set<String> consumerIds = getAllConsumerIds(topic, data);
		consumerIds.remove(MonitorData.TOTAL_KEY);
		
		for(String consumerId : consumerIds){
			result.add(getConsumerIdStats(topic, consumerId, data, intervalTimeSeconds, realStartTime, end, type));
		}
		
		
		return result;
	}

	private String getConsumerIdSubTitle(String consumerId) {
		if(consumerId.equals(MonitorData.TOTAL_KEY)){
			return "全局平均";
		}
		return "consumerID:" + consumerId;
	}

	private StatsData getConsumerIdStats(String topic, String consumerId,
			NavigableMap<Long, MonitorData> data, int intervalTimeSeconds,
			long start, long end, StatsDataType type) {
		
		ConsumerMonitorVisitor consumerMonitorVisitor = MonitorVisitorFactory.buildConsumerConsumerIdVisitor(topic, consumerId);
		visit(consumerMonitorVisitor, data);
		List<Long> stats = null;
		if(type == StatsDataType.SEND_DELAY){
			stats = consumerMonitorVisitor.buildSendDelay(intervalTimeSeconds);
		}else{
			stats = consumerMonitorVisitor.buildAckDelay(intervalTimeSeconds);
		}
		
		String subTitle = getConsumerIdSubTitle(consumerId);

		return new StatsData(new ConsumerStatsDataDesc(topic, subTitle, type) , stats, start, intervalTimeSeconds);
	}


	private Set<String> getAllConsumerIds(final String topic, NavigableMap<Long, MonitorData> data) {
		
		final Set<String> consumerIds = new HashSet<String>();
		
		for(Entry<Long, MonitorData> entry : data.entrySet()){
			
			ConsumerMonitorData consumerMonitorData = (ConsumerMonitorData) entry.getValue();
			consumerMonitorData.accept(new MonitorTopicVisitor() {
				
				@SuppressWarnings("unchecked")
				@Override
				public void visitTopic(@SuppressWarnings("rawtypes") TotalMap visitorData) {
					if(visitorData != null){
						consumerIds.addAll(visitorData.keySet());
					}
				}
				
				@Override
				public String getVisitTopic() {
					return topic;
				}
			});
		}
		
		return consumerIds;
	}

	@Override
	protected SwallowServerData createSwallowServerData() {
		return new ConsumerSwallowServerData();
	}

	@Override
	protected Class<? extends SwallowServerData> getServerDataClass() {
		
		return ConsumerSwallowServerData.class;
	}
	
	
	public static class ConsumerSwallowServerData extends SwallowServerData{

		@Override
		protected Class<? extends MonitorData> getMonitorDataClass() {
			return ConsumerMonitorData.class;
		}
	}



	@Override
	public List<StatsData> getSendDelayForAllConsumerId(String topic) {
		
		return getSendDelayForAllConsumerId(topic, getDefaultInterval(), getDefaultStart(), getDefaultEnd());
	}

	@Override
	public List<StatsData> getAckDelayForAllConsumerId(String topic) {
		
		return getAckDelayForAllConsumerId(topic, getDefaultInterval(), getDefaultStart(), getDefaultEnd());
	}

	@Override
	protected MonitorData createMonitorData() {
		return new ConsumerMonitorData();
	}

}