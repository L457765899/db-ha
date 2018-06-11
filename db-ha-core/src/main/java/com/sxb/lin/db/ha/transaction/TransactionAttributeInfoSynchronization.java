package com.sxb.lin.db.ha.transaction;

import org.springframework.transaction.support.TransactionSynchronizationAdapter;

public class TransactionAttributeInfoSynchronization extends TransactionSynchronizationAdapter{

	@Override
	public void afterCompletion(int status) {
		TransactionAttributeInfo transactionAttributeInfo = PropagationBehaviorSupport.get();
		if(transactionAttributeInfo != null){
			transactionAttributeInfo.restoreThreadLocalStatus();
		}
	}
	
}
