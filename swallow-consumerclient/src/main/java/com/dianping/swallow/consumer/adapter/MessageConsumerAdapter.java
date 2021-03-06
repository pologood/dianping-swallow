package com.dianping.swallow.consumer.adapter;

import java.util.Map;

import com.dianping.swallow.BackoutMessageException;
import com.dianping.swallow.Destination;
import com.dianping.swallow.MQService.ConsumerOptionKey;
import com.dianping.swallow.MessageConsumer;
import com.dianping.swallow.MessageListener;
import com.dianping.swallow.common.consumer.ConsumerType;
import com.dianping.swallow.consumer.Consumer;
import com.dianping.swallow.consumer.ConsumerConfig;
import com.dianping.swallow.consumer.impl.ConsumerFactoryImpl;
import com.dianping.swallow.impl.MongoMQService;

public class MessageConsumerAdapter implements MessageConsumer {

   private MessageConsumer oldConsumer = null;
   private Consumer        newConsumer = null;

   public MessageConsumerAdapter(MongoMQService oldMqService, Destination oldDest,
                                 Map<ConsumerOptionKey, Object> options, boolean startOldConsumer) {

      ConsumerConfig config = new ConsumerConfig();

      String consumerId = null;
      if (options != null) {
         consumerId = (String) options.get(ConsumerOptionKey.ConsumerID);
      }

      if (consumerId == null || "".equals(consumerId.trim())) {
         config.setConsumerType(ConsumerType.NON_DURABLE);
      }

      com.dianping.swallow.common.message.Destination newDest = com.dianping.swallow.common.message.Destination
            .topic(oldDest.getName());
      newConsumer = ConsumerFactoryImpl.getInstance().createConsumer(newDest, consumerId, config);
      if (startOldConsumer && oldMqService != null) {
         oldConsumer = oldMqService.createConsumer(oldDest, options);
      }
   }

   @Override
   public void setMessageListener(final MessageListener listener) {
      if (oldConsumer != null) {
         oldConsumer.setMessageListener(listener);
      }
      newConsumer.setListener(new com.dianping.swallow.consumer.MessageListener() {
         @Override
         public void onMessage(com.dianping.swallow.common.message.Message msg)
               throws com.dianping.swallow.consumer.BackoutMessageException {
            try {
               listener.onMessage(new StringMessageAdapter(msg));
            } catch (BackoutMessageException e) {
               throw new com.dianping.swallow.consumer.BackoutMessageException(e);
            }
         }
      });
      newConsumer.start();
   }

   @Override
   public void close() {
      if (oldConsumer != null) {
         oldConsumer.close();
      }
      if (newConsumer != null) {
         newConsumer.close();
      }
   }
}
