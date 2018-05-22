package com.sxb.lin.db.ha.mybatis.interceptor;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.ibatis.executor.statement.BaseStatementHandler;
import org.apache.ibatis.executor.statement.RoutingStatementHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.ReflectionUtils;

import com.mysql.jdbc.ReplicationConnectionProxy;

@Intercepts({
	@Signature(
		method = "prepare", 
		type = StatementHandler.class, 
		args = {  
			Connection.class
		}
	)
})
public class QueryInterceptor implements Interceptor{
	
	protected Logger logger = LoggerFactory.getLogger(QueryInterceptor.class);
	
	private AtomicInteger readFromMasterWhenNoSlavesCount = new AtomicInteger(0);
	
	private int notSwitchWhenNoSlavesCount = 30;
	
	private String today = this.getCurrentDayStr();
	
	private boolean noSlaves = false;

	@Override
	public Object intercept(Invocation invocation) throws Throwable {
		
		if(!TransactionSynchronizationManager.isSynchronizationActive()){
			this.switchToSlavesConnectionWhenReadOnly(invocation);//no TransactionDefinition no transaction
		}else if(!TransactionSynchronizationManager.isActualTransactionActive()){
			this.switchToSlavesConnectionWhenReadOnly(invocation);//have TransactionDefinition no transaction
		}else if(TransactionSynchronizationManager.isCurrentTransactionReadOnly()){
			//JtaTransactionManager not support read-only transaction
			//DataSourceTransactionManager support read-only transaction
			if(logger.isDebugEnabled()){
				MappedStatement mappedStatement = this.getMappedStatement(invocation);
				logger.debug("have a read-only transaction,so not switchToSlavesConnection,the mapper id is "+mappedStatement.getId());
			}
			this.switchToMasterConnectionWhenNoSlaves(invocation);
		}else if(!TransactionSynchronizationManager.isCurrentTransactionReadOnly()){
			if(logger.isDebugEnabled()){
				MappedStatement mappedStatement = this.getMappedStatement(invocation);
				logger.debug("have a read-write transaction,so not switchToSlavesConnection,the mapper id is "+mappedStatement.getId());
			}
		}
		
		return invocation.proceed();
	}

	@Override
	public Object plugin(Object target) {
		return Plugin.wrap(target, this);
	}

	@Override
	public void setProperties(Properties properties) {
		
	}
	
	protected StatementHandler getStatementHandler(Invocation invocation) throws IllegalArgumentException, IllegalAccessException{
		
		RoutingStatementHandler routingStatementHandler = (RoutingStatementHandler) invocation.getTarget();
		Field delegateField = ReflectionUtils.findField(RoutingStatementHandler.class, "delegate");
		delegateField.setAccessible(true);
		StatementHandler statementHandler = (StatementHandler) delegateField.get(routingStatementHandler);
		
		return statementHandler;
	}

	protected MappedStatement getMappedStatement(Invocation invocation) throws IllegalArgumentException, IllegalAccessException {
		
		StatementHandler statementHandler = this.getStatementHandler(invocation);
		Field mappedStatementField = ReflectionUtils.findField(BaseStatementHandler.class, "mappedStatement");
		mappedStatementField.setAccessible(true);
		MappedStatement mappedStatement = (MappedStatement) mappedStatementField.get(statementHandler);
		
		return mappedStatement;
	}
	
	/*protected Executor getExecutor(Invocation invocation) throws IllegalArgumentException, IllegalAccessException {
		
		StatementHandler statementHandler = this.getStatementHandler(invocation);
		Field executorField = ReflectionUtils.findField(BaseStatementHandler.class, "executor");
		executorField.setAccessible(true);
		Executor executor = (Executor) executorField.get(statementHandler);
		
		return executor;
	}
	
	protected BoundSql getBoundSql(Invocation invocation) throws IllegalArgumentException, IllegalAccessException {
		
		StatementHandler statementHandler = this.getStatementHandler(invocation);
		Field boundSqlField = ReflectionUtils.findField(BaseStatementHandler.class, "boundSql");
		boundSqlField.setAccessible(true);
		BoundSql boundSql = (BoundSql) boundSqlField.get(statementHandler);
		
		return boundSql;
	}*/
	
