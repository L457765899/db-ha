package com.sxb.web.db.ha.mybatis.interceptor;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.Properties;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.statement.BaseStatementHandler;
import org.apache.ibatis.executor.statement.RoutingStatementHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.transaction.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.ReflectionUtils;

import com.sxb.web.db.ha.mybatis.transaction.HATransaction;
import com.sxb.web.db.ha.mybatis.transaction.HATransactionFactory;

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
	
	protected HATransactionFactory transactionFactory;

	@Override
	public Object intercept(Invocation invocation) throws Throwable {
		
		if(!transactionFactory.isInited()){
			return invocation.proceed();
		}
		
		if(TransactionSynchronizationManager.isCurrentTransactionReadOnly()){
			Invocation replaceInvocation = this.replaceInvocation(invocation,false);
			if(replaceInvocation != null){
				return replaceInvocation.proceed();
			}
		}else if(TransactionSynchronizationManager.isSynchronizationActive() 
				&& TransactionSynchronizationManager.isActualTransactionActive()){
			if(logger.isDebugEnabled()){
				MappedStatement mappedStatement = this.getMappedStatement(invocation);
				logger.debug("have a transaction the mapper id is "+mappedStatement.getId());
			}
		}else{
			Invocation replaceInvocation = this.replaceInvocation(invocation,true);
			if(replaceInvocation != null){
				return replaceInvocation.proceed();
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
	
	public HATransactionFactory getTransactionFactory() {
		return transactionFactory;
	}

	public void setTransactionFactory(HATransactionFactory transactionFactory) {
		this.transactionFactory = transactionFactory;
	}
	
	protected StatementHandler getStatementHandler(Invocation invocation) throws IllegalArgumentException, IllegalAccessException{
		
		StatementHandler routingStatementHandler = (RoutingStatementHandler) invocation.getTarget();
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
	
	protected Executor getExecutor(Invocation invocation) throws IllegalArgumentException, IllegalAccessException {
		
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
	}
	
	protected Invocation replaceInvocation(Invocation invocation,boolean isAutoCommit) throws Throwable {
		
		MappedStatement mappedStatement = this.getMappedStatement(invocation);
		Executor executor = this.getExecutor(invocation);
		Transaction transaction = executor.getTransaction();
		
		if(transaction instanceof HATransaction){
			
			HATransaction ssmt = (HATransaction) transaction;
			SqlCommandType sqlCommandType = mappedStatement.getSqlCommandType();
			
			if(sqlCommandType == SqlCommandType.SELECT){
				
//				BoundSql boundSql = this.getBoundSql(invocation);
//				String sql = boundSql.getSql();
//				
//				String dbType = JdbcConstants.MYSQL;
//				List<SQLStatement> stmtList = SQLUtils.parseStatements(sql, dbType);
//				
//		        System.out.println("MapperId : " + mappedStatement.getId());
//				for (int i = 0; i < stmtList.size(); i++) {				 
//		            SQLStatement stmt = stmtList.get(i);
//		            MySqlSchemaStatVisitor visitor = new MySqlSchemaStatVisitor();
//		            stmt.accept(visitor);
//		            //获取表名称
//		            Map<Name, TableStat> tables = visitor.getTables();
//		            Set<Entry<Name, TableStat>> entrySet = tables.entrySet();
//		            for(Entry<Name, TableStat> entry : entrySet){
//		            	System.out.println("Tables : " + entry.getKey());	
//		            }
//		        }
				
				Connection readConnection = null;
				try {
					readConnection = ssmt.getSlaveConnection(isAutoCommit);
				} catch (Throwable e) {
					logger.error(e.getMessage(), e);
				}
				
				if(readConnection != null){
					
					Object[] args = {readConnection};
					Invocation replaceInvocation = new Invocation(invocation.getTarget(),invocation.getMethod(),args);
					
					if(logger.isDebugEnabled()){
						logger.debug("connection is success replace to execute mapper id:"+mappedStatement.getId());
					}
					
					return replaceInvocation;
				}
			}
			
		}
		
		return null;
	}
}
