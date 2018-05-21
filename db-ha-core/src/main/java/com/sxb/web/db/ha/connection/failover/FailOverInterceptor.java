package com.sxb.web.db.ha.connection.failover;

import com.sxb.web.db.ha.HAConfiguration;

public interface FailOverInterceptor {

	Object invoke(FailOverChain chain) throws Exception;
	
	void setConfiguration(HAConfiguration configuration);
	
}
