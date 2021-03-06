package com.dianping.swallow.web.dao;

import java.util.List;

import com.dianping.swallow.web.model.stats.ProducerServerStatsData;

/**
 * 
 * @author qiyin
 *
 *         2015年8月3日 下午2:38:13
 */
public interface ProducerServerStatsDataDao {

	boolean insert(ProducerServerStatsData serverStatsData);
	
	boolean insert(List<ProducerServerStatsData> serverStatsDatas);

	boolean removeLessThanTimeKey(long timeKey);

	List<ProducerServerStatsData> findSectionData(String ip, long startKey, long endKey);

	List<ProducerServerStatsData> findSectionData(long startKey, long endKey);

	ProducerServerStatsData findOldestData();
}
