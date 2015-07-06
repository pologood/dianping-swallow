package com.dianping.swallow.web.service;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.dianping.swallow.web.model.alarm.Alarm;

/**
 *
 *@author qiyin 
 * 
 */
public interface AlarmService  extends SwallowService {
	/**
	 * sms alarm
	 * @param mobile
	 * @param body
	 * @return
	 */
	public boolean sendSms(String mobile,String body);
	/**
	 * weiXin alarm
	 * @param email
	 * @param title
	 * @param content
	 * @return
	 */
	public boolean sendWeixin(String email,String title,String content);
	/**
	 * mail alarm
	 * @param email
	 * @param title
	 * @param content
	 * @return
	 */
	public boolean sendMail(String email,String title,String content);
	/**
	 * send sms weiXin mail
	 * @param ip
	 * @param title
	 * @param message
	 */
	public void sendAll(String ip,String title,String message);
	
	/**
	 * insert
	 * 
	 * @param alarm
	 * @return
	 */
	public boolean insert(Alarm alarm);

	/**
	 * update
	 * 
	 * @param alarm
	 * @return
	 */
	public boolean update(Alarm alarm);

	/**
	 * delete by id
	 * 
	 * @param ipDesc
	 * @return
	 */
	public int deleteById(String id);
	
	/**
	 * find by id
	 * @param id
	 * @return
	 */
	public Alarm findById(String id);

	/**
	 * find by receiver
	 * 
	 * @param Alarm
	 * @return
	 */
	public Map<String, Object> findByReceiver(String receiver, int offset, int limit);

	/**
	 * find by id
	 * 
	 * @param ipDesc
	 * @return
	 */
	public Map<String, Object> findByCreateTime(Date createTime, int offset, int limit);

	/**
	 * find all
	 * 
	 * @param 
	 * @return
	 */
	public List<Alarm> findAll();
}
