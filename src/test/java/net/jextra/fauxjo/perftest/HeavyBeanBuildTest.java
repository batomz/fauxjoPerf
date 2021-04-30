package net.jextra.fauxjo.perftest;

import net.jextra.fauxjo.BeanBuilder;
import net.jextra.fauxjo.perftest.jdbc.BeanBuilderJDBC_NoReflect;
import net.jextra.fauxjo.perftest.testbeans.heavy.HeavyBean;
import net.jextra.fauxjo.perftest.testbeans.heavy.HeavyBeanNoRefl;
import net.jextra.fauxjo.BeanBuilderJDBC;
import net.jextra.fauxjo.perftest.jdbc.db.DBCon;
import net.jextra.fauxjo.perftest.jdbc.db.H2Con;
import net.jextra.fauxjo.perftest.jdbc.db.PGCon;
import net.jextra.fauxjo.rsdef.cache.RSDefCache_Simple;
import net.jextra.fauxjo.perftest.util.Fab;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Performance test of a bean with many fields, configured to run as a unit test by default using H2 inMemory database.
 * <p>
 * EXPERIMENTAL: DO NOT USE IN PRODUCTION
 * If main is uncommented, it can be run against Postgres. See the configuration section for various testing scenarios.
 */
public class HeavyBeanBuildTest extends BeanBuildTest<HeavyBean> {
	public Logger _log = LoggerFactory.getLogger(HeavyBeanBuildTest.class);
	private DBCon _dbCon = null;
	private RSDefCache_Simple _rsCache;
	//----------------------------------------------------------------------------+
	//                                Configuration                               |
	//----------------------------------------------------------------------------+
	private DB _db = DB.H2;
	private USE_CASE _testCase = USE_CASE.CICD;
	private boolean _logRun0_rowsAndModuloRuns = true;
	private int _readRowCn = 20000; //smaller rowCn because there are so many cols
	private int _readRunCn = 10000; //10,000 rows per run provides a good warm up
	private final long _initRSDefCacheTimeToLiveMS = 60L * 60L * 1000L; //1 hour

	@Override
	public String getTableKey() { return HeavyBean.class.getSimpleName().toLowerCase(); }

	@Override
	public int getReadRowCn() { return _readRowCn; }

	@Override
	public void setReadRowCn(int readRowCn) { _readRowCn = readRowCn; }

	/** Return 10,000 rows per run provides a good warm up */
	@Override
	public int getReadRunCn() { return _readRunCn; }

	@Override
	public void setReadRunCn(int readRunCn) { _readRunCn = readRunCn; }

	@Override
	public boolean getLogRun0_rowsAndModuloRuns() { return _logRun0_rowsAndModuloRuns; }

	@Override
	public boolean getAllowMissingFields() { return false; }

	@Override
	public boolean getAutoCloseResultSet() { return false; }

	@Override
	public DBCon getDbCon() { return _dbCon; }

	@Override
	public boolean getRsDefL2enabled() { return true; }

	@Override
	public DB getDB() { return _db; }

	@Override
	public void setDB(DB db) { _db = db; }

	@Override
	public USE_CASE getUseCase() { return _testCase; }

	@Override
	public void setUseCase(USE_CASE useCase) {
		_testCase = useCase;
		if (getUseCase() == USE_CASE.CICD || getUseCase() == USE_CASE.DEV_VERBOSE) {
			_logRun0_rowsAndModuloRuns = true;
		} else {
			_logRun0_rowsAndModuloRuns = false;
		}
	}

	/**
	 * Cache-purge can be a hotspot, is the callers responsibility to configure.
	 * @see RSDefCache_Simple
	 */
	@Override
	public RSDefCache_Simple getCache() {
		return _rsCache;
	}

