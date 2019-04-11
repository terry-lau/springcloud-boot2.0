package com.easydatalink.tech.entity;

import java.io.Serializable;
import java.util.Date;

public class IdEntity implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1279088358813689451L;
	private Long id;// 对象ID
	private String createBy;
	private Date createTime;
	private String lastUpdateBy;
	private Date lastUpdateTime;
	private String isRemoved = "0";// 0:正常1：删除
	private Long version;// 版本锁

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getCreateBy() {
		return createBy;
	}

	public void setCreateBy(String createBy) {
		this.createBy = createBy;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public String getLastUpdateBy() {
		return lastUpdateBy;
	}

	public void setLastUpdateBy(String lastUpdateBy) {
		this.lastUpdateBy = lastUpdateBy;
	}

	public Date getLastUpdateTime() {
		return lastUpdateTime;
	}

	public void setLastUpdateTime(Date lastUpdateTime) {
		this.lastUpdateTime = lastUpdateTime;
	}

	public String getIsRemoved() {
		return isRemoved;
	}

	public void setIsRemoved(String isRemoved) {
		this.isRemoved = isRemoved;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

}
