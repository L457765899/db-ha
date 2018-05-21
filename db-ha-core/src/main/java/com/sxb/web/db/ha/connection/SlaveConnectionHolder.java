package com.sxb.web.db.ha.connection;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.mysql.jdbc.LoadBalancedConnectionProxy;
import com.sxb.web.db.ha.HAConfiguration;

public interface SlaveConnectionHolder {
	
	DataSource getSlaveDataSource();
	
	void setSlaveDataSource(DataSource readDataSource);
	
	Connection getSlaveConnection(boolean isAutoCommit);
	
	void setSlaveConnection(Connection slaveConnection);
	
	void close();
	
	void used();
	
	int getUsedCount();

	Connection setReadOnly(Connection connection, boolean isAutoCommit) throws SQLException;
	
	void setConfiguration(HAConfiguration configuration);

	SlaveConnectionHolder createProxySlaveConnectionHolder();
	
	LoadBalancedConnectionProxy getLoadBalancedConnectionProxy();

	boolean isAutoCommit();

	void resetConnection();
	
	String getHostPortPair();
}