	/**
	 * Return 3 BeanBuilders created representing different deserialization approaches for comparison:
	 * <ul>
	 * <li>fauxjo BeanBuilder (legacy)
	 * <li>BeanBuilderJDBC (proposed caching)
	 * <li>BeanBuilderJDBC_NoReflect (static map best-performance baseline)
	 * </ul>
	 * RSDefCache_Simple is created here.
	 */
	public List<BeanBuilder<?>> getBeanBuilders() throws Exception {
		//----------------------------------------------------------------------------+
		//                                 CACHE Creation                             |
		//----------------------------------------------------------------------------+
		//Worst-case purge configuration: runCn = 10000, readRowsPerRun = 1
		int _rsDefCachePutsPerPurge = Math.max(1000, getReadRunCn() / 100); //min 1000, for 100K runs purges every 1000th ResultSet (100X)
		_log.info("perfTests L1cache TTL: " + _initRSDefCacheTimeToLiveMS + ", rsDefCachePutsPerPurge: " + _rsDefCachePutsPerPurge);
		_rsCache = new RSDefCache_Simple(getRsDefL2enabled()).setConfig(_initRSDefCacheTimeToLiveMS, _rsDefCachePutsPerPurge);

		List<BeanBuilder<?>> serDeBuilders = new ArrayList<>();

		//Explicit legacy bean ensuring its functionality even though BeanBuilderJDBC can be a legacy BeanBuilder.
		BeanBuilder<HeavyBean> legacyBuilder = new BeanBuilder<>(HeavyBean.class);
		serDeBuilders.add(legacyBuilder);

		BeanBuilderJDBC<HeavyBean> optimizedBuilder = new BeanBuilderJDBC<>(HeavyBean.class, BeanBuilderJDBC.SerDe.REFLECT_OPTIMIZED, _rsCache);
		serDeBuilders.add(optimizedBuilder);

		BeanBuilderJDBC_NoReflect<HeavyBeanNoRefl> bareMetalBuilder = new BeanBuilderJDBC_NoReflect<>(HeavyBeanNoRefl.class);
		serDeBuilders.add(bareMetalBuilder); //NOTE! Uses a different bean!

		for(BeanBuilder<?> b: serDeBuilders) {
			b.setAllowMissingFields(getAllowMissingFields());
			b.setAutoCloseResultSet(getAutoCloseResultSet());
		}
		return serDeBuilders;
	}

	/**
	 * Create a table in DB (if not exists) with fields from HeavyBean.
	 * Manually create sql statement for easy visibility representing how a programmer might statically bind.
	 * <p>
	 * @param createRowsCn number of rows to create in the table
	 * @see #getSqlMkTable
	 */
	public void mkDB(int createRowsCn, boolean newline) throws Exception {
		if (_db != null && _db == DB.Postgresql) {
			_dbCon = new PGCon();
			_dbCon.connect(PGCon.PGDB.LOCALDB.URL);
		} else {
			_dbCon = new H2Con();
			_dbCon.connect(H2Con.H2DB.MEMORY.URL);
		}
		try {
			Connection con = _dbCon.getConnection();
			Statement stm = con.createStatement();
			StringBuilder sqlMkTbl = getSqlMkTable(_db, newline, new StringBuilder());
			if(_logRun0_rowsAndModuloRuns) _log.debug(sqlMkTbl.toString());
			stm.execute(sqlMkTbl.toString());
			 //If postgres, skip table creation and inserts if rows already exists.
			int rowCn = 0;
			if (_db == DB.Postgresql) {
				ResultSet rs = stm.executeQuery("SELECT count(*) from " + getTableKey() + ";");
				if (rs.next()) {
					rowCn = rs.getInt(1);
				}
			}
			//------------------------ create random rows with high entropy --------------------
			if(rowCn < 1) {
				NumberFormat[] nf = new NumberFormat[4];
				for(int i=0;i<nf.length;i++) {
					nf[i] = NumberFormat.getInstance();
					nf[i].setMaximumFractionDigits(i);
				} //entropy: 0 thru 4 max digits of precision
				int r = 0;
				StringBuilder sqlInsertRow = new StringBuilder();
				SimpleDateFormat iso8601 = new SimpleDateFormat(Fab.SIMPLE_DATE_FORMAT);
				NumberFormat n = null;
				for(r=0; r< createRowsCn; r++) { //row
					n = nf[(int) (Math.random() * 4d)]; //entropy: 0 thru 4 max digits of precision
					getSqlInsert(sqlInsertRow, newline, n, iso8601);
					if(_logRun0_rowsAndModuloRuns && r % 1000 == 0) {
						_log.debug("insert row[" + r + "]: " + sqlInsertRow);
					}
					stm.execute(sqlInsertRow.toString());
				}
				_log.info("Created " + r + " rows in " + _db.name() + " database");
			}
		} catch (Exception x) {
			if (_dbCon != null) {
				_dbCon.disconnect();
				throw x;
			}
		}
	}

