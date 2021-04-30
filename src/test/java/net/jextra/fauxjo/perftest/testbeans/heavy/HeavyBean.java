package net.jextra.fauxjo.perftest.testbeans.heavy;

import net.jextra.fauxjo.bean.Fauxjo;
import net.jextra.fauxjo.bean.FauxjoField;
import java.math.BigDecimal;
import java.sql.Timestamp;

/** * Fauxjo JavaBean representing a car rental for many-column performance test. */
public class HeavyBean extends Fauxjo implements java.io.Serializable {
	static final long serialVersionUID = 42L;

	// ============================================================
	// Constructors
	// ============================================================

	public HeavyBean() { }

	// ============================================================
	// Fields
	// ============================================================

	@FauxjoField("key")
	private String key;

	@FauxjoField("franchiseid")
	private String franchiseId;

	@FauxjoField("renterid")
	private String renterId;

	@FauxjoField("renteraccountnumber")
	private String renterAccountNumber;

	@FauxjoField("refererid")
	private String refererId;

	@FauxjoField("returnLocid")
	private String returnLocId;

	@FauxjoField("signedkey")
	private String signedKey;

	@FauxjoField("refkey")
	private String refKey;

	@FauxjoField("canceldate")
	private Timestamp cancelDate;

	@FauxjoField("cancelcode")
	private Integer cancelCode;

	@FauxjoField("cancelreason")
	private String cancelReason;

	@FauxjoField("signedtimeZone")
	private String signedTimeZone;

	@FauxjoField("signeddate")
	private Timestamp signedDate;

	@FauxjoField("rentalType")
	private String rentalType;

	@FauxjoField("rentalTypeexpirationdate")
	private Timestamp rentalTypeExpirationDate;

	@FauxjoField("rentalTypelotnum")
	private String rentalTypeLotNum;

	@FauxjoField("referrerorganizationname")
	private String referrerOrganizationName;

	@FauxjoField("outwardtrackingnumber")
	private String outwardTrackingNumber;

	@FauxjoField("inwardtrackingnumber")
	private String inwardTrackingNumber;

	@FauxjoField("entrydate")
	private Timestamp entryDate;

	@FauxjoField("rentalrequestDate")
	private Timestamp rentalRequestDate;

	@FauxjoField("rentalweightkg")
	private BigDecimal rentalWeightKG;

	@FauxjoField("renterpickupdate")
	private Timestamp renterPickupDate;

	@FauxjoField("rentalreleasedate")
	private Timestamp rentalReleaseDate;

	@FauxjoField("deliveredtoreturnLocdate")
	private Timestamp deliveredToReturnLocDate;

	@FauxjoField("receivedatreturnLocdate")
	private Timestamp receivedAtReturnLocDate;

	@FauxjoField("inspectiondate")
	private Timestamp inspectionDate;

	@FauxjoField("inspectionvalue")
	private String inspectionValue;

	@FauxjoField("inspectioncomment")
	private String inspectionComment;


	@FauxjoField("rentalTypemaintkey")
	private String rentalTypeMaintKey;

	// ============================================================
	// Methods
	// ============================================================

