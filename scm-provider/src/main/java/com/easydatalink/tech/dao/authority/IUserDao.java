package com.easydatalink.tech.dao.authority;

import com.easydatalink.tech.entity.authortity.SysUser;
import com.easydatalink.tech.orm.IMybatisDao;

public interface IUserDao extends IMybatisDao<SysUser>{
	
	public SysUser findUserByLoginName(String name);
	
}