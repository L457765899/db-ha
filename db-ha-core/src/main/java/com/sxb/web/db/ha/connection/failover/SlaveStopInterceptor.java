package com.sxb.web.db.ha.connection.failover;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.druid.pool.DruidPooledConnection;
import com.mysql.jdbc.ConnectionGroupManager;
import com.sxb.web.db.ha.HAConfiguration;

public class SlaveStopInterceptor implements FailOverInterceptor {
	
	private final static Logger logger = LoggerFactory.getLogger(SlaveStopInterceptor.class);

	private HAConfiguration configuration;
	
	@Override
	public Object invoke(FailOverChain chain) throws Exception {
		
		if(chain.getMethod().getName().equals("getSlaveConnection") && configuration.isUseStopCheck()){
			return invokeGetSlaveConnection(chain);
		}
		
		return chain.proceed();
	}

	@Override
	public void setConfiguration(HAConfiguration configuration) {
		this.configuration = configuration;
		StopHost.INSTANCE.setConfiguration(configuration);
	}
	
	protected Object invokeGetSlaveConnection(FailOverChain chain) throws Exception{
		
		if(StopHost.INSTANCE.isAllStop && !StopHost.INSTANCE.isCheckAllStop()){
			return null;
		}
		
		Object result = chain.proceed();
		String hostPortSpec = chain.getTarget().getHostPortPair();
		if(!StopHost.INSTANCE.getStopHostsUseStatus(hostPortSpec)){
			return disableConnection(result);
		}
		
		return result;
	}
	
	protected Object disableConnection(Object result) throws Exception {
		if(result instanceof DruidPooledConnection){
			if(logger.isDebugEnabled()){
				logger.debug("disable DruidPooledConnection "+((DruidPooledConnection)result).getConnectionHolder().getConnectionId());
			}
			((DruidPooledConnection)result).disable();
			return null;
		}
		return result;
	}

	public final static class StopHost {
		
		public final static StopHost INSTANCE = new StopHost();
		
		private Map<String,Integer> stopHostsFailCountMap;
		
		private Map<String,Long> stopHostsStopTimeMap;
		
		private Map<String,Long> stopHostsStartStopTimeMap;
		
		private boolean isAllStop;
		
		private long allStopTime;
		
		private HAConfiguration configuration;
		
		private StopHost(){
			this.stopHostsFailCountMap = new ConcurrentHashMap<String, Integer>();
			this.stopHostsStopTimeMap = new ConcurrentHashMap<String, Long>();
			stopHostsStartStopTimeMap = new ConcurrentHashMap<String, Long>();
			this.isAllStop = false;
			this.allStopTime = 0L;
		}

		public Map<String, Integer> getStopHostsFailCountMap() {
			return stopHostsFailCountMap;
		}
		
		public Map<String, Long> getStopHostsStopTimeMap() {
			return stopHostsStopTimeMap;
		}

		public void incrFailHostsCount(final String hostPortSpec,final String group){
			synchronized (hostPortSpec.intern()) {
				if(stopHostsFailCountMap.containsKey(hostPortSpec)){
					long stopHostsStartStopTime = stopHostsStartStopTimeMap.get(hostPortSpec);
					long nowTime = System.currentTimeMillis();
					if(nowTime - stopHostsStartStopTime <= configuration.getStopCheckCountTime()){
		        		int count = stopHostsFailCountMap.get(hostPortSpec);
		        		count++;
		        		if(logger.isDebugEnabled()){
		        			logger.debug("host "+hostPortSpec+" stop host fail count is "+count);
		        		}
		        		stopHostsFailCountMap.put(hostPortSpec, count);
		        		if(count >= configuration.getStopCheckCount() && this.getStopHostsUseStatus(hostPortSpec)){
		        			stopHostsStopTimeMap.put(hostPortSpec, System.currentTimeMillis());	
	    					try {
								ConnectionGroupManager.removeHost(group, hostPortSpec);			
								logger.error("the group "+group+" host "+hostPortSpec+" has already remove");
							} catch (Throwable e) {
								logger.error(e.getMessage(), e);
								stopHostsStopTimeMap.remove(hostPortSpec);							
							}        				        			
		        		}
					}else{
						if(this.getStopHostsUseStatus(hostPortSpec)){
							if(logger.isDebugEnabled()){
								logger.debug("add stop host fail beyond time limit.");
							}
							this.resetKey(hostPortSpec);
						}
					}
	        	}else{
	        		Date date = new Date();
	        		stopHostsFailCountMap.put(hostPortSpec, 1);
	        		stopHostsStartStopTimeMap.put(hostPortSpec, date.getTime());
	        		if(logger.isDebugEnabled()){
	        			logger.debug("host "+hostPortSpec+" stop host fail count is 1, start time "+date);
	        		}
	        	}
			}
		}
		
		public void resetStopHostsMap(){
			for(String key : stopHostsFailCountMap.keySet()){
				synchronized (key.intern()) {
					this.resetKey(key);
				}
			}
		}
		
		protected void resetKey(String key) {
			stopHostsFailCountMap.remove(key);
			stopHostsStopTimeMap.remove(key);		
			stopHostsStartStopTimeMap.remove(key);
			if(logger.isDebugEnabled()){
				logger.debug("reset Key "+key+".");
			}
		}
		
		public int getStopHostsFailCount(String hostPortSpec){
			if(stopHostsFailCountMap.containsKey(hostPortSpec)){
				return stopHostsFailCountMap.get(hostPortSpec);				
			}
			return 0;
		}
		
		public boolean getStopHostsUseStatus(String hostPortSpec){
			if(stopHostsStopTimeMap.containsKey(hostPortSpec)){
				return false;
			}
			return true;
		}

		public boolean isAllStop() {
			return isAllStop;
		}
		
		public boolean isCheckAllStop(){
			return StopHost.INSTANCE.allStopTime!=0L && 
					StopHost.INSTANCE.allStopTime-System.currentTimeMillis()>configuration.getStopCheckIntervalMillis();
		}

		public void setAllStop(boolean isAllStop) {
			this.isAllStop = isAllStop;
			if(isAllStop){
				Date date = new Date();
				this.allStopTime = date.getTime();
				if(logger.isDebugEnabled()){
					logger.debug("all host has already stop ,stop time is "+date);
				}
			}else{
				this.allStopTime = 0L;
			}
		}

		public void setConfiguration(HAConfiguration configuration) {
			this.configuration = configuration;
		}

		public long getAllStopTime() {
			return allStopTime;
		}

		public Map<String,Long> getStopHostsStartStopTimeMap() {
			return stopHostsStartStopTimeMap;
		}
	}

}
