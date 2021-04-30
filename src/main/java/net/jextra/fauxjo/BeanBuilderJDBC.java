package net.jextra.fauxjo;

import net.jextra.fauxjo.beandef.BeanDef;
import net.jextra.fauxjo.beandef.BeanDefCache;
import net.jextra.fauxjo.beandef.FieldDef;
import net.jextra.fauxjo.coercer.Coercer;
import net.jextra.fauxjo.rsdef.ColDef;
import net.jextra.fauxjo.rsdef.RSCrc32;
import net.jextra.fauxjo.rsdef.RSDef;
import net.jextra.fauxjo.rsdef.cache.RSDefCache_Simple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.*;

/**
 * A database deserialization test that caches ResultSet metadata.
 * <p>
 * EXPERIMENTAL: DO NOT USE IN PRODUCTION
 * RSDefCache_Simple features two types of RSDef caches:
 * <ul>
 *   <li>L1 cache: caches RSDef by hashcode for large ResultSets (more rows/cols = more benefit).</li>
 *   <li>L2 cache: caches by CRC32 of ResultSetMetaData colnames, indexes and SQLTypes for small ResultSets.</li>
 * </ul>
 * Note: An RSDef has no notion of the URL, schema or table of its data because that contract is defined by
 * the binding of the bean type to the fauxjo BeanBuilder.
 * <p>
 * @param <T> is a Fauxjo (a java bean) that this builds from jdbc ResultSet data.
 * @see RSDefCache_Simple
 */
public class BeanBuilderJDBC<T> extends BeanBuilder<T> { //implements BeanBuilderSerDeType
	public static Logger _log = LoggerFactory.getLogger(BeanBuilderJDBC.class);
	private final Class<T> _beanClass;
	private Constructor<T> _beanCtor; //avoid reflection lkup, this is known at ctor
	private BeanDef _beanDef;
	private Coercer _coercer;

	// Key = Lowercase column name (in code known as the "key").
	// Value = Information about the bean property.
	private RSDefCache_Simple _cache;
	private SerDe _serDeType = SerDe.REFLECT_LEGACY; //default
	private boolean _colsInRSnotNotBean_WarnSySLog;

	/** * Type of deserialization to perform. Also a placeholder for serialization. */
	public enum SerDe {
		REFLECT_LEGACY,
		REFLECT_OPTIMIZED
	}

	/** * Configures this as a LegacyBeanBuilder. */
  public BeanBuilderJDBC(Class<T> beanClass, boolean autoCloseResultSet) {
    super(beanClass, autoCloseResultSet);
		_beanClass = beanClass; //take a copy enabling getter
  }

	/** * Configures this as a LegacyBeanBuilder. */
  public BeanBuilderJDBC(Class<T> beanClass) {
    this(beanClass, false);
  }

