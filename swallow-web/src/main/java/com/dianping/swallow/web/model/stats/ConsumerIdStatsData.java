package com.dianping.swallow.web.model.stats;

import java.util.Date;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.dianping.swallow.web.model.event.Event;
import com.dianping.swallow.web.model.event.EventType;
import com.dianping.swallow.web.model.event.StatisType;

/**
 * 
 * @author qiyin
 *
 *         2015年7月31日 下午3:56:31
 */
@Document(collection = "CONSUMERID_STATS_DATA")
@CompoundIndexes({ @CompoundIndex(name = "IX_TIMEKEY_TOPICNAME_CONSUMERID", def = "{'timeKey': 1, 'topicName': -1, 'consumerId': -1}") })
public class ConsumerIdStatsData extends ConsumerStatsData {

	private String topicName;

	private String consumerId;

	private long totalSendQps;

	private long totalSendDelay;

	private long totalAckQps;

	private long totalAckDelay;

	private long totalAccumulation;

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

	public long getTotalSendQps() {
		return totalSendQps;
	}

	public void setTotalSendQps(long totalSendQps) {
		this.totalSendQps = totalSendQps;
	}

	public long getTotalSendDelay() {
		return totalSendDelay;
	}

	public void setTotalSendDelay(long totalSendDelay) {
		this.totalSendDelay = totalSendDelay;
	}

	public long getTotalAckQps() {
		return totalAckQps;
	}

	public void setTotalAckQps(long totalAckQps) {
		this.totalAckQps = totalAckQps;
	}

	public long getTotalAckDelay() {
		return totalAckDelay;
	}

	public void setTotalAckDelay(long totalAckDelay) {
		this.totalAckDelay = totalAckDelay;
	}

	public long getTotalAccumulation() {
		return totalAccumulation;
	}

	public void setTotalAccumulation(long totalAccumulation) {
		this.totalAccumulation = totalAccumulation;
	}

	public boolean checkSendQpsPeak(long expectQps) {
		return checkQpsPeak(getSendQps(), expectQps, StatisType.SENDQPS_PEAK);
	}

	public boolean checkSendQpsValley(long expectQps) {
		return checkQpsValley(getSendQps(), expectQps, StatisType.SENDQPS_VALLEY);
	}

	public boolean checkSendQpsFlu(long baseQps, long preQps, int flu) {
		return checkQpsFlu(getSendQps(), baseQps, preQps, flu, StatisType.SENDQPS_FLU);
	}

	public boolean checkSendDelay(long expectDelay) {
		return checkDelay(getSendDelay(), expectDelay, StatisType.SENDDELAY);
	}

	public boolean checkSendAccu(long expectAccu) {
		return checkAccu(getAccumulation(), expectAccu, StatisType.SENDACCU);
	}

	public boolean checkAckQpsPeak(long expectQps) {
		return checkQpsPeak(getAckQps(), expectQps, StatisType.ACKQPS_PEAK);
	}

	public boolean checkAckQpsValley(long expectQps) {
		return checkQpsValley(getAckQps(), expectQps, StatisType.ACKQPS_VALLEY);
	}

	public boolean checkAckQpsFlu(long baseQps, long preQps, int flu) {
		return checkQpsFlu(getAckQps(), baseQps, preQps, flu, StatisType.ACKQPS_FLU);
	}

	public boolean checkAckDelay(long expectDelay) {
		return checkDelay(getAckDelay(), expectDelay, StatisType.ACKDELAY);
	}

	public boolean checkQpsPeak(long qps, long expectQps, StatisType statisType) {
		if (qps != 0L) {
			if (qps > expectQps) {
				Event event = eventFactory.createConsumerIdEvent().setConsumerId(consumerId).setTopicName(topicName)
						.setCurrentValue(qps).setExpectedValue(expectQps).setStatisType(statisType)
						.setCreateTime(new Date()).setEventType(EventType.CONSUMER);
				eventReporter.report(event);
				return false;
			}
		}
		return true;
	}

	public boolean checkQpsValley(long qps, long expectQps, StatisType statisType) {
		if (qps != 0L) {
			if (qps < expectQps) {
				eventReporter.report(eventFactory.createConsumerIdEvent().setConsumerId(consumerId)
						.setTopicName(topicName).setCurrentValue(qps).setExpectedValue(expectQps)
						.setStatisType(statisType).setCreateTime(new Date()).setEventType(EventType.CONSUMER));
				return false;
			}
		}
		return true;
	}

	public boolean checkQpsFlu(long qps, long baseQps, long preQps, int flu, StatisType statisType) {
		if (qps == 0L || preQps == 0L) {
			return true;
		}

		if (qps > baseQps || preQps > baseQps) {

			if ((qps >= preQps && (qps / preQps > flu)) || (qps < preQps && (preQps / qps > flu))) {

				eventReporter.report(eventFactory.createConsumerIdEvent().setConsumerId(consumerId)
						.setTopicName(topicName).setCurrentValue(qps).setExpectedValue(preQps)
						.setStatisType(statisType).setCreateTime(new Date()).setEventType(EventType.CONSUMER));
				return false;

			}
		}
		return true;
	}

	public boolean checkDelay(long delay, long expectDelay, StatisType statisType) {
		delay = delay / 1000;
		if (delay != 0L && expectDelay != 0L) {
			if ((delay) > expectDelay) {
				eventReporter.report(eventFactory.createConsumerIdEvent().setConsumerId(consumerId)
						.setTopicName(topicName).setCurrentValue(delay).setExpectedValue(expectDelay)
						.setStatisType(statisType).setCreateTime(new Date()).setEventType(EventType.CONSUMER));
				return false;
			}
		}
		return true;
	}

	public boolean checkAccu(long accu, long expectAccu, StatisType statisType) {
		if (accu != 0L && expectAccu != 0L) {
			if (accu > expectAccu) {
				eventReporter.report(eventFactory.createConsumerIdEvent().setConsumerId(consumerId)
						.setTopicName(topicName).setCurrentValue(accu).setExpectedValue(expectAccu)
						.setStatisType(statisType).setCreateTime(new Date()).setEventType(EventType.CONSUMER));
				return false;
			}
		}
		return true;
	}

	public String generateKey() {
		return topicName + "&" + consumerId;
	}

	public void setTotalStatsDatas(ConsumerIdStatsData lastStatsData) {
		if (lastStatsData != null) {
			this.totalSendDelay = lastStatsData.getTotalSendDelay() + getSendDelay();
			this.totalSendQps = lastStatsData.getTotalSendQps() + getSendQps();
			this.totalAckDelay = lastStatsData.getTotalAckDelay() + getAckDelay();
			this.totalAckQps = lastStatsData.getTotalAckQps() + getAckQps();
			this.totalAccumulation = lastStatsData.getTotalAccumulation() + getAccumulation();
		} else {
			this.totalSendDelay = getSendDelay();
			this.totalSendQps = getSendQps();
			this.totalAckDelay = getAckDelay();
			this.totalAckQps = getAckQps();
			this.totalAccumulation = getAccumulation();
		}
	}

	@Override
	public String toString() {
		return "ConsumerIdStatsData [topicName=" + topicName + ", consumerId=" + consumerId + ", totalSendQps="
				+ totalSendQps + ", totalSendDelay=" + totalSendDelay + ", totalAckQps=" + totalAckQps
				+ ", totalAckDelay=" + totalAckDelay + ", totalAccumulation=" + totalAccumulation + "]";
	}

}
