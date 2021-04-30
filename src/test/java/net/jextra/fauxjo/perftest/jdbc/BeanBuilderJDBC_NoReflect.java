package net.jextra.fauxjo.perftest.jdbc;

import net.jextra.fauxjo.BeanBuilder;
import net.jextra.fauxjo.BeanBuilderJDBC;
import net.jextra.fauxjo.FauxjoException;
import java.lang.reflect.Constructor;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * No Reflection for field mapping however it is used to create a bean per row.
 * <p>
 * EXPERIMENTAL: DO NOT USE IN PRODUCTION
 * Best-case scenario for performance baseline.
 * ONLY FOR TESTING PERFORMANCE BASELINE - NOT INTENDED FOR ACTIVE USE
 * @see BeanBuilderJDBC
 */
public class BeanBuilderJDBC_NoReflect<T extends Bean_NoRefl> extends BeanBuilder<T> {
	private final Constructor<T> _beanCtor;
	public enum SerDeType {
		NOREFLECT_STATIC
	}

	private final AtomicBoolean fauxColsAreCached;
	private LinkedHashMap<String, BeanField_NoRefl> fauxColCache;
	//------------ No Reflection, best-case performance ----------------------

	public BeanBuilderJDBC_NoReflect(Class<T> beanClass) throws Exception {
		super(beanClass);
		fauxColsAreCached = new AtomicBoolean();
		_beanCtor = beanClass.getDeclaredConstructor();
	}

	/** * Return getAllowMissingColumns */
	public boolean getColsInBeanNotRS_throwException() {
		//return !super.getAllowMissingColumns(); //fauxjo 11.3.0-3
		return !super.getAllowMissingFields(); //fauxjo 11.3.0-4
	}

	/** * Return the name of the SerDe. */
	public String getSerDeName() {
		return SerDeType.NOREFLECT_STATIC.name();
	}

	/**
	 * Return buildBean_FAUXCOL_NOREFLECT.
	 * @param rs from which a bean will be deserialized for its current row.
	 */
	@Override
	public T buildBean(ResultSet rs) throws FauxjoException { //SQLException {
		return buildBean_FAUXCOL_NOREFLECT(rs);
	}

	//------------------------------------------------------------------------+
	//  FAUXCOL_NOREFLECT - Not proposing, ONLY to show best case performance |
	//------------------------------------------------------------------------+
	/**
	 * Builds a bean without reflection from rs ONLY as a best case performance baseline.
	 * @param rs to deserialize into beans
	 */
	private T buildBean_FAUXCOL_NOREFLECT(ResultSet rs) throws FauxjoException {
		T fjo;
		try {
			fjo = _beanCtor.newInstance();
			if (fauxColsAreCached.compareAndSet(false, true)) {
				fauxColCache = getFauxCols_FAUXCOL_NOREFLECT(rs, fjo);
			}
			fjo.deserialize(rs, fauxColCache, rs.getRow());
		} catch (Exception ex) {
			throw (ex instanceof FauxjoException ? (FauxjoException) ex : new FauxjoException(ex));
		}
		//return (T) fjo;
		return fjo;
	}

	/**
	 * Return a FauxCol map from the intersection of rs and fjo.
	 * @param rs used for metadata
	 * @param fjo a bean implementing FaxujoNoReflect
	 */
	private LinkedHashMap<String, BeanField_NoRefl> getFauxCols_FAUXCOL_NOREFLECT(ResultSet rs, Bean_NoRefl fjo) throws SQLException {
		LinkedHashMap<String, BeanField_NoRefl> fauxCols;
		try {
			List<String> colsInRSnotNotBean = new ArrayList<>();
			List<BeanField_NoRefl> colsInBeanNotRS = new ArrayList<>();
			ResultSetMetaData meta = rs.getMetaData();
			int columnCount = meta.getColumnCount();
			fauxCols = fjo.getNoReflectCols(new LinkedHashMap<>());
			colsInBeanNotRS.addAll(fauxCols.values());
			BeanField_NoRefl col;
			for (int i = 1; i <= columnCount; i++) {
				col = fauxCols.get(meta.getColumnName(i).toLowerCase());
				if(col != null) {
					col.setIndex(i);
					colsInBeanNotRS.remove(col);
				} else {
					if(getColsInBeanNotRS_throwException()) {
						throw new SQLException("ResultSet col: " + meta.getColumnName(i).toLowerCase() + " not in bean: " + fauxCols);
					} else {
						colsInRSnotNotBean.add(meta.getColumnName(i).toLowerCase());
					}
				}
			}
			if (!getColsInBeanNotRS_throwException()) {
				if (colsInBeanNotRS.size() > 0 && colsInRSnotNotBean.size() > 0) {
					throw new FauxjoException("colsInBeanNotRS={" + colsInBeanNotRS + "}, colsInRSnotNotBean={" + colsInRSnotNotBean + '}');
				} else if (colsInBeanNotRS.size() > 0) {
					throw new FauxjoException("colsInBeanNotRS={" + colsInBeanNotRS);
				} else if (colsInRSnotNotBean.size() > 0) {
					throw new FauxjoException("colsInRSnotNotBean={" + colsInRSnotNotBean + '}');
				}
			}
		} catch (Exception ex) {
			throw new FauxjoException("getFauxCols failed", ex);
		}
		return fauxCols;
	}

}