	protected void switchToSlavesConnectionWhenReadOnly(Invocation invocation) throws Exception {
		
		MappedStatement mappedStatement = this.getMappedStatement(invocation);
		SqlCommandType sqlCommandType = mappedStatement.getSqlCommandType();
		
		if(sqlCommandType == SqlCommandType.SELECT && !noSlaves){
			
			String currentDay = this.getCurrentDayStr();
			if(!today.equals(currentDay)){
				readFromMasterWhenNoSlavesCount.set(0);
				today = currentDay;
			}
			
			if(notSwitchWhenNoSlavesCount < readFromMasterWhenNoSlavesCount.intValue()){
				logger.warn("please check all slaves,possible all slaves are dead.");
				return;
			}
			
			Connection connection = (Connection) invocation.getArgs()[0];
			if(connection.isReadOnly()){
				return;//second third fourth...... getConnection of the session when slave active
			}
			
			//first getConnection of the session when slave active
			Connection unwrap = connection.unwrap(Connection.class);
			if(!Proxy.isProxyClass(unwrap.getClass())){
				return;
			}
			
			//avoid readOnly=true is master connection
			InvocationHandler invocationHandler = Proxy.getInvocationHandler(unwrap);
			if(invocationHandler instanceof ReplicationConnectionProxy){
				ReplicationConnectionProxy proxy = (ReplicationConnectionProxy) invocationHandler;
				if(!proxy.isSlavesConnection()){
					proxy.setReadOnly(true);
					if(!proxy.isSlavesConnection()){
						int incr = readFromMasterWhenNoSlavesCount.incrementAndGet();
						if(incr == notSwitchWhenNoSlavesCount + 1){
							logger.error("can not switch to slaves connection when read-only,possible all slaves are dead.");
						}else{
							logger.error("can not switch to slaves connection when read-only,please check all slaves.");
						}
					}
				}
			}
			
		}
		
	}
	
	protected void switchToMasterConnectionWhenNoSlaves(Invocation invocation) throws Exception {
		if(noSlaves){
			Connection connection = (Connection) invocation.getArgs()[0];
			if(!connection.isReadOnly()){
				return;//JtaTransactionManager not support read-only transaction
			}
			
			Connection unwrap = connection.unwrap(Connection.class);
			if(!Proxy.isProxyClass(unwrap.getClass())){
				return;
			}
			
			InvocationHandler invocationHandler = Proxy.getInvocationHandler(unwrap);
			if(invocationHandler instanceof ReplicationConnectionProxy){
				ReplicationConnectionProxy proxy = (ReplicationConnectionProxy) invocationHandler;
				if(proxy.isSlavesConnection()){
					logger.warn("no slaves can use,every connection will switch to master connection.");
					this.doSwitchToMasterConnection(proxy);
				}
			}
		}
	}
	
	protected void doSwitchToMasterConnection(ReplicationConnectionProxy proxy) throws Exception {
		Field currentConnectionField = ReflectionUtils.findField(ReplicationConnectionProxy.class, "currentConnection");
		currentConnectionField.setAccessible(true);
		com.mysql.jdbc.Connection currentConnection = (com.mysql.jdbc.Connection)currentConnectionField.get(proxy);
		com.mysql.jdbc.Connection masterConnection = proxy.getMasterConnection();
		
		masterConnection.setReadOnly(currentConnection.isReadOnly());
		masterConnection.setAutoCommit(currentConnection.getAutoCommit());
		masterConnection.setCatalog(currentConnection.getCatalog());
		masterConnection.setTransactionIsolation(currentConnection.getTransactionIsolation());
		masterConnection.setSessionMaxRows(currentConnection.getSessionMaxRows());
		currentConnectionField.set(proxy, masterConnection);
	}
	
	public String getCurrentDayStr() {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        return f.format(c.getTime());
    }

	public int getNotSwitchWhenNoSlavesCount() {
		return notSwitchWhenNoSlavesCount;
	}

	public void setNotSwitchWhenNoSlavesCount(int notSwitchWhenNoSlavesCount) {
		this.notSwitchWhenNoSlavesCount = notSwitchWhenNoSlavesCount;
	}

	public boolean isNoSlaves() {
		return noSlaves;
	}

	public void setNoSlaves(boolean noSlaves) {
		this.noSlaves = noSlaves;
	}
	
}
