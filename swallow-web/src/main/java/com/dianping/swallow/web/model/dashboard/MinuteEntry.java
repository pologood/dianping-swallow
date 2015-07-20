package com.dianping.swallow.web.model.dashboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;



/**
 * @author mingdongli
 *
 * 2015年7月7日上午9:36:34
 */
public class MinuteEntry {

	private Date time;

	List<Entry> delayEntry = new ArrayList<Entry>();
	
	private static final int ENTRYSIZE = 12;
	
	public MinuteEntry() {

	}

	public Date getTime() {
		return time;
	}

	public MinuteEntry setTime(Date time) {
		this.time = time;
		return this;
	}

	public List<Entry> getDelayEntry() {
		return delayEntry;
	}

	public MinuteEntry setDelayEntry(List<Entry> delayEntry) {
		this.delayEntry = delayEntry;
		return this;
	}
	
	public MinuteEntry addEntry(Entry entry){
		
		int entrySize = delayEntry.size();
		if(entrySize >= ENTRYSIZE){
			if(entry.getNumAlarm() > delayEntry.get(entrySize - 1).getNumAlarm()){
				delayEntry.remove(entrySize - 1);
				delayEntry.add(entry);
				Collections.sort(delayEntry);
			}
			return this;
		}
		delayEntry.add(entry);
		Collections.sort(delayEntry);
		return this;
	}

}

