package com.easydatalink.tech.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.easydatalink.tech.entity.authortity.SysUser;
import com.easydatalink.tech.provider.SysUserService;

@Service
public class SysUserServiceImpl {
	@Autowired
	private SysUserService sysUserService;

	public SysUser findByName(String username) {
		return sysUserService.findByName(username);
	}

}
