package net.jextra.fauxjo.perftest.util;

import java.math.BigDecimal;
import java.sql.JDBCType;
import java.sql.SQLType;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.UUID;

/** * Fabricates data for testing (unit and performance) using very simplistic methods. */
public class Fab {
		private static final long DAY_MS = 24L * 60L * 60L * 1000L;
		private static final int UUID_LEN = UUID.randomUUID().toString().length();
		public static final BigDecimal SQL_WT_MIN = new BigDecimal("00.1000");
		public static final BigDecimal SQL_WT_MAX = new BigDecimal("70.0000");
		public static final String SIMPLE_DATE_FORMAT = "yyyy-MM-dd hh:mm:ss"; //default Postgres format instead of iso8601 "yyyy-MM-dd'T'HH:mm:ss.000'Z'"

	/**
	 * Convenience method for varchar(255). Adds sql to create a column for a create table query.
 	 * @param name of the column
	 * @param constraints for the column, requires a leading space unless that is not desired
	 * @param delim typically a comma followed by a space
	 * @param newline whether to break the query into multiple lines
	 * @param b destination for the created sql
	 */
		public static void createTblCol_vc255(String name, String constraints, String delim, boolean newline, StringBuilder b) throws Exception {
			createTblCol(name, JDBCType.VARCHAR, "(255)" + (constraints != null ? constraints : ""), delim, newline, b);
		}

	/**
	 * Adds sql to create a column for a create table query.
 	 * @param name of the column
	 * @param sqlType from JDBCType
	 * @param constraints for the column, requires a leading space unless that is not desired
	 * @param delim typically a comma followed by a space
	 * @param newline whether to break the query into multiple lines
	 * @param b destination for the created sql
	 */
		public static void createTblCol(String name, SQLType sqlType, String constraints, String delim, boolean newline, StringBuilder b) throws Exception {
			if(newline) { b.append(' '); b.append(' '); }
			if(delim != null) b.append(delim);
			b.append(name).append(' ').append(sqlType.getName()).append(constraints != null ? constraints : "");
			if(newline) b.append('\n');
		}

		/** * Add weight to test numeric. */
		public static void insertWt(String fieldName, StringBuilder pre, StringBuilder post, boolean newline, BigDecimal min, BigDecimal max, NumberFormat nf) {
			addSqlField(fieldName, pre, newline);
			addSqlValue(getRandBD(min, max, 0.05f, nf), post, newline, nf);
		}
		public static void insertInt(String fieldName, StringBuilder pre, StringBuilder post, boolean newline, int min, int max) {
			addSqlField(fieldName, pre, newline);
			addSqlValue(getRandInt(min, max, 0.05f), post, newline);
		}
		public static void insertDt(String fieldName, StringBuilder pre, StringBuilder post, boolean newline, SimpleDateFormat sdf) {
			addSqlField(fieldName, pre, newline);
			addSqlValue(getRandDate(4, 365, 0.01f), post, newline, sdf);
		}
		public static void insertKV(String fieldName, StringBuilder pre, StringBuilder post, boolean newline) {
			addSqlField(fieldName, pre, newline);
			addSqlValue(getRandStr( 3, 16, 0.10f), post, newline);
		}
		public static void insertKV_Id(String fieldName, StringBuilder pre, StringBuilder post, boolean newline) {
			addSqlField(fieldName, pre, newline);
			addSqlValue(getRandStr(15, 20, 0.03f), post, newline);
		}
		public static void insertKV_Cd(String fieldName, StringBuilder pre, StringBuilder post, boolean newline) {
			addSqlField(fieldName, pre, newline);
			addSqlValue(getRandStr(3, 5, 0.05f), post, newline);
		}
		public static void insertKV_Note(String fieldName, StringBuilder pre, StringBuilder post, boolean newline) {
			addSqlField(fieldName, pre, newline);
			String comment = getRandStr(UUID_LEN >> 1, UUID_LEN, 0.9f);
			if(comment != null && comment.length() > 0) comment += ", " + (getRandStr(UUID_LEN >> 1, UUID_LEN, 0.9f) + ".");
			addSqlValue(comment, post, newline);
		}
		public static void addSqlField(String name, StringBuilder b, boolean newline) {
			if(newline) { b.append(' '); b.append(' '); }
			b.append(", ").append(name);
			if(newline) b.append('\n');
		}
		public static void addSqlValue(String str, StringBuilder b, boolean newline) {
			if(newline) { b.append(' '); b.append(' '); }
			b.append(", ");
			b.append(str != null ? "'" + str + "'" : "null");
			if(newline) b.append('\n');
		}
		public static void addSqlValue(Number num, StringBuilder b, boolean newline) {
			if(newline) { b.append(' '); b.append(' '); }
			b.append(", ").append(num != null ? num.toString() : "null");
			if(newline) b.append('\n');
		}
		public static void addSqlValue(BigDecimal bd, StringBuilder b, boolean newline, NumberFormat nf) {
			if(newline) { b.append(' '); b.append(' '); }
			b.append(", ").append(bd != null ? nf.format(bd) : "null");
			if(newline) b.append('\n');
		}
		public static void addSqlValue(java.util.Date dt, StringBuilder b, boolean newline, SimpleDateFormat sdf) {
			if(newline) { b.append(' '); b.append(' '); }
			b.append(", ").append(dt != null ? "'" + sdf.format(dt) + "'": "null");
			if(newline) b.append('\n');
		}
		public static Integer getRandInt(int min, int max, float nullProbabilityMax) {
			if(Math.random() < nullProbabilityMax) return null;
			return (int)((Math.random() * (max-min)) + min);
		}
		public static BigDecimal getRandBD(BigDecimal min, BigDecimal max, float nullProbabilityMax, NumberFormat nf) {
			if(Math.random() < nullProbabilityMax) return null;
			BigDecimal range = max.subtract(min);
			range = range.multiply(new BigDecimal(nf.format(Math.random()))).add(min);
			return range;
		}
		public static String getRandStr(int lenMin, int lenMax, float nullProbabilityMax) {
			if(Math.random() < nullProbabilityMax) return null;
			int len = Math.min(UUID_LEN, (int)((Math.random() * (lenMax - lenMin)) + lenMin));
			return UUID.randomUUID().toString().substring(0, len);
		}
		public static java.util.Date getRandDate(int daysAgoMin, int daysAgoMax, float nullProbabilityMax) {
			if(Math.random() < nullProbabilityMax) return null;
			long time = System.currentTimeMillis() - (DAY_MS * (long)(Math.random() * (daysAgoMax - daysAgoMin)));
			return new java.util.Date(time);
		}
}