	/**
	 * The main artifacts needed to do JDBC-Binding are:
	 * <ul>
	 *   <li>BeanDef (already in fauxjo): The bean, constructor, fields and methods</li>
	 *   <li>RSDef (not already in fauxjo): jdbc ResultSet, its columns, their indexes and sqlType.</li>
	 * </ul>
	 * Information about the bean is known at creation by fauxjo. Everything cacheable via reflection is cached
	 * including bean class, zero-args constructor and fields. When buildBean is called again (rentrant such as
	 * for the next row), the cached RSDef for its ResultSet can be used to get values by col index instead of name.
	 * An RSDef contains ColDefs unique per column index in a ResultSet but share FieldDefs.
	 * <p>
	 * NOTE: caller should setRSDefCacheConfig so resultSetDefCacheTimeToLive is sufficiently longer than the
	 * datastore ResultSet time-to-live. If useRSDefCacheL2 is true, a CRC32 checksum will be created for each ResultSet
	 * and if an RSDef exists in L2 cache with the same checksum (but from a previous ResultSet), a new RSDef will be
	 * created referencing its ColDefs and cached in L1 with the new ResultSet hashcode. Having resultSetDefCacheTimeToLive
	 * configured too low will result in memory thrash and performance will degrade.
	 *
	 * @param beanClass to deserialize
	 * @param serDeType deserialization serDeType this builder will use to build beans.
	 * @param cache if true, RSDefs are also cached by CRC32 checksum resulting in up to a 2X speedup.
	 * @see RSDefCache_Simple
	 */
	public BeanBuilderJDBC(Class<T> beanClass, SerDe serDeType, RSDefCache_Simple cache) throws Exception {
		super(beanClass);
		_beanClass = beanClass;
		_cache = cache;
		try { //The bean cannot change so cache its beanDef and reflection ctor
			_beanDef = BeanDefCache.getBeanDef(beanClass);
			_beanCtor = beanClass.getDeclaredConstructor();
			String fieldName = null;
			for(FieldDef fd: _beanDef.getFieldDefs().values()) {
				if(fd.getField() != null) {
					fd.getField().setAccessible(true);
					fieldName = fd.getField().getName().toLowerCase();
					_cache.putRSColName(fieldName); //2X speedup, store a reference to a String rather than ResultSet String
					_log.trace("    caching fieldName: " + fieldName);
				}
			} //make all bean fields accessible at startup instead repeating during runtime
			//rsDefCacheConfig = new AtomicBoolean();
			//if(rsDefCacheConfig.compareAndSet(false, true)) {
				if(serDeType != null) {
					_serDeType = serDeType;
				} else {
					_serDeType = SerDe.REFLECT_LEGACY;
				}
				_log.trace("BeanBuilderPOC initSerDeType: " + _serDeType);
			//}
		} catch (Exception x) {
			throw new Exception("Call failed: beanClass.getDeclaredConstructor(): " + beanClass.getSimpleName(), x);
		}
		_coercer = new Coercer();
	}


//		if(serDeType == SerDe.REFLECT_LEGACY) throw new Exception("RSDef cannot be configured for " + serDeType.name());

	/** * Return the bean class bound to this builder. */
	public Class<T> getBeanClass() {
		return _beanClass;
	}

	/** * Return the constructor from the bean bound to this builder. */
	public Constructor<T> getBeanCtor() {
		return _beanCtor;
	}

	/** * Return the SerDe for this BeanBuilder */
	public SerDe getSerDeType() {
		return _serDeType;
	}

	/** * Return the name of the SerDe. */
	public String getSerDeName() {
		return getSerDeType().name();
	}

	/** * Return !super.getAllowMissingFields() */
	public boolean getColsInBeanNotRS_throwException() {
		return !super.getAllowMissingFields();
	}

	/** * Marker method calls setAllowMissingColumns */
	public void setColsInBeanNotRS_throwException(boolean colsInBeanNotRS_throwException) {
		super.setAllowMissingFields(!colsInBeanNotRS_throwException);
	}

	/**
	 * Return whether caller wants to warn of ResultSet cols not in bean.
	 * @throws Exception if configured SerDe.REFLECT_LEGACY
	 */
	public boolean getColsInRSnotNotBean_WarnSySLog() throws Exception {
		if(_serDeType == SerDe.REFLECT_LEGACY) throw new Exception("ColsInRSnotNotBean cannot be configured for " + _serDeType.name());
		return _colsInRSnotNotBean_WarnSySLog;
	}

	/**
	 * Set true to warn of ResultSet cols not in bean.
	 * @throws Exception if configured SerDe.REFLECT_LEGACY
	 */
	public void setColsInRSnotNotBean_WarnSySLog(boolean colsInRSnotNotBean_WarnSySLog) throws Exception {
		if(_serDeType == SerDe.REFLECT_LEGACY) throw new Exception("ColsInRSnotNotBean cannot be configured for " + _serDeType.name());
		_colsInRSnotNotBean_WarnSySLog = colsInRSnotNotBean_WarnSySLog;
	}

	/**
	 * Return beans containing data from the ResultSet using specified SerDe deserialization.
	 * @param beans collection to populate from rs
	 * @param rs from which a bean will be deserialized for its current row.
	 * @param numRows number of beans desired
	 */
	@Override
	public void buildBeans( Collection<T> beans, ResultSet rs, int numRows ) throws SQLException {
		int counter = 0;
		while (rs.next() && (numRows < 0 || counter < numRows)) {
			beans.add(buildBean(rs));
			counter++;
		}
	}

