package uk.gov.dwp.health.ds1500controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import uk.gov.dwp.health.ds1500controller.domain.DSForm;
import uk.gov.dwp.health.ds1500controller.domain.exceptions.InvalidCharactersException;
import uk.gov.dwp.health.ds1500controller.domain.exceptions.InvalidJsonException;
import uk.gov.dwp.health.ds1500controller.utils.InputHelper;
import uk.gov.dwp.regex.NinoValidator;
import uk.gov.dwp.regex.PostCodeValidator;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

public class Ds1500JsonValidator {
  public static final String NINO_ERROR_MESSAGE = "Nino Validation Failed";
  private final InputHelper inputHelper = new InputHelper();

  private static final PhoneNumberUtil PHONE_UTIL = PhoneNumberUtil.getInstance();


  public DSForm validateAndTranslate(String jsonPayload) throws InvalidJsonException {
    DSForm form = new DSForm();
    JsonNode jsonNode;

    try {
      jsonNode = new ObjectMapper().readTree(jsonPayload);
    } catch (IOException e) {
      throw new InvalidJsonException(e);
    }

    String fullName = getMandatoryFieldFromJson(jsonNode, "patientName").toUpperCase(Locale.ROOT);
    Pattern validNamePattern = Pattern.compile("(^[A-Za-z][A-Za-z\\s\\-.']*?[A-Za-z]$)");
    String otherNames = getOtherNames(form, fullName.split(" "));
    form.setOtherNames(otherNames.trim());

    checkSurnameIsValid(form, validNamePattern);
    checkOtherNamesAreValid(form, validNamePattern);

    form.setAddress(getMandatoryFieldFromJson(jsonNode, "patientAddress").split("\n"));

    form.setPostcode(getMandatoryFieldFromJson(jsonNode, "patientPostcode"));
    checkPostcodeIsValid(form);

    String dateOfBirth =
        getMandatoryFieldFromJson(jsonNode, "patientDateOfBirth-day")
            + "/"
            + getMandatoryFieldFromJson(jsonNode, "patientDateOfBirth-month")
            + "/"
            + getMandatoryFieldFromJson(jsonNode, "patientDateOfBirth-year");
    checkValidDate(dateOfBirth);
    form.setDateOfBirth(dateOfBirth);

    checkAndSetDiagnosisDate(form, jsonNode);
    checkAndSetSpecialRulesDate(form, jsonNode);

    form.setnINumber(getFieldFromJson(jsonNode, "patientNino"));
    checkNinoIsValid(form);

    form.setDiagnosis(getMandatoryFieldFromJson(jsonNode, "diagnosis"));
    form.setOtherDiagnosis(getFieldFromJson(jsonNode, "otherDiagnoses"));
    form.setDiagnosisAware(getMandatoryFieldFromJson(jsonNode, "diagnosisAware"));
    form.setPatientAware(getMandatoryFieldFromJson(jsonNode, "patientAware"));

    form.setClinicalFeatures(getMandatoryFieldFromJson(jsonNode, "clinicalFeatures"));
    form.setTreatment(getMandatoryFieldFromJson(jsonNode, "treatment"));
    String declaration = getMandatoryFieldFromJson(jsonNode, "declaration");
    form.setDeclaration(declaration);

    checkGMCNumberIsValid(form, jsonNode, declaration);

    form.setDeclarerName(getMandatoryFieldFromJson(jsonNode, "gpName"));
    form.setDeclarerAddress(getMandatoryFieldFromJson(jsonNode, "gpAddress"));
    form.setDeclarerPostcode(getMandatoryFieldFromJson(jsonNode, "gpPostcode"));
    form.setDeclarerPhoneNumber(getMandatoryFieldFromJson(jsonNode, "gpPhone"));
    checkDeclarerPhoneNumberIsValid(form);

    DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
    String today = formatter.format(new Date());
    form.setDeclarationDate(today);

    return form;
  }

