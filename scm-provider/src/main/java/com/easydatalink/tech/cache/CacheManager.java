/**
 *2017年5月12日 下午9:40:38
 */
package com.easydatalink.tech.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.easydatalink.tech.utils.Log4jManager;
import com.easydatalink.tech.utils.StringHelper;

/**
 * @author: liubin
 *
 */
@Service
public class CacheManager<T> {

	@Autowired
	private RedisClient redisClient;

	/**
	 * 获取KEY VALUE形式的缓存对象，VALUE=JSON形式转为对象
	 * 
	 * @param cacheKey
	 * @param cls
	 * @return T
	 */
	public T getCache(String cacheKey, Class<T> cls) {
		String json = redisClient.get(cacheKey);
		if (StringHelper.isNull(json))
			return null;
		JSONObject jo = JSONObject.parseObject(json);
		return (T) JSONObject.toJavaObject(jo, cls);
	}

	/**
	 * 删除指定KEY的缓存
	 * 
	 * @param cacheKey
	 */
	public void deleteCache(String cacheKey) {
		redisClient.del(cacheKey);
	}

	/**
	 * 默认写入KEY VALUE缓存，存活期永久,VALUE=JSON
	 * 
	 * @param entity
	 * @param cacheKey
	 */
	public void set(String cacheKey, T entity) {
		set(cacheKey, entity, 0);
	}

	/**
	 * 写入缓存，可以指定存活期,0为永久,单位秒,VALUE=JSON
	 * 
	 * @param entity
	 * @param cacheKey
	 * @param cacheLifeSeconds
	 */
	public void set(String cacheKey, T entity, Integer cacheLifeSeconds) {
		Object json = JSONObject.toJSON(entity);
		if (json == null || StringHelper.isNull(json.toString()))
			return;
		if (cacheLifeSeconds <= 0)
			redisClient.set(cacheKey, json.toString());
		else
			redisClient.set(cacheKey, json.toString(), cacheLifeSeconds);
	}

	public boolean zexists(String key, String member) {
		return redisClient.zexist(key, member);
	}

	public boolean zrem(String key, String member) {
		try {
			redisClient.zrem(key, member);
		} catch (Exception e) {
			e.printStackTrace();
			Log4jManager.logError(e);
		}
		return true;
	}

	public Set<String> zrangeByScore(String key, String min, String max) {
		return redisClient.zrangeByScore(key, min, max);
	}

	public void zadd(String key, Map<String, Double> members) {
		redisClient.zadd(key, members);

	}

	public long zadd(String key, double score, String member) {
		return redisClient.zadd(key, score, member);
	}

	public long zcard(String key) {
		return redisClient.zcard(key);
	}

	/**
	 * 设置对应key的过期时间，单位秒
	 * 
	 * @param key
	 * @param sec
	 * @return boolean
	 */
	public boolean expire(String key, int sec) {
		try {
			redisClient.expire(key, sec);
		} catch (Exception e) {
			Log4jManager.logError(e);
			return false;
		}
		return true;
	}

	/**
	 * 查询指定key的过期时间，返回单位秒
	 * 
	 * @param key
	 * @return long
	 */
	public long ttl(String key) {
		return redisClient.ttl(key);
	}

	/**
	 * redis list结构，队列头部阻塞弹出
	 * 
	 * @param cacheKey
	 * @param cls
	 * @param timeInvSec
	 * @return T
	 */
	public T blpop(String cacheKey, Class<T> cls, Integer timeInvSec) {
		String o = redisClient.blpop(cacheKey, timeInvSec);
		if (StringHelper.isNull(o))
			return null;
		return (T) JSONObject.parseObject(o, cls);
	}

	/**
	 * redis list结构，向队列尾部添加
	 * 
	 * @param cacheKey
	 * @param entity
	 */
	public void rpush(String cacheKey, T entity) {
		Object json = JSONObject.toJSON(entity);
		if (json == null || StringHelper.isNull(json.toString()))
			return;
		redisClient.rpush(cacheKey, json.toString());
	}

	/**
	 * 返回redis list结构的队列长度
	 * 
	 * @param cacheKey
	 * @return Long
	 */
	public Long llenCache(String cacheKey) {
		return redisClient.llen(cacheKey);
	}

	/**
	 * 获取redis list中区间范围的数据
	 * @param cacheKey
	 * @param cls
	 * @param start
	 * @param end
	 * @return
	 */
	public List<T> lrange(String cacheKey, Class<T> cls, Long start, Long end) {
		List<T> ts = new ArrayList<T>();
		List<String> os = redisClient.lrange(cacheKey, start, end);
		if(os == null || os.size() <= 0 ) {
			return null;
		}
		for(int i=0; i<os.size(); i++) {
			JSONObject jo = JSONObject.parseObject(os.get(i));
			ts.add((T) JSONObject.parseObject(jo.toJSONString(), cls));
		}
		return ts;
	}
	
	/**
	 * 获取指定key的内容
	 * */
	public String get(String key){
		return redisClient.get(key);
	}
	
	/**
	 * 设置键值并设置过期时间
	 * @param key 键名
	 * @param value 键值
	 * @param second  过期时间
	 * @return
	 */
	public String set(String key, String value, int second){
		return redisClient.set(key, value, second);
	}
	
	/**
	 * 设置键值并设置过期时间
	 * @param key 键名
	 * @param value 键值
	 * @param second  过期时间
	 * @return
	 */
	public String setString(String key, String value){
		return redisClient.set(key, value);
	}
}
