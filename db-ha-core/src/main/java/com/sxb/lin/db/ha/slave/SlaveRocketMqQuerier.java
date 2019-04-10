package com.sxb.lin.db.ha.slave;

import java.util.List;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sxb.lin.db.ha.mybatis.interceptor.QueryInterceptor;

import redis.clients.jedis.JedisCluster;

public class SlaveRocketMqQuerier extends SlaveRedisQuerier implements SlaveQuerier{
	
	protected final static Logger logger = LoggerFactory.getLogger(SlaveRocketMqQuerier.class);
	
	protected final static String DB_HA_RECOVER_TOPIC = "db_ha_recover_topic";
	
	private DefaultMQProducer producer;
	
	private DefaultMQPushConsumer consumer;
	
	private QueryInterceptor queryInterceptor;
	
	public SlaveRocketMqQuerier(DefaultMQProducer producer) {
		super(null);
		this.producer = producer;
	}

	public SlaveRocketMqQuerier(JedisCluster redis,DefaultMQProducer producer) {
		super(redis);
		this.producer = producer;
	}

	@Override
	public void stopSlaves() {
		try {
			Message msg = new Message(DB_HA_RECOVER_TOPIC, "noSlaves=true".getBytes("UTF-8"));
			producer.send(msg, new SendCallback() {
				@Override
				public void onSuccess(SendResult sendResult) {
					
				}

				@Override
				public void onException(Throwable e) {
					logger.error(e.getMessage(), e);
				}
			});
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public void init() throws Exception {
		consumer = new DefaultMQPushConsumer("CONSUMER_DB_HA_RECOVER");
		consumer.setNamesrvAddr(producer.getNamesrvAddr());
        consumer.setMessageModel(MessageModel.BROADCASTING);
        consumer.subscribe(DB_HA_RECOVER_TOPIC, "*");
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs,
                ConsumeConcurrentlyContext context) {
            	if(msgs.size() > 0 && queryInterceptor != null) {
            		MessageExt messageExt = msgs.get(0);
            		String body = new String(messageExt.getBody());
            		if(body.equals("noSlaves=true")) {
            			queryInterceptor.setNoSlavesBySlaveQuerier(true);
            		}else if(body.equals("noSlaves=false")) {
            			queryInterceptor.setNoSlavesBySlaveQuerier(false);
            		}
            	}
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        consumer.start();
	}

	@Override
	public void destroy() {
		if(consumer != null) {
			consumer.shutdown();
		}
	}

	public QueryInterceptor getQueryInterceptor() {
		return queryInterceptor;
	}

	@Override
	public void setQueryInterceptor(QueryInterceptor queryInterceptor) {
		this.queryInterceptor = queryInterceptor;
	}
	
}
