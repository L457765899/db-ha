package com.sxb.lin.db.ha.transaction;

import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.CallbackPreferringPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class PropagationBehaviorSupport {
	
	private final static ThreadLocal<TransactionAttributeInfo> TRANSACTION_ATTRIBUTE_INFO = new ThreadLocal<>();
	
	private final static ThreadLocal<Boolean> IS_EXECUTE_UPDATE = new ThreadLocal<>();
	
	public static TransactionAttributeInfo get(){
		return TRANSACTION_ATTRIBUTE_INFO.get();
	}
	
	public static void set(TransactionAttributeInfo transactionAttributeInfo){
		TRANSACTION_ATTRIBUTE_INFO.set(transactionAttributeInfo);
	}
	
	public static void clear(){
		TRANSACTION_ATTRIBUTE_INFO.remove();
		IS_EXECUTE_UPDATE.remove();
	}
	
	public static Boolean isExecuteUpdate(){
		return IS_EXECUTE_UPDATE.get();
	}
	
	public static void executeQuery(){
		IS_EXECUTE_UPDATE.set(false);
	}
	
	public static void executeUpdate(){
		IS_EXECUTE_UPDATE.set(true);
	}
	
	private TransactionInterceptor transactionInterceptor;
	
	protected void init() throws NotSupportTransactionManagerException{
		if(transactionInterceptor != null && transactionInterceptor.getTransactionManager() != null){
			if(transactionInterceptor.getTransactionManager() instanceof CallbackPreferringPlatformTransactionManager){
				throw new NotSupportTransactionManagerException("db-ha not support CallbackPreferringPlatformTransactionManager.");
			}
			TransactionAttributeSource transactionAttributeSource = transactionInterceptor.getTransactionAttributeSource();
			TransactionAttributeSourceProxy proxy = new TransactionAttributeSourceProxy();
			proxy.setTransactionAttributeSource(transactionAttributeSource);
			transactionInterceptor.setTransactionAttributeSource(proxy);
		}
	}

	public TransactionInterceptor getTransactionInterceptor() {
		return transactionInterceptor;
	}

	public void setTransactionInterceptor(TransactionInterceptor transactionInterceptor) {
		this.transactionInterceptor = transactionInterceptor;
	}
	
	public boolean isNewSynchronization(){
		DefaultTransactionStatus transactionStatus = (DefaultTransactionStatus) TransactionInterceptor.currentTransactionStatus();
		return transactionStatus.isNewSynchronization();
	}
	
	public void registerSynchronization(){
		TransactionSynchronizationManager.registerSynchronization(new TransactionAttributeInfoSynchronization());
	}
}
