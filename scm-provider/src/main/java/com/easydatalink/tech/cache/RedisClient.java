package com.easydatalink.tech.cache;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.SortingParams;

import com.easydatalink.tech.utils.Log4jManager;
import com.easydatalink.tech.utils.StringHelper;

/**
 * redis工具类
 */
@Repository("RedisClient")
@Scope("singleton")
public class RedisClient {

	private Log log = LogFactory.getLog(RedisClient.class);

	private static JedisCluster jc;

	private static JedisPool pool = null;

	@Value("${redis.server.servers}")
	private String servers;
	@Value("${redis.server.iscluster}")
	private boolean isCluster;
	@Value("${redis.server.maxTotal}")
	private Integer maxTotal;
	@Value("${redis.server.maxRedirections}")
	private Integer maxRedirections;
	@Value("${redis.server.timeout}")
	private Integer timeout;

	// redis集群发生硬件故障/重启时，减少没用的日志输出，间隔30秒处理一次
	private static long errorWaitTimeout = 30 * 1000l;

	@PostConstruct
	public void init() {
		if (StringHelper.isNull(servers)) {
			throw new IllegalArgumentException("redis servers is null ,check config setting");
		}
		String[] serverList = StringHelper.splitStr(servers, ",");
		Set<HostAndPort> jedisClusterNodes = new HashSet<HostAndPort>();
		for (String e : serverList) {
			String[] temp = StringHelper.splitStr(e, ":");
			jedisClusterNodes.add(new HostAndPort(temp[0], new Integer(temp[1])));
		}

		if (isCluster) {
			GenericObjectPoolConfig config = new GenericObjectPoolConfig();
			if (maxTotal == 0)
				config.setMaxTotal(1500);
			else
				config.setMaxTotal(maxTotal);

			if (maxRedirections == null || maxRedirections <= 0)
				maxRedirections = 200;
			if (timeout == null || timeout <= 0)
				timeout = 10000;

			jc = new JedisCluster(jedisClusterNodes, timeout, maxRedirections,config);
		} else {
			initPool();
		}
		log.info("--------> Redis init success <---------");
	}

	private Jedis getResource() {
		if (pool == null)
			return getRealConnection();
		return pool.getResource();
	}

