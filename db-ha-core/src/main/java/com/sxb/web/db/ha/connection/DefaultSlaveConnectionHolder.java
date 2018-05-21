package com.sxb.web.db.ha.connection;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

import com.alibaba.druid.pool.DruidPooledConnection;
import com.mysql.jdbc.LoadBalancedConnectionProxy;
import com.mysql.jdbc.MySQLConnection;
import com.sxb.web.db.ha.HAConfiguration;
import com.sxb.web.db.ha.connection.failover.FailOverChain;
import com.sxb.web.db.ha.connection.proxy.ConnectionProxy;
import com.sxb.web.db.ha.connection.proxy.ConnectionProxyFactory;
import com.sxb.web.db.ha.connection.proxy.SlaveConnectionHolderProxy;

public class DefaultSlaveConnectionHolder implements SlaveConnectionHolder{
	
	private final static Logger logger = LoggerFactory.getLogger(DefaultSlaveConnectionHolder.class);

	private DataSource slaveDataSource;
	
	private Connection slaveConnection;
	
	private LoadBalancedConnectionProxy loadBalancedConnectionProxy;
	
	private int usedCount = 0;
	
	private boolean defaultAutoCommit = true;
	
	private boolean defaultReadOnly = false;
	
	private HAConfiguration configuration;
	
	private boolean autoCommit;
	
	@Override
	public SlaveConnectionHolder createProxySlaveConnectionHolder(){
		SlaveConnectionHolderProxy proxy = new SlaveConnectionHolderProxy();
		proxy.setTarget(this);
		proxy.setChain(new FailOverChain());
		proxy.getChain().setFailOverInterceptors(configuration.getFailOverInterceptors());
		
		return ConnectionProxyFactory.newInstance(proxy);
	}

	@Override
	public DataSource getSlaveDataSource() {
		return slaveDataSource;
	}

	@Override
	public void setSlaveDataSource(DataSource slaveDataSource) {
		this.slaveDataSource = slaveDataSource;
	}

	@Override
	public Connection getSlaveConnection(boolean isAutoCommit) {
		if(slaveDataSource == null){
			return null;
		}
		
		this.setAutoCommit(isAutoCommit);
		this.used();
		
		if(slaveConnection != null){
			return slaveConnection;
		}
		
		try {
			Connection conn = slaveDataSource.getConnection();
			this.unwrap(conn);
			ConnectionProxy proxy = new ConnectionProxy(conn);
			Connection connection = ConnectionProxyFactory.newInstance(proxy);
			slaveConnection = this.setReadOnly(connection,isAutoCommit);
		} catch (SQLException e){
			logger.error(e.getMessage(), e);
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
		}	
		
		return slaveConnection;
	}

	@Override
	public void setSlaveConnection(Connection slaveConnection) {
		this.slaveConnection = slaveConnection;
	}

	@Override
	public int getUsedCount() {
		return usedCount;
	}
	
	@Override
	public void close() {
		try{
			if(slaveConnection != null){
				slaveConnection.close();
			}
		}catch(Throwable e){
			logger.error(e.getMessage(), e);
		}finally{
			slaveConnection = null;
			loadBalancedConnectionProxy = null;
		}
	}
	
	@Override
	public void used(){
		this.usedCount++;
	}
	
	@Override
	public Connection setReadOnly(Connection connection,boolean isAutoCommit) throws SQLException{
		defaultReadOnly= connection.isReadOnly();
		defaultAutoCommit = connection.getAutoCommit();
		connection.setReadOnly(true);
		
		if(!isAutoCommit && defaultAutoCommit && configuration.isUseReadOnlyTransaction()){
			connection.setAutoCommit(false);
		}
		
		return connection;
	}
	
	@Override
	public void resetConnection(){
		try {
			if(slaveConnection != null){
				if(configuration.isUseReadOnlyTransaction() && !slaveConnection.getAutoCommit()){
					this.commit();
					slaveConnection.setAutoCommit(defaultAutoCommit);
				}
				
				slaveConnection.setReadOnly(defaultReadOnly);
			}
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	protected void commit() throws SQLException{
		slaveConnection.commit();
	}
	
	protected void unwrap(Connection connection){
		if(connection instanceof DruidPooledConnection){
			DruidPooledConnection druidPooledConnection = (DruidPooledConnection)connection;
			com.alibaba.druid.proxy.jdbc.ConnectionProxy connectionProxy = 
					(com.alibaba.druid.proxy.jdbc.ConnectionProxy) druidPooledConnection.getConnection();
			this.loadBalancedConnectionProxy = 
					(LoadBalancedConnectionProxy) Proxy.getInvocationHandler(connectionProxy.getRawObject());
		}
	}

	public boolean isDefaultAutoCommit() {
		return defaultAutoCommit;
	}

	public void setDefaultAutoCommit(boolean defaultAutoCommit) {
		this.defaultAutoCommit = defaultAutoCommit;
	}

	public boolean isDefaultReadOnly() {
		return defaultReadOnly;
	}

	public void setDefaultReadOnly(boolean defaultReadOnly) {
		this.defaultReadOnly = defaultReadOnly;
	}
	
	public HAConfiguration getConfiguration() {
		return configuration;
	}

	@Override
	public void setConfiguration(HAConfiguration configuration) {
		this.configuration = configuration;
	}

	@Override
	public LoadBalancedConnectionProxy getLoadBalancedConnectionProxy() {
		return loadBalancedConnectionProxy;
	}

	public void setLoadBalancedConnectionProxy(
			LoadBalancedConnectionProxy loadBalancedConnectionProxy) {
		this.loadBalancedConnectionProxy = loadBalancedConnectionProxy;
	}

	@Override
	public boolean isAutoCommit() {
		return autoCommit;
	}

	public void setAutoCommit(boolean autoCommit) {
		this.autoCommit = autoCommit;
	}

	@Override
	public String getHostPortPair() {
		if(loadBalancedConnectionProxy != null){
			try {
				Field currentConnectionField = ReflectionUtils.findField(LoadBalancedConnectionProxy.class, "currentConnection");
				currentConnectionField.setAccessible(true);
				MySQLConnection currentConnection = (MySQLConnection) currentConnectionField.get(loadBalancedConnectionProxy);
				return currentConnection.getHostPortPair();
			} catch (IllegalArgumentException e) {
				logger.error(e.getMessage(), e);
			} catch (IllegalAccessException e) {
				logger.error(e.getMessage(), e);
			}
		}
		return null;
	}
}
