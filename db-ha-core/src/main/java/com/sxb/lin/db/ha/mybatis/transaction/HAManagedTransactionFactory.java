package com.sxb.lin.db.ha.mybatis.transaction;

import java.sql.Connection;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.session.TransactionIsolationLevel;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.transaction.managed.ManagedTransactionFactory;

public class HAManagedTransactionFactory extends ManagedTransactionFactory {
	
	private boolean closeConnection = true;

	public void setProperties(Properties props) {
		if (props != null) {
			String closeConnectionProperty = props.getProperty("closeConnection");
			if (closeConnectionProperty != null) {
				closeConnection = Boolean.valueOf(closeConnectionProperty);
			}
		}
	}

	@Override
	public Transaction newTransaction(Connection conn) {
		return new HAManagedTransaction(conn,closeConnection);
	}

	@Override
	public Transaction newTransaction(DataSource ds,TransactionIsolationLevel level, boolean autoCommit) {
		return new HAManagedTransaction(ds,level,closeConnection);
	}
	
}
