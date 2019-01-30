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
import com.mysql.jdbc.MySQLConnection;
import com.mysql.jdbc.ReplicationConnection;

public class HAAtomikosXAPooledConnection extends AtomikosXAPooledConnection {
	
	private Connection connection;
	
	private boolean usePingMethod = true;

	public HAAtomikosXAPooledConnection(XAConnection xaConnection, 
			JdbcTransactionalResource jdbcTransactionalResource,
			ConnectionPoolProperties props) throws SQLException {
		super(xaConnection, jdbcTransactionalResource, props);
		this.connection = xaConnection.getConnection();
		if(props instanceof HAAtomikosDataSourceBean) {
			this.usePingMethod = ((HAAtomikosDataSourceBean) props).isUsePingMethod();
		}
	}

	@Override
	protected void testUnderlyingConnection() throws CreateConnectionException {
		
		if(usePingMethod) {
			try {
				Connection unwrap = connection.unwrap(Connection.class);
				if(unwrap instanceof MySQLConnection) {
					MySQLConnection mySQLConnection = (MySQLConnection) unwrap;
	            	mySQLConnection.ping();
	                return;
				}
			} catch (SQLException e) {
				throw new CreateConnectionException ( "Error unwrap connection" ,  e );
			}
		}
		
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
		
		try {
			Connection unwrap = connection.unwrap(Connection.class);
			if(unwrap instanceof ReplicationConnection) {
				ReplicationConnection replicationConnection = (ReplicationConnection) unwrap;
				PreparedStatement stmt = null;
				try {
					if(replicationConnection.isMasterConnection() 
		        			&& !connection.isReadOnly() && useSlave) {
		        		connection.setReadOnly(true);
		        		stmt = connection.prepareStatement(testQuery);
						stmt.execute();
						connection.setReadOnly(false);
		        	}else if(!replicationConnection.isMasterConnection() 
		        			&& connection.isReadOnly() && !useSlave) {
		        		connection.setReadOnly(false);
		        		stmt = connection.prepareStatement(testQuery);;
		        		stmt.execute();
		        		connection.setReadOnly(true);
		        	}
				} catch (SQLException e) {
					throw new CreateConnectionException ( "Error executing testQuery" ,  e );
				} finally {
					if(stmt != null) {
						stmt.close();
					}
				}
			}
		} catch (SQLException e) {
			throw new CreateConnectionException ( "Error unwrap connection" ,  e );
		}
	}

	@Override
	public void destroy() {
		super.destroy();
		connection = null;
	}

}
