package com.easydatalink.tech.service.authority.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.easydatalink.tech.dao.authority.IUserDao;
import com.easydatalink.tech.entity.authortity.SysUser;
import com.easydatalink.tech.service.MybatisManager;
import com.easydatalink.tech.service.authority.IUserManager;

@Service
public class UserManager extends MybatisManager<SysUser, IUserDao> implements IUserManager {

	private static Logger logger = LoggerFactory.getLogger(UserManager.class);

	@Autowired
	private IUserDao userDao;

	@Override
	public SysUser findUserByLoginName(String name) {
		return userDao.findUserByLoginName(name);
	}

}
