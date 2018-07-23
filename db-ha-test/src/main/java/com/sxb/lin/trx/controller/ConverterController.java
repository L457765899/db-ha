package com.sxb.lin.trx.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.gson.Gson;
import com.sxb.lin.trx.util.RetUtil;

@Controller
@RequestMapping("/transaction/Converter")
public class ConverterController {

	@RequestMapping(value="/getList.json")
	@ResponseBody
	public String getList(int id,String name,String quey,String com){
		List<Object> list = new ArrayList<>();
		list.add(id);
		list.add(name);
		list.add(quey);
		list.add(com);
		return new Gson().toJson(RetUtil.getRetValue(list));
	}
	
	@RequestMapping(value="/getObject.json")
	@ResponseBody
	public String getObject(@ModelAttribute("query") Query query){
		return new Gson().toJson(RetUtil.getRetValue(query));
	}
	
	static class Query {
		
		private int id;
		
		private String name;
		
		private String quey;
		
		private String com;

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getQuey() {
			return quey;
		}

		public void setQuey(String quey) {
			this.quey = quey;
		}

		public String getCom() {
			return com;
		}

		public void setCom(String com) {
			this.com = com;
		}
	}
}
