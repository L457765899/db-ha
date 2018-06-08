package com.sxb.lin.db.ha.slave;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.JedisCommands;

public class SlaveRedisQuerier implements SlaveQuerier{
	
	private final static String DB_HA_SLAVE_ALREADY_FIXED_COUNT_KEY = "DB_HA_SLAVE_ALREADY_FIXED_COUNT_KEY";
	
	private final static Logger logger = LoggerFactory.getLogger(SlaveRedisQuerier.class);
	
	private JedisCommands redis;
	
	public SlaveRedisQuerier(JedisCommands redis) {
		this.redis = redis;
	}

	@Override
	public boolean isAlreadyFixedReplicate() {
		
		String count = redis.get(DB_HA_SLAVE_ALREADY_FIXED_COUNT_KEY);
		if(count != null){
			try {
				if(Integer.parseInt(count) >= 0){
					redis.incr(DB_HA_SLAVE_ALREADY_FIXED_COUNT_KEY);
					logger.info("slave's replicate are already fixed,get result "+count+" from redis.");
					return true;
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
		
		return false;
	}

	public JedisCommands getRedis() {
		return redis;
	}

	public void setRedis(JedisCommands redis) {
		this.redis = redis;
	}

}
