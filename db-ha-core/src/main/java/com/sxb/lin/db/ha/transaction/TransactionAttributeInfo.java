package com.sxb.lin.db.ha.transaction;

import java.lang.reflect.Method;

import org.springframework.transaction.interceptor.TransactionAttribute;

public class TransactionAttributeInfo {

	private Method method;
	
	private Class<?> targetClass;
	
	private TransactionAttribute transactionAttribute;

	public Method getMethod() {
		return method;
	}

	public void setMethod(Method method) {
		this.method = method;
	}

	public Class<?> getTargetClass() {
		return targetClass;
	}

	public void setTargetClass(Class<?> targetClass) {
		this.targetClass = targetClass;
	}

	public TransactionAttribute getTransactionAttribute() {
		return transactionAttribute;
	}

	public void setTransactionAttribute(TransactionAttribute transactionAttribute) {
		this.transactionAttribute = transactionAttribute;
	}
	
	
}
