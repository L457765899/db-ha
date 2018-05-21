package com.sxb.lin.trx.service;

import java.util.Map;

public interface UserService {

	Map<String,Object> addUser();
	
	Map<String,Object> selectUser();
	
	Map<String,Object> getUser();

	Map<String,Object> takeUser();
	
	Map<String,Object> canUser();
	
	Map<String,Object> readUser();
	
	Map<String,Object> manyTransaction();
	
	Map<String,Object> savepoint();

	Map<String,Object> addSleepUser();

	Map<String,Object> getUsers();

	Map<String,Object> takeUsers();

	Map<String,Object> selectSleepUser();
}
