package net.jextra.fauxjo.rsdef;

import java.util.*;

/**
 * Defines the RSColDefs of a ResultSet. Adapted from net.jextra.fauxjo.BeanDef
 * <p>
 * EXPERIMENTAL: DO NOT USE IN PRODUCTION
 * Retrieving metadata is slow. This object stores the info from jdbc ResultSet columns so it can be cached.
 * Contains ColDefs that are unique to a ResultSet (hashcode).
 */
public class RSDef {

	// ============================================================
	// Fields
	// ============================================================

	private final LinkedHashMap<String, ColDef> _colDefs;
	private final int _rsHashcode; //ResultSet hashcode is the key for this Object (see hashcode and equals)
	private final long _createdOnTimestamp = System.currentTimeMillis();

	// ============================================================
	// Constructors
	// ============================================================

	/**
	 * Construct a new representation of a ResultSet.
	 * @param resultSetHashcode is the hashcode from a ResultSet
	 */
	public RSDef(int resultSetHashcode, List<ColDef> colDefList) {
		_rsHashcode = resultSetHashcode;
		_colDefs = new LinkedHashMap<>();
		for(ColDef col: colDefList) _colDefs.put(col.getName(), col);
	}

	/** Construct a new representation of a ResultSet from a copy of another
	 * taking a reference to its ColDefs to avoid creating new objects.
	 * @param resultSetHashcode for the source ResultSet
	 * @param rsDef to copy
	 */
	public RSDef(int resultSetHashcode, RSDef rsDef) {
		_rsHashcode = resultSetHashcode;
		_colDefs = rsDef._colDefs; //reference
	}

	// ============================================================
	// Methods
	// ============================================================

	// ----------
	// public
	// ----------

	public int getRSHashcode() {
		return _rsHashcode;
	}

	public boolean containsCol(String colname) {
		return _colDefs.containsKey(colname);
	}

	/** * Return a copy of the internal map of ColDefs */
	public Collection<ColDef> getColDefs() {
		return _colDefs.values();
	}

	/**
	 * Return true if o is the same object or if the same class and <code>o.getRSHashcode() == getRSHashcode()</code>
	 * @param o the other object to compare with this
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RSDef rsDef = (RSDef) o;
		return _rsHashcode == rsDef._rsHashcode;
	}

	@Override
	public int hashCode() {
		return Objects.hash(_rsHashcode);
	}

	public long getCreatedOnTimestamp() {
		return _createdOnTimestamp;
	}

	/**
	 * Return true if this was created before the specified timeToLive.
	 * createdOnTimestamp before the duration <code>System.currentTimeMillis() - duration</code>.
	 * @param timeToLive in milliseconds
	 */
	public boolean isOlderThan(long timeToLive) {
		return getCreatedOnTimestamp() < System.currentTimeMillis() - timeToLive;
	}

}