	public String getKey() {
		return key;
	}
	public void setKey(String keyP) {
		key = keyP;
	}
	public String getFranchiseId() {
		return franchiseId;
	}
	public void setFranchiseId(String franchiseIdP) {
		franchiseId = franchiseIdP;
	}
	public String getRenterId() {
		return renterId;
	}
	public void setRenterId(String renterIdP) {
		renterId = renterIdP;
	}
	public String getRenterAccountNumber() {
		return renterAccountNumber;
	}
	public void setRenterAccountNumber(String patientAccountNumberP) {
		renterAccountNumber = patientAccountNumberP;
	}
	public String getRefererId() {
		return refererId;
	}
	public void setRefererId(String refererIdP) {
		refererId = refererIdP;
	}
	public String getReturnLocId() {
		return returnLocId;
	}
	public void setReturnLocId(String returnLocIdP) {
		returnLocId = returnLocIdP;
	}
	public String getSignedKey() {
		return signedKey;
	}
	public void setSignedKey(String signedKeyP) {
		signedKey = signedKeyP;
	}
	public String getRefKey() {
		return refKey;
	}
	public void setRefKey(String refKeyP) {
		refKey = refKeyP;
	}
	public Timestamp getCancelDate() {
		return cancelDate;
	}
	public void setCancelDate(Timestamp cancelDateP) {
		cancelDate = cancelDateP;
	}
	public Integer getCancelCode() {
		return cancelCode;
	}
	public void setCancelCode(Integer cancelCodeP) {
		cancelCode = cancelCodeP;
	}
	public String getCancelReason() {
		return cancelReason;
	}
	public void setCancelReason(String cancelReasonP) {
		cancelReason = cancelReasonP;
	}
	public String getSignedTimeZone() {
		return signedTimeZone;
	}
	public void setSignedTimeZone(String signedTimeZoneP) {
		signedTimeZone = signedTimeZoneP;
	}
	public Timestamp getSignedDate() {
		return signedDate;
	}
	public void setSignedDate(Timestamp signedDateP) {
		signedDate = signedDateP;
	}
	public String getRentalType() {
		return rentalType;
	}
	public void setRentalType(String rentalTypeP) {
		rentalType = rentalTypeP;
	}
	public Timestamp getRentalTypeExpirationDate() {
		return rentalTypeExpirationDate;
	}
	public void setRentalTypeExpirationDate(Timestamp rentalTypeExpirationDateP) { rentalTypeExpirationDate = rentalTypeExpirationDateP; }
	public String getRentalTypeLotNum() {
		return rentalTypeLotNum;
	}
	public void setRentalTypeLotNum(String rentalTypeLotNumP) {
		rentalTypeLotNum = rentalTypeLotNumP;
	}
	public String getReferrerOrganizationName() {
		return referrerOrganizationName;
	}
	public void setReferrerOrganizationName(String referrerOrganizationNameP) { referrerOrganizationName = referrerOrganizationNameP; }
	public String getOutwardTrackingNumber() {
		return outwardTrackingNumber;
	}
	public void setOutwardTrackingNumber(String outwardTrackingNumberP) { outwardTrackingNumber = outwardTrackingNumberP; }
	public String getInwardTrackingNumber() {
		return inwardTrackingNumber;
	}
	public void setInwardTrackingNumber(String inwardTrackingNumberP) {
		inwardTrackingNumber = inwardTrackingNumberP;
	}
	public Timestamp getEntryDate() {
		return entryDate;
	}
	public void setEntryDate(Timestamp entryDateP) {
		entryDate = entryDateP;
	}
	public Timestamp getRentalRequestDate() {
		return rentalRequestDate;
	}
	public void setRentalRequestDate(Timestamp rentalRequestDateP) {
		rentalRequestDate = rentalRequestDateP;
	}

	public BigDecimal getRentalWeightKG() { return rentalWeightKG; }
	public void setRentalWeightKG(BigDecimal rentalWeightKGP) { rentalWeightKG = rentalWeightKGP; }

	public Timestamp getRenterPickupDate() {
		return renterPickupDate;
	}
	public void setRenterPickupDate(Timestamp renterPickupDateP) {
		renterPickupDate = renterPickupDateP;
	}
	public Timestamp getRentalReleaseDate() {
		return rentalReleaseDate;
	}
	public void setRentalReleaseDate(Timestamp rentalReleaseDateP) {
		rentalReleaseDate = rentalReleaseDateP;
	}
	public Timestamp getDeliveredToReturnLocDate() {
		return deliveredToReturnLocDate;
	}
	public void setDeliveredToReturnLocDate(Timestamp deliveredToReturnLocDateP) { deliveredToReturnLocDate = deliveredToReturnLocDateP; }
	public Timestamp getReceivedAtReturnLocDate() {
		return receivedAtReturnLocDate;
	}
	public void setReceivedAtReturnLocDate(Timestamp receivedAtReturnLocDateP) {
		receivedAtReturnLocDate = receivedAtReturnLocDateP;
	}
	public Timestamp getInspectionDate() {
		return inspectionDate;
	}
	public void setInspectionDate(Timestamp inspectionDateP) {
		inspectionDate = inspectionDateP;
	}
	public String getInspectionValue() {
		return inspectionValue;
	}
	public void setInspectionValue(String inspectionValueP) {
		inspectionValue = inspectionValueP;
	}
	public String getInspectionComment() {
		return inspectionComment;
	}
	public void setInspectionComment(String inspectionCommentP) {
		inspectionComment = inspectionCommentP;
	}
	public String getRentalTypeMaintKey() {
		return rentalTypeMaintKey;
	}
	public void setRentalTypeMaintKey(String bundleKeyP) {
		rentalTypeMaintKey = bundleKeyP;
	}

	/** * Return a subset cols with various data types for reporting. */
	@Override
	public String toString() {
		return "HB{" +
			"key='" + key + '\'' +
			", franchiseId='" + franchiseId + '\'' +
			", renterId='" + renterId + '\'' +
			", signedDt=" + signedDate +
			", rentalWeightKG=" + rentalWeightKG +
			", cancelCd=" + cancelCode +
			'}';
	}
}