  private void checkGMCNumberIsValid(DSForm form, JsonNode jsonNode, String declaration)
      throws InvalidJsonException {
    if ("General Practitioner".equals(declaration)
        || "GMC registered consultant".equals(declaration)) {
      form.setGmcNumber(Integer.parseInt(getMandatoryFieldFromJson(jsonNode, "gmcNumber")));
      validateGmcNumber(form);
      form.setOther("");
    } else if (!"Specialist nurse".equals(declaration)) {
      form.setOther(getMandatoryFieldFromJson(jsonNode, "declarationAdditionalDetail"));
    }
  }

  private void checkNinoIsValid(DSForm form) throws InvalidJsonException {
    if (form.getnINumber() != null && !form.getnINumber().isEmpty()) {
      form.setnINumber(form.getnINumber().toUpperCase(Locale.ROOT));
      if (!NinoValidator.validateNINO(form.getnINumber())) {
        throw new InvalidJsonException(NINO_ERROR_MESSAGE);
      }
    }
  }

  private void checkPostcodeIsValid(DSForm form) throws InvalidJsonException {
    if (!PostCodeValidator.validateInput(form.getPostcode())) {
      throw new InvalidJsonException(
          String.format("'patientPostcode' fails validation : '%s'", form.getPostcode()));
    }
  }

  private void checkOtherNamesAreValid(DSForm form, Pattern validNamePattern)
      throws InvalidJsonException {
    if (!form.getOtherNames().isEmpty()
        && !validNamePattern.matcher(form.getOtherNames()).matches()) {
      throw new InvalidJsonException(
          String.format("'patientName' contains invalid characters: '%s'", form.getOtherNames()));
    }
  }

  private void checkSurnameIsValid(DSForm form, Pattern validNamePattern)
      throws InvalidJsonException {
    if (form.getSurname().length() > 35) {
      throw new InvalidJsonException("'surname' is longer than the maximum allowable length");
    }

    if (!validNamePattern.matcher(form.getSurname()).matches()) {
      throw new InvalidJsonException(
          String.format("'patientName' contains invalid characters: '%s'", form.getSurname()));
    }
  }

  private void validateGmcNumber(DSForm form) throws InvalidJsonException {
    if (form.getGmcNumber() > 9999999 || form.getGmcNumber() <= 0) {
      throw new InvalidJsonException(
          String.format(
              "'gmcNumber' must be a positive 7 digit number less than 9999999; was %d",
              form.getGmcNumber()));
    }
  }

  private String getOtherNames(DSForm form, String[] allNames) {
    StringBuilder otherNames = new StringBuilder();
    if (allNames.length >= 1) {
      form.setSurname(allNames[allNames.length - 1]);
      for (int i = 0; i < allNames.length - 1; i++) {
        otherNames.append(allNames[i]).append(" ");
      }
    }
    return otherNames.toString();
  }

  private void checkAndSetDiagnosisDate(DSForm form, JsonNode jsonNode)
      throws InvalidJsonException {
    String dayField = getMandatoryFieldFromJson(jsonNode, "dateOfDiagnosis-day");
    String yearField = getMandatoryFieldFromJson(jsonNode, "dateOfDiagnosis-year");
    String monthField = getMandatoryFieldFromJson(jsonNode, "dateOfDiagnosis-month");
    String diagnosisDate = dayField + "/" + monthField + "/" + yearField;

    String buildDateForChecking = dayField + "/" + monthField + "/" + yearField;
    checkValidDate(buildDateForChecking);

    String birthDay = getMandatoryFieldFromJson(jsonNode, "patientDateOfBirth-day");
    String birthMonth = getMandatoryFieldFromJson(jsonNode, "patientDateOfBirth-month");
    String birthYear = getMandatoryFieldFromJson(jsonNode, "patientDateOfBirth-year");

    if ((Integer.parseInt(birthYear) > Integer.parseInt(yearField))
        || ((Integer.valueOf(yearField).equals(Integer.valueOf(birthYear)))
            && (Integer.parseInt(monthField) < Integer.parseInt(birthMonth)))
        || (Integer.valueOf(yearField).equals(Integer.valueOf(birthYear))
            && Integer.valueOf(monthField).equals(Integer.valueOf(birthMonth))
            && Integer.valueOf(birthDay) > Integer.valueOf(dayField))) {
      throw new InvalidJsonException("Date of diagnosis cannot be earlier than Patient DOB");
    }

    form.setDiagnosisDate(diagnosisDate);
  }

