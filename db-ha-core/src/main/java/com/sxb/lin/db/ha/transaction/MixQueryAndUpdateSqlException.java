package com.sxb.lin.db.ha.transaction;

public class MixQueryAndUpdateSqlException extends Exception{

	private static final long serialVersionUID = 1L;

	public MixQueryAndUpdateSqlException() {
		super();
	}
	
	public MixQueryAndUpdateSqlException(String message) {
		super(message);
	}

	public MixQueryAndUpdateSqlException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public MixQueryAndUpdateSqlException(String message, Throwable cause) {
		super(message, cause);
	}

	public MixQueryAndUpdateSqlException(Throwable cause) {
		super(cause);
	}
	
}
