package com.easydatalink.tech.dao.authority.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.easydatalink.tech.dao.authority.IUserDao;
import com.easydatalink.tech.entity.authortity.SysUser;
import com.easydatalink.tech.mapper.authority.UserMapper;
import com.easydatalink.tech.orm.MyBatisDao;

@Repository
public class UserDao extends MyBatisDao<SysUser> implements IUserDao {	
	@SuppressWarnings("unused")
	private static Logger logger = LoggerFactory.getLogger(UserDao.class);
	
	@Autowired
	private UserMapper userMapper;
	
	public SysUser findUserByLoginName(String name){
		return userMapper.findUserByLoginName(name);
	}
	
}
