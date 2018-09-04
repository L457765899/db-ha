package com.sxb.lin.db.ha.mybatis.transaction;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.ibatis.transaction.Transaction;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class TransactionProxy implements Transaction {
	
	private Transaction transaction;
	
	private Connection connection;
	
	public TransactionProxy(Transaction transaction) {
		this.transaction = transaction;
	}

	@Override
	public Connection getConnection() throws SQLException {
		if(connection == null){
			this.connection = transaction.getConnection();
		}
		return connection;
	}

	@Override
	public void commit() throws SQLException {
		transaction.commit();
	}

	@Override
	public void rollback() throws SQLException {
		transaction.rollback();
	}

	@Override
	public void close() throws SQLException {
		if(connection != null){
			if(!TransactionSynchronizationManager.isSynchronizationActive()){
				if(connection.isReadOnly()){
					connection.setReadOnly(false);
				}
			}else if(!TransactionSynchronizationManager.isActualTransactionActive()){
				if(connection.isReadOnly()){
					connection.setReadOnly(false);
				}
			}
		}
		transaction.close();
	}

	@Override
	public Integer getTimeout() throws SQLException {
		return transaction.getTimeout();
	}

}
