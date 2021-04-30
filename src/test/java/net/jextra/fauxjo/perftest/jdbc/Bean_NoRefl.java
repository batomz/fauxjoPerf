package net.jextra.fauxjo.perftest.jdbc;

import java.sql.ResultSet;
import java.util.LinkedHashMap;

/**
 * Thin interface with methods needed to deserialize without reflection. Beans that implement will provide their
 * own deserialization (static binding between ResultSet and their setters.
 * Created only for unit testing best-case performance baseline.
 */
public interface Bean_NoRefl {
	public LinkedHashMap<String, BeanField_NoRefl> getNoReflectCols(LinkedHashMap<String, BeanField_NoRefl> fauxColMap) throws Exception;
	public void deserialize(ResultSet rs, LinkedHashMap<String, BeanField_NoRefl> fauxColsWithColIndexes, Object rowIndex) throws Exception;
}
