package net.jextra.fauxjo.perftest;

import net.jextra.fauxjo.BeanBuilder;
import net.jextra.fauxjo.perftest.jdbc.BeanBuilderJDBC_NoReflect;
import net.jextra.fauxjo.perftest.testbeans.lite.LiteBean;
import net.jextra.fauxjo.perftest.testbeans.lite.LiteBeanNoRefl;
import net.jextra.fauxjo.BeanBuilderJDBC;
import net.jextra.fauxjo.perftest.jdbc.db.DBCon;
import net.jextra.fauxjo.perftest.jdbc.db.H2Con;
import net.jextra.fauxjo.perftest.jdbc.db.PGCon;
import net.jextra.fauxjo.rsdef.cache.RSDefCache_Simple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Performance test of a bean with few fields, configured to run as a unit test by default using H2 inMemory database.
 * <p>
 * EXPERIMENTAL: DO NOT USE IN PRODUCTION
 * If main is uncommented, it can be run against Postgres. See the configuration section for various testing scenarios.
 */
public class LiteBeanBuildTest extends BeanBuildTest<LiteBean> {
	public Logger _log = LoggerFactory.getLogger(LiteBeanBuildTest.class);
	private DBCon _dbCon = null;
	private RSDefCache_Simple _rsCache;
	//----------------------------------------------------------------------------+
	//                                Configuration                               |
	//----------------------------------------------------------------------------+
	private DB _db = DB.H2;
	private USE_CASE _testCase = USE_CASE.CICD;
	private boolean _logRun0_rowsAndModuloRuns = true;
	private int _readRowCn = 200000;
	private int _readRunCn = 10000;
	private final long _initRSDefCacheTimeToLiveMS = 60L * 60L * 1000L; //1 hour

	@Override
	public String getTableKey() { return LiteBean.class.getSimpleName().toLowerCase(); }
	@Override
	public int getReadRowCn() { return _readRowCn; }
	@Override
		public void setReadRowCn(int readRowCn) { _readRowCn = readRowCn; }
	/** * Return 10,000 rows per run provides a good warm up */
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
		if(getUseCase() == USE_CASE.CICD || getUseCase() == USE_CASE.DEV_VERBOSE) {
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
		int rsDefCachePutsPerPurge = Math.max(1000, getReadRunCn() / 100); //min 1000, for 100K runs purges every 1000th ResultSet (100X)
		_log.info("perfTests L1cache TTL: " + _initRSDefCacheTimeToLiveMS + ", rsDefCachePutsPerPurge: " + rsDefCachePutsPerPurge);
		_rsCache = new RSDefCache_Simple(getRsDefL2enabled()).setConfig(_initRSDefCacheTimeToLiveMS, rsDefCachePutsPerPurge);

		List<BeanBuilder<?>> serDeBuilders = new ArrayList<>();

		//Explicit legacy bean ensuring its functionality even though BeanBuilderJDBC can be a legacy BeanBuilder.
		BeanBuilder<LiteBean> legacyBuilder = new BeanBuilder<>(LiteBean.class);
		serDeBuilders.add(legacyBuilder);

		BeanBuilderJDBC<LiteBean> optimizedBuilder = new BeanBuilderJDBC<>(LiteBean.class, BeanBuilderJDBC.SerDe.REFLECT_OPTIMIZED, _rsCache);
		serDeBuilders.add(optimizedBuilder);

		BeanBuilderJDBC_NoReflect<LiteBeanNoRefl> bareMetalBuilder = new BeanBuilderJDBC_NoReflect<>(LiteBeanNoRefl.class);
		serDeBuilders.add(bareMetalBuilder); //NOTE! Uses a different bean!

		for(BeanBuilder<?> b: serDeBuilders) {
			b.setAllowMissingFields(getAllowMissingFields());
			b.setAutoCloseResultSet(getAutoCloseResultSet());
		}
		return serDeBuilders;
	}

