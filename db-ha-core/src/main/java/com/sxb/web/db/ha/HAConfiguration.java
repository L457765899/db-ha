package com.sxb.web.db.ha;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.transaction.TransactionFactory;

import com.alibaba.druid.pool.DruidDataSource;
import com.sxb.web.db.ha.connection.failover.FailOverInterceptor;
import com.sxb.web.db.ha.connection.failover.SlaveAutoCommitInterceptor;
import com.sxb.web.db.ha.connection.failover.SlaveStopInterceptor;
import com.sxb.web.db.ha.connection.failover.SlaveSyncFailInterceptor;
import com.sxb.web.db.ha.mybatis.interceptor.QueryInterceptor;
import com.sxb.web.db.ha.mybatis.transaction.HAManagedTransactionFactory;
import com.sxb.web.db.ha.mybatis.transaction.HASpringManagedTransactionFactory;
import com.sxb.web.db.ha.mybatis.transaction.HATransactionFactory;

public class HAConfiguration {
	
	/**
	 * 事务交由spring管理
	 */
	private String type = "SpringManaged";
	
	/**
	 * 是否开启只读事务
	 */
	private boolean useReadOnlyTransaction = true;
	
	/**
	 * 自动提交时，是否切换物理连接
	 * 注意：不要和驱动包自带的
	 *     loadBalanceAutoCommitStatementThreshold
	 *     参数同时使用
	 */
	private boolean useAutoCommitPick = true;
	
	/**
	 * 是否使用show slave status检查从库同步状态
	 */
	private boolean useSyncFailCheck = true;
	
	/**
	 * 恢复同步失败的节点，需要等待的时间间隔
	 */
	private int syncFailCheckIntervalMillis = 1000*60*60*24;
	
	/**
	 * 是否开启数据库宕机检查
	 */
	private boolean useStopCheck = true;
	
	/**
	 * 失败多少次，才认为数据库宕机了
	 */
	private int stopCheckCount = 15;
	
	/**
	 * 在多少时间内，失败多少次，才认为数据库宕机了
	 */
	private int stopCheckCountTime = 1000*60*30;
	
	/**
	 * 恢复宕机数据库，需要等待的时间间隔
	 */
	private int stopCheckIntervalMillis = 1000*60*60*3;
	
	/**
	 * 是否开启读写分离
	 */
	private boolean inited = true;
	
	/**
	 * 主库数据源和从库数据源的对应关系
	 */
	private Map<DataSource,DruidDataSource> dataSourceRelationMap;
	
	/**
	 * 实现故障转移的拦截器
	 */
	private List<FailOverInterceptor> failOverInterceptors;
	
	/**
	 * 事务工厂
	 */
	private HATransactionFactory transactionFactory;
	
	/**
	 * mybatis的拦截器
	 */
	private Interceptor interceptor;
	
	public HAConfiguration() {
		this.setDefaultFailOverInterceptors();
	}
	
	protected void setDefaultFailOverInterceptors(){
		failOverInterceptors = new ArrayList<FailOverInterceptor>();
		if(useStopCheck){
			SlaveStopInterceptor slaveStopInterceptor = new SlaveStopInterceptor();
			slaveStopInterceptor.setConfiguration(this);
			failOverInterceptors.add(slaveStopInterceptor);
		}
		if(useSyncFailCheck){
			SlaveSyncFailInterceptor slaveSyncFailInterceptor = new SlaveSyncFailInterceptor();
			slaveSyncFailInterceptor.setConfiguration(this);
			failOverInterceptors.add(slaveSyncFailInterceptor);
		}
		if(useAutoCommitPick){
			SlaveAutoCommitInterceptor slaveAutoCommitInterceptor = new SlaveAutoCommitInterceptor();
			slaveAutoCommitInterceptor.setConfiguration(this);
			failOverInterceptors.add(slaveAutoCommitInterceptor);
		}
	}
	
