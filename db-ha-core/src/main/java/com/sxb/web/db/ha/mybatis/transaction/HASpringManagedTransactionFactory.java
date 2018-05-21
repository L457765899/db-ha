package com.sxb.web.db.ha.mybatis.transaction;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.ibatis.session.TransactionIsolationLevel;
import org.apache.ibatis.transaction.Transaction;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;

import com.alibaba.druid.pool.DruidDataSource;
import com.sxb.web.db.ha.HAConfiguration;

public class HASpringManagedTransactionFactory extends SpringManagedTransactionFactory implements HATransactionFactory{
	
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
		this.configuration = configuration;
	}

	@Override
	public Transaction newTransaction(DataSource dataSource, TransactionIsolationLevel level, boolean autoCommit) {
		HASpringManagedTransaction transaction = new HASpringManagedTransaction(dataSource);
		transaction.setConfiguration(configuration);
		Map<DataSource, DruidDataSource> dataSourceRelationMap = configuration.getDataSourceRelationMap();
		if(dataSourceRelationMap!=null && inited){
			transaction.setSlaveDataSource(dataSourceRelationMap.get(dataSource));
		}
	    return transaction;
	}
	
	@Override
	public void init() throws Exception{
		Map<DataSource, DruidDataSource> dataSourceRelationMap = configuration.getDataSourceRelationMap();
		if(dataSourceRelationMap!=null && dataSourceRelationMap.size()>0){
			Set<Entry<DataSource, DruidDataSource>> entrySet = dataSourceRelationMap.entrySet();
			for(Entry<DataSource, DruidDataSource> entry : entrySet){
				entry.getValue().init();
				setInited(true);
			}
		}
	}
	
}
