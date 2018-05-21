package com.sxb.web.db.ha.connection.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import com.sxb.web.db.ha.connection.SlaveConnectionHolder;
import com.sxb.web.db.ha.connection.failover.FailOverChain;

public class SlaveConnectionHolderProxy implements InvocationHandler{
	
	private SlaveConnectionHolder target;
	
	private FailOverChain chain;

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		chain.reset();
		chain.setMethod(method);
		chain.setArgs(args);
		chain.setTarget(target);
		return chain.proceed();
	}

	public SlaveConnectionHolder getTarget() {
		return target;
	}

	public void setTarget(SlaveConnectionHolder target) {
		this.target = target;
	}

	public FailOverChain getChain() {
		return chain;
	}

	public void setChain(FailOverChain chain) {
		this.chain = chain;
	}

}
