package com.sxb.web.db.ha.mybatis.transaction;

import org.apache.ibatis.transaction.TransactionFactory;

import com.sxb.web.db.ha.HAConfiguration;

public interface HATransactionFactory extends TransactionFactory{

	boolean isInited();
	
	void setInited(boolean inited);
	
	void init() throws Exception;

	void setConfiguration(HAConfiguration configuration);
}
