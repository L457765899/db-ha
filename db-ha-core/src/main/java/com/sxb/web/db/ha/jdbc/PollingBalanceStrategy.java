package com.sxb.web.db.ha.jdbc;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

import com.mysql.jdbc.BalanceStrategy;
import com.mysql.jdbc.Connection;
import com.mysql.jdbc.ConnectionImpl;
import com.mysql.jdbc.LoadBalancedConnectionProxy;
import com.sxb.web.db.ha.connection.failover.SlaveStopInterceptor.StopHost;

public class PollingBalanceStrategy implements BalanceStrategy{
	
	private final static Logger logger = LoggerFactory.getLogger(PollingBalanceStrategy.class);
	
	private int currentHostIndex = -1;
	
	private String group;

	@Override
	public void init(Connection conn, Properties props) throws SQLException {
		group = props.getProperty("loadBalanceConnectionGroup");
	}

	@Override
	public void destroy() {
		
	}

	@Override
	public ConnectionImpl pickConnection(LoadBalancedConnectionProxy proxy, List<String> configuredHosts, 
			Map<String, ConnectionImpl> liveConnections,long[] responseTimes, int numRetries) throws SQLException {
		
        int numHosts = configuredHosts.size();
        SQLException ex = null;
        Map<String, Long> blackList = proxy.getGlobalBlacklist();
        List<String> createFialList = new ArrayList<String>();

        for (int attempts = 0; attempts < numRetries;) {
        	
        	Map<String,Long> disableList = new HashMap<String, Long>();
        	disableList.putAll(blackList);
        	disableList.putAll(StopHost.INSTANCE.getStopHostsStopTimeMap());
        	
            if (numHosts == 1) {
                this.currentHostIndex = 0; 
            } else if (this.currentHostIndex == -1) {
                int random = (int) Math.floor((Math.random() * numHosts));

                for (int i = random; i < numHosts; i++) {
                    if (!disableList.containsKey(configuredHosts.get(i))) {
                        this.currentHostIndex = i;
                        break;
                    }
                }

                if (this.currentHostIndex == -1) {
                    for (int i = 0; i < random; i++) {
                        if (!disableList.containsKey(configuredHosts.get(i))) {
                            this.currentHostIndex = i;
                            break;
                        }
                    }
                }

                if (this.currentHostIndex == -1) {
                    blackList = proxy.getGlobalBlacklist(); 

                    try {
                        Thread.sleep(250);
                    } catch (InterruptedException e) {
                    }

                    continue; 
                }       
            } else {

                int i = this.currentHostIndex + 1;
                boolean foundGoodHost = false;

                for (; i < numHosts; i++) {
                    if (!disableList.containsKey(configuredHosts.get(i))) {
                        this.currentHostIndex = i;
                        foundGoodHost = true;
                        break;
                    }
                }

                if (!foundGoodHost) {
                    for (i = 0; i <= this.currentHostIndex; i++) {
                        if (!disableList.containsKey(configuredHosts.get(i))) {
                            this.currentHostIndex = i;
                            foundGoodHost = true;
                            break;
                        }
                    }
                }

                if (!foundGoodHost) {
                	StopHost.INSTANCE.setAllStop(true);
                	String message = "all host has remove or add to global black list,no host to pick connection.";
                	logger.error(message);
                    throw new SQLException(message);
                }
            }

            String hostPortSpec = configuredHosts.get(this.currentHostIndex);

            ConnectionImpl conn = liveConnections.get(hostPortSpec);

            if (conn == null) {
                try {
                    conn = proxy.createConnectionForHost(hostPortSpec);
                    StopHost.INSTANCE.setAllStop(false);
                } catch (SQLException sqlEx) {
                    ex = sqlEx;
                    if (this.shouldExceptionTriggerConnectionSwitch(proxy,sqlEx)) {
                    	
                        proxy.addToGlobalBlacklist(hostPortSpec);                        
                        createFialList.add(hostPortSpec);
                        
                        if(configuredHosts.size() == createFialList.size() + blackList.size()){
                        	StopHost.INSTANCE.setAllStop(true);
                        	String message = "all host has stop,no host to pick connection.";
                        	logger.error(message);
                        	throw new SQLException(message);
                        }else{                      	
                        	//logger.error("the host "+hostPortSpec+" to pick connection has exception : "+sqlEx.getMessage(),sqlEx);
                        	logger.error("the host "+hostPortSpec+" to pick connection has exception : "+sqlEx.getMessage());
                        	StopHost.INSTANCE.incrFailHostsCount(hostPortSpec,group);
                        	continue;
                        }
                    }
                    throw sqlEx;
                }
            }

            return conn;
        }

        if (ex != null) {
            throw ex;
        }

        return null; 
    }

	private boolean shouldExceptionTriggerConnectionSwitch(LoadBalancedConnectionProxy proxy,Throwable t){
		try {
			Method method = ReflectionUtils.findMethod(LoadBalancedConnectionProxy.class, 
					"shouldExceptionTriggerConnectionSwitch",Throwable.class);
			method.setAccessible(true);
			return (boolean) method.invoke(proxy,t);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return true;
		}
	}

}
