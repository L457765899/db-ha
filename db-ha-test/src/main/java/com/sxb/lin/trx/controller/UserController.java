package com.sxb.lin.trx.controller;

import javax.annotation.Resource;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.gson.Gson;
import com.sxb.lin.trx.service.UserService;


@Controller
@RequestMapping("/transaction/User")
public class UserController {
	
	@Resource(name="userService")
	private UserService userService;

	@RequestMapping(value="/addUser.json")
	@ResponseBody
	public String addUser(){
		return new Gson().toJson(userService.addUser());
	}
	
	@RequestMapping(value="/addSleepUser.json")
	@ResponseBody
	public String addSleepUser(){
		return new Gson().toJson(userService.addSleepUser());
	}
	
	@RequestMapping(value="/selectUser.json")
	@ResponseBody
	public String selectUser(){
		return new Gson().toJson(userService.selectUser());
	}
	
	@RequestMapping(value="/selectSleepUser.json")
	@ResponseBody
	public String selectSleepUser(){
		return new Gson().toJson(userService.selectSleepUser());
	}
	
	@RequestMapping(value="/getUser.json")
	@ResponseBody
	public String getUser(){
		return new Gson().toJson(userService.getUser());
	}
	
	@RequestMapping(value="/getUsers.json")
	@ResponseBody
	public String getUsers(){
		return new Gson().toJson(userService.getUsers());
	}
	
	@RequestMapping(value="/takeUser.json")
	@ResponseBody
	public String takeUser(){
		return new Gson().toJson(userService.takeUser());
	}
	
	@RequestMapping(value="/takeUsers.json")
	@ResponseBody
	public String takeUsers(){
		return new Gson().toJson(userService.takeUsers());
	}
	
	@RequestMapping(value="/canUser.json")
	@ResponseBody
	public String canUser(){
		return new Gson().toJson(userService.canUser());
	}
	
	@RequestMapping(value="/readUser.json")
	@ResponseBody
	public String readUser(){
		return new Gson().toJson(userService.readUser());
	}
	
	@RequestMapping(value="/manyTransaction.json")
	@ResponseBody
	public String manyTransaction(){
		return new Gson().toJson(userService.manyTransaction());
	}
	
	@RequestMapping(value="/savepoint.json")
	@ResponseBody
	public String savepoint(){
		return new Gson().toJson(userService.savepoint());
	}
}
