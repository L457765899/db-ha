package com.sxb.lin.db.ha.druid;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

import com.alibaba.druid.pool.DruidPooledConnection;
import com.alibaba.druid.pool.ValidConnectionChecker;
import com.alibaba.druid.pool.ValidConnectionCheckerAdapter;
import com.alibaba.druid.proxy.jdbc.ConnectionProxy;
import com.alibaba.druid.util.JdbcUtils;
import com.mysql.jdbc.MySQLConnection;
import com.mysql.jdbc.ReplicationConnection;

public class HAMySqlValidConnectionChecker extends ValidConnectionCheckerAdapter 
		implements ValidConnectionChecker, Serializable {
	
	private static final long serialVersionUID = 1L;

    public static final String DEFAULT_VALIDATION_QUERY = "SELECT 1";
    
    private boolean usePingMethod = true;

    public HAMySqlValidConnectionChecker(){
        configFromProperties(System.getProperties());
    }

    @Override
    public void configFromProperties(Properties properties) {
        String property = properties.getProperty("druid.mysql.usePingMethod");
        if ("true".equals(property)) {
            setUsePingMethod(true);
        } else if ("false".equals(property)) {
            setUsePingMethod(false);
        }
    }

    public boolean isUsePingMethod() {
        return usePingMethod;
    }

    public void setUsePingMethod(boolean usePingMethod) {
        this.usePingMethod = usePingMethod;
    }

    public boolean isValidConnection(Connection conn, String validateQuery, int validationQueryTimeout) throws Exception {
        if (conn.isClosed()) {
            return false;
        }

        if (usePingMethod) {
            if (conn instanceof DruidPooledConnection) {
                conn = ((DruidPooledConnection) conn).getConnection();
            }

            if (conn instanceof ConnectionProxy) {
                conn = ((ConnectionProxy) conn).getRawObject();
            }

            if (conn instanceof MySQLConnection) {
            	MySQLConnection mySQLConnection = (MySQLConnection) conn;
            	mySQLConnection.ping();
                return true;
            }
        }

        String query = validateQuery;
        if (validateQuery == null || validateQuery.isEmpty()) {
            query = DEFAULT_VALIDATION_QUERY;
        }
        
        Statement stmt = null;
        ResultSet rs = null;
        Statement slavesStmt = null;
        ResultSet slavesRs = null;
        try {
            stmt = conn.createStatement();
            if (validationQueryTimeout > 0) {
                stmt.setQueryTimeout(validationQueryTimeout);
            }
            rs = stmt.executeQuery(query);
            
			if (conn instanceof DruidPooledConnection) {
			    conn = ((DruidPooledConnection) conn).getConnection();
			}
			
			if (conn instanceof ConnectionProxy) {
			    conn = ((ConnectionProxy) conn).getRawObject();
			}
            if(conn instanceof ReplicationConnection) {
            	ReplicationConnection replicationConnection = (ReplicationConnection) conn;
            	Connection slavesConnection = replicationConnection.getSlavesConnection();
            	Connection masterConnection = replicationConnection.getMasterConnection();
            	if(slavesConnection != null && replicationConnection.isMasterConnection()) {
            		slavesStmt = slavesConnection.createStatement();
            		if (validationQueryTimeout > 0) {
            			slavesStmt.setQueryTimeout(validationQueryTimeout);
                    }
            		slavesRs = slavesStmt.executeQuery(query);
            	}else if(masterConnection != null && !replicationConnection.isMasterConnection()) {
            		slavesStmt = masterConnection.createStatement();
            		if (validationQueryTimeout > 0) {
            			slavesStmt.setQueryTimeout(validationQueryTimeout);
                    }
            		slavesRs = slavesStmt.executeQuery(query);
            	}
            }
            
            return true;
        } finally {
            JdbcUtils.close(rs);
            JdbcUtils.close(stmt);
            JdbcUtils.close(slavesRs);
            JdbcUtils.close(slavesStmt);
        }

    }

}
