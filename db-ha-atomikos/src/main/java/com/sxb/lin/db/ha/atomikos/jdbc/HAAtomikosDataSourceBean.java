package com.sxb.lin.db.ha.atomikos.jdbc;

import com.atomikos.datasource.pool.ConnectionFactory;
import com.atomikos.jdbc.AtomikosDataSourceBean;

public class HAAtomikosDataSourceBean extends AtomikosDataSourceBean{

	private static final long serialVersionUID = 1L;

	@Override
	protected ConnectionFactory doInit() throws Exception {
		ConnectionFactory cf = super.doInit();
		return new HAAtomikosXAConnectionFactory(cf);
	}
	
}
