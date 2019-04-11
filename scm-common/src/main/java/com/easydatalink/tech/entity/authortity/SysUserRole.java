package com.easydatalink.tech.entity.authortity;

import com.easydatalink.tech.entity.IdEntity;

public class SysUserRole extends IdEntity {

    /**
	 * 
	 */
	private static final long serialVersionUID = 2169853602297339850L;

	private Long userId;

    private Long roleId;

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public Long getRoleId() {
		return roleId;
	}

	public void setRoleId(Long roleId) {
		this.roleId = roleId;
	}

}