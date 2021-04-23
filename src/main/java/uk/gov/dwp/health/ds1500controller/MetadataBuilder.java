package uk.gov.dwp.health.ds1500controller;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.dwp.health.ds1500controller.domain.DSForm;
import uk.gov.dwp.health.ds1500controller.domain.Ds1500Metadata;
import uk.gov.dwp.regex.InvalidNinoException;
import uk.gov.dwp.regex.NinoValidator;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public class MetadataBuilder {
  private static final Logger LOG = LoggerFactory.getLogger(MetadataBuilder.class);

  public Ds1500Metadata buildPayload(DSForm form, LocalDate submissionDate)
      throws InvalidNinoException {
    Ds1500Metadata metadata = new Ds1500Metadata();

    metadata.setBusinessUnitID("20");
    metadata.setClassification(0);
    metadata.setDocumentType(1242);
    metadata.setDocumentSource(4);
    metadata.setClaimRef(RandomStringUtils.randomAlphanumeric(30));

    if (form.getSurname() != null) {
      metadata.setSurname(form.getSurname());
    }
    if (form.getOtherNames() != null) {
      metadata.setForename(form.getOtherNames());
    }
    if (form.getPostcode() != null) {
      metadata.setPostCode(form.getPostcode());
    }
    if (form.getnINumber() != null && NinoValidator.validateNINO(form.getnINumber())) {
      metadata.setNino(new NinoValidator(form.getnINumber()));
    }
    if (form.getDateOfBirth() != null) {
      setApplicationTargetUnit(form.getDateOfBirth(), submissionDate, metadata);
      metadata.setDateOfBirth(buildDOB(form.getDateOfBirth()));
    }

    return metadata;
  }

  private int buildDOB(String dateOfBirth) {
    int dobValue;
    try {
      LocalDate dateTime = LocalDate.parse(dateOfBirth, DateTimeFormatter.ofPattern("d/M/yyyy"));
      String constructedDOB =
          Integer.toString(dateTime.getYear())
              + ""
              + String.format("%02d", dateTime.getMonthValue())
              + ""
              + String.format("%02d", dateTime.getDayOfMonth());
      dobValue = Integer.parseInt(constructedDOB);
    } catch (DateTimeParseException e) {
      LOG.error("Unable to format {}", dateOfBirth);
      LOG.debug(e.getClass().getName(), e);
      dobValue = 0;
    }

    return dobValue;
  }

  private void setApplicationTargetUnit(
      String applicantDOB, LocalDate submissionDate, Ds1500Metadata headerInfo) {
    try {
      LocalDate birthDate =
          LocalDate.parse(applicantDOB, DateTimeFormatter.ofPattern("d/M/yyyy", Locale.ENGLISH));

      if (Period.between(birthDate, submissionDate).getYears() < 16) {
        headerInfo.setBusinessUnitID("29");
        headerInfo.setBenefitType(23);
        headerInfo.setOfficePostcode("DL19QX");
      } else if (Period.between(birthDate, submissionDate).getYears() < 65) {
        headerInfo.setBusinessUnitID("20");
        headerInfo.setBenefitType(7);
      } else {
        headerInfo.setBusinessUnitID("34");
        headerInfo.setBenefitType(36);
        headerInfo.setOfficePostcode("AA19QX");
      }
    } catch (DateTimeParseException e) {
      LOG.error("Unable to parse {} :: {}", applicantDOB, e.getMessage());
      LOG.debug(e.getClass().getName(), e);
    }
  }
}