	/**
	 * Create a table in DB (if not exists) with fields from LiteBean.
	 * Manually create sql statement for easy visibility representing how a programmer might statically bind.
	 */
	@Override
	public void mkDB(int createRowsCn, boolean newline) throws Exception {
		try {
			if (_db != null && _db == DB.Postgresql) {
				_dbCon = new PGCon();
				_dbCon.connect(PGCon.PGDB.LOCALDB.URL);
			} else {
				_dbCon = new H2Con();
				_dbCon.connect(H2Con.H2DB.MEMORY.URL);
			}
			Connection con = _dbCon.getConnection();
			Statement stm = con.createStatement();
			//If postgres, skip table creation and inserts if rows already exists.
			int rowCn = 0;
			if (_db == DB.Postgresql) {
				stm.execute("CREATE TABLE IF NOT EXISTS " + getTableKey() + " (id serial, name VARCHAR(255), weight numeric, age integer);");
				ResultSet rs = stm.executeQuery("SELECT count(*) from " + getTableKey() + ";");
				if (rs.next()) {
					rowCn = rs.getInt(1);
				}
			} else {
				stm.execute("CREATE TABLE IF NOT EXISTS " + getTableKey() + " (id BIGINT auto_increment, name VARCHAR(255), weight numeric, age integer);");
			}
			//------------------------ create random rows with high entropy --------------------
			if(rowCn < 1) {
				NumberFormat[] nf = new NumberFormat[4]; //entropy: 0 thru 4 max digits of precision
				for (int i = 0; i < nf.length; i++) {
					nf[i] = NumberFormat.getInstance();
					nf[i].setMaximumFractionDigits(i);
				} //entropy: 0 thru 4 max digits of precision
				BigDecimal weight = null;
				int age = 0;
				String name = null;
				NumberFormat n = null;
				int indexOfFour = 0;
				int r = 0;
				for (r = 0; r < createRowsCn; r++) { //row
					n = nf[(int) (Math.random() * 4d)]; //entropy: 0 thru 4 max digits of precision
					weight = new BigDecimal(n.format((Math.random() * 150d) + 100d)); //100:250 with random range of 150
					age = (int) (Math.random() * 80) + 20;
					name = UUID.randomUUID().toString();
					double dist = Math.random();
					if (dist > 0.18d) { //simulate worst-case scenario entropy (strongly unique and missing values)
						stm.execute("INSERT INTO " + getTableKey() + " (name,weight,age) values ('" + name + "', " + weight + ", " + age + ");");
					} else if (dist > 0.12) { //10% of the time either weight or age will be null
						stm.execute("INSERT INTO " + getTableKey() + " (name,age) values ('" + name + "', " + age + ");");
					} else if (dist > 0.03d) { //10% of the time either weight or age will be null
						stm.execute("INSERT INTO " + getTableKey() + " (name,weight) values ('" + name + "', " + weight + ");");
					} else { //10% of the time either weight or age will be null
						stm.execute("INSERT INTO " + getTableKey() + " (name) values ('" + name + "');");
					}
					//_log.trace(nm + ", weight: " + w + ", age: " + a);
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

	/* Uncomment and run from main to manually test using Postgres else will run as unit test using H2 inMemoryDb
	public static void main(String[] args) {
		LiteBeanBuildTest test = new LiteBeanBuildTest();
		test.setReadRowCn(2000); //rowRead reduced from 200k to 2k for Postgres
		test.setReadRunCn(1000); //runCn reduced from 10k to 1k for Postgres
		test.setUseCase(USE_CASE.DEV_TERSE);
		System.out.println("Strongly recommend you vacuumdb after the first time run and before testing: vacuumdb -U postgres fjoperf");
		try {
		  test.setDB(DB.Postgresql);
		  test.perfTests();
		} catch(Exception x) {
			Exception y = new Exception("Failed manual test", x);
			y.printStackTrace();
		}
	} */

}