	/**
	 * Return a bean containing data from the current ResultSet row using specified SerDe deserialization.
	 * @param rs from which a bean will be deserialized for its current row.
	 */
	@Override
	public T buildBean(ResultSet rs) throws FauxjoException { //SQLException {
		if (_serDeType == SerDe.REFLECT_OPTIMIZED) { //test
			return buildBean_REFLECT_OPTIMIZED(rs);
		} else{
			return super.buildBean(rs); //legacy
		}
	}

	//---------------------------------------------------------------------------------+
	//  REFLECTION_OPTIMIZED - proposed balance between performance and compatibility  |
	//---------------------------------------------------------------------------------+

	/**
	 * Return a bean deserialized with data from the current ResultSet row.
	 * @param rs from which a bean will be deserialized for its current row.
	 */
	private T buildBean_REFLECT_OPTIMIZED(ResultSet rs) throws FauxjoException {
		RSDef rsDef = null;
		T bean;
		try {
			rsDef = getRSDef_REFLECT_OPTIMIZED(rs); //ensured the RSDef is created and in the cache
			bean = _beanCtor.newInstance();
			deserializeRowToBean_REFLECT_OPTIMIZED(rs, rsDef, rs.getRow(), bean);
		} catch (Exception x) {
			throw new FauxjoException("buildBean_REFLECTION_DIRECT failed", x);
		}
		return bean;
	}

	/**
	 * Return a bean deserialized with data from the current ResultSet row.
	 * ColInRSnotInBean and colInBeanNotInRS checking is done for the ResultSet improving this method.
	 * @param rs ResultSet whose current row will be deserialized into a bean
	 * @param rsDef created by createRSDef_REFLECT_OPTIMIZED
	 * @param rowIndex for reporting
	 * @param dstBean to populate
	 */
	private T deserializeRowToBean_REFLECT_OPTIMIZED(ResultSet rs, RSDef rsDef, Object rowIndex, T dstBean) throws SQLException {
		ColDef col = null; //for exception reporting
		try {
			for(ColDef c : rsDef.getColDefs()) {
				col = c;
				FieldDef fieldDef = col.getBeanFieldDef();
				Object value = null;
				if (col.getIsArray()) {
					Array array = rs.getArray(col.getIndex());
					if (array != null) {
						value = array.getArray();
					}
				} else if ((value = rs.getObject(col.getIndex())) != null) {
					try {
						Class<?> targetClass = fieldDef.getValueClass();
						value = _coercer.convertTo(value, targetClass);
					} catch (FauxjoException ex) {
						throw new FauxjoException("Failed to coerce " + col, ex);
					}
				}
				if(value == null) continue;
				Field field = col.getBeanFieldDef().getField(); //use FauxCol instead of using BeanDef
				if (field != null) {
					try {
						field.set(dstBean, value);
					} catch (Exception ex) {
						throw new FauxjoException("Unable to write to field [" + field.getName() + "]", ex);
					}
				} else { //field and method are mutually exclusive in legacy code
					Method writeMethod = col.getBeanFieldDef().getWriteMethod();
					if (writeMethod != null) {
						try {
							writeMethod.invoke(dstBean, value);
						} catch (Exception ex) {
							throw new FauxjoException("Unable to invoke write method [" + writeMethod.getName() + "]", ex);
						}
					}
				}
			}
		} catch (Exception x) {
			throw new FauxjoException("Deserialize[col][row] failed: " + (col != null ? "col: " + col : "unknown col") + "[" + rowIndex + "]", x);
		}
		return dstBean;
	}

