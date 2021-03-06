package com.dianping.swallow.web.dao.impl;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.bson.types.BSONTimestamp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import com.dianping.swallow.common.internal.util.MongoUtils;
import com.dianping.swallow.web.controller.dto.MessageQueryDto;
import com.dianping.swallow.web.dao.MessageDao;
import com.dianping.swallow.web.model.Message;
import com.google.common.collect.Lists;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

/**
 * @author mingdongli
 *
 *         2015年4月20日 下午9:31:41
 */
@Component
public class DefaultMessageDao extends AbstractDao implements MessageDao {

	@Autowired
	private WebMongoManager webMongoManager;
	public static final String TIMEFORMAT = "yyyy-MM-dd HH:mm:ss";
	public static final String ID = "_id";
	private static final String MESSAGE_COLLECTION = "c";
	private static final String OID = "o_id";
	private static final String GT = "gt";
	private static final String SIZE = "size";
	private static final String MESSAGE = "message";
	private static final String SI = "si";
	private static final String V = "v";
	private static final String C = "c";
	private static final String P = "p";
	private static final String IP = "_p";
	private static final String T = "t";
	private static final String S = "s";

	public void setWebMongoManager(DefaultWebMongoManager webMongoManager) {

		this.webMongoManager = webMongoManager;
	}

	@Override
	public boolean createMessage(Message p, String topicName) {
		try {
			this.webMongoManager.getMessageMongoTemplate(topicName).insert(p, MESSAGE_COLLECTION);
			return true;
		} catch (Exception e) {
			logger.error("Error when save message " + p, e);
		}
		return false;
	}

	@Override
	public Message readById(long mid, String topicName) {
		DBCollection collection = this.webMongoManager.getMessageMongoTemplate(topicName).getCollection(
				MESSAGE_COLLECTION);
		DBObject query = BasicDBObjectBuilder.start().add(ID, MongoUtils.longToBSONTimestamp(mid)).get();
		DBObject result = collection.findOne(query);
		if (result != null) {
			Message swallowMessage = new Message();
			try {
				convert(result, swallowMessage);
				return swallowMessage;
			} catch (RuntimeException e) {
				logger.error("Error when convert resultset to SwallowMessage.", e);
			}
		}
		return (Message) result;
	}

	@Override
	public boolean update(Message p, String topicName) {
		try {
			this.webMongoManager.getMessageMongoTemplate(topicName).save(p, MESSAGE_COLLECTION);
			return true;
		} catch (Exception e) {
			logger.error("Error when save topic " + p, e);
		}
		return false;
	}

	@Override
	public int deleteById(String id, String topicName) {
		Query query = new Query(Criteria.where(ID).is(id));
		WriteResult result = this.webMongoManager.getMessageMongoTemplate(topicName).remove(query, Message.class,
				MESSAGE_COLLECTION);
		return result.getN();
	}

	@Override
	public long count(String topicName) {
		Query query = new Query();
		return this.webMongoManager.getMessageMongoTemplate(topicName).count(query, MESSAGE_COLLECTION);
	}

	@Override
	public Map<String, Object> findByTopicname(MessageQueryDto messageQueryDto) {

		String topicName = messageQueryDto.getTopic();
		String baseMid = messageQueryDto.getBasemid();
		boolean sort = messageQueryDto.isSort();
		int offset = messageQueryDto.getOffset();
		int limit = messageQueryDto.getLimit();
		List<Message> messageList = new ArrayList<Message>();
		Query query = new Query();
		if (!sort) {
			query.with(new Sort(new Sort.Order(Direction.DESC, ID)));
		} else {
			query.with(new Sort(new Sort.Order(Direction.ASC, ID)));
		}
		if (StringUtils.isEmpty(baseMid)) {
			query.skip(offset).limit(limit);
			query.fields().exclude(C);
			messageList = this.webMongoManager.getMessageMongoTemplate(topicName).find(query, Message.class,
					MESSAGE_COLLECTION);

		} else {
			DBCollection collection = this.webMongoManager.getMessageMongoTemplate(topicName).getCollection(
					MESSAGE_COLLECTION);
			Long mid = Long.parseLong(baseMid);
			if (mid > 0) {
				messageList = getMessageFromOneSide(mid, limit, collection, true, sort);
			} else {
				messageList = getMessageFromOneSide(-mid, limit, collection, false, sort);
			}
		}

		return getResponse(messageList, this.count(topicName));
	}

	@Override
	public Map<String, Object> findSpecific(MessageQueryDto messageQueryDto, long mid) {
		if (mid == 0) {
			return findSpecificWithoutId(messageQueryDto);
		} else
			return findSpecificWithId(messageQueryDto, mid);

	}

	private Map<String, Object> findSpecificWithoutId(MessageQueryDto messageQueryDto) {

		String topicName = messageQueryDto.getTopic();
		boolean sort = messageQueryDto.isSort();
		int offset = messageQueryDto.getOffset();
		int limit = messageQueryDto.getLimit();
		Query query = new Query();
		if (!sort) {
			query.with(new Sort(new Sort.Order(Direction.DESC, ID)));
		} else {
			query.with(new Sort(new Sort.Order(Direction.ASC, ID)));
		}
		query.skip(offset).limit(limit);
		if (limit - offset != 1) { // return C until it is necessary
			query.fields().exclude(C);
		}
		List<Message> messageList = this.webMongoManager.getMessageMongoTemplate(topicName).find(query, Message.class,
				MESSAGE_COLLECTION);

		return getResponse(messageList, this.count(topicName));
	}

