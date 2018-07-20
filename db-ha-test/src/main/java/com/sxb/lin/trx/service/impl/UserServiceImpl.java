package com.sxb.lin.trx.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sxb.lin.trx.db.dao.UserMapper;
import com.sxb.lin.trx.db.model.User;
import com.sxb.lin.trx.service.QuoteService;
import com.sxb.lin.trx.service.UserService;
import com.sxb.web.commons.util.RetUtil;

@Service("userService")
public class UserServiceImpl implements UserService {

	@Autowired
	private UserMapper userMapper;
	
	@Resource(name="quoteService")
	private QuoteService quoteService;

	/**
	 * spring事务管理器:创建连接，spring事务管理器:自己回收连接，mybatis使用spring创建的连接
	 * 当同时使用xml和注解配置事务时，会过两个事务拦截器，xml配置的拦截器先执行，注解配置的拦截器后执行，连接的创建、提交、回滚、回收都在xml配置的拦截器中执行
	 */
	@Override
	@Transactional(rollbackFor=Exception.class)
	public Map<String, Object> addUser() {
		User u = new User();
		u.setAccount("18280095123");
		u.setName("lin-3");
		u.setPwd("222222");
		userMapper.insertSelective(u);
		//throw new RuntimeException();
		return RetUtil.getRetValue(true);
	}

	/**
	 * propagation="REQUIRED" read-only="true"
	 * 只读属性，是有作用的
	 * spring事务管理器:创建连接，spring事务管理器:回收连接，mybatis使用spring创建的连接
	 */
	@Override
	@Transactional(readOnly=true)
	public Map<String, Object> selectUser() {
		User user = userMapper.selectByPrimaryKey(1);
		return RetUtil.getRetValue(user);
	}
	
	/**
	 * propagation="SUPPORTS" read-only="true"
	 * 只读属性，是不起作用的
	 * mybatis:创建连接，mybatis注册在事务管理器的钩子(@DataSourceUtils.ConnectionSynchronization):回收连接，mybatis使用自己创建的连接
	 */
	@Override
	public Map<String, Object> getUser() {
		User user = userMapper.selectByPrimaryKey(16);
		user = userMapper.selectByPrimaryKey(1);
		return RetUtil.getRetValue(user);
	}

	/**
	 * mybatis创建连接，自己回收连接
	 */
	@Override
	//@Transactional(propagation=Propagation.SUPPORTS)
	public Map<String, Object> takeUser() {
		User user = userMapper.selectByPrimaryKey(16);
		user = userMapper.selectByPrimaryKey(1);
		//Quote quote = quoteService.trQuote();
		//Object[] arrays = {user,quote};
		//return RetUtil.getRetValue(arrays);
		return RetUtil.getRetValue(user);
	}

	/**
	 * 内部调用内部的方法，spring的事务传播机制是不起作用的
	 */
	@Override
	@Transactional(rollbackFor=Exception.class)
	public Map<String, Object> canUser() {
		
		User updateUser = new User();
		updateUser.setId(1);
		updateUser.setName("林立彬"+System.currentTimeMillis());
		userMapper.updateByPrimaryKeySelective(updateUser);
		
		this.addUser();
		
		return RetUtil.getRetValue(true);
	}

	/**
	 * 只读事务不能执行修改操作，会报错
	 */
	@Override
	@Transactional(readOnly=true)
	public Map<String, Object> readUser() {
		User user = userMapper.selectByPrimaryKey(1);
		this.addUser();
		return RetUtil.getRetValue(user);
	}

	/**
	 * 事务会回滚，会等所有的操作都执行完了，才回滚
	 */
	@Override
	@Transactional(rollbackFor=Exception.class)
	public Map<String, Object> manyTransaction() {
		this.addUser();
		quoteService.addQuote();
		try {
			quoteService.addException();
		} catch (Exception e) {
			quoteService.addQuote();
			e.printStackTrace();
		}
		quoteService.addQuote();
		return RetUtil.getRetValue(true);
	}

	/**
	 * 出异常的方法回滚，整个事务不会回滚
	 */
	@Override
	@Transactional(propagation=Propagation.NESTED,rollbackFor=Exception.class)
	public Map<String, Object> savepoint() {
		quoteService.savepoint1();
		try {
			quoteService.savepointException();
		} catch (Exception e) {
			quoteService.savepoint2();
			e.printStackTrace();
		}
		quoteService.savepoint3();
		return RetUtil.getRetValue(true);
	}

	@Override
	@Transactional(rollbackFor=Exception.class)
	public Map<String, Object> addSleepUser() {
		
		User u = new User();
		u.setAccount("18280095124");
		u.setName("lin-4");
		u.setPwd("44444");
		userMapper.insertSelective(u);
		
		System.out.println("睡眠开始。。。。");
		try {
			Thread.sleep(60000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("睡眠结束。。。。");
		
		User s = new User();
		s.setAccount("18280095125");
		s.setName("lin-5");
		s.setPwd("55555");
		userMapper.insertSelective(s);
		
		return RetUtil.getRetValue(true);
	}

	@Override
	public Map<String, Object> getUsers() {
		
		long startTime = System.currentTimeMillis();
		User user1 = userMapper.selectByPrimaryKey(1);
		User user2 = userMapper.selectByPrimaryKey(16);
		List<User> list = new ArrayList<User>();
		list.add(user1);
		list.add(user2);
		long endTime = System.currentTimeMillis();
		
		System.out.println(endTime - startTime);
		
		return RetUtil.getRetValue(list);
	}

	@Override
	public Map<String, Object> takeUsers() {
		
		User user1 = userMapper.selectByPrimaryKey(1);
		User user2 = userMapper.selectByPrimaryKey(16);
		List<User> list = new ArrayList<User>();
		list.add(user1);
		list.add(user2);
		
		return RetUtil.getRetValue(list);
	}

	@Override
	public Map<String, Object> selectSleepUser() {
		User user = userMapper.selectByPrimaryKey(1);
		try {
			Thread.sleep(60000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return RetUtil.getRetValue(user);
	}

}