	/**
	 * Return RSDef with ColDefs created by createColDef_REFLECT_OPTIMIZED from the ResultSet and matching BeanDef.
	 * @param rs ResultSet used to create the RSDef
	 * @see RSDef
	 */
	private RSDef getRSDef_REFLECT_OPTIMIZED(ResultSet rs) throws SQLException {
		RSDef resSetDef = _cache.getL1(rs.hashCode());
		if (resSetDef != null) {
			//_log.trace("  < L1"); //found by hashcode in L1s
			return resSetDef;
		}
		long crc32 = 0L;
		try {
			ResultSetMetaData meta = rs.getMetaData();

			if(_cache.getL2_isEnabled()) { //L2 (EXPERIMENTAL) early out 2X improvement for short-lived ResultSets
				crc32 = RSCrc32.getCRC(meta);
				if((resSetDef = _cache.getL2(crc32)) != null) {
					resSetDef = new RSDef(rs.hashCode(), resSetDef);
					_cache.reqPurge();
					_cache.putL1(rs.hashCode(), resSetDef);
					//_log.trace("  < L2"); //found by CRC32(colnames+indexes) in L2
					return resSetDef;
				}
			} //L2 early out -----------------------------------
			int columnCount = meta.getColumnCount();
			long rsDefBuildStart = System.currentTimeMillis();
			List<ColDef> resSetDefList = new ArrayList<>();
			List<String> colsInRSnotNotBeanWarn = (getColsInRSnotNotBean_WarnSySLog() ? new ArrayList<>() : null);
			String rsColNm = null;

			for (int i = 1; i <= columnCount; i++) { //map every column possible
				rsColNm = meta.getColumnName(i).toLowerCase();
				JDBCType sqlType = JDBCType.valueOf(meta.getColumnType(i));
				//EXPERIMENT AVOIDING A NEW STRING FOR THE COLNAME
				ColDef colDef = null;
				if(_cache.getRSColNameIsCached(rsColNm)) {
					colDef = createColDef_REFLECT_OPTIMIZED(_cache.getRSColName(rsColNm), i, sqlType);
				} else {
					_cache.putRSColName(rsColNm);
					colDef = createColDef_REFLECT_OPTIMIZED(rsColNm, i, sqlType);
				}
				if (colDef != null) {
					resSetDefList.add(colDef);
				} else if (colsInRSnotNotBeanWarn != null) {
					colsInRSnotNotBeanWarn.add(meta.getColumnName(i).toLowerCase());
				}
			}
			resSetDef = new RSDef(rs.hashCode(), resSetDefList);
			//report colsInBeanNotRS and colsInRSnotNotBean
			if (!getColsInBeanNotRS_throwException()) {
				List<String> colsInBeanNotRSexception = new ArrayList<>(); //unmapped fields (missing in RS) this is equivalent to allowMissingColumns
				for(String fieldname: _beanDef.getFieldDefs().keySet()) {
					if (!resSetDef.containsCol(fieldname)) {
						colsInBeanNotRSexception.add(fieldname);
					}
				}
				if (colsInRSnotNotBeanWarn != null && colsInRSnotNotBeanWarn.size() > 0) {
					_log.warn("WARNING: colsInRSnotNotBean={" + colsInRSnotNotBeanWarn + "}");
				} else if (colsInBeanNotRSexception.size() > 0) {
					throw new FauxjoException("colsInBeanNotRS={" + colsInBeanNotRSexception);
				}
			}
			//cache
			_cache.reqPurge();
			_cache.putL1(rs.hashCode(), resSetDef);
			if(_cache.getL2_isEnabled()) _cache.putL2(crc32, resSetDef);

		} catch (Exception ex) {
			throw new FauxjoException("getFauxCols failed", ex);
		}
		//_log.warn("Performance hit, caching REFLECT_OPTIMIZED ResultSet metadata in millis: " + (System.currentTimeMillis() - rsDefBuildStart));
		return resSetDef;
	}

	/**
	 * Return a new ColDef if beanDef contains the same field else null.
	 * @param colName of the ResultSet column lowercase
	 * @param colIndex column number in the ResultSet (1:n)
	 * @param sqlType of the column in a ResultSet from its metadata
	 */
	private ColDef createColDef_REFLECT_OPTIMIZED(String colName, int colIndex, SQLType sqlType) throws SQLException {
		FieldDef fieldDef = _beanDef.getFieldDef(colName);
		if(fieldDef == null || fieldDef.getField() == null) return null; //resultSet column with colName is not a field in the bean
		ColDef rsCol = new ColDef(colName, colIndex, sqlType).setBeanFieldDef(fieldDef);
		return rsCol;
	}





}