	@Override
	public Map<String, Object> findByTime(MessageQueryDto messageQueryDto) {

		String topicName = messageQueryDto.getTopic();
		String baseMid = messageQueryDto.getBasemid();
		boolean sort = messageQueryDto.isSort();
		int offset = messageQueryDto.getOffset();
		int limit = messageQueryDto.getLimit();
		Date startdt = messageQueryDto.getStartdt();
		Date stopdt = messageQueryDto.getStopdt();
		List<Message> list = new ArrayList<Message>();

		DBCollection collection = this.webMongoManager.getMessageMongoTemplate(topicName).getCollection(
				MESSAGE_COLLECTION);
		Long startlong = MongoUtils.getLongByDate(startdt);
		Long stoplong = MongoUtils.getLongByDate(stopdt);
		DBObject query = BasicDBObjectBuilder
				.start()
				.add(ID,
						BasicDBObjectBuilder.start().add("$gt", MongoUtils.longToBSONTimestamp(startlong))
								.add("$lt", MongoUtils.longToBSONTimestamp(stoplong)).get()).get();
		DBObject orderBy;
		if (!sort) {
			orderBy = BasicDBObjectBuilder.start().add(ID, -1).get();
		} else {
			orderBy = BasicDBObjectBuilder.start().add(ID, 1).get();
		}
		DBCursor cursor = collection.find(query).skip(offset).sort(orderBy).limit(limit);
		DBCursor cursorall = collection.find(query);
		Long size = (long) cursorall.count();

		if (StringUtils.isNotEmpty(baseMid)) {
			long time = 0;
			if (baseMid.contains(":")) {
				if (!baseMid.startsWith("-")) {
					time = startlong;
				} else {
					time = -stoplong;
				}
			} else {
				time = Long.parseLong(baseMid);
			}
			if (time < 0) {
				list = getMessageFromOneSide(-time, limit, collection, false, sort);
				return getResponse(list, size);
			} else {
				list = getMessageFromOneSide(time, limit, collection, true, sort);
				return getResponse(list, size);
			}
		}

		try {
			while (cursor.hasNext()) {
				DBObject result = cursor.next();

				Message swallowMessage = new Message();
				try {
					convert(result, swallowMessage);
					list.add(swallowMessage);
				} catch (RuntimeException e) {
					if (logger.isErrorEnabled()) {
						logger.error("Error when convert resultset to WebSwallowMessage.", e);
					}
				}
			}
		} finally {
			cursor.close();
		}
		return getResponse(list, size);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Object> findByTimeAndId(MessageQueryDto messageQueryDto, long mid) {

		String topicName = messageQueryDto.getTopic();
		int offset = messageQueryDto.getOffset();
		int limit = messageQueryDto.getLimit();
		Date startdt = messageQueryDto.getStartdt();
		Date stopdt = messageQueryDto.getStopdt();
		List<Message> list = new ArrayList<Message>();
		Query query = new Query();
		if (mid == 0) {
			query.with(new Sort(new Sort.Order(Direction.DESC, ID)));
			query.skip(offset).limit(limit);
			list = this.webMongoManager.getMessageMongoTemplate(topicName).find(query, Message.class,
					MESSAGE_COLLECTION);
		} else {
			list = (List<Message>) findSpecificWithId(messageQueryDto, mid).get(MESSAGE);
		}

		Long startlong = MongoUtils.getLongByDate(startdt);
		Long stoplong = MongoUtils.getLongByDate(stopdt);
		for (int i = 0; i < list.size(); ++i) {
			if (!(mid > startlong && mid < stoplong))
				list.remove(i);
		}
		return getResponse(list, (long) list.size());

	}

	@SuppressWarnings({ "unchecked" })
	private void convert(DBObject result, Message swallowMessage) {
		BSONTimestamp timestamp = (BSONTimestamp) result.get(ID);
		BSONTimestamp originalTimestamp = (BSONTimestamp) result.get(OID);
		if (originalTimestamp != null)
			swallowMessage.setO_id(originalTimestamp);
		swallowMessage.set_id(timestamp);

		swallowMessage.setC((String) result.get(C));
		swallowMessage.setV((String) result.get(V));
		swallowMessage.setGt((Date) result.get(GT));
		Map<String, String> propertiesBasicDBObject = (Map<String, String>) result.get(P);
		if (propertiesBasicDBObject != null) {
			HashMap<String, String> properties = new HashMap<String, String>(propertiesBasicDBObject);
			swallowMessage.setP(properties.toString());
		}
		Map<String, String> internalPropertiesBasicDBObject = (Map<String, String>) result.get(IP);
		if (internalPropertiesBasicDBObject != null) {
			HashMap<String, String> properties = new HashMap<String, String>(internalPropertiesBasicDBObject);
			swallowMessage.set_p(properties.toString());
		}
		swallowMessage.setS((String) result.get(S));
		swallowMessage.setT((String) result.get(T));
		swallowMessage.setSi((String) result.get(SI));
	}

	private Map<String, Object> findSpecificWithId(MessageQueryDto messageQueryDto, long mid) {
		String topicName = messageQueryDto.getTopic();
		DBCollection collection = this.webMongoManager.getMessageMongoTemplate(topicName).getCollection(
				MESSAGE_COLLECTION);
		DBObject query = BasicDBObjectBuilder.start().add(ID, MongoUtils.longToBSONTimestamp(mid)).get();
		DBObject result = collection.findOne(query);
		List<Message> messageList = new ArrayList<Message>();
		if (result != null) {
			Message swallowMessage = new Message();
			try {
				convert(result, swallowMessage);
				messageList.add(swallowMessage);
			} catch (RuntimeException e) {
				if (logger.isErrorEnabled()) {
					logger.error("Error when convert resultset to WebSwallowMessage.", e);
				}
			}
		}
		return getResponse(messageList, (long) messageList.size());
	}

	public WebMongoManager getWebwebMongoManager() {
		return this.webMongoManager;
	}

	private Map<String, Object> getResponse(List<Message> list, Long size) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put(SIZE, size);
		map.put(MESSAGE, list);
		return map;
	}

