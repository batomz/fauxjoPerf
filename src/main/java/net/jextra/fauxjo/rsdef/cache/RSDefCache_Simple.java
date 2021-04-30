package net.jextra.fauxjo.rsdef.cache;

import net.jextra.fauxjo.rsdef.RSDef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * A simple RSDef (ResultSet definition) cache.
 * <p>
 * EXPERIMENTAL: DO NOT USE IN PRODUCTION
 * L1 keys by hashcode avoiding rentrant ResultSet metadata retrieval.
 * L2 keys by a CRC32 of col names, indices and sqlTypes as only those are needed to build beans, not the table or
 * the query or the server URL or anything else.
 * Think of L2 cache the ResultSet performance floor and L1 cache as the ceiling.
 */
public class RSDefCache_Simple {
	private long _rsDefTimeToLive = 12L * 60L * 60L * 1000L;
	private int _rsDefGetsPerPurge = 1000; //cost to rebuild is nominal
	private final LinkedHashMap<Integer, RSDef> _rsDefCacheL1;
	private HashMap<Long, RSDef> _rsDefCacheL2;    //EXPERIMENTAL: CRC32 (colIndex, colName, sqlType),
	private final List<RSDef> _rsDefCacheL1purge; //TODO: purge on a background thread
	private final HashMap<String, String> _rsColNameMap; //Use String references not RS strings to create ColDefs 2X speedup
	private boolean _useL2cache = false;
	private long _rsDefCacheL1missCn = 0L;
	private long _rsDefCacheL1callsSincePurge = 0L;
	private long _rsDefCacheL1callCn = 0L;
	private Long _rsDefCacheL2missCn = 0L;

	/**
	 * If setConfig is not called, the default cache entry time-to-live is 12 hours.
	 * @param useL2cache true to use the experimental L2 cache
	 */
	public RSDefCache_Simple(boolean useL2cache) throws Exception {
		_rsColNameMap = new HashMap<>();
		_rsDefCacheL1 = new LinkedHashMap<>();
		_rsDefCacheL1purge = new ArrayList<>();
		_useL2cache = useL2cache;
		if (useL2cache) {
			_rsDefCacheL2missCn = 0L;
			_rsDefCacheL2 = new HashMap<>();
		}
	}

	/**
	 * Return as builder pattern after configuring the caches which can be changed during runtime.
	 * Affects L1 and if configured, the L2 cache.
	 * rsDefCacheTimeToLive should be sufficiently longer than the datastore ResultSet time-to-live.
	 * <p>
	 * It is the callers responsibility to determine the optimal rsDefCacheTimeToLive through testing and in
	 * consideration that RSDef creation is very fast and occupied memory is extremely small. Eg tens-of-thousands of
	 * cache entires occupy only MB of memory. A trivial and crude method is used to purge old RSDefs which will be an
	 * opportunity for future work. After every rsDefCachePutsPerPurge number of requests, RSDefs older than
	 * rsDefCacheTimeToLive are marked as purgeable and after the next cycle will be removed from the cache and
	 * eligible for garbage collection.
	 * <p>
	 * Note that it is expected this method signature will change in the future.
	 * @param rsDefCacheTimeToLive minimum milliseconds an RSDef is remains active in cache (see discussion above).
	 * @param rsDefCacheGetsPerPurge number getL1 calls before setting RSDefs older than rsDefCacheTimeToLive purge eligible.
	 * @throws Exception if SerDe.REFLECT_LEGACY which does not support caching.
	 */
	public RSDefCache_Simple setConfig(long rsDefCacheTimeToLive, int rsDefCacheGetsPerPurge) throws Exception {
		_rsDefTimeToLive = rsDefCacheTimeToLive;
		_rsDefGetsPerPurge = rsDefCacheGetsPerPurge;
		return this;
	}

	/**
	 * Return the cache entry time to live in milliseconds or throws Exception if SerDe.REFLECT_LEGACY.
	 * @throws Exception if configured SerDe.REFLECT_LEGACY
	 */
	public long getConfig_TimeToLive() throws Exception {
		return _rsDefTimeToLive;
	}

	/**
	 * Return the number of cache puts until a purge is evaluated or throws Exception if SerDe.REFLECT_LEGACY.
	 * @throws Exception if configured SerDe.REFLECT_LEGACY
	 */
	public int getConfig_GetsPerPurge() throws Exception {
		return _rsDefGetsPerPurge;
	}

	//----------------+
	//   Accessors    |
	//----------------+

	public void putL1(Integer rsHashCode, RSDef rsDef) {
		_rsDefCacheL1.put(rsHashCode, rsDef);
	}
	public RSDef getL1(Integer rsHashCode) {
		_rsDefCacheL1callsSincePurge++;
		_rsDefCacheL1callCn++;
		RSDef rd = _rsDefCacheL1.get(rsHashCode);
		if(rd == null) _rsDefCacheL1missCn++;
		return rd;
	}

