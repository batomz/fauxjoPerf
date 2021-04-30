package net.jextra.fauxjo.perftest;

import net.jextra.fauxjo.BeanBuilder;
import net.jextra.fauxjo.ResultSetIterator;
import net.jextra.fauxjo.BeanBuilderJDBC;
import net.jextra.fauxjo.perftest.jdbc.BeanBuilderJDBC_NoReflect;
import net.jextra.fauxjo.perftest.jdbc.db.DBCon;
import net.jextra.fauxjo.rsdef.cache.RSDefCache_Simple;
import org.junit.After;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import static net.jextra.fauxjo.BeanBuilderJDBC.SerDe.REFLECT_LEGACY;
import static org.junit.Assert.assertTrue;

/**
 * Datastore deserialization performance test comparing approaches representing a gradient of real-world uses.
 * <p>
 * EXPERIMENTAL: DO NOT USE IN PRODUCTION
 * Run as a unit test defaulting to H2 inMemory rdbms or as a manual test from main method using postgresql.
 * Deserializing data from jdbc ResultSets into java beans via reflection is known to be ~3X slower than static binding.
 * The following are the two major hotspots:
 * <ol>
 *   <li>RSDef: Calling getResultSetMetaData to map column name/sqlType, and then getting values from the ResultSet</li>
 *   <li>BeanDef: Using Reflection to set the value from the ResultSet [row][col] onto the bean field.</li>
 * </ol>
 * Fauxjo BeanBuilder.buildBean(ResultSet) API is rentrant (no ResultSet pre/post hooks for caching)
 * and already caches BeanDefs. Therefore, these performance tests are concerned with testing and comparing performance
 * of BeanBuilderJDBC, an experimental BeanBuilder, to cache RSDefs (ResultSet definitions) which are expensive
 * to calculate per row and even more per row and column.
 * <p>
 * The combinatorial of test scenarios includes:
 * <ul>
 * <li>Use case: such as fast for continuous integration unit testing or exhaustive for manual testing
 * <li>Database: (H2 inMemory or postgresql)
 * <li>BeanBuilder: deserialization type reflection vs. static mapping bean to ResultSet
 * <li>Number of rows to read per run
 * <li>Number of runs
 * </ul>
 * The following BeanBuilders are tested in this order:
 * <ol>
 *   <li>BeanBuilder (REFLECT_LEGACY dynamic): legacy fauxjo BeanBuilder without modification which caches BeanDefs
 *       but does not cache ResultSet meta data.
 *   <li>BeanBuilderJDBC (REFLECT_OPTIMIZED dynamic): caches RSDefs utilizing fauxjo BeanDefs.
 *   <li>BeanBuilderJDBC_NoReflect (FAUXJO_NOTREFLECT static): best-performance baseline, avoids column reflection
 *       which requires plumbing code changes when either the bean or database change.
 * </ol><p>
 * @see BeanBuilderJDBC
 */
public abstract class BeanBuildTest<T> {
	public Logger _log = LoggerFactory.getLogger(BeanBuildTest.class);
	private long _serDeOptimizedRScnTotal = 0L;

	//--------------------------------------------------------------------------+
	//            Constants for configuring various test scenarios              |
	//--------------------------------------------------------------------------+
	public enum USE_CASE {
		CICD, //continuous integration/deployment with minimal debug logging (standard unit test should run fast)
		DEV_VERBOSE, //development with verbose debug logging
		DEV_TERSE, //development with least debug logging
		EXTENDED  //for performing manual tests that will run a long time
	}
	public enum DB {
		H2, //see H2Con
		Postgresql //see PGCon
	}

	/** * Create a table in DB (if not exists) with fields from the Bean type. */
	public abstract void mkDB(int createRowsCn, boolean newline) throws Exception;

	/** * Return a list of BeanBuilder subclasses each configured to test. */
	public abstract List<BeanBuilder<?>> getBeanBuilders() throws Exception;

	/** * Return the cache for monitoring/metrics. */
	public abstract RSDefCache_Simple getCache();


