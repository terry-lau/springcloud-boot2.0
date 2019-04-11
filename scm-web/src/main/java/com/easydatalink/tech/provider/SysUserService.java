package com.easydatalink.tech.provider;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.easydatalink.tech.entity.authortity.SysUser;

/**
 * 用户管理
 */
@FeignClient(name = "scm-provider")
public interface SysUserService{
	
	@RequestMapping("/authority/user/findByName")
	SysUser findByName(@RequestParam("username") String username);
}
