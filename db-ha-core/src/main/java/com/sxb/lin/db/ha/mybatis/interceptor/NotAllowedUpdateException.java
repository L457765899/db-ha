package com.sxb.lin.db.ha.mybatis.interceptor;

public class NotAllowedUpdateException extends Exception{

	private static final long serialVersionUID = 1L;

	public NotAllowedUpdateException() {
		super();
	}

	public NotAllowedUpdateException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public NotAllowedUpdateException(String message, Throwable cause) {
		super(message, cause);
	}

	public NotAllowedUpdateException(String message) {
		super(message);
	}

	public NotAllowedUpdateException(Throwable cause) {
		super(cause);
	}
	

}
