package com.sxb.web.db.ha.connection.failover;

import java.lang.reflect.Method;

import org.springframework.util.ReflectionUtils;

import com.mysql.jdbc.LoadBalancedConnectionProxy;
import com.sxb.web.db.ha.HAConfiguration;

public class SlaveAutoCommitInterceptor implements FailOverInterceptor{
	
	private HAConfiguration configuration;

	@Override
	public Object invoke(FailOverChain chain) throws Exception {
		
		if(chain.getMethod().getName().equals("resetConnection") && configuration.isUseAutoCommitPick()){
			return this.invokeUnsetConnection(chain);
		}
		
		return chain.proceed();
	}

	@Override
	public void setConfiguration(HAConfiguration configuration) {
		this.configuration = configuration;
	}

	protected Object invokeUnsetConnection(FailOverChain chain) throws Exception{
		
		Object result = chain.proceed();
		if(chain.getTarget().isAutoCommit()){
			LoadBalancedConnectionProxy loadBalancedConnectionProxy = chain.getTarget().getLoadBalancedConnectionProxy();
			if(loadBalancedConnectionProxy != null){
				Method method = ReflectionUtils.findMethod(LoadBalancedConnectionProxy.class, "pickNewConnection");
				method.setAccessible(true);
				method.invoke(loadBalancedConnectionProxy);
			}
		}
		
		return result;
	}

}
