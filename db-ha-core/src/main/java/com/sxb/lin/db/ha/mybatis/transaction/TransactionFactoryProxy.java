package com.sxb.lin.db.ha.mybatis.transaction;

import java.sql.Connection;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.session.TransactionIsolationLevel;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.transaction.TransactionFactory;

public class TransactionFactoryProxy implements TransactionFactory{
	
	private TransactionFactory transactionFactory;
	
	public TransactionFactoryProxy(TransactionFactory transactionFactory) {
		this.transactionFactory = transactionFactory;
	}

	@Override
	public void setProperties(Properties props) {
		transactionFactory.setProperties(props);
	}

	@Override
	public Transaction newTransaction(Connection conn) {
		Transaction transaction = transactionFactory.newTransaction(conn);
		return new TransactionProxy(transaction);
	}

	@Override
	public Transaction newTransaction(DataSource dataSource,
			TransactionIsolationLevel level, boolean autoCommit) {
		Transaction transaction = transactionFactory.newTransaction(dataSource, level, autoCommit);
		return new TransactionProxy(transaction);
	}

}