	public synchronized TransactionFactory buildTransactionFactory() throws Exception{
		if(dataSourceRelationMap == null){
			throw new Exception("dataSourceRelationMap can not be null.");
		}
		if(transactionFactory != null){
			throw new Exception("transactionFactory has been build.");
		}
		if(type.equals("SpringManaged")){
			transactionFactory = new HASpringManagedTransactionFactory();
		}else if(type.equals("Managed")){
			transactionFactory = new HAManagedTransactionFactory();
		}else{
			throw new Exception("type is error,type is only SpringManaged or Managed.");
		}
		
		transactionFactory.setConfiguration(this);
		if(inited){
			transactionFactory.init();
		}
		
		return transactionFactory;
	}
	
	
	public synchronized Interceptor buildInterceptor() throws Exception{
		if(interceptor != null){
			throw new Exception("interceptor has been build.");
		}
		if(transactionFactory == null){
			throw new Exception("transactionFactory must be build before.");
		}
		QueryInterceptor queryInterceptor = new QueryInterceptor();
		queryInterceptor.setTransactionFactory(transactionFactory);
		interceptor = queryInterceptor;
		
		return interceptor;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public boolean isUseReadOnlyTransaction() {
		return useReadOnlyTransaction;
	}

	public void setUseReadOnlyTransaction(boolean useReadOnlyTransaction) {
		this.useReadOnlyTransaction = useReadOnlyTransaction;
	}

	public boolean isInited() {
		return inited;
	}

	public void setInited(boolean inited) {
		this.inited = inited;
	}

	public Map<DataSource, DruidDataSource> getDataSourceRelationMap() {
		return dataSourceRelationMap;
	}

	public void setDataSourceRelationMap(
			Map<DataSource, DruidDataSource> dataSourceRelationMap) {
		this.dataSourceRelationMap = dataSourceRelationMap;
	}

	public List<FailOverInterceptor> getFailOverInterceptors() {
		return failOverInterceptors;
	}

	public void setFailOverInterceptors(List<FailOverInterceptor> failOverInterceptors) {
		this.failOverInterceptors = failOverInterceptors;
	}
	
	public void addFailOverInterceptor(FailOverInterceptor failOverInterceptor) {
		this.failOverInterceptors.add(failOverInterceptor);
	}

	public HATransactionFactory getTransactionFactory() {
		return transactionFactory;
	}

	public void setTransactionFactory(HATransactionFactory transactionFactory) {
		this.transactionFactory = transactionFactory;
	}

	public Interceptor getInterceptor() {
		return interceptor;
	}

	public void setInterceptor(Interceptor interceptor) {
		this.interceptor = interceptor;
	}

	public boolean isUseAutoCommitPick() {
		return useAutoCommitPick;
	}

	public void setUseAutoCommitPick(boolean useAutoCommitPick) {
		this.useAutoCommitPick = useAutoCommitPick;
	}

	public boolean isUseSyncFailCheck() {
		return useSyncFailCheck;
	}

	public void setUseSyncFailCheck(boolean useSyncFailCheck) {
		this.useSyncFailCheck = useSyncFailCheck;
	}

	public int getSyncFailCheckIntervalMillis() {
		return syncFailCheckIntervalMillis;
	}

	public void setSyncFailCheckIntervalMillis(int syncFailCheckIntervalMillis) {
		this.syncFailCheckIntervalMillis = syncFailCheckIntervalMillis;
	}

	public boolean isUseStopCheck() {
		return useStopCheck;
	}

	public void setUseStopCheck(boolean useStopCheck) {
		this.useStopCheck = useStopCheck;
	}

	public int getStopCheckCount() {
		return stopCheckCount;
	}

	public void setStopCheckCount(int stopCheckCount) {
		this.stopCheckCount = stopCheckCount;
	}

	public int getStopCheckIntervalMillis() {
		return stopCheckIntervalMillis;
	}

	public void setStopCheckIntervalMillis(int stopCheckIntervalMillis) {
		this.stopCheckIntervalMillis = stopCheckIntervalMillis;
	}

	public int getStopCheckCountTime() {
		return stopCheckCountTime;
	}

	public void setStopCheckCountTime(int stopCheckCountTime) {
		this.stopCheckCountTime = stopCheckCountTime;
	}
}
