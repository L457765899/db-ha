package com.sxb.lin.db.ha.slave;

public interface SlaveQuerier {

	/**
	 * 主从复制失败是否已经修复
	 * @return
	 */
	boolean isAlreadyFixedReplicate();
}
