package com.sxb.lin.db.ha.atomikos.jdbc;

import java.lang.reflect.Field;
import java.sql.SQLException;

import javax.sql.XAConnection;
import javax.sql.XADataSource;

import org.springframework.util.ReflectionUtils;

import com.atomikos.datasource.pool.ConnectionFactory;
import com.atomikos.datasource.pool.ConnectionPoolProperties;
import com.atomikos.datasource.pool.CreateConnectionException;
import com.atomikos.datasource.pool.XPooledConnection;
import com.atomikos.datasource.xa.jdbc.JdbcTransactionalResource;
import com.atomikos.logging.Logger;
import com.atomikos.logging.LoggerFactory;

public class HAAtomikosXAConnectionFactory implements ConnectionFactory{
	
	private static final Logger LOGGER = LoggerFactory.createLogger(HAAtomikosXAConnectionFactory.class);
	
	private JdbcTransactionalResource jdbcTransactionalResource;
	
	private XADataSource xaDataSource;
	
	private ConnectionPoolProperties props;
	
	public HAAtomikosXAConnectionFactory(ConnectionFactory connectionFactory) throws IllegalArgumentException, IllegalAccessException {
		
		Field jdbcTransactionalResourceField = ReflectionUtils.findField(connectionFactory.getClass(), "jdbcTransactionalResource");
		jdbcTransactionalResourceField.setAccessible(true);
		this.jdbcTransactionalResource = (JdbcTransactionalResource) jdbcTransactionalResourceField.get(connectionFactory);
		
		Field xaDataSourceField = ReflectionUtils.findField(connectionFactory.getClass(), "xaDataSource");
		xaDataSourceField.setAccessible(true);
		this.xaDataSource = (XADataSource) xaDataSourceField.get(connectionFactory);
		
		Field propsField = ReflectionUtils.findField(connectionFactory.getClass(), "props");
		propsField.setAccessible(true);
		this.props = (ConnectionPoolProperties) propsField.get(connectionFactory);
	}

	@Override
	public XPooledConnection createPooledConnection() throws CreateConnectionException {
		try {
			XAConnection xaConnection = xaDataSource.getXAConnection();
			return new HAAtomikosXAPooledConnection ( xaConnection, jdbcTransactionalResource, props );
		} catch ( SQLException e ) {
			String msg = "XAConnectionFactory: failed to create pooled connection - DBMS down or unreachable?";
			LOGGER.logWarning ( msg , e );
			throw new CreateConnectionException ( msg , e );
		}
	}

}
