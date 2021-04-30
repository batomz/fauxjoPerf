package net.jextra.fauxjo.perftest.jdbc;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.SQLType;

public class BeanField_NoRefl {
	private final String _name;
	private SQLType _dType;
	private Integer _index;
	private Boolean _notNull;
	private Boolean _primKey;
	private final boolean _dIsArray;
	private Class<?> _jClazz;
	private Field _jField;
	private Method _jWrMethd;

	/**
	 * Represents a field of a ResultSetMetaData or NoReflect_Bean and used to exchange values between them without reflection.
	 * <p>
	 * EXPERIMENTAL: DO NOT USE IN PRODUCTION
	 * Anything that can be mapped at startup will be to reduce runtime costs.
	 * Used only as a baseline to compare dynamic bean binding performance which is typically ~3X slower.
	 * @param colName from either a ResultSet or bean field.
	 * @param colIsArray - true if the value of the col that backs this is an array.
	 */
	public BeanField_NoRefl(String colName, boolean colIsArray) {
		_name = colName;
		_dIsArray = colIsArray;
	}

	public BeanField_NoRefl(String colName, boolean colIsArray, SQLType dType, Class<?> jClazz) {
		_name = colName.toLowerCase();
		_dIsArray = colIsArray;
		_dType = dType;
		_jClazz = jClazz;
	}

	public String getName() {
		return _name;
	}
	public boolean getIsArray() {
		return _dIsArray;
	}


	public BeanField_NoRefl setIndex(Integer index) {
		_index = index;
		return this;
	}
	public Integer getIndex() {
		return _index;
	}

	public Class<?> getJClazz() {
		return _jClazz;
	}
	public BeanField_NoRefl setJClazz(Class<?> jClzz) {
		_jClazz = jClzz;
		return this;
	}

	public Boolean getNotNull() {
		return _notNull;
	}

	public BeanField_NoRefl setNotNull(Boolean notNull) {
		_notNull = notNull;
		return this;
	}

	public Boolean getPrimKey() {
		return _primKey;
	}

	public BeanField_NoRefl setPrimKey(Boolean primKey) {
		_primKey = primKey;
		return this;
	}

	public Method getJWrMethd() {
		return _jWrMethd;
	}

	public BeanField_NoRefl setJWrMethd(Method jWrMethd) {
		_jWrMethd = jWrMethd;
		return this;
	}

	public SQLType getDType() {
		return _dType;
	}

	public BeanField_NoRefl setDType(SQLType dType) {
		_dType = dType;
		return this;
	}

	public Field getJField() {
		return _jField;
	}

	public BeanField_NoRefl setJField(Field jFfield) {
		_jField = jFfield;
		return this;
	}

	@Override
	public String toString() {
		return "NoRefl_BeanField{" + _name + "=" + _index + "}";
	}
}
