package net.jextra.fauxjo.rsdef;

import net.jextra.fauxjo.beandef.FieldDef;

import java.sql.SQLException;
import java.sql.SQLType;

/**
 * Specific to a ResultSet, represents a column at an index and its metadata with the bean fauxjo FieldDef.
 * <p>
 * EXPERIMENTAL: DO NOT USE IN PRODUCTION
 * Retrieving metadata is slow. This object stores the jdbc column info so it can be cached.
 * FieldDefs are shared by many ColDefs that are unique to an RSDef which is unique to a ResultSet (hashcode).
 */
public class ColDef {
	private final String _name;
	private final int _index;
	private final SQLType _sqlType;
	private boolean _isNotNull;
	private boolean _isPrimaryKey;
	private FieldDef _beanFieldDef;

	/**
	 * Caches ResultSet metadata to optimize building beans while iterating a ResultSet.
	 * ~120% speedup measured by KitDev_RSBeanBuilderPOC_Test.
	 * @param name of the ResultSet column lowercase
	 * @param index column number specific to a ResultSet (1:n)
	 * @param sqlType of the column in a ResultSet from its metadata
	 */
	public ColDef(String name, int index, SQLType sqlType) throws SQLException {
		if(name == null || (name = name.trim()).length() < 1) throw new SQLException("col name missing or empty");
		if(sqlType == null) throw new SQLException(name + " col SQLType is null, see SQLType and JDBCType for info");
		_name = name;
		_index = index;
		_sqlType = sqlType;
	}

	public String getName() {
		return _name;
	}

	/** * Return the index of the column this represents from a specific ResultSet. */
	public int getIndex() {
		return _index;
	}

	public SQLType getSqlType() {
		return _sqlType;
	}

	public boolean getIsNotNull() {
		return _isNotNull;
	}

	public void setIsNotNull(Boolean notNull) {
		_isNotNull = notNull;
	}

	public boolean getIsPrimaryKey() {
		return _isPrimaryKey;
	}

	public void setIsPrimaryKey(Boolean isPrimaryKey) {
		_isPrimaryKey = isPrimaryKey;
	}

	/** * Return the fauxjo FieldDef shared by many ColDefs and used to build beans. */
	public FieldDef getBeanFieldDef() {
		return _beanFieldDef;
	}

	public ColDef setBeanFieldDef(FieldDef beanFieldDef) {
		_beanFieldDef = beanFieldDef;
		return this;
	}

	public boolean getIsArray() {
		return _sqlType.getVendorTypeNumber() == java.sql.Types.ARRAY;
	}

}