	/**
	 * After creating data in DB if not existing, executes all performance test combinations capturing their metrics.
	 * To represent a wide variety of real-world scenarios, tests are combinations of the following:
	 * <ul>
	 *   <li>Number of rows: selects (reads) at least 1, 10, and possibly more rows from the database depending on USE_CASE
	 *   <li>BeanBuilder deserialization types: (from getBeanBuilders())
	 * </ul>
	 */
	@Test
	public void perfTests() throws Exception {
		NumberFormat nf = NumberFormat.getInstance();
		int readRowsPerRun = 1;
		try {
			List<String> testStatsMsg = new ArrayList<>();
			String testDsc = "perfTest = { runCn = " + getReadRunCn() + ", readRowsPerRun = ";

			//Build the serDe builders - fail fast (call before mkDB)
			List<BeanBuilder<?>> serDeBuilders = getBeanBuilders();
			_log.info(this.toString()); //log test configuration

			//----------------------------------------------------------------------------+
			//                     DB connection / creation if needed                     |
			//----------------------------------------------------------------------------+
			mkDB(getReadRowCn(), true);

			//----------------------------------------------------------------------------+
			//                       SerDeType X readRow test runs                        |
			//----------------------------------------------------------------------------+
			readRowsPerRun = 1; //Use: webapp lookup
			_log.debug("-------------[ readRowsPerRun: " + nf.format(readRowsPerRun) +  " ]-------------");
			testStatsMsg.add(testDsc + readRowsPerRun + ", stats = " + testSerDeBuilders(getReadRunCn(), readRowsPerRun, serDeBuilders));

			readRowsPerRun = Math.min(getReadRowCn(), 10); //Use: webapp results page
			_log.debug("\"-------------[ readRowsPerRun: " + nf.format(readRowsPerRun) +  " ]-------------");
			testStatsMsg.add(testDsc + readRowsPerRun + ", stats = " + testSerDeBuilders(getReadRunCn(), readRowsPerRun, serDeBuilders));

			if(getUseCase().ordinal() > USE_CASE.CICD.ordinal()) { //prevent accidentally running long performance tests
				readRowsPerRun = Math.min(getReadRowCn(), 100); //Use: eg app example
				_log.debug("\"-------------[ readRowsPerRun: " + nf.format(readRowsPerRun) + " ]-------------");
				testStatsMsg.add(testDsc + readRowsPerRun + ", stats = " + testSerDeBuilders(getReadRunCn(), readRowsPerRun, serDeBuilders));

				if (getUseCase().ordinal() > USE_CASE.DEV_TERSE.ordinal()) { //prevent accidentally running long performance tests
					readRowsPerRun = Math.min(getReadRowCn(), 1000); //Use: small ETL or internal app
					_log.debug("\"-------------[ readRowsPerRun: " + nf.format(readRowsPerRun) + " ]-------------");
					testStatsMsg.add(testDsc + readRowsPerRun + ", stats = " + testSerDeBuilders(getReadRunCn(), readRowsPerRun, serDeBuilders));
				}
				if (getUseCase().ordinal() == USE_CASE.EXTENDED.ordinal()) { //prevent accidentally running long performance tests
					//----------------------------------------------------------------------------+
					//  Extended SerDeType X readRow test runs -WARNING! These take a long time!  |
					//----------------------------------------------------------------------------+
					readRowsPerRun = Math.min(getReadRowCn(), 10000); //Use: larger ETL example
					_log.debug("----------------------------[ readRowsPerRun: " + nf.format(readRowsPerRun) + " ]--------------------------------------");
					testStatsMsg.add(testDsc + readRowsPerRun + ", stats = " + testSerDeBuilders(getReadRunCn(), readRowsPerRun, serDeBuilders));

					readRowsPerRun = Math.min(getReadRowCn(), 100000);  //Use: data migration/recovery
					_log.debug("----------------------------[ readRow	sPerRun: " + nf.format(readRowsPerRun) + " ]--------------------------------------");
					testStatsMsg.add(testDsc + readRowsPerRun + " , stats = " + testSerDeBuilders(getReadRunCn(), readRowsPerRun, serDeBuilders));
				}
			}
			for(String stats : testStatsMsg) {
				_log.info(stats);
			}
			_log.info(getCache().toString());
//			Assert.assertEquals(_serDeOptimizedRScnTotal, getCache().getRSDefCacheL1_callCn());
		} catch(Exception x) {
			_log.error("perfTests failed (readRowsPerRun: " + readRowsPerRun + " and useRSDefCacheL2: " + getRsDefL2enabled() + ")", x);
		} finally {
			if(getDbCon() != null) {
				getDbCon().disconnect();
			}
		}
	}

