package com.sxb.lin.db.ha.transaction;

import java.lang.reflect.Method;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class TransactionAttributeSourceProxy implements TransactionAttributeSource{
	
	private TransactionAttributeSource transactionAttributeSource;

	@Override
	public TransactionAttribute getTransactionAttribute(Method method,Class<?> targetClass) {
		
		TransactionAttribute transactionAttribute = transactionAttributeSource.getTransactionAttribute(method, targetClass);
		
		if(transactionAttribute != null){
			if(TransactionSynchronizationManager.isSynchronizationActive()){
				int propagationBehavior = transactionAttribute.getPropagationBehavior();
				if(TransactionSynchronizationManager.isActualTransactionActive()){
					if(propagationBehavior == TransactionDefinition.PROPAGATION_REQUIRES_NEW
						|| propagationBehavior == TransactionDefinition.PROPAGATION_NOT_SUPPORTED){
						this.bindToThread(method, targetClass, transactionAttribute);
					}
				}else{
					if(propagationBehavior == TransactionDefinition.PROPAGATION_REQUIRED
						|| propagationBehavior == TransactionDefinition.PROPAGATION_REQUIRES_NEW
						|| propagationBehavior == TransactionDefinition.PROPAGATION_NESTED){
						this.bindToThread(method, targetClass, transactionAttribute);
					}
				}
			}else{
				this.bindToThread(method, targetClass, transactionAttribute);
			}
		}
		
		return transactionAttribute;
	}
	
	protected void bindToThread(Method method,Class<?> targetClass,TransactionAttribute transactionAttribute){
		TransactionAttributeInfo transactionAttributeInfo = new TransactionAttributeInfo();
		transactionAttributeInfo.setMethod(method);
		transactionAttributeInfo.setTargetClass(targetClass);
		transactionAttributeInfo.setTransactionAttribute(transactionAttribute);
		transactionAttributeInfo.bindToThread();
	}

	public TransactionAttributeSource getTransactionAttributeSource() {
		return transactionAttributeSource;
	}

	public void setTransactionAttributeSource(
			TransactionAttributeSource transactionAttributeSource) {
		this.transactionAttributeSource = transactionAttributeSource;
	}

	
}
