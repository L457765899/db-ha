package com.sxb.lin.db.ha.mybatis.interceptor;

public class NotSupportReadOnlyException extends Exception{

	private static final long serialVersionUID = 1L;

	public NotSupportReadOnlyException() {
		super();
	}

	public NotSupportReadOnlyException(String arg0, Throwable arg1,
			boolean arg2, boolean arg3) {
		super(arg0, arg1, arg2, arg3);
	}

	public NotSupportReadOnlyException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public NotSupportReadOnlyException(String arg0) {
		super(arg0);
	}

	public NotSupportReadOnlyException(Throwable arg0) {
		super(arg0);
	}
	
}
