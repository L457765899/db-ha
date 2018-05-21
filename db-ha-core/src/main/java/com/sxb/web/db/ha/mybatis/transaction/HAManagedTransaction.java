package com.sxb.web.db.ha.mybatis.transaction;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.ibatis.session.TransactionIsolationLevel;
import org.apache.ibatis.transaction.managed.ManagedTransaction;

import com.sxb.web.db.ha.HAConfiguration;
import com.sxb.web.db.ha.connection.DefaultSlaveConnectionHolder;
import com.sxb.web.db.ha.connection.SlaveConnectionHolder;

public class HAManagedTransaction extends ManagedTransaction implements HATransaction{
	
	private SlaveConnectionHolder slaveConnectionHolder;

	public HAManagedTransaction(Connection connection,boolean closeConnection) {
		super(connection, closeConnection);
		slaveConnectionHolder = new DefaultSlaveConnectionHolder();
	}

	public HAManagedTransaction(DataSource ds,TransactionIsolationLevel level, boolean closeConnection) {
		super(ds, level, closeConnection);
		slaveConnectionHolder = new DefaultSlaveConnectionHolder();
	}

	@Override
	public void setSlaveDataSource(DataSource slaveDataSource) {
		slaveConnectionHolder.setSlaveDataSource(slaveDataSource);
	}

	@Override
	public Connection getSlaveConnection(boolean isAutoCommit) {
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
