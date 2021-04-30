package net.jextra.fauxjo.perftest.testbeans.heavy;

import net.jextra.fauxjo.perftest.jdbc.Bean_NoRefl;
import net.jextra.fauxjo.perftest.jdbc.BeanField_NoRefl;

import java.math.BigDecimal;
import java.sql.JDBCType;
import java.sql.ResultSet;
import java.util.LinkedHashMap;

/**
 * Static mapping deserializes jdbc ResultSet data into a pojo ONLY AS A BEST CASE PERFORMANCE BASELINE.
 * Use to compare reflection-based deserialization with this non-reflection serialization.
 * BEST-PERFORMANCE BASELINE, STATIC MAPPING IS DIFFICULT TO MAINTAIN, DO NOT DO THIS.
 * HeavyBean with many fields (compared to LightBean) ensures tests represent a variety of bean sizes.
 */
public class HeavyBeanNoRefl extends HeavyBean implements Bean_NoRefl, java.io.Serializable {
	static final long serialVersionUID = 42L;

	//Hypothesis that reflection cost is in lkup, mapping the field may perform the same as setters and allow coercers?
	//public static final NoRefl_BeanField FC_id     = new NoRefl_BeanField("id",     false).setDType(JDBCType.INTEGER).setJClazz(Integer.class);
	//BEST-PERFORMANCE BASELINE, STATIC MAPPING IS DIFFICULT TO MAINTAIN, DO NOT DO THIS.
	public static final BeanField_NoRefl FC_key                      = new BeanField_NoRefl("key"                     , false, JDBCType.VARCHAR  , String.class      );
	public static final BeanField_NoRefl FC_franchiseId              = new BeanField_NoRefl("franchiseId"             , false, JDBCType.VARCHAR  , String.class      );
	public static final BeanField_NoRefl FC_renterId                 = new BeanField_NoRefl("renterId"                , false, JDBCType.VARCHAR  , String.class      );
	public static final BeanField_NoRefl FC_renterAccountNumber      = new BeanField_NoRefl("renterAccountNumber"     , false, JDBCType.VARCHAR  , String.class      );
	public static final BeanField_NoRefl FC_refererId                = new BeanField_NoRefl("refererId"               , false, JDBCType.VARCHAR  , String.class      );
	public static final BeanField_NoRefl FC_returnLocId              = new BeanField_NoRefl("returnLocId"             , false, JDBCType.VARCHAR  , String.class      );
	public static final BeanField_NoRefl FC_signedKey                = new BeanField_NoRefl("signedKey"               , false, JDBCType.VARCHAR  , String.class      );
	public static final BeanField_NoRefl FC_refKey                   = new BeanField_NoRefl("refKey"                  , false, JDBCType.VARCHAR  , String.class      );
	public static final BeanField_NoRefl FC_cancelDate               = new BeanField_NoRefl("cancelDate"              , false, JDBCType.TIMESTAMP, java.util.Date.class   );
	public static final BeanField_NoRefl FC_cancelCode               = new BeanField_NoRefl("cancelCode"              , false, JDBCType.INTEGER  , Integer.class     );
	public static final BeanField_NoRefl FC_cancelReason             = new BeanField_NoRefl("cancelReason"            , false, JDBCType.VARCHAR  , String.class      );
	public static final BeanField_NoRefl FC_signedTimeZone           = new BeanField_NoRefl("signedTimeZone"          , false, JDBCType.VARCHAR  , String.class      );
	public static final BeanField_NoRefl FC_signedDate               = new BeanField_NoRefl("signedDate"              , false, JDBCType.TIMESTAMP, java.util.Date.class   );
	public static final BeanField_NoRefl FC_rentalType               = new BeanField_NoRefl("rentalType"              , false, JDBCType.VARCHAR  , String.class      );
	public static final BeanField_NoRefl FC_rentalTypeExpirationDate = new BeanField_NoRefl("rentalTypeExpirationDate", false, JDBCType.TIMESTAMP, java.util.Date.class   );
	public static final BeanField_NoRefl FC_rentalTypeLotNum         = new BeanField_NoRefl("rentalTypeLotNum"        , false, JDBCType.VARCHAR  , String.class      );
	public static final BeanField_NoRefl FC_referrerOrganizationName = new BeanField_NoRefl("referrerOrganizationName", false, JDBCType.VARCHAR  , String.class      );
	public static final BeanField_NoRefl FC_outwardTrackingNumber    = new BeanField_NoRefl("outwardTrackingNumber"   , false, JDBCType.VARCHAR  , String.class      );
	public static final BeanField_NoRefl FC_inwardTrackingNumber     = new BeanField_NoRefl("inwardTrackingNumber"    , false, JDBCType.VARCHAR  , String.class      );
	public static final BeanField_NoRefl FC_entryDate                = new BeanField_NoRefl("entryDate"               , false, JDBCType.TIMESTAMP, java.util.Date.class   );
	public static final BeanField_NoRefl FC_rentalRequestDate        = new BeanField_NoRefl("rentalRequestDate"       , false, JDBCType.TIMESTAMP, java.util.Date.class   );
	public static final BeanField_NoRefl FC_rentalWeightKG           = new BeanField_NoRefl("rentalWeightKG"          , false, JDBCType.NUMERIC  , BigDecimal.class  );
	public static final BeanField_NoRefl FC_renterPickupDate         = new BeanField_NoRefl("renterPickupDate"        , false, JDBCType.TIMESTAMP, java.util.Date.class   );
	public static final BeanField_NoRefl FC_rentalReleaseDate        = new BeanField_NoRefl("rentalReleaseDate"       , false, JDBCType.TIMESTAMP, java.util.Date.class   );
	public static final BeanField_NoRefl FC_deliveredToReturnLocDate = new BeanField_NoRefl("deliveredToReturnLocDate", false, JDBCType.TIMESTAMP, java.util.Date.class   );
	public static final BeanField_NoRefl FC_receivedAtReturnLocDate  = new BeanField_NoRefl("receivedAtReturnLocDate" , false, JDBCType.TIMESTAMP, java.util.Date.class   );
	public static final BeanField_NoRefl FC_inspectionDate           = new BeanField_NoRefl("inspectionDate"          , false, JDBCType.TIMESTAMP, java.util.Date.class   );
	public static final BeanField_NoRefl FC_inspectionValue          = new BeanField_NoRefl("inspectionValue"         , false, JDBCType.VARCHAR  , String.class      );
	public static final BeanField_NoRefl FC_inspectionComment        = new BeanField_NoRefl("inspectionComment"       , false, JDBCType.VARCHAR  , String.class      );
	public static final BeanField_NoRefl FC_rentalTypeMaintKey       = new BeanField_NoRefl("rentalTypeMaintKey"      , false, JDBCType.VARCHAR  , String.class      );
	/**
	 * Return a map of FauxCol by name so caller can set the colIndex from ResultSetMetaData.
	 * BEST-PERFORMANCE BASELINE, STATIC MAPPING IS DIFFICULT TO MAINTAIN, DO NOT DO THIS.
	 * @param dstColDefMap will have cols added to it, typically will be a new LinkedHashMap.
	 */
	public LinkedHashMap<String, BeanField_NoRefl> getNoReflectCols(LinkedHashMap<String, BeanField_NoRefl> dstColDefMap) throws Exception {
		dstColDefMap.put(FC_key.getName(), FC_key);
		dstColDefMap.put(FC_franchiseId.getName(), FC_franchiseId);
		dstColDefMap.put(FC_renterId.getName(), FC_renterId);
		dstColDefMap.put(FC_renterAccountNumber.getName(), FC_renterAccountNumber);
		dstColDefMap.put(FC_refererId.getName(), FC_refererId);
		dstColDefMap.put(FC_returnLocId.getName(), FC_returnLocId);
		dstColDefMap.put(FC_signedKey.getName(), FC_signedKey);
		dstColDefMap.put(FC_refKey.getName(), FC_refKey);
		dstColDefMap.put(FC_cancelDate.getName(), FC_cancelDate);
		dstColDefMap.put(FC_cancelCode.getName(), FC_cancelCode);
		dstColDefMap.put(FC_cancelReason.getName(), FC_cancelReason);
		dstColDefMap.put(FC_signedTimeZone.getName(), FC_signedTimeZone);
		dstColDefMap.put(FC_signedDate.getName(), FC_signedDate);
		dstColDefMap.put(FC_rentalType.getName(), FC_rentalType);
		dstColDefMap.put(FC_rentalTypeExpirationDate.getName(), FC_rentalTypeExpirationDate);
		dstColDefMap.put(FC_rentalTypeLotNum.getName(), FC_rentalTypeLotNum);
		dstColDefMap.put(FC_referrerOrganizationName.getName(), FC_referrerOrganizationName);
		dstColDefMap.put(FC_outwardTrackingNumber.getName(), FC_outwardTrackingNumber);
		dstColDefMap.put(FC_inwardTrackingNumber.getName(), FC_inwardTrackingNumber);
		dstColDefMap.put(FC_entryDate.getName(), FC_entryDate);
		dstColDefMap.put(FC_rentalRequestDate.getName(), FC_rentalRequestDate);
		dstColDefMap.put(FC_rentalWeightKG.getName(), FC_rentalWeightKG);
		dstColDefMap.put(FC_renterPickupDate.getName(), FC_renterPickupDate);
		dstColDefMap.put(FC_rentalReleaseDate.getName(), FC_rentalReleaseDate);
		dstColDefMap.put(FC_deliveredToReturnLocDate.getName(), FC_deliveredToReturnLocDate);
		dstColDefMap.put(FC_receivedAtReturnLocDate .getName(), FC_receivedAtReturnLocDate);
		dstColDefMap.put(FC_inspectionDate.getName(), FC_inspectionDate);
		dstColDefMap.put(FC_inspectionValue.getName(), FC_inspectionValue);
		dstColDefMap.put(FC_inspectionComment.getName(), FC_inspectionComment);
		dstColDefMap.put(FC_rentalTypeMaintKey.getName(), FC_rentalTypeMaintKey);
		return dstColDefMap;
	}

