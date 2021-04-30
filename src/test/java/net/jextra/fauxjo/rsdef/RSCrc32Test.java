package net.jextra.fauxjo.rsdef;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;

public class RSCrc32Test {
	public RSCrc32Test() { }

	/**
	 * Quick test to show that CRC (cyclic redundancy check) differs if the order of RSDef col, names or indexes changes.
	 * RSCrc32 should not be susceptible to namespace collisions if the input entropy is very low like with RSDefs
	 * which will see a subset or all of the same colnames but in different order.
	 * Note that CRC32 impl may differ by environment (JVM/container/VM/OS/cpu) so RSCrc32.getHash result may differ.
	 */
	@Test
	public void demonstrateCRCorderAltersHash() {
		Logger _log = LoggerFactory.getLogger(RSCrc32Test.class);

		long rs0 = 0L;
		LinkedHashMap<String, Integer> cols0 = new LinkedHashMap<>();
		cols0.put("id", 1);
		cols0.put("name", 2);
		cols0.put("weight", 3);
		cols0.put("age", 4);
		try {
			rs0 = RSCrc32.getCRC(cols0);
			_log.debug(Long.toHexString(rs0));
		} catch (Exception x) {
			_log.error("main failed", x);
		}
		//Assert.assertEquals("2140da0d", rs0);

		//transpose weight and name fields
		long rs1 = 0L;
		LinkedHashMap<String, Integer> cols1 = new LinkedHashMap<>();
		cols1.put("id", 1);
		cols1.put("weight", 2);
		cols1.put("name", 3);
		cols1.put("age", 4);
		try {
			rs1 = RSCrc32.getCRC(cols1);
			_log.debug(Long.toHexString(rs1));
		} catch (Exception x) {
			_log.error("main failed", x);
		}
		Assert.assertNotEquals(rs0, rs1);

		//transpose indexes
		long rs2 = 0L;
		LinkedHashMap<String, Integer> cols2 = new LinkedHashMap<>();
		cols2.put("id", 2);
		cols2.put("name", 1);
		cols2.put("weight", 3);
		cols2.put("age", 4);
		try {
			rs2 = RSCrc32.getCRC(cols2);
			_log.debug(Long.toHexString(rs2));
		} catch (Exception x) {
			_log.error("main failed", x);
		}
		Assert.assertNotEquals(rs2, rs0);
		Assert.assertNotEquals(rs2, rs1);

	}
}