	@Override
	public Message loadFirstMessage(String topicName) {

		long count = this.count(topicName);
		if(count == 0){
			return null;
		}
		int offset = 0;
		Message msg = null;
		while(msg == null && offset <= count){
			try{
				Query query = new Query();
				query.with(new Sort(new Sort.Order(Direction.ASC, ID))).skip(offset).limit(1);
				query.fields().include(ID);
				msg = this.webMongoManager.getMessageMongoTemplate(topicName).findOne(query, Message.class,
						MESSAGE_COLLECTION);

			}catch(ConverterNotFoundException e){
				offset++;
			}
		}

		return msg;
	}

	private List<Message> getMessageFromOneSide(Long messageId, int size, DBCollection collection, boolean side,
			boolean sort) {

		DBObject dbo;
		DBObject orderBy;
		if (side) {
			dbo = BasicDBObjectBuilder.start().add("$gt", MongoUtils.longToBSONTimestamp(messageId)).get();
			orderBy = BasicDBObjectBuilder.start().add(ID, Integer.valueOf(1)).get();
		} else {
			if (messageId == 1) {
				dbo = BasicDBObjectBuilder.start().add("$lt", MongoUtils.getTimestampByCurTime()).get();
			} else {
				dbo = BasicDBObjectBuilder.start().add("$lt", MongoUtils.longToBSONTimestamp(messageId)).get();
			}
			orderBy = BasicDBObjectBuilder.start().add(ID, Integer.valueOf(-1)).get();
		}
		DBObject query = BasicDBObjectBuilder.start().add(ID, dbo).get();
		DBObject exclude = BasicDBObjectBuilder.start().add(C, 0).get();
		DBCursor cursor = collection.find(query, exclude).sort(orderBy).limit(size);

		List<Message> list = new ArrayList<Message>();
		try {
			while (cursor.hasNext()) {
				DBObject result = cursor.next();
				Message swallowMessage = new Message();
				try {
					convert(result, swallowMessage);
					list.add(swallowMessage);
				} catch (RuntimeException e) {
					logger.error("Error when convert resultset to Message.", e);
				}
			}
		} finally {
			cursor.close();
		}

		if (messageId == 1) {
			return Lists.reverse(list);
		}
		return list;
	}

	@Override
	public Map<String, Object> exportMessages(String topicName, Date startdt, Date stopdt) {
		int maxSize = 1000000;
		Map<String, Object> map = new HashMap<String, Object>();
		DBCollection collection = this.webMongoManager.getMessageMongoTemplate(topicName).getCollection(
				MESSAGE_COLLECTION);
		Long startlong = MongoUtils.getLongByDate(startdt);
		Long stoplong = MongoUtils.getLongByDate(stopdt);
		DBObject query = BasicDBObjectBuilder
				.start()
				.add(ID,
						BasicDBObjectBuilder.start().add("$gt", MongoUtils.longToBSONTimestamp(startlong))
								.add("$lt", MongoUtils.longToBSONTimestamp(stoplong)).get()).get();
		DBObject orderBy = BasicDBObjectBuilder.start().add(ID, 1).get();
		DBCursor cursor = collection.find(query).sort(orderBy);
		int totalcount = cursor.count();
		DBCursor dbc = collection.find().limit(1);
		if (dbc.hasNext()) {
			DBObject result = dbc.next();
			int size = 0;
			try {
				size = result.toString().getBytes("UTF-8").length;
				if (size > 1000) {
					maxSize = maxSize * 1000 / size;
				}
			} catch (UnsupportedEncodingException e) {
				logger.info("Encoding error of cursor result");
			} finally {
				dbc.close();
			}
		}
		map.put("message", cursor);
		map.put("maxsize", maxSize);
		map.put("total", totalcount);
		return map;

	}

}
