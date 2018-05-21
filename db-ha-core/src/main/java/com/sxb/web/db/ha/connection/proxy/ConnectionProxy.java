package com.sxb.web.db.ha.connection.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.Connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sxb.web.db.ha.util.Md5Util;

public class ConnectionProxy implements InvocationHandler{
	
	private Logger logger;	
	
	private Connection targetConnection;
	
	private String id;
	
	public ConnectionProxy(Connection targetConnection) {
		this.targetConnection = targetConnection;
		this.logger = LoggerFactory.getLogger(ConnectionProxy.class);
		this.id = Md5Util.md5Hex(targetConnection.toString());
	}
	
	public Logger getLogger() {
		return logger;
	}

	public void setLogger(Logger logger) {
		this.logger = logger;
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {  
		if(method.getName().equals("close")){
			this.closeLog();
		}
		return method.invoke(targetConnection, args);
	}

	public void createLog(){
		logger.debug("id is "+id+" connection is create");
	}
	
	public void closeLog(){
		logger.debug("id is "+id+" connection is close");
	}
	
	public Connection getTargetConnection(){
		return targetConnection;
	}
	
}
