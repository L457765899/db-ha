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
            	DruidPooledConnection druidPooledConnection = (DruidPooledConnection) conn;
            	if(druidPooledConnection.isClosed()) {
            		return false;
            	}
                conn = druidPooledConnection.getConnection();
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
        try {
            stmt = conn.createStatement();
            if (validationQueryTimeout > 0) {
                stmt.setQueryTimeout(validationQueryTimeout);
            }
            rs = stmt.executeQuery(query);
        } finally {
        	JdbcUtils.close(rs);
            JdbcUtils.close(stmt);
        }
        
        Connection mySQLConnection = conn;
        if (mySQLConnection instanceof DruidPooledConnection) {
        	mySQLConnection = ((DruidPooledConnection) mySQLConnection).getConnection();
        }
        
        if(mySQLConnection instanceof ConnectionProxy) {
        	mySQLConnection = ((ConnectionProxy) mySQLConnection).getRawObject();
        }
        
    	if(mySQLConnection instanceof ReplicationConnection) {
			ReplicationConnection replicationConnection = (ReplicationConnection) mySQLConnection;
			Statement otherStmt = null;
	        ResultSet otherRs = null;
	        try {
				if(replicationConnection.isMasterConnection() && !conn.isReadOnly()) {
		        	conn.setReadOnly(true);
		        	otherStmt = conn.createStatement();
		            if (validationQueryTimeout > 0) {
		            	otherStmt.setQueryTimeout(validationQueryTimeout);
		            }
		            otherRs = otherStmt.executeQuery(query);
		            conn.setReadOnly(false);
            	}else if(!replicationConnection.isMasterConnection() && conn.isReadOnly()){
			        conn.setReadOnly(false);
            		otherStmt = conn.createStatement();
		            if (validationQueryTimeout > 0) {
		            	otherStmt.setQueryTimeout(validationQueryTimeout);
		            }
		            otherRs = otherStmt.executeQuery(query);
		            conn.setReadOnly(true);
            	}
	        } finally {
	            JdbcUtils.close(otherStmt);
	            JdbcUtils.close(otherRs);
	        }
		}
        
        return true;
    }

}