	/**
	 * Explicit mapping, static binding, easiest to understand and see, but least flexible.
   * <p>
	 * STATIC MAPPING IS DIFFICULT TO MAINTAIN, DO NOT DO THIS. See Fauxjo.
	 * @param db used to make db dialect-specific sql
	 * @param newlines if true, the query will be pretty-formatted
	 * @param b will be cleared and populated with sql
	 */
	public StringBuilder getSqlMkTable(DB db, boolean newlines, StringBuilder b) throws Exception {
		b.setLength(0);
		boolean nl = newlines;
		String delim = ", ";
		b.append("CREATE TABLE IF NOT EXISTS " + getTableKey() + " (" + (nl ? "\n" : ""));
		{ //first row does not have a preceding comma
			if(nl) b.append("  ");
			Fab.createTblCol("key", JDBCType.VARCHAR, "(255)", "", nl, b);
		} //Fab.createTblCol_vc255("key",                       null, delim, nl, b);
		Fab.createTblCol_vc255("franchiseId",               null, delim, nl, b);
		Fab.createTblCol_vc255("renterId",                  null, delim, nl, b);
		Fab.createTblCol_vc255("renterAccountNumber",       null, delim, nl, b);
		Fab.createTblCol_vc255("refererId",                 null, delim, nl, b);
		Fab.createTblCol_vc255("returnLocId",               null, delim, nl, b);
		Fab.createTblCol_vc255("signedKey",                 null, delim, nl, b);
		Fab.createTblCol_vc255("refKey",                    null, delim, nl, b);
		Fab.createTblCol("cancelDate",                      JDBCType.TIMESTAMP, null, delim, nl, b);
		Fab.createTblCol("cancelCode",                      JDBCType.INTEGER, null, delim, nl, b);
		Fab.createTblCol_vc255("cancelReason",              null, delim, nl, b);
		Fab.createTblCol_vc255("signedTimeZone",            null, delim, nl, b);
		Fab.createTblCol("signedDate",                      JDBCType.TIMESTAMP, null, delim, nl, b);
		Fab.createTblCol_vc255("rentalType",                null, delim, nl, b);
		Fab.createTblCol("rentalTypeExpirationDate",        JDBCType.TIMESTAMP, null, delim, nl, b);
		Fab.createTblCol_vc255("rentalTypeLotNum",          null, delim, nl, b);
		Fab.createTblCol_vc255("referrerOrganizationName",  null, delim, nl, b);
		Fab.createTblCol_vc255("outwardTrackingNumber",     null, delim, nl, b);
		Fab.createTblCol_vc255("inwardTrackingNumber",      null, delim, nl, b);
		Fab.createTblCol("entryDate",                       JDBCType.TIMESTAMP, null, delim, nl, b);
		Fab.createTblCol("rentalRequestDate",               JDBCType.TIMESTAMP, null, delim, nl, b);
		Fab.createTblCol("rentalWeightKG",                  JDBCType.NUMERIC, null, delim, nl, b);
		Fab.createTblCol("renterPickupDate",                JDBCType.TIMESTAMP, null, delim, nl, b);
		Fab.createTblCol("rentalReleaseDate",               JDBCType.TIMESTAMP, null, delim, nl, b);
		Fab.createTblCol("deliveredToReturnLocDate",        JDBCType.TIMESTAMP, null, delim, nl, b);
		Fab.createTblCol("receivedAtReturnLocDate",         JDBCType.TIMESTAMP, null, delim, nl, b);
		Fab.createTblCol("inspectionDate",                  JDBCType.TIMESTAMP, null, delim, nl, b);
		Fab.createTblCol_vc255("inspectionValue",           null, delim, nl, b);
		Fab.createTblCol_vc255("inspectionComment",         null, delim, nl, b);
		Fab.createTblCol_vc255("rentalTypeMaintKey",        null, delim, nl, b);

		b.append(");" + (nl ? "\n" : ""));
		return b;
	}

