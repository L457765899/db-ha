package com.sxb.lin.db.ha.mybatis.interceptor;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.ReflectionUtils;

import com.alibaba.druid.util.JdbcUtils;
import com.mysql.jdbc.ReplicationConnectionProxy;
import com.sxb.lin.db.ha.slave.SlaveQuerier;

@Intercepts({
	@Signature(
		method = "prepare", 
		type = StatementHandler.class, 
		args = {  
			Connection.class,
			Integer.class
		}
	)
})
public class QueryInterceptor implements Interceptor{
	
	private final static Logger logger = LoggerFactory.getLogger(QueryInterceptor.class);
	
	private final static AtomicBoolean needValidateSlaveStatus = new AtomicBoolean(true);
	
	private int notSwitchWhenNoSlavesCount = 30;
	
	@Value("${spring.db.ha.noSlaves:false}")
	private boolean noSlaves = false;
	
	private int initialDelay = 10;
	
	private int period = 60;
	
	private SlaveQuerier slaveQuerier;
	
	private ScheduledExecutorService scheduledExecutorService = null;
	
	protected void init(){
		scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
		scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				//logger.info("compare and set needValidateSlaveStatus.");
				needValidateSlaveStatus.compareAndSet(false, true);
			}
		}, initialDelay, period, TimeUnit.SECONDS);
		logger.info("QueryInterceptor have start.");
	}
	
	protected void destroy(){
		if(scheduledExecutorService != null){
			if(scheduledExecutorService.isShutdown()){
				scheduledExecutorService = null;
			}else{
				scheduledExecutorService.shutdown();
				scheduledExecutorService = null;
			}
		}
	}

	@Override
	public Object intercept(Invocation invocation) throws Throwable {
		
		if(!TransactionSynchronizationManager.isSynchronizationActive()){
			this.switchToSlavesConnectionWhenReadOnly(invocation,false);//no TransactionDefinition no transaction
		}else if(!TransactionSynchronizationManager.isActualTransactionActive()){
			this.switchToSlavesConnectionWhenReadOnly(invocation,true);//have TransactionDefinition no transaction
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
	
	protected ReplicationConnectionProxy getProxy(Connection connection) throws SQLException{
		Connection unwrap = connection.unwrap(Connection.class);
		if(Proxy.isProxyClass(unwrap.getClass())){
			InvocationHandler invocationHandler = Proxy.getInvocationHandler(unwrap);
			if(invocationHandler instanceof ReplicationConnectionProxy){
				ReplicationConnectionProxy proxy = (ReplicationConnectionProxy) invocationHandler;
				return proxy;
			}
		}
		return null;	
	}
	
	protected void switchToSlavesConnectionWhenReadOnly(Invocation invocation,
			boolean isHaveTransactionDefinition) throws Exception {
		
		MappedStatement mappedStatement = this.getMappedStatement(invocation);
		SqlCommandType sqlCommandType = mappedStatement.getSqlCommandType();
		
		if(sqlCommandType != SqlCommandType.SELECT){
			if(!isHaveTransactionDefinition){
				try {
					this.notAllowedUpdateWithNoTransactionDefinition(mappedStatement);
				} catch (NotAllowedUpdateException e) {
					logger.warn(e.getMessage(),e);
				}
			}
			return;
		}
		
		if(!TransactionSynchronizationManager.isCurrentTransactionReadOnly() 
				&& isHaveTransactionDefinition){
			return;
		}
		
		Connection connection = (Connection) invocation.getArgs()[0];
		ReplicationConnectionProxy proxy = this.getProxy(connection);
		if(proxy == null){
			return;
		}
		
		if(noSlaves){
			
			this.validateSlaveIsAlreadyFixed();
			
		}else{
			
			if(connection.isReadOnly()){
				this.validateSlaveStatus(connection,proxy);
				return;//have TransactionDefinition no transaction,second third fourth...... getConnection of the session when slave active
			}
			
			//first getConnection of the session when slave active
			if(!proxy.isSlavesConnection()){
				connection.setReadOnly(true);
				//readOnly=true is master connection
				if(!proxy.isSlavesConnection()){
					logger.error("can not switch to slaves connection when read-only,possible all slaves are dead.");
				}else{
					this.validateSlaveStatus(connection,proxy);
				}
			}
			
		}
		
	}
	
	protected void switchToMasterConnectionWhenNoSlaves(Invocation invocation) throws Exception {
		Connection connection = (Connection) invocation.getArgs()[0];
		if(!connection.isReadOnly()){
			try {
				this.notSupportReadOnlyWithJtaTransactionManager(invocation);
			} catch (NotSupportReadOnlyException e) {
				logger.warn(e.getMessage(),e);
			}
			
			return;//JtaTransactionManager not support read-only transaction
		}
		
		if(noSlaves){
			ReplicationConnectionProxy proxy = this.getProxy(connection);
			if(proxy == null){
				return;
			}
			
			if(proxy.isSlavesConnection()){
				logger.warn("no slaves can use,every connection will switch to master connection.");
				this.doSwitchToMasterConnection(proxy);
			}
			
			this.validateSlaveIsAlreadyFixed();
		}else{
			this.validateSlaveStatus(connection,null);
		}
	}
	
	private void doSwitchToMasterConnection(ReplicationConnectionProxy proxy) throws Exception {
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
	
	private void validateSlaveStatus(Connection connection,ReplicationConnectionProxy proxy) throws SQLException{
		if(proxy == null){
			proxy = this.getProxy(connection);
			if(proxy == null){
				return;
			}
		}
		
		if(proxy.isSlavesConnection() && needValidateSlaveStatus.compareAndSet(true, false)){
			Statement stmt = null;
            ResultSet rs = null;
            try {
            	String io = "",sql="";
            	long startTime = System.currentTimeMillis();
                stmt = connection.createStatement();
                rs = stmt.executeQuery("show slave status");
                while(rs.next()){
                	String Slave_IO_Running = rs.getString("Slave_IO_Running");
                	String Slave_SQL_Running = rs.getString("Slave_SQL_Running");
                	if(Slave_IO_Running.equals("No") || Slave_SQL_Running.equals("No")){
                		this.setNoSlaves(true);
                	}
                	io += Slave_IO_Running;
                	sql += Slave_SQL_Running;
                }
                long endTime = System.currentTimeMillis();
            	long time = endTime - startTime;
            	logger.info("{Slave_IO_Running:"+io+",Slave_SQL_Running:"+sql+",time:"+time+"}");
            } catch(Throwable e) {
            	logger.info(e.getMessage(),e);
            } finally {
                JdbcUtils.close(rs);
                JdbcUtils.close(stmt);
            }
		}
	}
	
	private void validateSlaveIsAlreadyFixed(){
		if(slaveQuerier != null && needValidateSlaveStatus.compareAndSet(true, false)){
			logger.error("slaves are synchronization failed,now validate slave is already fixed.");
			if(slaveQuerier.isAlreadyFixedReplicate()){
				this.setNoSlaves(false);
			}
		}
	}
	
	private void notSupportReadOnlyWithJtaTransactionManager(Invocation invocation) throws Exception {
		MappedStatement mappedStatement = this.getMappedStatement(invocation);
		String msg = mappedStatement.getId() + " is in a read write transaction,"
				+ "jtaTransactionManager not support read-only transaction,please remove service's transaction definition.";
		throw new NotSupportReadOnlyException(msg);
	}
	
	private void notAllowedUpdateWithNoTransactionDefinition(MappedStatement mappedStatement) throws NotAllowedUpdateException{
		String msg = mappedStatement.getId() + " is not allowed,because it in a no transaction definition service's method."
				+ "please set the service's method use transaction or set the propagation is SUPPORTS and readOnly is false.";
		throw new NotAllowedUpdateException(msg);
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
		if(noSlaves){
			logger.error("all slave are replicate failed,set all slave unavailable.");
		}else{
			logger.info("all slave's replicate are already fixed,set all slave available.");
		}
	}

	public int getInitialDelay() {
		return initialDelay;
	}

	public void setInitialDelay(int initialDelay) {
		this.initialDelay = initialDelay;
	}

	public int getPeriod() {
		return period;
	}

	public void setPeriod(int period) {
		this.period = period;
	}

	public SlaveQuerier getSlaveQuerier() {
		return slaveQuerier;
	}

	public void setSlaveQuerier(SlaveQuerier slaveQuerier) {
		this.slaveQuerier = slaveQuerier;
	}
	
}
