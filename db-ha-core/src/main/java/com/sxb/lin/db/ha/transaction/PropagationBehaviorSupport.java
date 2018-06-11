package com.sxb.lin.db.ha.transaction;

import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.CallbackPreferringPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class PropagationBehaviorSupport {
	
	private final static ThreadLocal<TransactionAttributeInfo> TRANSACTION_ATTRIBUTE_INFO = new ThreadLocal<>();
	
	public static TransactionAttributeInfo get(){
		return TRANSACTION_ATTRIBUTE_INFO.get();
	}
	
	public static void set(TransactionAttributeInfo transactionAttributeInfo){
		TRANSACTION_ATTRIBUTE_INFO.set(transactionAttributeInfo);
	}
	
	public static Boolean isExecuteUpdate(){
		TransactionAttributeInfo transactionAttributeInfo = TRANSACTION_ATTRIBUTE_INFO.get();
		if(transactionAttributeInfo == null){
			return null;
		}
		return transactionAttributeInfo.getIsExecuteUpdate();
	}
	
	public static void executeQuery(){
		TransactionAttributeInfo transactionAttributeInfo = TRANSACTION_ATTRIBUTE_INFO.get();
		if(transactionAttributeInfo != null){
			transactionAttributeInfo.setIsExecuteUpdate(false);
		}
	}
	
	public static void executeUpdate(){
		TransactionAttributeInfo transactionAttributeInfo = TRANSACTION_ATTRIBUTE_INFO.get();
		if(transactionAttributeInfo != null){
			transactionAttributeInfo.setIsExecuteUpdate(true);
		}
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
