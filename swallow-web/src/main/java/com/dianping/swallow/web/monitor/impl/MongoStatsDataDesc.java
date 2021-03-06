package com.dianping.swallow.web.monitor.impl;

import com.dianping.swallow.common.server.monitor.data.StatisDetailType;
import com.dianping.swallow.web.monitor.StatsDataDesc;

/**
 * Author   mingdongli
 * 15/12/23  下午8:24.
 */
public class MongoStatsDataDesc extends AbstractServerDataDesc implements StatsDataDesc {

    public MongoStatsDataDesc(String catalog, String ip, StatisDetailType dt) {
        super(catalog, ip, dt);
    }

}