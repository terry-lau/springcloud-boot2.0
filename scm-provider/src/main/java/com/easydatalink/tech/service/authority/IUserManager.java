package com.easydatalink.tech.service.authority;

import com.easydatalink.tech.entity.authortity.SysUser;
import com.easydatalink.tech.service.IManager;

public interface IUserManager extends IManager<SysUser>{
	
	/**
	 * 根据用户登录名查询用户信息
	 * */
	public SysUser findUserByLoginName(String name);
	
	
}