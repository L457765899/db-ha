package com.sxb.web.db.ha.connection.proxy;

import java.lang.reflect.Proxy;
import java.sql.Connection;

import com.sxb.web.db.ha.connection.SlaveConnectionHolder;

public class ConnectionProxyFactory {

	public static Connection newInstance(ConnectionProxy proxy){
		
		Connection connection = (Connection)Proxy.newProxyInstance(
				Connection.class.getClassLoader(), new Class[] {Connection.class}, proxy);
		proxy.createLog();
		return connection;
		
	}
	
	public static SlaveConnectionHolder newInstance(SlaveConnectionHolderProxy proxy){
		
		SlaveConnectionHolder slaveConnectionHolder = (SlaveConnectionHolder)Proxy.newProxyInstance(
				SlaveConnectionHolder.class.getClassLoader(), new Class[] {SlaveConnectionHolder.class}, proxy);
		return slaveConnectionHolder;
		
	}
}