	/**
	 * Determine if direct setters are ~3X faster by removing reflection and an intermediate hashmap.
	 * If feasible, can look at codegen+JIT based on annotations for non-breaking changes.
	 * BEST-PERFORMANCE BASELINE, STATIC MAPPING IS DIFFICULT TO MAINTAIN, DO NOT DO THIS.
	 * @param rs from db
	 * @param rowIndex optional, used for exception reporting
	 */
	public void deserialize(ResultSet rs, LinkedHashMap<String, BeanField_NoRefl> dstColDefMapWithRSindexes, Object rowIndex) throws Exception {
		int colIndex = 0;
		BeanField_NoRefl col = null;
		try {
			for(BeanField_NoRefl c: dstColDefMapWithRSindexes.values()) {
				col = c;
				colIndex = c.getIndex();  //does not use reflection
				       if (col == FC_key                      ) { setKey(rs.getString(colIndex));
				} else if (col == FC_franchiseId              ) { setFranchiseId(rs.getString(colIndex));
				} else if (col == FC_renterId                 ) { setRenterId(rs.getString(colIndex));
				} else if (col == FC_renterAccountNumber      ) { setRenterAccountNumber(rs.getString(colIndex));
				} else if (col == FC_refererId                ) { setRefererId(rs.getString(colIndex));
				} else if (col == FC_returnLocId              ) { setReturnLocId(rs.getString(colIndex));
				} else if (col == FC_signedKey                ) { setSignedKey(rs.getString(colIndex));
				} else if (col == FC_refKey                   ) { setRefKey(rs.getString(colIndex));
				} else if (col == FC_cancelDate               ) { setCancelDate(rs.getTimestamp(colIndex));
				} else if (col == FC_cancelCode               ) { setCancelCode(rs.getInt(colIndex));
				} else if (col == FC_cancelReason             ) { setCancelReason(rs.getString(colIndex));
				} else if (col == FC_signedTimeZone           ) { setSignedTimeZone(rs.getString(colIndex));
				} else if (col == FC_signedDate               ) { setSignedDate(rs.getTimestamp(colIndex));
				} else if (col == FC_rentalType               ) { setRentalType(rs.getString(colIndex));
				} else if (col == FC_rentalTypeExpirationDate ) { setRentalTypeExpirationDate(rs.getTimestamp(colIndex));
				} else if (col == FC_rentalTypeLotNum         ) { setRentalTypeLotNum(rs.getString(colIndex));
				} else if (col == FC_referrerOrganizationName ) { setReferrerOrganizationName(rs.getString(colIndex));
				} else if (col == FC_outwardTrackingNumber    ) { setOutwardTrackingNumber(rs.getString(colIndex));
				} else if (col == FC_inwardTrackingNumber     ) { setInwardTrackingNumber(rs.getString(colIndex));
				} else if (col == FC_entryDate                ) { setEntryDate(rs.getTimestamp(colIndex));
				} else if (col == FC_rentalRequestDate        ) { setRentalRequestDate(rs.getTimestamp(colIndex));
				} else if (col == FC_rentalWeightKG           ) { setRentalWeightKG(rs.getBigDecimal(colIndex));
				} else if (col == FC_renterPickupDate         ) { setRenterPickupDate(rs.getTimestamp(colIndex));
				} else if (col == FC_rentalReleaseDate        ) { setRentalReleaseDate(rs.getTimestamp(colIndex));
				} else if (col == FC_deliveredToReturnLocDate ) { setDeliveredToReturnLocDate(rs.getTimestamp(colIndex));
				} else if (col == FC_receivedAtReturnLocDate  ) { setReceivedAtReturnLocDate(rs.getTimestamp(colIndex));
				} else if (col == FC_inspectionDate           ) { setInspectionDate(rs.getTimestamp(colIndex));
				} else if (col == FC_inspectionValue          ) { setInspectionValue(rs.getString(colIndex));
				} else if (col == FC_inspectionComment        ) { setInspectionComment(rs.getString(colIndex));
				} else if (col == FC_rentalTypeMaintKey       ) { setRentalTypeMaintKey(rs.getString(colIndex));
				}
			}
		} catch (Exception x) {
			throw new Exception("Deserialize[col][row] failed: " + (col != null ? "col: " + col: "") + "[" + rowIndex + "]", x);
		}
	}


}
