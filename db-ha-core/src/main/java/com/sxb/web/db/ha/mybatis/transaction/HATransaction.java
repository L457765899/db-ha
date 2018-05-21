package com.sxb.web.db.ha.mybatis.transaction;

import java.sql.Connection;

import javax.sql.DataSource;

import org.apache.ibatis.transaction.Transaction;

import com.sxb.web.db.ha.HAConfiguration;
import com.sxb.web.db.ha.connection.SlaveConnectionHolder;

public interface HATransaction extends Transaction{

	void setSlaveConnectionHolder(SlaveConnectionHolder slaveConnectionHolder);

	void setSlaveDataSource(DataSource slaveDataSource);

	Connection getSlaveConnection(boolean isAutoCommit);
	
	void setConfiguration(HAConfiguration configuration);
}
