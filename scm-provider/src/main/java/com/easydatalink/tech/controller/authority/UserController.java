package com.easydatalink.tech.controller.authority;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.easydatalink.tech.entity.authortity.SysUser;
import com.easydatalink.tech.service.authority.IUserManager;

@RestController
@RequestMapping("/authority/user")
public class UserController {

	@Autowired
	private IUserManager userManager;
	
	@RequestMapping(value = "/findByName", method = RequestMethod.POST)
	public SysUser findByName(@RequestParam String loginName) {
		SysUser luser = userManager.findUserByLoginName(loginName);
		return luser;
	}
}
