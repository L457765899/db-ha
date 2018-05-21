package com.sxb.lin.trx.service.impl;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sxb.lin.trx.db.dao.QuoteMapper;
import com.sxb.lin.trx.db.model.Quote;
import com.sxb.lin.trx.service.QuoteService;
import com.sxb.web.commons.util.RetUtil;

@Service("quoteService")
public class QuoteServiceImpl implements QuoteService{
	
	@Autowired
	private QuoteMapper quoteMapper;

	@Override
	@Transactional(rollbackFor=Exception.class)
	public Map<String, Object> addQuote() {
		Quote quote = new Quote();
		quote.setCar("速腾");
		quote.setCount(1);
		quote.setUser(1);
		quote.setQuote(-2000);
		quoteMapper.insertSelective(quote);
		return RetUtil.getRetValue(true);
	}

	@Override
	@Transactional(rollbackFor=Exception.class)
	public Map<String, Object> addException() {
		Quote quote = new Quote();
		quote.setCar("途观");
		quote.setCount(2);
		quote.setUser(2);
		quote.setQuote(-5000);
		quoteMapper.insertSelective(quote);
		throw new RuntimeException();
	}

	@Override
	@Transactional(propagation=Propagation.NESTED,rollbackFor=Exception.class)
	public void savepoint1() {
		Quote quote = new Quote();
		quote.setCar("马自达2");
		quote.setCount(2);
		quote.setUser(2);
		quote.setQuote(-5000);
		quoteMapper.insertSelective(quote);
	}

	@Override
	@Transactional(propagation=Propagation.NESTED,rollbackFor=Exception.class)
	public void savepoint2() {
		Quote quote = new Quote();
		quote.setCar("马自达3");
		quote.setCount(2);
		quote.setUser(2);
		quote.setQuote(-5000);
		quoteMapper.insertSelective(quote);
	}

	@Override
	@Transactional(propagation=Propagation.NESTED,rollbackFor=Exception.class)
	public void savepoint3() {
		Quote quote = new Quote();
		quote.setCar("马自达5");
		quote.setCount(2);
		quote.setUser(2);
		quote.setQuote(-5000);
		quoteMapper.insertSelective(quote);
	}

	@Override
	@Transactional(propagation=Propagation.NESTED,rollbackFor=Exception.class)
	public void savepointException() {
		Quote quote = new Quote();
		quote.setCar("马自达6");
		quote.setCount(2);
		quote.setUser(2);
		quote.setQuote(-5000);
		quoteMapper.insertSelective(quote);
		throw new RuntimeException();
	}

	@Override
	@Transactional(rollbackFor=Exception.class)
	public Quote trQuote() {
		Quote quote = quoteMapper.selectByPrimaryKey(1);
		return quote;
	}

}