	/**
	 * Return a list of textual aggregate stats from specified runs and rows per run per BeanBuilder.
	 * For each beanBuildersToTest, reads specified rows into beans per readRun recording the avg millis per run.
	 * @param readRunCn number of runs reading readRowsPerRun rows.
	 * @param readRowsPerRun number of runs to read readRunCn rows.
	 * @param beanBuildersToTest has BeanBuilders to test, if fauxjo legacy builder comparison stats will be captured.
	 */
	private List<String> testSerDeBuilders(int readRunCn, int readRowsPerRun, List<BeanBuilder<?>> beanBuildersToTest) throws Exception {
		String perfStatsJSON;
		_log.debug("RSBeanBuilderPOC_Test conf readRunCn: " + readRunCn + ", readRowsPerRun: " + readRowsPerRun);
		String serdeCur = null;
		List<String> statsJSON = new ArrayList<>();
		try {
			Integer rowCnLogModulo = null; //if null, the first run will not show its rows
			if(getLogRun0_rowsAndModuloRuns()) rowCnLogModulo = Math.max(1, (int)Math.sqrt(readRowsPerRun)); //if null, the first run will not show its rows
			NumberFormat nf = NumberFormat.getInstance();
			nf.setMaximumFractionDigits(3);
			double runReadTimeAvg;
			double runReadTimeAvg_LEGACY = 0d;
			for(BeanBuilder<?> beanBuilder : beanBuildersToTest) {
				runReadTimeAvg = testReadRunsForBeanBuilder(beanBuilder, readRunCn, readRowsPerRun, rowCnLogModulo, nf);
				if(beanBuilder instanceof BeanBuilderJDBC_NoReflect) {
					serdeCur = ((BeanBuilderJDBC_NoReflect<?>)beanBuilder).getSerDeName();
					perfStatsJSON = getResultMsg_Stats(serdeCur, runReadTimeAvg, runReadTimeAvg_LEGACY, nf);
					_log.debug(perfStatsJSON);
				} else if(beanBuilder instanceof BeanBuilderJDBC) {
					serdeCur = ((BeanBuilderJDBC<?>)beanBuilder).getSerDeName();
					perfStatsJSON = getResultMsg_Stats(serdeCur, runReadTimeAvg, runReadTimeAvg_LEGACY, nf);
					_log.debug(perfStatsJSON);
					_serDeOptimizedRScnTotal += ((long)readRunCn * (long)readRowsPerRun);
				} else { //legacy
					serdeCur = REFLECT_LEGACY.name();
					runReadTimeAvg_LEGACY = runReadTimeAvg;
					perfStatsJSON = getResultMsg_Time(serdeCur, runReadTimeAvg_LEGACY, nf);
					_log.debug(perfStatsJSON);
				}
				statsJSON.add(perfStatsJSON);
			}
		} catch(Exception x) {
			_log.error("testSerDes failed" + (serdeCur != null ? " for SerDeType: " + serdeCur : ""), x);
		}
		return statsJSON;
	}

	/** * Return textual average run milliseconds for the legacy reflection BeanBuilder. */
	String getResultMsg_Time(String serdeType, double runReadTimeAvg_Legacy, NumberFormat nf) {
		return("\n    { serde = '" + padR(serdeType + "',", 22) + " runAvgMS = " + nf.format(runReadTimeAvg_Legacy) + "}");
	}

	/** * Return statistics for non-legacy BeanBuilder against fauxjo legacy reflection BeanBuilder. */
	String getResultMsg_Stats(String serdeType, double runReadTimeAvg, double runReadTimeAvg_Legacy, NumberFormat nf) {
		double ratio;
		if(runReadTimeAvg_Legacy == 0d) { //prevent div/0
			_log.warn("runReadTimeAvg_Legacy = 0, enable its block for stats");
			return getResultMsg_Time(serdeType, runReadTimeAvg, nf);
		}
		ratio=runReadTimeAvg/runReadTimeAvg_Legacy;
		return "\n    { serde = '" + padR(serdeType + "',", 22) + " runAvgMS = " + nf.format(runReadTimeAvg) + ", serdeDivByLEGACY = " + nf.format(ratio) + ", speedup = " + nf.format(1D/ratio) + " }";
	}

	String padR(String str, int cn) {
		return String.format("%1$-" + cn + "s", str);
	}

