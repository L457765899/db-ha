package com.sxb.lin.db.ha.atomikos.jdbc;

import com.atomikos.datasource.pool.ConnectionFactory;
import com.atomikos.jdbc.AtomikosDataSourceBean;

public class HAAtomikosDataSourceBean extends AtomikosDataSourceBean {

	private static final long serialVersionUID = 1L;
	
	private boolean usePingMethod = true;

	@Override
	protected ConnectionFactory doInit() throws Exception {
		ConnectionFactory cf = super.doInit();
		return new HAAtomikosXAConnectionFactory(cf);
	}

	public boolean isUsePingMethod() {
		return usePingMethod;
	}

	public void setUsePingMethod(boolean usePingMethod) {
		this.usePingMethod = usePingMethod;
	}
	
}
