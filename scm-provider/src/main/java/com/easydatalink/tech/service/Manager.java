package com.easydatalink.tech.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.easydatalink.tech.dao.IDao;
import com.easydatalink.tech.entity.IdEntity;
import com.easydatalink.tech.page.Page;
import com.easydatalink.tech.utils.ReflectionUtils;

public class Manager<T extends IdEntity, D extends IDao<T>> implements IManager<T> {
	@Autowired
	protected D dao;

	public void setDao(D dao) {
		this.dao = dao;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public T get(Long id) {
		return dao.get(id);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public T getByCode(final String code) {
		return dao.getByCode(code);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public T getByCode(final T entity) {
		return dao.getByCode(entity);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<T> getAll() {
		return dao.getAll();
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void delete(Long id) {
		dao.delete(id);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void delete(T entity) {
		dao.delete(entity);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void deleteComplete(final Long id) {
		dao.deleteComplete(id);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Long insert(T entity) {
		Long id = dao.insert(entity);
		return id;
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public boolean update(T entity) {
		boolean b = dao.update(entity);
		return b;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public T saveOrUpdate(T entity) {
		if (entity.getId() == null || get(entity.getId()) == null) {
			Long id = insert(entity);
			entity.setId(id);
		} else {
			update(entity);
		}
		return entity;
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void batchInsert(List<T> entities) {
		dao.batchInsert(entities);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void batchUpdate(List<T> entities) {
		dao.batchUpdate(entities);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<T> findBy(String propName, Object propValue) {
		return dao.findBy(propName, propValue);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public T findUniqueBy(String propName, Object propValue) {
		return dao.findUniqueBy(propName, propValue);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<T> findByIds(List<Long> ids) {
		return dao.findByIds(ids);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void batchDelete(List<Long> ids) {
		dao.batchDelete(ids);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<T> findByLists(String propName, List<Object> list) {
		return dao.findByLists(propName, list);
	}

	/**
	 * 调用这个方法谨记要自己去实现
	 */
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Page<T> findPage(final Page<T> page, String sql, final Map<String, ?> values) {
		return null;
	}

	protected Class getBeanClass() {
		Class cls = ReflectionUtils.getSuperClassGenericType(dao.getClass());
		return cls;
	}

	private Class getManagerInterface() {
		Class[] clz = getClass().getInterfaces();
		if (clz == null) {
			return null;
		}
		for (Class c : clz) {
			if (IManager.class.isAssignableFrom(c)) {
				return c;
			}
		}
		return null;
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void batchDeleteComplete(List<Long> ids) {
		for (Long id : ids) {
			this.deleteComplete(id);
		}

	}

}