	private Jedis getRealConnection() {
		try {
			String[] serverList = StringHelper.splitStr(servers, ",");
			String[] temp = StringHelper.splitStr(serverList[0], ":");
			// Jedis res = new Jedis(temp[0], Integer.parseInt(temp[1]));
			JedisPoolConfig config = new JedisPoolConfig();
			config.setMaxTotal(500);
			config.setMaxIdle(10);
			config.setMaxWaitMillis(100000l);
			config.setTestOnBorrow(true);
//			pool = new JedisPool(config, temp[0], Integer.parseInt(temp[1]));
			//使用redis第2个db
			pool = new JedisPool(config, temp[0], Integer.parseInt(temp[1]), 2000, null, 1);
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return pool.getResource();
	}

	private void returnResource(Jedis jedis) {
		try {
			if (jedis != null) {
				if (pool != null) {
					 pool.returnResource(jedis);
					jedis.close();
				}
			}
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	private void initPool() {
		getRealConnection();
	}

	/**
	 * SortedSet（有序集合）
	 * 返回有序集 key 中，所有 score 值介于 min 和 max 之间(包括等于 min 或 max )的成员。有序集成员按 score 值递增(从小到大)次序排列。
	 * @param key
	 * @param min
	 * @param max
	 * @return
	 */
	public Set<String> zrangeByScore(String key, String min, String max) {
		try {
			if (isCluster)
				return jc.zrangeByScore(key, min, max);
			else
				return zrangeByScoreSingle(key, min, max);
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return zrangeByScore(key, min, max);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}
	/**
	 * SortedSet（有序集合）
	 * 返回有序集 key 中，所有 score 值介于 min 和 max 之间(包括等于 min 或 max )的成员。有序集成员按 score 值递增(从小到大)次序排列。
	 * @param key
	 * @param min
	 * @param max
	 * @return
	 */
	private Set<String> zrangeByScoreSingle(String key, String min, String max) {
		try {
			Set<String> _value = null;
			Jedis jedis = getResource();
			_value = jedis.zrangeByScore(key, min, max);
			returnResource(jedis);
			return _value;
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return zrangeByScoreSingle(key, min, max);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}
	/**
	 * SortedSet（有序集合）
	 * 返回有序集 key 中，指定区间内的成员
	 * @param key
	 * @param start
	 * @param end
	 * @return
	 */
	public Set<String> zrange(String key, long start, long end) {
		try {
			if (isCluster)
				return jc.zrange(key, start, end);
			else
				return zrangeSingle(key, start, end);
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return zrange(key, start, end);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * SortedSet（有序集合）
	 * 返回有序集 key 中，指定区间内的成员
	 * @param key
	 * @param start
	 * @param end
	 * @return
	 */
	private Set<String> zrangeSingle(String key, long start, long end) {
		try {
			Set<String> _value = null;
			Jedis jedis = getResource();
			_value = jedis.zrange(key, start, end);
			returnResource(jedis);

			return _value;
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return zrangeSingle(key, start, end);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * SortedSet（有序集合）
	 * 将一个或多个 member 元素及其 score 值加入到有序集 key 当中。
	 * @param key
	 * @param members
	 * @return
	 */
	public Long zadd(String key, Map<String, Double> members) {
		try {
			if (isCluster)
				return jc.zadd(key, members);
			else
				return zaddSignle(key, members);
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return zadd(key, members);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}
	/**
	 * SortedSet（有序集合）
	 * 将一个或多个 member 元素及其 score 值加入到有序集 key 当中。
	 * @param key
	 * @param members
	 * @return
	 */
	private Long zaddSignle(String key, Map<String, Double> members) {
		try {
			long _value = 0;
			Jedis jedis = getResource();
			_value = jedis.zadd(key, members);
			returnResource(jedis);

			return _value;
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return zaddSignle(key, members);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}

	/**
	 *  SortedSet（有序集合）
	 * 返回有序集 key 的基数。
	 * @param key
	 * @return
	 */
	public long zcard(String key) {
		try {
			if (isCluster)
				return jc.zcard(key);
			else
				return zcardSingle(key);
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return zcard(key);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return 0l;
	}
	/**
	 *  SortedSet（有序集合）
	 * 返回有序集 key 的基数。
	 * @param key
	 * @return
	 */
	private long zcardSingle(String key) {
		try {
			long _value = 0;
			Jedis jedis = getResource();
			_value = jedis.zcard(key);
			returnResource(jedis);

			return _value;
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return zcardSingle(key);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return 0l;
	}
	/**
	 *  SortedSet（有序集合）
	 * 将一个或多个 member 元素及其 score 值加入到有序集 key 当中。
	 * @param key
	 * @return
	 */
	public long zadd(String key, double score, String member) {
		try {
			if (isCluster)
				return jc.zadd(key, score, member);
			else
				return zaddSingle(key, score, member);
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return zadd(key, score, member);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return 0l;
	}
	/**
	 *  SortedSet（有序集合）
	 * 将一个或多个 member 元素及其 score 值加入到有序集 key 当中。
	 * @param key
	 * @return
	 */
	private long zaddSingle(String key, double score, String member) {
		try {
			long _value = 0;
			Jedis jedis = getResource();
			_value = jedis.zadd(key, score, member);
			returnResource(jedis);

			return _value;
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return zaddSingle(key, score, member);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return 0l;
	}
	/**
	 *  SortedSet（有序集合）
	 * 移除有序集 key 中的一个或多个成员，不存在的成员将被忽略。
	 * @param key
	 * @param member
	 */
	public void zrem(String key, String member) {
		try {
			if (isCluster)
				jc.zrem(key, member);
			else {
				long _value = 0;
				Jedis jedis = getResource();
				_value = jedis.zrem(key, member);
				returnResource(jedis);
			}
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				zrem(key, member);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	/**
	 * 为有序集 key 的成员 member 的 score 值加上增量 increment
	 * 
	 * @param key
	 * @param score
	 * @param member
	 * @return
	 */
	public double zincrby(String key, double score, String member) {
		try {
			if (isCluster)
				return jc.zincrby(key, score, member);
			else
				return zincrbySingle(key, score, member);
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return zincrby(key, score, member);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return 0.0d;
	}
	/**
	 * 为有序集 key 的成员 member 的 score 值加上增量 increment
	 * 
	 * @param key
	 * @param score
	 * @param member
	 * @return
	 */
	private double zincrbySingle(String key, double score, String member) {
		try {
			double _value = 0;
			Jedis jedis = getResource();
			_value = jedis.zincrby(key, score, member);
			returnResource(jedis);

			return _value;
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return zincrbySingle(key, score, member);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return 0.0d;
	}

	/**
	 * 判断一个有序集合中是否包含某一个元素
	 * 
	 * @param key
	 * @param member
	 * @return
	 */
	public boolean zexist(String key, String member) {
		try {
			if (isCluster) {
				boolean _value = jc.zrank(key, member) != null;
				return _value;
			} else {
				return zexistSingle(key, member);
			}
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return zexist(key, member);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return false;
	}
	/**
	 * 判断一个有序集合中是否包含某一个元素
	 * 
	 * @param key
	 * @param member
	 * @return
	 */
	private boolean zexistSingle(String key, String member) {
		try {
			boolean _value = false;
			Jedis jedis = getResource();
			_value = jedis.zrank(key, member) != null;
			returnResource(jedis);

			return _value;
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return zexistSingle(key, member);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return false;
	}

	/**
	 * Set（集合）
	 * 删除一个元素
	 * @param key
	 * @param members
	 * @return long
	 */
	public long srem(String key, String... members) {
		try {
			if (isCluster)
				return jc.srem(key, members);
			else {
				long _value = 0L;
				Jedis jedis = getResource();
				_value = jedis.srem(key, members);
				returnResource(jedis);

				return _value;
			}
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return srem(key, members);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return 0l;
	}

	/**
	 * Set（集合）
	 * 获取一个指定key的set合集总数
	 * @param key
	 * @return long
	 */
	public long scard(String key) {
		try {
			if (isCluster)
				return jc.scard(key);
			else {
				long _value = 0L;
				Jedis jedis = getResource();
				_value = jedis.scard(key);
				returnResource(jedis);
				return _value;
			}
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return scard(key);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return 0l;
	}

	/**
	 * Set（集合）
	 * 向一个指定的集合中添加元素
	 * @param key
	 * @param members
	 * @return 被添加到集合中的新元素的数量，不包括被忽略的元素。
	 */
	public long sadd(String key, String... members) {
		try {
			if (isCluster)
				return jc.sadd(key, members);
			else
				return saddSingle(key, members);
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return sadd(key, members);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return 0l;
	}

	/**
	 * Set（集合）
	 * 向一个指定的集合中添加元素
	 * @param key
	 * @param members
	 * @return 被添加到集合中的新元素的数量，不包括被忽略的元素。
	 */
	private long saddSingle(String key, String... members) {
		try {
			long _value = 0L;
			Jedis jedis = getResource();
			_value = jedis.sadd(key, members);
			returnResource(jedis);

			return _value;
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return saddSingle(key, members);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return 0l;
	}

	/**
	 * Set（集合）
	 * 从队列中弹出指定数量的对象<br>
	 * @param key
	 * @param count
	 * @return Set<String>
	 */
	public Set<String> spop(String key, int count) {
		try {
			if (isCluster)
				return jc.spop(key, count);
			else
				return spopSingle(key, count);
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return spop(key, count);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}
	/**
	 * Set（集合）
	 * 从队列中弹出指定数量的对象<br>
	 * @param key
	 * @param count
	 * @return Set<String>
	 */
	private Set<String> spopSingle(String key, int count) {
		try {
			Jedis jedis = getResource();
			Set<String> res = jedis.spop(key, count);
			returnResource(jedis);
			return res;
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return spopSingle(key, count);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * Set（集合）
	 * 从队列中弹出单个对象<br>
	 * @param key
	 * @return String
	 */
	public String spop(String key) {
		try {
			if (isCluster)
				return jc.spop(key);
			else
				return spopSingle(key);
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return spop(key);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}
	/**
	 * Set（集合）
	 * 从队列中弹出单个对象<br>
	 * @param key
	 * @return String
	 */
	private String spopSingle(String key) {
		try {
			Jedis jedis = getResource();
			String res = jedis.spop(key);
			returnResource(jedis);
			return res;
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return spopSingle(key);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * Hash（哈希表）
	 * 向hash中一次加入多个值
	 * @param key
	 * @param hash
	 * @return
	 */
	public String hmset(String key, Map<String, String> hash) {
		try {
			if (isCluster)
				return jc.hmset(key, hash);
			else
				return hmsetSingle(key, hash);
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return hmset(key, hash);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * Hash（哈希表）
	 * 向hash中一次加入多个值
	 * @param key
	 * @param hash
	 * @return
	 */
	private String hmsetSingle(String key, Map<String, String> hash) {
		try {
			String _value = null;
			Jedis jedis = getResource();
			_value = jedis.hmset(key, hash);
			returnResource(jedis);

			return _value;
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return hmsetSingle(key, hash);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * Hash（哈希表）
	 * 在哈希表中添加内容
	 * @param key
	 * @param field
	 * @param value
	 * @return
	 */
	public boolean hset(String key, String field, String value) {
		try {
			if (isCluster) {
				jc.hset(key, field, value);
				return true;
			} else {
				return hsetSingle(key, field, value);
			}
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return hset(key, field, value);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return false;
	}
	/**
	 * Hash（哈希表）
	 * 在哈希表中添加内容
	 * @param key
	 * @param field
	 * @param value
	 * @return
	 */
	private boolean hsetSingle(String key, String field, String value) {
		try {
			Jedis jedis = getResource();
			jedis.hset(key, field, value);
			returnResource(jedis);

			return true;
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return hsetSingle(key, field, value);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return false;
	}

	/**
	 * String（字符串）
	 * 将指定的key+1，如果该key不存在，则先初始化为0，然后再进行自增操作
	 * @param key
	 *            需要自增的key
	 * @return 自增后的值
	 */
	public long incr(String key) {
		try {
			if (isCluster)
				return jc.incr(key);
			else
				return incrSingle(key);
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return incr(key);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return 0l;
	}
	/**
	 * String（字符串）
	 * 将指定的key+1，如果该key不存在，则先初始化为0，然后再进行自增操作
	 * @param key
	 *            需要自增的key
	 * @return 自增后的值
	 */
	private long incrSingle(String key) {
		try {
			long _value = 0L;
			Jedis jedis = getResource();
			_value = jedis.incr(key);
			returnResource(jedis);

			return _value;
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return incrSingle(key);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return 0l;
	}

	/**
	 * 查看指定key的剩余时间
	 * @param key
	 * @return 当 key 不存在时，返回 -2 。当 key 存在但没有设置剩余生存时间时，返回 -1 。否则，以秒为单位，返回 key
	 *         的剩余生存时间。
	 */
	public long ttl(String key) {
		try {
			if (isCluster)
				return jc.ttl(key);
			else
				return ttlSingle(key);
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return ttl(key);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return 0l;
	}
	/**
	 * 查看指定key的剩余时间
	 * @param key
	 * @return 当 key 不存在时，返回 -2 。当 key 存在但没有设置剩余生存时间时，返回 -1 。否则，以秒为单位，返回 key
	 *         的剩余生存时间。
	 */
	private long ttlSingle(String key) {
		try {
			long _value = 0L;
			Jedis jedis = getResource();
			_value = jedis.ttl(key);
			returnResource(jedis);

			return _value;

		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return ttlSingle(key);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return 0l;
	}

	/**
	 * 对指定的key进行排序
	 * 
	 * @param key
	 * @param params
	 * @return
	 */
	public List<String> sort(String key, SortingParams params) {
		try {
			if (isCluster)
				return jc.sort(key, params);
			else
				return sortSingle(key, params);
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return sort(key, params);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}
	/**
	 * 对指定的key进行排序
	 * 
	 * @param key
	 * @param params
	 * @return
	 */
	private List<String> sortSingle(String key, SortingParams params) {
		try {
			List<String> _value = null;
			Jedis jedis = getResource();
			_value = jedis.sort(key, params);
			returnResource(jedis);

			return _value;
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return sortSingle(key, params);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * 将指定的key在指定的时间戳过期
	 * 
	 * @param key
	 * @param unixTime
	 *            unix时间戳
	 * @return 如果生存时间设置成功，返回 1 。当 key 不存在或没办法设置生存时间，返回 0 。
	 */
	public long expireAt(String key, long unixTime) {
		try {
			if (isCluster)
				return jc.expireAt(key, unixTime);
			else
				return expireAtSingle(key, unixTime);
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return expireAt(key, unixTime);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return 0l;
	}
	/**
	 * 将指定的key在指定的时间戳过期
	 * 
	 * @param key
	 * @param unixTime
	 *            unix时间戳
	 * @return 如果生存时间设置成功，返回 1 。当 key 不存在或没办法设置生存时间，返回 0 。
	 */
	private long expireAtSingle(String key, long unixTime) {
		try {
			long _value = 0L;
			Jedis jedis = getResource();
			_value = jedis.expireAt(key, unixTime);
			returnResource(jedis);

			return _value;

		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return expireAtSingle(key, unixTime);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return 0l;
	}

	/**
	 * 移除给定key的生存周期
	 * 
	 * @param key
	 * @return 移除成功返回1，失败返回0
	 */
	public long persist(String key) {
		try {
			if (isCluster)
				return jc.persist(key);
			else
				return persistSingle(key);

		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return persist(key);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return 0l;
	}
	/**
	 * 移除给定key的生存周期
	 * 
	 * @param key
	 * @return 移除成功返回1，失败返回0
	 */
	private long persistSingle(String key) {
		try {
			long _value = 0L;
			Jedis jedis = getResource();
			_value = jedis.persist(key);
			returnResource(jedis);

			return _value;

		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return persistSingle(key);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return 0l;
	}

	/**
	 * 将指定的key设置过期时间
	 * 
	 * @param key 指定的key
	 * @param seconds 过期时间
	 * @return
	 */
	public long expire(String key, int seconds) {
		try {
			if (isCluster)
				return jc.expire(key, seconds);
			else
				return expireSignle(key, seconds);

		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return expire(key, seconds);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return 0l;
	}
	/**
	 * 将指定的key设置过期时间
	 * 
	 * @param key 指定的key
	 * @param seconds 过期时间
	 * @return
	 */
	private long expireSignle(String key, int seconds) {
		try {
			long _value = 0L;
			Jedis jedis = getResource();
			_value = jedis.expire(key, seconds);
			returnResource(jedis);

			return _value;

		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return expireSignle(key, seconds);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return 0l;
	}

	/**
	 * 判断给定的键值是否存在
	 * 
	 * @param key 需要判断的键值
	 * @return
	 */
	public boolean exist(String key) {
		try {
			if (isCluster)
				return jc.exists(key);
			else
				return existSingle(key);
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return exist(key);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return false;

	}
	/**
	 * 判断给定的键值是否存在
	 * 
	 * @param key 需要判断的键值
	 * @return
	 */
	private boolean existSingle(String key) {
		try {
			boolean _value = false;
			Jedis jedis = getResource();
			_value = jedis.exists(key);
			returnResource(jedis);

			return _value;

		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return existSingle(key);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return false;
	}

	/**
	 * 如果key不存在则设置，否则不设置
	 * @param key 键名
	 * @param value 键值
	 * @return 如果设置成功则返回1，否则返回0
	 */
	public long setIfNotExist(String key, String value) {
		try {
			if (isCluster)
				return jc.setnx(key, value);
			else
				return setIfNotExistSingle(key, value);
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return setIfNotExist(key, value);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		return 0l;
	}
	/**
	 * 如果key不存在则设置，否则不设置
	 * @param key 键名
	 * @param value 键值
	 * @return 如果设置成功则返回1，否则返回0
	 */
	private long setIfNotExistSingle(String key, String value) {
		try {
			long _value = 0;
			Jedis jedis = getResource();
			_value = jedis.setnx(key, value);
			returnResource(jedis);

			return _value;

		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return setIfNotExistSingle(key, value);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return 0l;
	}

	/**
	 * 设置键值并设置过期时间
	 * @param key 键名
	 * @param value 键值
	 * @param second  过期时间
	 * @return
	 */
	public String set(String key, String value, int second) {
		try {
			if (isCluster)
				return jc.setex(key, second, value);
			else
				return setSingle(key, value, second);
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return set(key, value, second);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}
	/**
	 * 设置键值并设置过期时间
	 * @param key 键名
	 * @param value 键值
	 * @param second  过期时间
	 * @return
	 */
	private String setSingle(String key, String value, int second) {
		try {
			String _value = null;
			Jedis jedis = getResource();
			_value = jedis.setex(key, second, value);
			returnResource(jedis);

			return _value;

		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return setSingle(key, value, second);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * <p>
	 * <strong>SET key value</strong>
	 * </p>
	 * String（字符串）
	 * 将字符串值 <tt class="docutils literal"><span class="pre">value</span></tt>
	 * 关联到 <tt class="docutils literal"><span class="pre">key</span></tt> 。
	 * </p>
	 * 
	 * @param key 键名
	 * @param value 键值
	 * @return
	 */
	public String set(String key, String value) {
		try {
			if (isCluster)
				return jc.set(key, value);
			else
				return setSingle(key, value);

		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return set(key, value);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}

	private String setSingle(String key, String value) {
		try {
			String _value = null;
			Jedis jedis = getResource();
			_value = jedis.set(key, value);
			returnResource(jedis);

			return _value;

		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return setSingle(key, value);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}

	public String set(String key, Object value) {
		try {
			if (isCluster)
				return jc.set(key, value.toString());
			else
				return setSingle(key, value.toString());
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return set(key, value);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			return null;
		}
	}

	/**
	 * <b>DEL key [key ...]</b> <br/>
	 * 删除给定的一个或多个 key 。<br/>
	 * 不存在的 key 会被忽略。<br/>
	 * <b>可用版本：</b><br/>
	 * >= 1.0.0 <br/>
	 * <b>时间复杂度：</b><br/>
	 * O(N)， N 为被删除的 key 的数量。<br/>
	 * 删除单个字符串类型的 key ，时间复杂度为O(1)。 <br/>
	 * 删除单个列表、集合、有序集合或哈希表类型的key ，时间复杂度为O(M)， M 为以上数据结构内的元素数量。<br/>
	 * <b>返回值：</b></br> 被删除 key 的数量。
	 * 
	 * @param keys
	 *            需要被删除的key
	 * @return
	 */
	public void del(String... keys) {
		for (String e : keys) {
			del(e);
		}
	}

	public long del(String key) {
		try {
			if (isCluster)
				return jc.del(key);
			else
				return delSingle(key);
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return del(key);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return 0l;
	}

	private long delSingle(String... keys) {
		try {
			long value = 0;
			Jedis jedis = getResource();
			value = jedis.del(keys);
			returnResource(jedis);

			return value;

		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return delSingle(keys);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return 0l;
	}

	/**
	 * 获取指定key的内容
	 * 
	 * @param key
	 * @return
	 */
	public String get(String key) {
		try {
			if (isCluster)
				return jc.get(key);
			else
				return getSingle(key);
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return get(key);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}

	private String getSingle(String key) {
		try {
			String value = null;
			Jedis jedis = getResource();
			value = jedis.get(key);
			returnResource(jedis);
			return value;
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return getSingle(key);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * 在哈希中获取指定key的内容
	 * Hash（哈希表）
	 * @param key
	 * @return
	 */
	public String hget(String key, String field) {
		try {
			if (isCluster)
				return jc.hget(key, field);
			else
				return hgetSingle(key, field);
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return hget(key, field);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}
	/**
	 * 在哈希中获取指定key的内容
	 * Hash（哈希表）
	 * @param key
	 * @return
	 */
	private String hgetSingle(String key, String field) {
		try {
			String value = null;
			Jedis jedis = getResource();
			value = jedis.hget(key, field);
			returnResource(jedis);

			return value;

		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return hgetSingle(key, field);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * 指定的key内，指定field是否存在
	 * Hash（哈希表）
	 * @param key
	 * @param field
	 * @return boolean
	 */
	public boolean hexist(String key, String field) {
		try {
			if (isCluster)
				return jc.hexists(key, field);
			else {
				boolean _value = false;
				Jedis jedis = getResource();
				_value = jedis.hexists(key, field);
				returnResource(jedis);
				return _value;
			}
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return hexist(key, field);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return false;
	}

	/**
	 * 删除一个list中的指定个数的指定元素<br>
	 * 当count=0的时候则删除所有相同元素<br>
	 * List（列表）
	 * @param key
	 * @param count
	 * @param value
	 * @return long
	 */
	public long lrem(String key, int count, String value) {
		try {
			if (isCluster)
				return jc.lrem(key, count, value);
			else {
				Jedis jedis = getResource();
				long res = jedis.lrem(key, count, value);
				returnResource(jedis);
				return res;
			}

		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return lrem(key, count, value);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return 0l;
	}
	/**
	 * List（列表）
	 * 返回列表 key 中指定区间内的元素，区间以偏移量 start 和 stop 指定。
	 * @param key
	 * @param start
	 * @param end
	 * @return
	 */
	public List<String> lrange(String key, long start, long end) {
		try {
			if (isCluster)
				return jc.lrange(key, start, end);
			else
				return lrangeSingle(key, start, end);
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return lrange(key, start, end);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}
	/**
	 * List（列表）
	 * 返回列表 key 中指定区间内的元素，区间以偏移量 start 和 stop 指定。
	 * @param key
	 * @param start
	 * @param end
	 * @return
	 */
	private List<String> lrangeSingle(String key, long start, long end) {
		try {
			List<String> value = null;
			Jedis jedis = getResource();
			value = jedis.lrange(key, start, end);
			returnResource(jedis);
			return value;
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return lrangeSingle(key, start, end);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * List（列表）
	 * 指定队列位置取值
	 * 返回列表 key 中，下标为 index 的元素
	 * @param key
	 * @param index
	 * @return String
	 */
	public String lindex(String key, long index) {
		try {
			if (isCluster)
				return jc.lindex(key, index);
			else
				return lindexSingle(key, index);
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return lindex(key, index);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}
	/**
	 * List（列表）
	 * 指定队列位置取值
	 * 返回列表 key 中，下标为 index 的元素
	 * @param key
	 * @param index
	 * @return String
	 */
	private String lindexSingle(String key, long index) {
		try {
			String value = null;
			Jedis jedis = getResource();
			value = jedis.lindex(key, index);
			returnResource(jedis);

			return value;
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return lindexSingle(key, index);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * List（列表）
	 * 判断指定值是否在队列中
	 * @param key
	 * @param value
	 * @return boolean
	 */
	public boolean isExistInList(String key, String value) {
		try {
			List<String> l = lrange(key, 0, -1);
			for (String e : l) {
				if (e.equals(value))
					return true;
			}
			return false;
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return isExistInList(key, value);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return false;
	}

	/**
	 * List（列表）
	 * 向队列里添加一个唯一值,list必须为非空
	 * @param key
	 * @param vals
	 * @return long
	 */
	public long lpushUnique(String key, String val) {
		try {
			long res = 0l;
			if (isCluster) {
				if (!isExistInList(key, val)) {
					res = jc.lpush(key, val);
				}
			} else {
				if (!isExistInList(key, val)) {
					res = lpushSingle(key, val);
				}
			}
			return res;
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return lpushUnique(key, val);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return 0l;
	}

	/**
	 * List（列表）
	 * 向队列指定位置覆盖一个新值
	 * @param key
	 * @param index
	 * @param value
	 */
	public void lset(String key, long index, String value) {
		try {
			if (isCluster)
				jc.lset(key, index, value);
			else
				lsetSingle(key, index, value);
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				lset(key, index, value);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}

	}

	private void lsetSingle(String key, long index, String value) {
		try {
			Jedis jedis = getResource();
			value = jedis.lset(key, index, value);
			returnResource(jedis);
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				lsetSingle(key, index, value);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}

	}

	/**
	 * List（列表）
	 * 向列表头部加入一个元素
	 * @param key  需要加入的列表名称
	 * @param vals  需要加入的列表的值
	 * @return
	 */
	public long lpush(String key, String... vals) {
		try {
			if (isCluster)
				return jc.lpush(key, vals);
			else
				return lpushSingle(key, vals);
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return lpush(key, vals);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return 0l;
	}
	/**
	 * List（列表）
	 * 向列表头部加入一个元素
	 * @param key  需要加入的列表名称
	 * @param vals  需要加入的列表的值
	 * @return
	 */
	private long lpushSingle(String key, String... vals) {
		try {
			long value = 0L;
			Jedis jedis = getResource();
			value = jedis.lpush(key, vals);
			returnResource(jedis);
			return value;
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return lpushSingle(key, vals);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return 0l;
	}

	/**
	 * List（列表）
	 * 从队列头部取出一个元素
	 * @param key 队列名称
	 * @return
	 */
	public String lpop(String key) {
		try {
			if (isCluster)
				return jc.lpop(key);
			else
				return lpopSingle(key);
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return lpop(key);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}
	/**
	 * List（列表）
	 * 从队列头部取出一个元素
	 * @param key 队列名称
	 * @return
	 */
	private String lpopSingle(String key) {
		try {
			String value = null;
			Jedis jedis = getResource();
			value = jedis.lpop(key);
			returnResource(jedis);

			return value;
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return lpopSingle(key);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * List（列表）
	 * 从队尾加入元素
	 * @param key  队列名称
	 * @param vals 需要加入的元素
	 * @return
	 */
	public long rpush(String key, String... vals) {
		try {

			if (isCluster)
				return jc.rpush(key, vals);
			else
				return rpushSingle(key, vals);
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return rpush(key, vals);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return 0l;
	}
	
	/**
	 * List（列表）
	 * 从队尾加入元素
	 * @param key  队列名称
	 * @param vals 需要加入的元素
	 * @return
	 */
	private long rpushSingle(String key, String... vals) {
		try {
			long value = 0L;
			Jedis jedis = getResource();
			value = jedis.rpush(key, vals);
			returnResource(jedis);

			return value;
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return rpushSingle(key, vals);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return 0l;
	}

	/**
	 *  List（列表）
	 * 从队尾取出一个元素
	 * @param key
	 * @return
	 */
	public String rpop(String key) {
		try {
			if (isCluster)
				return jc.rpop(key);
			else
				return rpopSingle(key);
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return rpop(key);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}

	/**
	 *  List（列表）
	 * 从队尾取出一个元素
	 * @param key
	 * @return
	 */
	private String rpopSingle(String key) {
		try {
			String value = null;
			Jedis jedis = getResource();
			value = jedis.rpop(key);
			returnResource(jedis);

			return value;

		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return rpopSingle(key);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * List（列表）
	 * 从队列头取出一个元素, 阻塞
	 * @param key
	 * @return timeout (秒) timeout==0 一直阻塞,直到有lpush
	 */
	public String blpop(String key, int timeout) {
		try {
			if (isCluster) {
				List<String> lstStr = jc.blpop(timeout, key);
				if (lstStr == null || lstStr.size() <= 0)
					return null;
				return lstStr.get(1);
			} else {
				return blpopSingle(key, timeout);
			}
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return blpop(key, timeout);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}
	/**
	 * List（列表）
	 * 从队列头取出一个元素, 阻塞
	 * @param key
	 * @return timeout (秒) timeout==0 一直阻塞,直到有lpush
	 */
	private String blpopSingle(String key, int timeout) {
		try {
			String value = null;
			Jedis jedis = getResource();
			List<String> lstStr = jedis.blpop(timeout, key);
			value = lstStr == null ? null : lstStr.get(1);
			returnResource(jedis);
			return value;
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return blpopSingle(key, timeout);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}
	/**
	 * List（列表）
	 * 弹出多个key
	 * @param timeout
	 * @param keys
	 * @return
	 */
	public String blpopMutilKey(int timeout, String... keys) {
		try {
			if (isCluster) {
				List<String> lstStr = null;
				for (String e : keys) {
					long len = llen(e);
					if (len <= 0)
						continue;
					lstStr = jc.blpop(1, e);
					if (lstStr != null)
						break;
				}
				if (lstStr == null || lstStr.size() <= 0) {
					return null;
				}
				return lstStr.get(1);
			} else {
				return blpopSingleMutilKey(timeout, keys);
			}

		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return blpopMutilKey(timeout, keys);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}
	/**
	 * List（列表）
	 * 弹出多个key
	 * @param timeout
	 * @param keys
	 * @return
	 */
	private String blpopSingleMutilKey(int timeout, String... keys) {
		try {
			String value = null;
			Jedis jedis = getResource();
			List<String> lstStr = jedis.blpop(timeout, keys);
			lstStr = null;
			value = lstStr == null ? null : lstStr.get(1);
			returnResource(jedis);
			return value;

		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return blpopSingleMutilKey(timeout, keys);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * 从队尾取出一个元素, 阻塞
	 * 
	 * @param key
	 * @param timeout
	 *            (秒) timeout==0 一直阻塞,直到有lpush
	 * @return
	 */
	public String brpop(String key, int timeout) {
		try {
			if (isCluster) {
				List<String> lstStr = jc.brpop(timeout, key);
				if (lstStr == null || lstStr.size() <= 0)
					return null;
				return lstStr.get(1);
			} else {
				return brpopSingle(key, timeout);
			}

		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return brpop(key, timeout);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}

	private String brpopSingle(String key, int timeout) {
		try {
			String value = null;
			Jedis jedis = getResource();
			List<String> lstStr = jedis.brpop(timeout, key);
			value = lstStr == null ? null : lstStr.get(1);
			returnResource(jedis);

			return value;

		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return brpopSingle(key, timeout);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}

	public String brpopMutilKey(int timeout, String... keys) {
		try {
			if (isCluster) {
				List<String> lstStr = null;
				for (String e : keys) {
					long len = llen(e);
					if (len <= 0)
						continue;
					lstStr = jc.brpop(1, e);
					if (lstStr != null)
						break;
				}
				if (lstStr == null || lstStr.size() <= 0) {
					return null;
				}
				return lstStr.get(1);
			} else {
				return brpopSingleMutilKey(timeout, keys);
			}
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return brpopMutilKey(timeout, keys);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}

	private String brpopSingleMutilKey(int timeout, String... keys) {
		try {
			String value = null;
			Jedis jedis = getResource();
			List<String> lstStr = jedis.brpop(timeout, keys);
			value = lstStr == null ? null : lstStr.get(1);
			returnResource(jedis);

			return value;

		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return brpopSingleMutilKey(timeout, keys);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * 队列长度
	 * 
	 * @param key
	 * @return
	 */
	public long llen(String key) {
		try {
			if (isCluster)
				return jc.llen(key);
			else
				return llenSingle(key);

		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return llen(key);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return 0l;
	}

	private long llenSingle(String key) {

		try {
			long value = 0;
			Jedis jedis = getResource();
			value = jedis.llen(key);
			returnResource(jedis);

			return value;

		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return llenSingle(key);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return 0l;
	}

	/**
	 * 判断一个指定成员是否在队列中
	 * 
	 * @param key
	 * @param member
	 * @return
	 */
	public boolean sismember(String key, String member) {
		try {
			if (isCluster)
				return jc.sismember(key, member);
			else {
				boolean value = false;
				Jedis jedis = getResource();
				value = jedis.sismember(key, member);
				returnResource(jedis);
				return value;
			}
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return sismember(key, member);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return false;
	}

	/**
	 * 获取一个集合中的所有元素
	 * 
	 * @param key
	 * @return
	 */
	public Set<String> smembers(String key) {
		try {
			if (isCluster)
				return jc.smembers(key);
			else
				return smembersSingle(key);
		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return smembers(key);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}

	private Set<String> smembersSingle(String key) {
		try {
			Set<String> value = null;
			Jedis jedis = getResource();
			value = jedis.smembers(key);
			returnResource(jedis);

			return value;

		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return smembersSingle(key);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * 获取某一个哈希中的所有值
	 * 
	 * @param key
	 * @return
	 */
	public List<String> hvals(String key) {
		try {
			if (isCluster)
				return jc.hvals(key);
			else
				return hvalsSingle(key);

		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return hvals(key);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}

	private List<String> hvalsSingle(String key) {
		try {
			List<String> value = null;
			Jedis jedis = getResource();
			value = jedis.hvals(key);
			returnResource(jedis);

			return value;

		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return hvalsSingle(key);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * 获取某一个哈希中的所有键
	 * 
	 * @param key
	 * @return
	 */
	public Set<String> hkeys(String key) {
		try {
			if (isCluster)
				return jc.hkeys(key);
			else
				return hkeysSingle(key);

		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return hkeys(key);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}

	private Set<String> hkeysSingle(String key) {
		try {
			Set<String> value = null;
			Jedis jedis = getResource();
			value = jedis.hkeys(key);
			returnResource(jedis);

			return value;

		} catch (Exception e) {
			Log4jManager.logError(e);
			try {
				Thread.currentThread().sleep(errorWaitTimeout);
				return hkeysSingle(key);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}

}
