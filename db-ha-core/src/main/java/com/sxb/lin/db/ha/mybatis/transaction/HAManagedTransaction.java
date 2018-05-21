package com.sxb.lin.db.ha.mybatis.transaction;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.ibatis.session.TransactionIsolationLevel;
import org.apache.ibatis.transaction.managed.ManagedTransaction;

public class HAManagedTransaction extends ManagedTransaction {
	
	private Connection connection;

	public HAManagedTransaction(Connection connection, boolean closeConnection) {
		super(connection, closeConnection);
	}

	public HAManagedTransaction(DataSource ds, TransactionIsolationLevel level,boolean closeConnection) {
		super(ds, level, closeConnection);
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
		if(connection.isReadOnly()){
			connection.setReadOnly(false);
		}
		super.close();
	}
	
}
