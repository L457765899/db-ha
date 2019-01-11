package com.sxb.lin.db.ha.atomikos.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.sql.XAConnection;

import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.atomikos.datasource.pool.ConnectionPoolProperties;
import com.atomikos.datasource.pool.CreateConnectionException;
import com.atomikos.datasource.xa.jdbc.JdbcTransactionalResource;
import com.atomikos.jdbc.AtomikosXAPooledConnection;
import com.mysql.jdbc.ReplicationConnection;

public class HAAtomikosXAPooledConnection extends AtomikosXAPooledConnection{
	
	private Connection connection;

	public HAAtomikosXAPooledConnection(XAConnection xaConnection, 
			JdbcTransactionalResource jdbcTransactionalResource,
			ConnectionPoolProperties props) throws SQLException {
		super(xaConnection, jdbcTransactionalResource, props);
		this.connection = xaConnection.getConnection();
	}

	@Override
	protected void testUnderlyingConnection() throws CreateConnectionException {
		super.testUnderlyingConnection();
		String testQuery = getTestQuery();
		if(testQuery == null) {
			return;
		}
		
        boolean useSlave = false;
		if(!TransactionSynchronizationManager.isSynchronizationActive()){
			//no TransactionDefinition no transaction
			useSlave = true;
		}else if(!TransactionSynchronizationManager.isActualTransactionActive()){
			//have TransactionDefinition no transaction
			if(TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
				//have readOnly TransactionDefinition no transaction
				useSlave = true;
			}
		}else if(TransactionSynchronizationManager.isCurrentTransactionReadOnly()){
			//have TransactionDefinition readOnly transaction
			useSlave = true;
		}else if(!TransactionSynchronizationManager.isCurrentTransactionReadOnly()){
			useSlave = false;
		}
		
		Connection unwrap = null;
		try {
			unwrap = connection.unwrap(Connection.class);
		} catch (SQLException e) {
			throw new CreateConnectionException ( "Error unwrap connection" ,  e );
		}
        
		if(unwrap instanceof ReplicationConnection) {
			try {
				ReplicationConnection replicationConnection = (ReplicationConnection) unwrap;
	        	Connection slavesConnection = replicationConnection.getSlavesConnection();
	        	Connection masterConnection = replicationConnection.getMasterConnection();
	        	if(replicationConnection.isMasterConnection() && useSlave 
	        			&& slavesConnection != null && !slavesConnection.isClosed()) {
	        		PreparedStatement stmt = slavesConnection.prepareStatement(testQuery);;
					stmt.execute();
					stmt.close();
	        	}else if(!replicationConnection.isMasterConnection() && !useSlave 
	        			&& masterConnection != null && !masterConnection.isClosed()) {
	        		PreparedStatement stmt = masterConnection.prepareStatement(testQuery);;
	        		stmt.execute();
	        		stmt.close();
	        	}
			} catch (SQLException e) {
				throw new CreateConnectionException ( "Error executing testQuery" ,  e );
			}
		}
	}

}
