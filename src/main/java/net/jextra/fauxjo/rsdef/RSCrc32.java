package net.jextra.fauxjo.rsdef;

import net.jextra.fauxjo.rsdef.cache.RSDefCache_Simple;
import java.io.ByteArrayOutputStream;
import java.sql.ResultSetMetaData;
import java.util.LinkedHashMap;
//import java.util.zip.CRC32C; //since jdk9, Intel optimized to 3 ops
import java.util.zip.CRC32; //less efficient but does not adversely impact performance

public class RSCrc32 {

	/**
	 * Creates a CRC from ResulSetMetaData to use as the key for experimental L2 cache.
	 * <p>
   * EXPERIMENTAL: DO NOT USE IN PRODUCTION
	 * The CRC is created from the ResultSet metadata colnames, indexes and SQLTypes but not its other
	 * fields like URL or tablename. Since this cache is effectively bound to a bean, one or more of the same
	 * column definitions are expected to be seen repetitively minimizing possibility of CRC namespace collisions.
	 * Note that CRC32 impl may differ by environment (JVM/container/VM/OS/cpu) so RSCrc32.getHash result may differ.
	 * @param meta from a new ResultSet that is a cache-miss (not in L1 cache by its hashcode).
	 * @see RSDefCache_Simple
	 */
	public static long getCRC(ResultSetMetaData meta) throws Exception {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		for(int colIndex=1;colIndex<meta.getColumnCount();colIndex++) {
			if(meta.getColumnName(colIndex) == null) continue;
			bos.write(colIndex);
			bos.write(meta.getColumnName(colIndex).getBytes()); //ignore case (same query will return same RSDef)
			bos.write(meta.getColumnType(colIndex));
		}
		bos.flush();
		bos.close();
		//CRC32C crc32C = new CRC32C();
		CRC32 crc32C = new CRC32();
		crc32C.update(bos.toByteArray());
		return crc32C.getValue();
	}

	/**
	 * Creates a CRC from ResultSetMetaData to use as the key for experimental L2 cache.
	 * <p>
	 * The CRC is created from the ResultSet metadata colnames and indexes but not its other
	 * fields like URL or tablename. Since this cache is effectively bound to a bean, one or more of the same
	 * column definitions are expected to be seen repetitively minimizing possibility of CRC namespace collisions.
	 * Note that CRC32 impl may differ by environment (JVM/container/VM/OS/cpu) so RSCrc32.getHash result may differ.
	 * @param cols from a new ResultSet
	 * @see RSDefCache_Simple
	 */
	public static long getCRC(LinkedHashMap<String, Integer> cols) throws Exception {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		for(String key: cols.keySet()) {
			if(key == null || cols.get(key) == null) continue;
			bos.write(cols.get(key));
			bos.write(key.getBytes()); //ignore case (same query will return same RSDef)
		}
		bos.flush();
		bos.close();
		//CRC32C crc32C = new CRC32C();
		CRC32 crc32C = new CRC32();
		crc32C.update(bos.toByteArray());
		return crc32C.getValue();
	}

}