	/**
	 * Return a StringBuilder with the sql required to create a table for the test if it does not exist.
	 * Manually create sql statement for easy visibility representing how a programmer might statically bind.
	 * <p>
	 * STATIC MAPPING IS DIFFICULT TO MAINTAIN, DO NOT DO THIS. See Fauxjo.
	 * @param pre will be cleared and populated with sql
	 * @param newlines if true, the query will be pretty-formatted
	 * @param nf for formatting BigDecimals
	 * @param dtFrm for formatting dates
	 */
	public StringBuilder getSqlInsert(StringBuilder pre, boolean newlines, NumberFormat nf, SimpleDateFormat dtFrm) throws Exception {
		pre.setLength(0);
		StringBuilder post = new StringBuilder();
		boolean nl = newlines; //reassign to shorten code below
		pre.append("INSERT INTO " + getTableKey() + " (" + (nl ? "\n" : ""));
		{ //Pretty-format the sql, first row does not have a preceding comma
			if(nl) pre.append("    ").append("key").append('\n');
			else pre.append("key");
			if(nl) post.append("    '").append(Fab.getRandStr(15, 20, 0.03f)).append("'").append('\n');
			else post.append(Fab.getRandStr(15, 20, 0.03f));
		} //Fab.insertKV_Cd("key",                    pre, post, nl);
		Fab.insertKV_Id("franchiseId",            pre, post, nl);
		Fab.insertKV_Cd("renterId",               pre, post, nl);
		Fab.insertKV_Cd("renterAccountNumber",    pre, post, nl);
		Fab.insertKV("refererId",                 pre, post, nl);
		Fab.insertKV_Id("returnLocId",            pre, post, nl);
		Fab.insertKV_Id("signedKey",              pre, post, nl);
		Fab.insertKV_Id("refKey",                 pre, post, nl);
		Fab.insertDt("cancelDate",                pre, post, nl, dtFrm);
		Fab.insertInt("cancelCode",               pre, post, nl, -1, 11);
		Fab.insertKV("cancelReason",              pre, post, nl);
		Fab.insertKV("signedTimeZone",            pre, post, nl);
		Fab.insertDt("signedDate",                pre, post, nl, dtFrm);
		Fab.insertKV_Cd("rentalType",             pre, post, nl);
		Fab.insertDt("rentalTypeExpirationDate",  pre, post, nl, dtFrm);
		Fab.insertKV_Cd("rentalTypeLotNum",       pre, post, nl);
		Fab.insertKV("referrerOrganizationName",  pre, post, nl);
		Fab.insertKV("outwardTrackingNumber",     pre, post, nl);
		Fab.insertKV("inwardTrackingNumber",      pre, post, nl);
		Fab.insertDt("entryDate",                 pre, post, nl, dtFrm);
		Fab.insertDt("rentalRequestDate",         pre, post, nl, dtFrm);
		Fab.insertWt("rentalWeightKG",            pre, post, nl, Fab.SQL_WT_MIN, Fab.SQL_WT_MAX, nf);
		Fab.insertDt("renterPickupDate",          pre, post, nl, dtFrm);
		Fab.insertDt("rentalReleaseDate",         pre, post, nl, dtFrm);
		Fab.insertDt("deliveredToReturnLocDate",  pre, post, nl, dtFrm);
		Fab.insertDt("receivedAtReturnLocDate",   pre, post, nl, dtFrm);
		Fab.insertDt("inspectionDate",            pre, post, nl, dtFrm);
		Fab.insertKV("inspectionValue",           pre, post, nl);
		Fab.insertKV_Note("inspectionComment",    pre, post, nl);
		Fab.insertKV_Cd("rentalTypeMaintKey",     pre, post, nl);

		pre.append(") values (" + (nl ? "\n" : ""));
		pre.append(post);
		pre.append(");");
		if(nl) pre.append('\n');
		return pre;
	}

	/* Uncomment and run from main to manually test using Postgres else will run as unit test using H2 inMemoryDb
	public static void main(String[] args) {
		HeavyBeanBuildTest test = new HeavyBeanBuildTest();
		test.setReadRowCn(2000); //rowRead reduced for Postgres
		test.setReadRunCn(2000); //runCn reduced for Postgres
		test.setUseCase(USE_CASE.DEV_TERSE);
		System.out.println("Strongly recommend you vacuumdb after the first time run and before testing: vacuumdb -U postgres fjoperf");
		try {
		  test.setDB(DB.Postgresql);
		  System.out.println(test.toString());
		  test.perfTests();
		} catch(Exception x) {
			Exception y = new Exception("Failed manual test", x);
			y.printStackTrace();
		}
	} */

}
