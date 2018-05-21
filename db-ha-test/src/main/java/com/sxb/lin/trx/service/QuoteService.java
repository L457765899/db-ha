package com.sxb.lin.trx.service;

import java.util.Map;

import com.sxb.lin.trx.db.model.Quote;

public interface QuoteService {

	Map<String,Object> addQuote();
	
	Map<String,Object> addException();

	void savepoint1();
	
	void savepoint2();
	
	void savepoint3();
	
	void savepointException();
	
	Quote trQuote();
}
