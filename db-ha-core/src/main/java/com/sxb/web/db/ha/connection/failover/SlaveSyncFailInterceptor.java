package com.sxb.web.db.ha.connection.failover;

import java.util.HashMap;
import java.util.Map;

import com.mysql.jdbc.ConnectionGroupManager;
import com.sxb.web.db.ha.HAConfiguration;

/**
 * 
 * @author llb
 * 改类主要实现的功能是，当执行一定数量的查询语句后，用show slave status去从数据库查询同步状态看是否同步失败，失败则删除响应节点。
 * 注意：查询语句的数量采用'int++'来计数，由于这样计数不具有原子性，所以查询语句实际执行的数量要超过参数设置的数量，但这并没有什么影响。
 *
 */
public class SlaveSyncFailInterceptor implements FailOverInterceptor {
	
	private HAConfiguration configuration;

	@Override
	public Object invoke(FailOverChain chain) throws Exception {
		if(chain.getMethod().getName().equals("getSlaveConnection") && configuration.isUseSyncFailCheck()){
			return invokeGetSlaveConnection(chain);
		}
		return chain.proceed();
	}
	
	@Override
	public void setConfiguration(HAConfiguration configuration) {
		this.configuration = configuration;
	}
	
	protected Object invokeGetSlaveConnection(FailOverChain chain) throws Exception{
		
		Object result = chain.proceed();
		System.out.println(chain.getTarget().getHostPortPair());
		
		return result;
	}

	public Map<String,Map<String,Integer>> getGroupHostsMap() {
		return GroupHost.INSTANCE.getGroupHostsMap();
	}
	
	private final static class GroupHost{
		
		private final static GroupHost INSTANCE = new GroupHost();
		
		private Map<String,Map<String,Integer>> groupHostsMap;

		private GroupHost() {
			groupHostsMap = new HashMap<String, Map<String,Integer>>();
			String registeredConnectionGroups = ConnectionGroupManager.getRegisteredConnectionGroups();
			String[] groups = registeredConnectionGroups.split(",");
			for(String group : groups){
				String activeHostLists = ConnectionGroupManager.getActiveHostLists(group);
				String[] hosts = activeHostLists.split(",");
				Map<String,Integer> hostsMap = new HashMap<String, Integer>();
				for(String host : hosts){
					hostsMap.put(host.replace("(1)", ""), 0);
				}
				groupHostsMap.put(group, hostsMap);
			}
		}

		public Map<String, Map<String, Integer>> getGroupHostsMap() {
			return groupHostsMap;
		}
	}

}