	/** * EXPERIMENTAL: CRC32 (colIndex, colName, sqlType) */
	public void putL2(Long crc32, RSDef rsDef) {
		_rsDefCacheL2.put(crc32, rsDef);
	}

	/** * Return RSDef by EXPERIMENTAL: CRC32 (colIndex, colName, sqlType) */
	public RSDef getL2(Long crc32) throws Exception {
		if(!_useL2cache) throw new Exception("L2 is not enabled");
		RSDef rd = _rsDefCacheL2.get(crc32);
		if(rd == null) _rsDefCacheL2missCn++;
		return rd;
	}
	/** * Return true if this was created to use an L2 cache. */
	public boolean getL2_isEnabled() {
		return _useL2cache;
	}

	/** * Reusing colname String references to create ColDefs offers a 2X speedup. */
	public void putRSColName(String colname) {
		_rsColNameMap.put(colname, colname);
	}

	/** * Return cached String reference instead of using a new String object from re-entrant ResultSet call. */
	public String getRSColName(String rsColname) {
		return _rsColNameMap.get(rsColname);
	}

	/** * Return true if rsColname is cached. */
	public boolean getRSColNameIsCached(String rsColname) {
		return _rsColNameMap.containsKey(rsColname);
	}

	/**
	 * If cache size exceeds rsDefCachePutsPerPurge entries, marks RSDefs older than rsDefCacheTimeToLive eligible to
	 * be purged on the next call. Since rsDefCacheL1 is in chronological order, only iterates until the first RSDef
	 * that is not expired is found. Even though removed from the cache, dangling references will not be gc'd until
	 * they are no longer being used so it this operation should not create system instability.
	 */
	public void reqPurge() {
		if(_rsDefCacheL1callsSincePurge++ > _rsDefGetsPerPurge) {
			_rsDefCacheL1callsSincePurge = 0;
			for(RSDef oldRSDef : _rsDefCacheL1purge) {
				_rsDefCacheL1.remove(oldRSDef.getRSHashcode());
			}
			_rsDefCacheL1purge.clear();
			for (RSDef r : _rsDefCacheL1.values()) { //break upon first non-expired RSDef
				//_log.warn("    rsHashcode: " + r.getRSHashcode() + ", age: " + r.getCreatedOnTimestamp());
				if(r.isOlderThan(_rsDefTimeToLive)) {
					_rsDefCacheL1purge.add(r);
					continue;
				}
				break;
			}
		}
	}

	//---------------------------------------------+
	//           L1 Monitoring/TODO JMX            |
	//---------------------------------------------+
	/** * Return total number of entries in cache including active and awaiting purge for monitoring. */
	public int getRSDefCacheL1_size() {
		return _rsDefCacheL1.size();
	}
	/** * Return total number of calls to getL1. */
	public long getRSDefCacheL1_callCn() { return _rsDefCacheL1callCn; }

	/** * Return count of entries in cache that are active. */
	public int getRSDefCacheL1_cnActive() {
		return getRSDefCacheL1_size() - getRSDefCacheL1_cnPurgeable();
	}
	/** * Return count of entries in cache that are eligible for and awating purge. */
	public int getRSDefCacheL1_cnPurgeable() {
		return _rsDefCacheL1purge.size();
	}
	/** * Return number of L1 cache misses (getL1 returned null). */
	public long getRSDefCacheL1_missCn() {
		return _rsDefCacheL1missCn;
	}

	//---------------------------------------------+
	//           L2 Monitoring/TODO JMX            |
	//---------------------------------------------+
	/** * Return 0 if getRSDefCacheInUse returns false else number of entries in L2 cache. */
	public int getRSDefCacheL2_size() throws Exception {
		return _rsDefCacheL2.size();
	}
	/** * Return number of L2 cache misses if enabled (getL2 returned null) else null. */
	public Long getRSDefCacheL2_missCn() {
		return _rsDefCacheL2missCn;
	}

	@Override
	public String toString() {
		return "RSDefCache_Simple {" +
			" conf { rsDefTimeToLive=" + _rsDefTimeToLive +
			", rsDefGetsPerPurge=" + _rsDefGetsPerPurge +
			" }, rsColNameMap { size=" + _rsColNameMap.size() +
			" }, L1 { size=" + _rsDefCacheL1.size() +
			", active=" + getRSDefCacheL1_cnActive() +
			", missCn=" + _rsDefCacheL1missCn +
			", callCn=" + _rsDefCacheL1callCn +
			" }, rsDefCacheL2 { " +
			"useL2=" + _useL2cache +
			(!_useL2cache ? "" : ", size=" + _rsDefCacheL2.size() + ", L2MissCn=" + _rsDefCacheL2missCn + "") +
			" } }";
	}

}
