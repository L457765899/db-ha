package com.sxb.web.db.ha.mybatis.transaction;

import java.sql.Connection;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.session.TransactionIsolationLevel;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.transaction.managed.ManagedTransactionFactory;

import com.alibaba.druid.pool.DruidDataSource;
import com.sxb.web.db.ha.HAConfiguration;

public class HAManagedTransactionFactory extends ManagedTransactionFactory implements HATransactionFactory {
	
	private DruidDataSource slaveDataSource;
	
	private boolean closeConnection = true;
	
	private boolean inited = false;
	
	private HAConfiguration configuration;

	@Override
	public boolean isInited() {
		return inited;
	}

	@Override
	public void setInited(boolean inited) {
		this.inited = inited;
	}

	public HAConfiguration getConfiguration() {
		return configuration;
	}
	
	@Override
	public void setConfiguration(HAConfiguration configuration) {
		slaveDataSource = configuration.getDataSourceRelationMap().values().iterator().next();
		this.configuration = configuration;
	}

	@Override
	public void setProperties(Properties props) {
	    if (props != null) {
	        String closeConnectionProperty = props.getProperty("closeConnection");
	        if (closeConnectionProperty != null) {
	        	closeConnection = Boolean.valueOf(closeConnectionProperty);
	        }
	    }
	}
	
	protected Transaction setValue(HAManagedTransaction transaction){
		transaction.setConfiguration(configuration);
		if(slaveDataSource != null && inited){
			transaction.setSlaveDataSource(slaveDataSource);
		}
		return transaction;
	}

	@Override
	public Transaction newTransaction(Connection conn) {
		HAManagedTransaction transaction = new HAManagedTransaction(conn,closeConnection);
		return setValue(transaction);
	}

	@Override
	public Transaction newTransaction(DataSource ds,TransactionIsolationLevel level, boolean autoCommit) {
		HAManagedTransaction transaction = new HAManagedTransaction(ds, level, closeConnection);
	    return setValue(transaction);
	}

	@Override
	public void init() throws Exception{
		if(slaveDataSource != null){
			slaveDataSource.init();
			setInited(true);
		}
	}
	
}
