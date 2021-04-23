package uk.gov.dwp.health.ds1500controller.domain;

import uk.gov.dwp.components.drs.Metadata;
import uk.gov.dwp.regex.NinoValidator;

public class Ds1500Metadata extends Metadata {
  private NinoValidator nino;
  private int dateOfBirth;
  private String surname;
  private String forename;
  private String postCode;
  private String lOBCaseID = "";
  private String officePostcode;
  private int benefitType = 7;
  private String claimRef;

  public String getClaimRef() {
    return claimRef;
  }

  public void setClaimRef(String claimRef) {
    this.claimRef = claimRef;
  }

  public NinoValidator getNino() {
    return nino;
  }

  public void setNino(NinoValidator nino) {
    this.nino = nino;
  }

  public int getDateOfBirth() {
    return dateOfBirth;
  }

  public void setDateOfBirth(int dateOfBirth) {
    this.dateOfBirth = dateOfBirth;
  }

  public String getSurname() {
    return surname;
  }

  public void setSurname(String surname) {
    this.surname = surname;
  }

  public String getForename() {
    return forename;
  }

  public void setForename(String forename) {
    this.forename = forename;
  }

  public String getPostCode() {
    return postCode;
  }

  public void setPostCode(String postCode) {
    this.postCode = postCode;
  }

  public String getLOBCaseID() {
    return lOBCaseID;
  }

  public void setLOBCaseID(String lOBCaseID) {
    this.lOBCaseID = lOBCaseID;
  }

  public String getOfficePostcode() {
    return officePostcode;
  }

  public void setOfficePostcode(String officePostcode) {
    this.officePostcode = officePostcode;
  }

  public int getBenefitType() {
    return benefitType;
  }

  public void setBenefitType(int benefitType) {
    this.benefitType = benefitType;
  }
}
