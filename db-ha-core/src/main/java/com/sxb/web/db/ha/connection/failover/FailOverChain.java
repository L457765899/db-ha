package com.sxb.web.db.ha.connection.failover;

import java.lang.reflect.Method;
import java.util.List;

import com.sxb.web.db.ha.connection.SlaveConnectionHolder;

public class FailOverChain {

	private List<FailOverInterceptor> failOverInterceptors;
	
	private int interceptorSize;
	
	private Method method;
	
	private Object[] args;
	
	private SlaveConnectionHolder target;
	
	private int interceptorIndex = 0;

	public List<FailOverInterceptor> getFailOverInterceptors() {
		return failOverInterceptors;
	}

	public void setFailOverInterceptors(List<FailOverInterceptor> failOverInterceptors) {
		this.failOverInterceptors = failOverInterceptors;
		this.interceptorSize = failOverInterceptors.size();
	}
	
	public Method getMethod() {
		return method;
	}

	public void setMethod(Method method) {
		this.method = method;
	}

	public Object[] getArgs() {
		return args;
	}

	public void setArgs(Object[] args) {
		this.args = args;
	}
	
	public SlaveConnectionHolder getTarget() {
		return target;
	}

	public void setTarget(SlaveConnectionHolder target) {
		this.target = target;
	}
	
	public void reset(){
		method = null;
		args = null;
		target = null;
		interceptorIndex = 0;
	}

	public Object proceed() throws Exception{
		if(failOverInterceptors!=null && interceptorSize>0 && interceptorIndex<interceptorSize){
			FailOverInterceptor failOverInterceptor = failOverInterceptors.get(interceptorIndex++);
			return failOverInterceptor.invoke(this);
		}else{
			return method.invoke(target, args);
		}
	}
}
