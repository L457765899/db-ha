package com.sxb.lin.trx.db.dao;

import com.sxb.lin.trx.db.model.Quote;

public interface QuoteMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(Quote record);

    int insertSelective(Quote record);

    Quote selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Quote record);

    int updateByPrimaryKey(Quote record);
}