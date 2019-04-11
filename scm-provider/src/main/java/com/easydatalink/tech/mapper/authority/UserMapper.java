package com.easydatalink.tech.mapper.authority;

import com.easydatalink.tech.entity.authortity.SysUser;
import com.easydatalink.tech.orm.Mapper;

public interface UserMapper extends Mapper<SysUser>{
	
	public SysUser findUserByLoginName(String name);
	
}