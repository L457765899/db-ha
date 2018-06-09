package com.sxb.lin.db.ha.transaction;

import java.lang.reflect.Method;

import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttributeSource;

public class TransactionAttributeSourceProxy implements TransactionAttributeSource{
	
	private TransactionAttributeSource transactionAttributeSource;

	@Override
	public TransactionAttribute getTransactionAttribute(Method method,Class<?> targetClass) {
		
		TransactionAttribute transactionAttribute = transactionAttributeSource.getTransactionAttribute(method, targetClass);
		
		if(transactionAttribute != null){
			TransactionAttributeInfo transactionAttributeInfo = new TransactionAttributeInfo();
			transactionAttributeInfo.setMethod(method);
			transactionAttributeInfo.setTargetClass(targetClass);
			transactionAttributeInfo.setTransactionAttribute(transactionAttribute);
			PropagationBehaviorSupport.set(transactionAttributeInfo);
		}
		
		return transactionAttribute;
	}

	public TransactionAttributeSource getTransactionAttributeSource() {
		return transactionAttributeSource;
	}

	public void setTransactionAttributeSource(
			TransactionAttributeSource transactionAttributeSource) {
		this.transactionAttributeSource = transactionAttributeSource;
	}

	
}
