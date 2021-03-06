package com.dianping.swallow.web.alarmer.container;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dianping.swallow.web.model.alarm.AlarmMeta;
import com.dianping.swallow.web.model.alarm.AlarmType;
import com.dianping.swallow.web.service.AlarmMetaService;

/**
 * 
 * @author qiyin
 *
 *         2015年8月3日 上午11:33:46
 */
@Component("alarmMetaContainer")
public class AlarmMetaContainerImpl extends AbstractAlamerContainer implements AlarmMetaContainer {

	private static final Map<Integer, AlarmMeta> alarmMetas = new ConcurrentHashMap<Integer, AlarmMeta>();

	@Autowired
	private AlarmMetaService alarmMetaService;

	@Override
	public AlarmMeta getAlarmMeta(int metaId) {
		return alarmMetas.get(metaId);
	}

	@Override
	protected void doInitialize() throws Exception {
		super.doInitialize();
		interval = 300;
		delay = 5;
		containerName = "AlarmMetaContainer";
	}

	@Override
	public void doLoadResource() {
		logger.info("[doLoadResource] scheduled load alarmMeta info.");
		List<AlarmMeta> alarmMetaTemps = alarmMetaService.findByPage(0, AlarmType.values().length);
		if (alarmMetaTemps != null && !alarmMetaTemps.isEmpty()) {
			for (AlarmMeta alarmMeta : alarmMetaTemps) {
				alarmMetas.put(alarmMeta.getType().getNumber(), alarmMeta);
			}
		}
	}
	
	@Override
	public int getInterval() {
		return interval;
	}

	@Override
	public int getDelay() {
		return delay;
	}

}
