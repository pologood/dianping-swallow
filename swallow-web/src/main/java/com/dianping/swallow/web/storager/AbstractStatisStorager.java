package com.dianping.swallow.web.storager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.dianping.swallow.common.internal.lifecycle.impl.AbstractLifecycle;
import com.dianping.swallow.common.internal.util.CommonUtils;

/**
 *
 * @author qiyin
 *
 */
public abstract class AbstractStatisStorager extends AbstractLifecycle {
	
	protected volatile AtomicLong dataCount = new AtomicLong();

	protected static final int INIT_VALUE = 0;

	protected static final long DEFAULT_VALUE = -1L;
	
	protected volatile AtomicLong lastTimeKey = new AtomicLong();

	private int storagerInterval = 30;

	private static ScheduledExecutorService scheduled = Executors
			.newScheduledThreadPool(CommonUtils.DEFAULT_CPU_COUNT * 2);

	private ScheduledFuture<?> future = null;

	@Override
	protected void doInitialize() throws Exception {
		super.doInitialize();
		dataCount.set(INIT_VALUE);
		lastTimeKey.set(DEFAULT_VALUE);
	}

	@Override
	protected void doStart() throws Exception {
		super.doStart();
		startStorage();
	}

	private void startStorage() {
		 setFuture(scheduled.scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				try {
					//doStorage();
				} catch (Throwable th) {
					logger.error("[startStorage]", th);
				} finally {

				}
			}

		}, getStoragerInterval(), getStoragerInterval(), TimeUnit.SECONDS));
	}

	protected abstract void doStorage();

	public int getStoragerInterval() {
		return storagerInterval;
	}

	public void setStoragerInterval(int storagerInterval) {
		this.storagerInterval = storagerInterval;
	}

	public ScheduledFuture<?> getFuture() {
		return future;
	}

	public void setFuture(ScheduledFuture<?> future) {
		this.future = future;
	}

}