package com.sxb.lin.db.ha.mybatis.transaction;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.mybatis.spring.transaction.SpringManagedTransaction;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class HASpringManagedTransaction extends SpringManagedTransaction {
	
	private Connection connection;
	
	public HASpringManagedTransaction(DataSource dataSource) {
		super(dataSource);
	}
	
	@Override
	public Connection getConnection() throws SQLException {
		if(connection == null){
			this.connection = super.getConnection();
		}
		return connection;
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
		super.close();
	}

}