  private void checkAndSetSpecialRulesDate(DSForm form, JsonNode jsonNode)
          throws InvalidJsonException {
    String yearField = getMandatoryFieldFromJson(jsonNode, "dateOfSpecialRules-year");
    String monthField = getMandatoryFieldFromJson(jsonNode, "dateOfSpecialRules-month");
    String dayField = getMandatoryFieldFromJson(jsonNode, "dateOfSpecialRules-day");
    String specialRulesDate = dayField + "/" + monthField + "/" + yearField;

    String buildDateForChecking = dayField + "/" + monthField + "/" + yearField;
    checkValidDate(buildDateForChecking);

    String diagnosisDay = getMandatoryFieldFromJson(jsonNode, "dateOfDiagnosis-day");
    String diagnosisYear = getMandatoryFieldFromJson(jsonNode, "dateOfDiagnosis-year");
    String diagnosisMonth = getMandatoryFieldFromJson(jsonNode, "dateOfDiagnosis-month");

    if ((Integer.parseInt(diagnosisYear) > Integer.parseInt(yearField))
            || ((Integer.valueOf(yearField).equals(Integer.valueOf(diagnosisYear)))
            && (Integer.parseInt(monthField) < Integer.parseInt(diagnosisMonth)))
            || (Integer.valueOf(yearField).equals(Integer.valueOf(diagnosisYear))
            && Integer.valueOf(monthField).equals(Integer.valueOf(diagnosisMonth))
            && Integer.valueOf(diagnosisDay) > Integer.valueOf(dayField))) {
      throw new
           InvalidJsonException("Date of special rules cannot be earlier than Date of diagnosis");
    }

    form.setSpecialDate(specialRulesDate);
  }

  private void checkDeclarerPhoneNumberIsValid(DSForm form) throws InvalidJsonException {
    Phonenumber.PhoneNumber tel;
    try {
      tel = PHONE_UTIL.parse(form.getDeclarerPhoneNumber(), "GB");
    } catch (NumberParseException e) {
      throw new InvalidJsonException("Invalid format for GP Phone number");
    }

    if (!PHONE_UTIL.isValidNumber(tel)) {
      throw new InvalidJsonException("Invalid format for GP Phone number");
    }
  }

  private String getFieldFromJson(JsonNode jsonNode, String fieldName) throws InvalidJsonException {
    if (jsonNode.get(fieldName) != null) {
      try {
        return inputHelper.cleanInput(jsonNode.get(fieldName).textValue());
      } catch (InvalidCharactersException e) {
        throw new InvalidJsonException(e);
      }
    } else {
      return null;
    }
  }

  private String getMandatoryFieldFromJson(JsonNode jsonNode, String fieldName)
      throws InvalidJsonException {
    if (jsonNode.get(fieldName) != null) {
      String fieldValue = null;
      try {
        fieldValue = inputHelper.cleanInput(jsonNode.get(fieldName).textValue());
      } catch (InvalidCharactersException e) {
        throw new InvalidJsonException(e);
      }
      if (!fieldValue.trim().isEmpty()) {
        return fieldValue;
      }
    }
    throw new InvalidJsonException(fieldName + " is a mandatory field");
  }

  private void checkValidDate(String date) throws InvalidJsonException {
    Date dateValue = stringToDate(date);
    SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy");
    String year = yearFormat.format(dateValue);
    if (Integer.parseInt(year) < 1890) {
      throw new InvalidJsonException("Date is invalid");
    }
    if (dateValue.after(new Date())) {
      throw new InvalidJsonException("Date is in the future");
    }
  }

  private Date stringToDate(String date) throws InvalidJsonException {
    DateFormat df = new SimpleDateFormat("dd/MM/yyy");
    df.setLenient(false);
    Date dateValue;
    try {
      dateValue = df.parse(date);

    } catch (ParseException e) {
      throw new InvalidJsonException(e);
    }

    return dateValue;
  }
}