	/**
	 * Return avgTime of all runs where builder deserializes a row into a Fauxjo (a java bean) per readRowCn.
	 * @param builder to test
	 * @param runCn number of runs to perform
	 * @param readRowCn number of rows to read per run
	 * @param rowCnLogModulo if not null, will log per row modulo this value (eg every 1000th row)
	 * @param nf to format avgRunTime
	 */
	private double testReadRunsForBeanBuilder(BeanBuilder<?> builder, int runCn, int readRowCn, Integer rowCnLogModulo, NumberFormat nf) throws Exception {
		double runtimeAvg = 0D;
		int run = 0;
		try {
			Connection con = getDbCon().getConnection();
			if(con == null || con.isClosed()) {
				throw new Exception("Failed to get ResultSet, Connection was null or closed, see logs for prior errors.");
			}
			Statement stm;
			long readtimeRun;
			long avgMS = 0L;
			for(run=0;run<runCn;run++) {
				stm = con.createStatement();
				ResultSet rs = stm.executeQuery("SELECT * FROM " + getTableKey()); //reads all rows from the database (ROWCN)
				ResultSetIterator<?> rsi = new ResultSetIterator<>(rs, builder);
				assertTrue(rsi.hasNext());
				long readtimeStart = System.currentTimeMillis();
				if (rsi.hasNext()) {
					int r = 0;
					for (Object bean : rsi) { //forces each bean to be created
						if(r >= readRowCn) break;
						if(rowCnLogModulo != null && (rowCnLogModulo == 0 || run == 0 && r % rowCnLogModulo == 0)) {
							_log.debug(String.format("  fauxjo row[%d] %s", r, bean.toString()));
						}
						r++;
					}
				}
				if(run == 0) continue; //Do not contribute the first run to the avg, it is not warmed up.
				readtimeRun = (System.currentTimeMillis() - readtimeStart);
				avgMS += readtimeRun;
			}
			runtimeAvg = (double)(avgMS) / (runCn - 1);
			if(builder instanceof BeanBuilderJDBC_NoReflect) {
				_log.debug(((BeanBuilderJDBC_NoReflect<?>) builder).getSerDeName() + " runtimeAvg: " + nf.format(runtimeAvg));
			} else if(builder instanceof BeanBuilderJDBC) {
				_log.debug(((BeanBuilderJDBC<?>)builder).getSerDeName() + " runtimeAvg: " + nf.format(runtimeAvg));
			} else {
				_log.debug(REFLECT_LEGACY.name() + " runtimeAvg: " + nf.format(runtimeAvg));
			}
		} catch(Exception x) {
			_log.error("run " + run + " failed", x);
		}
		return runtimeAvg;
	}

	public abstract String getTableKey();

	/** * Return number of rows to create in the datastore which is the maximum row read count per run. */
	public abstract int getReadRowCn();
	public abstract void setReadRowCn(int readRowCn);

	/** * Return the number of runs, each reading getReadRowCn rows. */
	public abstract int getReadRunCn();
	public abstract void setReadRunCn(int readRunCn);

	/** * Return true if logging modulo is requested. */
	public abstract boolean getLogRun0_rowsAndModuloRuns();

	/** * Return true if the bean is allowed to have fields not in the database. */
	public abstract boolean getAllowMissingFields();

	/** * Return true the ResultSet shall be explicitly closed, a fauxjo construct. */
	public abstract boolean getAutoCloseResultSet();

	/** * Return a class representing a datastore connection. */
	public abstract DBCon getDbCon();
	/** * Return true if experimental L2 caching should be enabled. */
	public abstract boolean getRsDefL2enabled();

	/** * Return the datastore requested. */
	public abstract DB getDB();
	public abstract void setDB(DB db);

	/** * Return the use case requested. */
	public abstract USE_CASE getUseCase();
	public abstract void setUseCase(USE_CASE useCase);

	/** * Close the dbCon. */
	@After
	public void takeDown() throws Exception {
		if(getDbCon() != null) getDbCon().disconnect();
	}

	/** * Return test configuration details provided by the subclass. */
	@Override
	public String toString() {
		return getClass().getSimpleName() + "{" +
			" db=" + getDB() +
			", useCase=" + getUseCase() +
			", rowCreateCn=" + getReadRowCn() +
			", readRunCn=" + getReadRunCn() +
			", logRun0_rowsAndModuloRuns=" + getLogRun0_rowsAndModuloRuns() +
			(getCache() != null ? ", " + getCache().toString() : "") +
			" }";
	}

}

