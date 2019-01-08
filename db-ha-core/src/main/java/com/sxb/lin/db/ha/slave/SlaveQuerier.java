package com.sxb.lin.db.ha.slave;

import com.sxb.lin.db.ha.mybatis.interceptor.QueryInterceptor;

public interface SlaveQuerier {

	/**
	 * 主从复制失败是否已经修复
	 * @return
	 */
	boolean isAlreadyFixedReplicate();
	
	/**
	 * 停止slave时回调改方法
	 */
	void stopSlaves();
	
	void setQueryInterceptor(QueryInterceptor queryInterceptor);
	
	void init() throws Exception;
	
	void destroy();
}
