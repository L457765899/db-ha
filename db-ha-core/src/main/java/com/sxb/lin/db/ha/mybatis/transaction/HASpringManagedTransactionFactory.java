package com.sxb.lin.db.ha.mybatis.transaction;

import javax.sql.DataSource;

import org.apache.ibatis.session.TransactionIsolationLevel;
import org.apache.ibatis.transaction.Transaction;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;

public class HASpringManagedTransactionFactory extends SpringManagedTransactionFactory {
	
	@Override
	public Transaction newTransaction(DataSource dataSource, TransactionIsolationLevel level, boolean autoCommit) {
	    return new HASpringManagedTransaction(dataSource);
	}
	
}
