package net.jextra.fauxjo.perftest.testbeans.lite;

import net.jextra.fauxjo.perftest.jdbc.BeanField_NoRefl;
import net.jextra.fauxjo.perftest.jdbc.Bean_NoRefl;

import java.math.BigDecimal;
import java.sql.JDBCType;
import java.sql.ResultSet;
import java.util.LinkedHashMap;

/**
 * Avoids reflection for a ~3X speedup over legacy fauxjo.
 * ONLY TO FOR PERFORMANCE BENCHMARK, NOT INTENDED AS A SOLUTION.
 * Lite processing required due to few fields should result in smaller performance improvements than HeavyBean.
 */
public class LiteBeanNoRefl extends LiteBean implements Bean_NoRefl, java.io.Serializable {
	static final long serialVersionUID = 42L;

	//Hypothesis that reflection cost is in lkup, mapping the field may perform the same as setters and allow coercers?
	public static final BeanField_NoRefl FC_id     = new BeanField_NoRefl("id",     false).setDType(JDBCType.INTEGER).setJClazz(Integer.class);
	public static final BeanField_NoRefl FC_name   = new BeanField_NoRefl("name",   false).setDType(JDBCType.LONGVARCHAR).setJClazz(String.class);
	public static final BeanField_NoRefl FC_weight = new BeanField_NoRefl("weight", false).setDType(JDBCType.NUMERIC).setJClazz(BigDecimal.class);
	public static final BeanField_NoRefl FC_age    = new BeanField_NoRefl("age",    false).setDType(JDBCType.INTEGER).setJClazz(Integer.class);

	/**
	 * Return a map of FauxCol by name so caller can set the colIndex from ResultSetMetaData.
	 * @param dstColDefMap will have cols added to it, typically will be a new LinkedHashMap.
	 */
	public LinkedHashMap<String, BeanField_NoRefl> getNoReflectCols(LinkedHashMap<String, BeanField_NoRefl> dstColDefMap) throws Exception {
		dstColDefMap.put(FC_id.getName(), FC_id);
		dstColDefMap.put(FC_name.getName(), FC_name);
		dstColDefMap.put(FC_weight.getName(), FC_weight);
		dstColDefMap.put(FC_age.getName(), FC_age);
		return dstColDefMap;
	}

	/**
	 * Determine if direct setters are ~3X faster by removing reflection and an intermediate hashmap.s
	 * If feasible, can look at codegen+JIT based on annotations for non-breaking changes.
	 * @param rs from db
	 * @param rowIndex optional, used for exception reporting
	 */
	public void deserialize(ResultSet rs, LinkedHashMap<String, BeanField_NoRefl> dstColDefMapWithRSindexes, Object rowIndex) throws Exception {
		int colIndex = 0;
		BeanField_NoRefl col = null;
		try {
			for(BeanField_NoRefl c: dstColDefMapWithRSindexes.values()) {
				col = c;
				colIndex = c.getIndex();  //does not use reflection
				if (col == FC_id)     {
					setId(rs.getInt(colIndex));
			  } else if (col == FC_name) {
					setName(rs.getString(colIndex));
			  } else if (col == FC_weight) {
					setWeight(rs.getBigDecimal(colIndex));
			 } else if (col == FC_age) {
					setAge(rs.getInt(colIndex));
			 }
			}
		} catch (Exception x) {
			throw new Exception("Deserialize[col][row] failed: " + (col != null ? "col: " + col: "") + "[" + rowIndex + "]", x);
		}
	}

}


