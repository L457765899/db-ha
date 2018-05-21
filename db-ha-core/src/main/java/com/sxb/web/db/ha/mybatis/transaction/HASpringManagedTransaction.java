package com.sxb.web.db.ha.mybatis.transaction;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.mybatis.spring.transaction.SpringManagedTransaction;

import com.sxb.web.db.ha.HAConfiguration;
import com.sxb.web.db.ha.connection.DefaultSlaveConnectionHolder;
import com.sxb.web.db.ha.connection.SlaveConnectionHolder;

public class HASpringManagedTransaction extends SpringManagedTransaction implements HATransaction{
	
	private SlaveConnectionHolder slaveConnectionHolder;
	
	public HASpringManagedTransaction(DataSource dataSource) {
		super(dataSource);
		slaveConnectionHolder = new DefaultSlaveConnectionHolder();
	}

	@Override
	public void setSlaveDataSource(DataSource slaveDataSource) {
		slaveConnectionHolder.setSlaveDataSource(slaveDataSource);
	}

	@Override
	public Connection getSlaveConnection(boolean isAutoCommit){
		return slaveConnectionHolder.getSlaveConnection(isAutoCommit);
	}

	@Override
	public void close() throws SQLException {
		super.close();
		slaveConnectionHolder.resetConnection();
		slaveConnectionHolder.close();
	}
	
	@Override
	public void setSlaveConnectionHolder(SlaveConnectionHolder slaveConnectionHolder) {
		this.slaveConnectionHolder = slaveConnectionHolder;
	}

	@Override
	public void setConfiguration(HAConfiguration configuration) {
		this.slaveConnectionHolder.setConfiguration(configuration);
		this.slaveConnectionHolder = this.slaveConnectionHolder.createProxySlaveConnectionHolder();
	}

	public SlaveConnectionHolder getSlaveConnectionHolder() {
		return slaveConnectionHolder;
	}
}
