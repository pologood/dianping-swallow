package com.dianping.swallow.consumerserver.netty;

import java.io.IOException;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dianping.swallow.common.consumer.ACKHandlerType;
import com.dianping.swallow.common.consumer.ConsumerMessageType;
import com.dianping.swallow.common.packet.PktConsumerMessage;
import com.dianping.swallow.consumerserver.impl.MongoHeartbeater;
import com.dianping.swallow.consumerserver.worker.ConsumerId;
import com.dianping.swallow.consumerserver.worker.ConsumerInfo;
import com.dianping.swallow.consumerserver.worker.ConsumerWorkerManager;

@SuppressWarnings("deprecation")
@ChannelPipelineCoverage("all")
public class MessageServerHandler extends SimpleChannelUpstreamHandler {

   private static final Logger LOG        = LoggerFactory.getLogger(MongoHeartbeater.class);
   
   private static ChannelGroup channelGroup = new DefaultChannelGroup();
   
   private ConsumerWorkerManager workerManager;

   private ConsumerId            consumerId;

   private ConsumerInfo          consumerInfo;

   private int                   clientThreadCount = 1;

   private boolean               readyClose        = Boolean.FALSE;

   public MessageServerHandler(ConsumerWorkerManager workerManager) {
      this.workerManager = workerManager;
   }

   @Override
   public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
      LOG.info("one client connected!");
      channelGroup.add(e.getChannel());
      System.out.println("haha");
   }

   @Override
   public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {

      //收到PktConsumerACK，按照原流程解析
      final Channel channel = e.getChannel();
      if (e.getMessage() instanceof PktConsumerMessage) {
         PktConsumerMessage consumerPacket = (PktConsumerMessage) e.getMessage();
         if (ConsumerMessageType.GREET.equals(consumerPacket.getType())) {
            clientThreadCount = consumerPacket.getThreadCount();
            consumerId = new ConsumerId(consumerPacket.getConsumerId(), consumerPacket.getDest());
            consumerInfo = new ConsumerInfo(consumerId, consumerPacket.getConsumerType());
            workerManager.handleGreet(channel, consumerInfo, clientThreadCount);
         } else {
            if (consumerPacket.getNeedClose() || readyClose) {
               if(!readyClose){
                  Thread thread = workerManager.getThreadFactory().newThread(new Runnable() {

                     @Override
                     public void run() {
                        try {
                           Thread.sleep(workerManager.getConfigManager().getCloseChannelMaxWaitingTime());
                        } catch (InterruptedException e) {
                           LOG.error("CloseChannelThread InterruptedException", e);
                        }
                        workerManager.handleChannelDisconnect(channel, consumerInfo);
                     }
                  }, consumerInfo.toString() + "-CloseChannelThread-");
                  thread.setDaemon(true);
                  thread.start();
               }
               clientThreadCount--;
               readyClose = Boolean.TRUE;
            }
            if (readyClose && clientThreadCount == 0) {
               workerManager.handleAck(channel, consumerInfo, consumerPacket.getMessageId(),
                     ACKHandlerType.CLOSE_CHANNEL);
            } else if (readyClose && clientThreadCount > 0) {
               workerManager.handleAck(channel, consumerInfo, consumerPacket.getMessageId(), ACKHandlerType.NO_SEND);
            } else if (!readyClose) {
               workerManager.handleAck(channel, consumerInfo, consumerPacket.getMessageId(),
                     ACKHandlerType.SEND_MESSAGE);
            }
         }

      } else {
         LOG.error("the received message is not PktConsumerMessage");
      }

   }

   @Override
   public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
      
      channelGroup.remove(e.getChannel());
      //只有IOException的时候才需要处理。
      if (e.getCause() instanceof IOException) {
         LOG.error("Client disconnected!", e.getCause());
         Channel channel = e.getChannel();
         workerManager.handleChannelDisconnect(channel, consumerInfo);
         channel.close();
      }
      LOG.info("something exception happened!", e.getCause());
   }
   
   @Override
   public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
      channelGroup.remove(e.getChannel());
      super.channelDisconnected(ctx, e);
   }

   @Override
   public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
      channelGroup.remove(e.getChannel());
      super.channelClosed(ctx, e);
   }

   public static ChannelGroup getChannelGroup() {
      return channelGroup;
   }
   
}
