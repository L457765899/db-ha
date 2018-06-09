package com.sxb.lin.db.ha.transaction;

public class NotSupportTransactionManagerException extends Exception{

	
	private static final long serialVersionUID = 1L;

	public NotSupportTransactionManagerException() {
		super();
	}

	public NotSupportTransactionManagerException(String message,
			Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public NotSupportTransactionManagerException(String message, Throwable cause) {
		super(message, cause);
	}

	public NotSupportTransactionManagerException(String message) {
		super(message);
	}

	public NotSupportTransactionManagerException(Throwable cause) {
		super(cause);
	}

}
