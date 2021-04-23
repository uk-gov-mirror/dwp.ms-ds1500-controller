package uk.gov.dwp.health.ds1500controller;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Test;
import uk.gov.dwp.health.ds1500controller.domain.DSForm;
import uk.gov.dwp.health.ds1500controller.domain.exceptions.InvalidJsonException;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.fail;

@SuppressWarnings({"squid:S1192", "squid:S00107", "squid:S3776", "java:S5961"}) // string literals allowed & multiple params allowed & complexity removed (for params)
public class Ds1500JsonValidatorTest {

    private static final String STRING_WITH_TOO_MANY_CHARACTERS = "\"abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefg hijk\"";
    private static final String STRING_WITH_INVALID_CHARACTERS = "\"&lt;script&gt;$(document).ready(function(){alert('malicious');});&lt;/script&gt;\"";
    private final Ds1500JsonValidator validator = new Ds1500JsonValidator();


    @Test
    public void confirmValidJsonIsTransformedFromGPConsultant() throws InvalidJsonException {

        DSForm dsForm = validator.validateAndTranslate(buildFullRequest());
        assertThat("Surname mismatch", dsForm.getSurname(), is("MAN"));
        assertThat("Forename mismatch", dsForm.getOtherNames(), is("FAKE"));
        assertThat("Address line 1 mismatch", dsForm.getAddress()[0], is("23 Fake Road"));
        assertThat("Address line 2 mismatch", dsForm.getAddress()[1], is("Fake Park"));
        assertThat("Address line 3 mismatch", dsForm.getAddress()[2], is("Fake"));
        assertThat("PostCode mismatch", dsForm.getPostcode(), is("S2 2RZ"));
        assertThat("DOB mismatch", dsForm.getDateOfBirth(), is("15/02/1972"));
        assertThat("Diagnosis mismatch", dsForm.getDiagnosisDate(), is("05/2015"));
        assertThat("NINO mismatch", dsForm.getnINumber(), is("AA370773A"));
        assertThat("Diagnosis mismatch", dsForm.getDiagnosis(), is("Extreme grumpiness"));
        assertThat("Other diagnosis mismatch", dsForm.getOtherDiagnosis(), is("Mild allergies to dub step"));
        assertThat("Patient Aware mismatch", dsForm.getPatientAware(), is("Yes"));
        assertThat("Form requestor mismatch", dsForm.getFormRequestor(), is("Representative"));
        assertThat("Representative line 1 mismatch", dsForm.getRepresentative1(), is("Representative name"));
        assertThat("Representative line 2 mismatch", dsForm.getRepresentative2(), is("21 Representative Road"));
        assertThat("Representative line 3 mismatch", dsForm.getRepresentative3(), is("NW1 3ER"));
        assertThat("Clinical features mismatch", dsForm.getClinicalFeatures(), is("Mr Coupe's clinical features"));
        assertThat("Treatment mismatch", dsForm.getTreatment(), is("Beer, loud music, fluffy kittens"));
        assertThat("Other treatment mismatch", dsForm.getOtherTreatment(), is("None whatsoever"));
        assertThat("Declaration mismatch", dsForm.getDeclaration(), is("General Practitioner"));
        assertThat("Other declaration person mismatch", dsForm.getOther(), is(""));
        assertThat("Declarer name mismatch", dsForm.getDeclarerName(), is("Dr. Michael Hfuhruhurr"));
        assertThat("Declarer address mismatch", dsForm.getDeclarerAddress(), is("Porter Brook Medical Centre"));
        assertThat("Declarer phone mismatch", dsForm.getDeclarerPhoneNumber(), is("0114 258 8520"));
        assertThat("Declaration date mismatch", dsForm.getDeclarationDate(), is(not(nullValue())));
        assertThat("GMC Number mismatch", dsForm.getGmcNumber(), is(1234567));
    }

    @Test
    public void confirmNameWithValidCharactersIsAccepted() throws InvalidJsonException {
        DSForm dsform = validator.validateAndTranslate((buildFullRequestWithMissingFieldGPConsultant("patientName", "\"mr eric scanlon-o'hanlon jnr\"")));
        assertThat("Patient Name", (dsform.getOtherNames() + " " + dsform.getSurname()), is("MR ERIC SCANLON-O'HANLON JNR"));
    }

    @Test
    public void confirmNameWithInvalidCharactersFailsAndThrowsAnException() {
        try {
            validator.validateAndTranslate(buildFullRequestWithMissingFieldGPConsultant("patientName", "\"* the gaul 3rd\""));
            fail("Name with invalid characters should throw an exception");
        } catch (InvalidJsonException e) {
            assertThat("error caught", e.getMessage(), containsString("'patientName' contains invalid characters: "));
        }
    }

    @Test
    public void confirmVariousNamesAreInvalidAndThrowsAnException() {
        String[] patientNames = {"\"Emma-Claire.\"",
            "\"John'  Test\"",
            "\"John'\"",
            "\"J. R. R. Tolkien\"",
            "\"Mr. J. Cale\""
        };

        for (String patient : patientNames) {
            try {

                validator.validateAndTranslate(buildFullRequestWithMissingFieldGPConsultant("patientName", patient));
                fail(String.format("Name with invalid characters should throw an exception for '%s'", patient));

            } catch (InvalidJsonException e) {
                assertThat("error caught", e.getMessage(), containsString("'patientName' contains invalid characters: "));
            }
        }
    }

    @Test
    public void confirmInvalidPatientPostcodeThrowsAnException() {
        try {
            validator.validateAndTranslate(buildFullRequestWithMissingFieldGPConsultant("patientPostcode", "\"123456789\""));
            fail("Postcode over 8 characters should throw an exception");
        } catch (InvalidJsonException e) {
            assertThat("error caught", e.getMessage(), containsString("'patientPostcode' fails validation"));
        }
    }

    @Test
    public void confirmNationalInsuranceNumberIsCapitalised() throws InvalidJsonException {
        DSForm dsForm = validator.validateAndTranslate(buildFullRequestWithMissingFieldGPConsultant("patientNino", "\"aa370773a\""));
        assertThat("NINO mismatch", dsForm.getnINumber(), is("AA370773A"));
    }

    @Test
    public void confirmNationalInsuranceNumberIsValidWithoutASuffixIsAccepted() throws InvalidJsonException {
        DSForm dsForm = validator.validateAndTranslate(buildFullRequestWithMissingFieldGPConsultant("patientNino", "\"AA370773A\""));
        assertThat("NINO mismatch", dsForm.getnINumber(), is("AA370773A"));
    }

    @Test
    public void confirmThatAnEmptyNationalInsuranceNumberIsAccepted() throws InvalidJsonException {
        DSForm dsform = validator.validateAndTranslate(buildFullRequestWithMissingFieldGPConsultant("patientNino", "\"\""));
        assertThat("NINO mismatch", dsform.getnINumber(), is(""));
    }

    @Test
    public void confirmOtherRelevantDiagnosisIsOptionalAndWillAcceptAnEmptyString() throws InvalidJsonException {
        DSForm dsform = validator.validateAndTranslate((buildFullRequestWithMissingFieldGPConsultant("otherDiagnoses", "\"\"")));
        assertThat("Other diagnosis mismatch", dsform.getOtherDiagnosis(), is(""));
    }

    @Test
    public void confirmOtherTreatmentIsOptionalAndWillAcceptAnEmptyString() throws InvalidJsonException {
        DSForm dsform = validator.validateAndTranslate((buildFullRequestWithMissingFieldGPConsultant("otherIntervention", "\"\"")));
        assertThat("Other intervention mismatch", dsform.getOtherTreatment(), is(""));
    }

    @Test
    public void confirmOtherTreatmentIsOptionalAndWillAcceptAnPopulatedString() throws InvalidJsonException {
        String expectedResult = "this is not blank";
        DSForm dsform = validator.validateAndTranslate((buildFullRequestWithMissingFieldGPConsultant("otherIntervention", "\"" + expectedResult + "\"")));
        assertThat("Other intervention mismatch", dsform.getOtherTreatment(), is(expectedResult));
    }

    @Test
    public void confirmThatABlankNationalInsuranceNumberIsAccepted() throws InvalidJsonException {
        DSForm dsform = validator.validateAndTranslate(buildFullRequestWithMissingFieldGPConsultant("patientNino", null));
        assertThat("NINO mismatch", dsform.getnINumber(), nullValue());
    }

    @Test
    public void confirmInvalidNationalInsuranceNumberFailsAndThrowsAnException() {
        try {
            validator.validateAndTranslate(buildFullRequestWithMissingFieldGPConsultant("patientNino", "\"Invalid National Insurance Number\""));
            fail("Invalid National Insurance Number");
        } catch (InvalidJsonException e) {
            assertThat("error caught", e.getMessage(), containsString(Ds1500JsonValidator.NINO_ERROR_MESSAGE));
        }
    }

    @Test
    public void confirmDiagnosisDateInSameMonthAndYearAsDOBIsValid() throws InvalidJsonException {
        String partialJsonResponse = "{\n" +
            "  \"patientName\":\"Pele\",\n" +
            "  \"patientAddress\":\"23 Fake Road\\nFake Park\\nFake\",\n" +
            "  \"patientPostcode\":\"S2 2RZ\",\n" +
            "  \"patientDateOfBirth-day\":\"15\",\n" +
            "  \"patientDateOfBirth-month\":\"02\",\n" +
            "  \"patientDateOfBirth-year\":\"1972\",\n" +
            "  \"diagnosis\":\"Extreme grumpiness\",\n" +
            "  \"dateOfDiagnosis-month\":\"02\",\n" +
            "  \"dateOfDiagnosis-year\":\"1972\",\n" +
            getReponseForOtherDiagnosesOnwards();
        DSForm dsForm = validator.validateAndTranslate(partialJsonResponse);
        assertThat(dsForm.getDiagnosisDate(), is("02/1972"));
    }

    @Test
    public void confirmDiagnosisDateInCurrentMonthIsValid() throws InvalidJsonException {
        LocalDate workingDate = LocalDate.now().plus(1, ChronoUnit.DAYS);

        if (workingDate.getMonthValue() > LocalDate.now().getMonthValue()) {
            workingDate = LocalDate.now(); // cover off the month being in the future
        }

        String dateMonth = workingDate.getMonthValue() < 10 ? String.format("0%d", workingDate.getMonthValue()) : String.valueOf(workingDate.getMonthValue());
        String dateDay = workingDate.getDayOfMonth() < 10 ? String.format("0%d", workingDate.getDayOfMonth()) : String.valueOf(workingDate.getDayOfMonth());

        String partialJsonResponse = String.format("{\n" +
            "  \"patientName\":\"Pele\",\n" +
            "  \"patientAddress\":\"23 Fake Road\\nFake Park\\nFake\",\n" +
            "  \"patientPostcode\":\"S2 2RZ\",\n" +
            "  \"patientDateOfBirth-day\":\"%s\",\n" +
            "  \"patientDateOfBirth-month\":\"%s\",\n" +
            "  \"patientDateOfBirth-year\":\"%d\",\n" +
            "  \"diagnosis\":\"Extreme grumpiness\",\n" +
            "  \"dateOfDiagnosis-month\":\"%s\",\n" +
            "  \"dateOfDiagnosis-year\":\"%d\",\n" +
            getReponseForOtherDiagnosesOnwards(), dateDay, dateMonth, workingDate.getYear() - 2, dateMonth, workingDate.getYear());

        DSForm dsForm = validator.validateAndTranslate(partialJsonResponse);
        assertThat(dsForm.getDiagnosisDate(), is(String.format("%s/%d", dateMonth, workingDate.getYear())));
    }

    @Test
    public void confirmDiagnosisDateInFebruaryWithDateOfBirthInOctoberIsValid() throws InvalidJsonException {
        String partialJsonResponse = "{\n" +
            "  \"patientName\":\"Pele\",\n" +
            "  \"patientAddress\":\"23 Fake Road\\nFake Park\\nFake\",\n" +
            "  \"patientPostcode\":\"S2 2RZ\",\n" +
            "  \"patientDateOfBirth-day\":\"31\",\n" +
            "  \"patientDateOfBirth-month\":\"10\",\n" +
            "  \"patientDateOfBirth-year\":\"1973\",\n" +
            "  \"diagnosis\":\"Extreme grumpiness\",\n" +
            "  \"dateOfDiagnosis-month\":\"02\",\n" +
            "  \"dateOfDiagnosis-year\":\"2012\",\n" +
            getReponseForOtherDiagnosesOnwards();

        validator.validateAndTranslate(partialJsonResponse);
    }

    @Test
    public void confirmDiagnosisDateInLaterMonthAndSameYearAsDOBIsValid() throws InvalidJsonException {
        String partialJsonResponse = "{\n" +
            "  \"patientName\":\"Pele\",\n" +
            "  \"patientAddress\":\"23 Fake Road\\nFake Park\\nFake\",\n" +
            "  \"patientPostcode\":\"S2 2RZ\",\n" +
            "  \"patientDateOfBirth-day\":\"15\",\n" +
            "  \"patientDateOfBirth-month\":\"02\",\n" +
            "  \"patientDateOfBirth-year\":\"1972\",\n" +
            "  \"diagnosis\":\"Extreme grumpiness\",\n" +
            "  \"dateOfDiagnosis-month\":\"03\",\n" +
            "  \"dateOfDiagnosis-year\":\"1972\",\n" +
            getReponseForOtherDiagnosesOnwards();
        DSForm dsForm = validator.validateAndTranslate(partialJsonResponse);
        assertThat(dsForm.getDiagnosisDate(), is("03/1972"));
    }

    @Test
    public void confirmDiagnosisDateInEarlierMonthAndLaterYearAsDOBIsValid() throws InvalidJsonException {
        String partialJsonResponse = "{\n" +
            "  \"patientName\":\"Pele\",\n" +
            "  \"patientAddress\":\"23 Fake Road\\nFake Park\\nFake\",\n" +
            "  \"patientPostcode\":\"S2 2RZ\",\n" +
            "  \"patientDateOfBirth-day\":\"15\",\n" +
            "  \"patientDateOfBirth-month\":\"02\",\n" +
            "  \"patientDateOfBirth-year\":\"1972\",\n" +
            "  \"diagnosis\":\"Extreme grumpiness\",\n" +
            "  \"dateOfDiagnosis-month\":\"01\",\n" +
            "  \"dateOfDiagnosis-year\":\"1973\",\n" +
            getReponseForOtherDiagnosesOnwards();
        DSForm dsForm = validator.validateAndTranslate(partialJsonResponse);
        assertThat(dsForm.getDiagnosisDate(), is("01/1973"));
    }

    @Test
    public void confirmDiagnosisInEarlierMonthAndSameYearAsDOBFailsAndThrowsAnException() {
        String partialJsonResponse = "{\n" +
            "  \"patientName\":\"Pele\",\n" +
            "  \"patientAddress\":\"23 Fake Road\\nFake Park\\nFake\",\n" +
            "  \"patientPostcode\":\"S2 2RZ\",\n" +
            "  \"patientDateOfBirth-day\":\"15\",\n" +
            "  \"patientDateOfBirth-month\":\"02\",\n" +
            "  \"patientDateOfBirth-year\":\"1972\",\n" +
            "  \"diagnosis\":\"Extreme grumpiness\",\n" +
            "  \"dateOfDiagnosis-month\":\"01\",\n" +
            "  \"dateOfDiagnosis-year\":\"1972\",\n" +
            getReponseForOtherDiagnosesOnwards();
        try {
            validator.validateAndTranslate(partialJsonResponse);
            fail("Date of Diagnosis earlier than DOB should fail and throw an exception");
        } catch (Exception e) {
            assertThat("error caught", e.getMessage(), containsString("Date of diagnosis cannot be earlier than Patient DOB"));
        }
    }

    @Test
    public void confirmDiagnosisInSameMonthAndEarlierYearAsDOBFailsAndThrowsAnException() {
        String partialJsonResponse = "{\n" +
            "  \"patientName\":\"Pele\",\n" +
            "  \"patientAddress\":\"23 Fake Road\\nFake Park\\nFake\",\n" +
            "  \"patientPostcode\":\"S2 2RZ\",\n" +
            "  \"patientDateOfBirth-day\":\"15\",\n" +
            "  \"patientDateOfBirth-month\":\"02\",\n" +
            "  \"patientDateOfBirth-year\":\"1972\",\n" +
            "  \"diagnosis\":\"Extreme grumpiness\",\n" +
            "  \"dateOfDiagnosis-month\":\"02\",\n" +
            "  \"dateOfDiagnosis-year\":\"1971\",\n" +
            getReponseForOtherDiagnosesOnwards();
        try {
            validator.validateAndTranslate(partialJsonResponse);
            fail("Date of Diagnosis earlier than DOB should fail and throw an exception");
        } catch (Exception e) {
            assertThat("error caught", e.getMessage(), containsString("Date of diagnosis cannot be earlier than Patient DOB"));
        }
    }

    @Test
    public void confirmDiagnosisInLaterMonthAndEarlierYearAsDOBFailsAndThrowsAnException() {
        String partialJsonResponse = "{\n" +
            "  \"patientName\":\"Pele\",\n" +
            "  \"patientAddress\":\"23 Fake Road\\nFake Park\\nFake\",\n" +
            "  \"patientPostcode\":\"S2 2RZ\",\n" +
            "  \"patientDateOfBirth-day\":\"15\",\n" +
            "  \"patientDateOfBirth-month\":\"02\",\n" +
            "  \"patientDateOfBirth-year\":\"1972\",\n" +
            "  \"diagnosis\":\"Extreme grumpiness\",\n" +
            "  \"dateOfDiagnosis-month\":\"03\",\n" +
            "  \"dateOfDiagnosis-year\":\"1971\",\n" +
            getReponseForOtherDiagnosesOnwards();
        try {
            validator.validateAndTranslate(partialJsonResponse);
            fail("Date of Diagnosis earlier than DOB should fail and throw an exception");
        } catch (Exception e) {
            assertThat("error caught", e.getMessage(), containsString("Date of diagnosis cannot be earlier than Patient DOB"));
        }
    }

    @Test
    public void confirmInvalidNationalInsuranceNumberPrefixFailsAndThrowsAnException() {
        String[] invalidNinoPrefix = {"BG", "GB", "NK", "KN", "TN", "NT", "QQ", "ZZ"};
        for (String invalidPrefix : invalidNinoPrefix) {
            try {
                validator.validateAndTranslate(buildFullRequestWithMissingFieldGPConsultant("patientNino", "\"" + invalidPrefix + "123456a\""));
                fail(invalidPrefix + " is an invalid NINO prefix");
            } catch (InvalidJsonException e) {
                assertThat("error caught", e.getMessage(), containsString(Ds1500JsonValidator.NINO_ERROR_MESSAGE));
            }
        }
    }

    @Test
    public void confirmNationalInsuranceNumberWithANInvalidInitialLetterFailsAndThrowsAnException() {
        String[] invalidInitialNinoLetter = {"D", "F", "I", "Q", "U", "V"};
        for (String invalidInitialLetter : invalidInitialNinoLetter) {
            try {
                validator.validateAndTranslate(buildFullRequestWithMissingFieldGPConsultant("patientNino", "\"" + invalidInitialLetter + "b123456a\""));
                fail(invalidInitialLetter + " is an invalid initial letter in a NINO");
            } catch (InvalidJsonException e) {
                assertThat("error caught", e.getMessage(), containsString(Ds1500JsonValidator.NINO_ERROR_MESSAGE));
            }
        }
    }

    @Test
    public void confirmNationalInsuranceNumberWithAnInvaidSecondLetterFailsAndThrowsAnException() {
        String[] invalidSecondNinoLetter = {"D", "F", "I", "O", "Q", "U", "V"};
        for (String invalidSecondLetter : invalidSecondNinoLetter) {
            try {
                validator.validateAndTranslate(buildFullRequestWithMissingFieldGPConsultant("patientNino", "\"n" + invalidSecondLetter + "123456a\""));
                fail(invalidSecondLetter + " is and invalid second initial in a NINO");
            } catch (InvalidJsonException e) {
                assertThat("error caught", e.getMessage(), containsString(Ds1500JsonValidator.NINO_ERROR_MESSAGE));

            }
        }
    }

    @Test
    public void confirmNationalInsuranceNumberWithInvalidNumberSetFailsAndThrowsAnException() {
        String[] invalidNINONumberSet = {"ABCDEF", "1", "12", "123", "1234", "12345", "X23456", "1X3456", "12X456", "123X56", "1234X6", "12345X"};
        for (String invalidNumberSet : invalidNINONumberSet) {
            try {
                validator.validateAndTranslate(buildFullRequestWithMissingFieldGPConsultant("patientNino", "\"AA" + invalidNumberSet + "A\""));
                fail(invalidNumberSet + " is an invalid NINO number set");
            } catch (InvalidJsonException e) {
                assertThat("error caught", e.getMessage(), containsString(Ds1500JsonValidator.NINO_ERROR_MESSAGE));
            }
        }
    }

    @Test
    public void confirmNationalInsuranceNumberWithInvalidSuffixFailsAndThrowsAnException() {
        String[] invalidNINOSuffix = {"E", "P", "1", "X", "K"};
        for (String invalidSuffix : invalidNINOSuffix) {
            try {
                validator.validateAndTranslate(buildFullRequestWithMissingFieldGPConsultant("patientNino", "\"AA370773" + invalidSuffix + "\""));
                fail(invalidSuffix + " is an invalid NINO suffix");
            } catch (InvalidJsonException e) {
                assertThat("error caught", e.getMessage(), containsString(Ds1500JsonValidator.NINO_ERROR_MESSAGE));
            }
        }
    }

    @Test
    public void confirmInvalidRepresentativePostcodeThrowsAnException() {
        try {
            validator.validateAndTranslate(buildFullRequestWithMissingFieldGPConsultant("representativePostcode", "\"123456789\""));
            fail("Representative postcode over 8 characters should throw an exception");
        } catch (InvalidJsonException e) {
            assertThat("error caught", e.getMessage(), containsString("'representativePostcode' fails validation"));
        }
    }

    @Test
    public void confirmThatABlankRepresentativePostcodeIsAccepted() throws InvalidJsonException {
        DSForm dsform = validator.validateAndTranslate(buildFullRequestWithMissingFieldGPConsultant("representativePostcode", null));
        assertThat("Representative line 3 mismatch", dsform.getRepresentative3(), nullValue());
    }

    @Test
    public void confirmThatABlankRepresentativePostcodeIsAccepted2() throws InvalidJsonException {
        DSForm dsform = validator.validateAndTranslate(buildFullRequestWithMissingFieldGPConsultant("representativePostcode", "\"\""));
        assertThat("Representative line 3 mismatch", dsform.getRepresentative3(), is(equalTo("")));
    }

    @Test
    public void confirmThatIvalidStartingPatientNameFails() {
        try {
            validator.validateAndTranslate(buildFullRequestWithMissingFieldGPConsultant("patientName", "\"-not allowed\""));
            fail("should have thrown a format error on patient name");

        } catch (InvalidJsonException e) {
            assertThat("expected json format exception", e.getMessage(), containsString("contains invalid characters"));
        }
    }

    @Test
    public void confirmThatIvalidFullStopPatientNameFails() {
        try {
            validator.validateAndTranslate(buildFullRequestWithMissingFieldGPConsultant("patientName", "\".not allowed\""));
            fail("should have thrown a format error on patient name");

        } catch (InvalidJsonException e) {
            assertThat("expected json format exception", e.getMessage(), containsString("contains invalid characters"));
        }
    }

    @Test
    public void confirmThatIvalidStartingPatientSurnameFails() {
        try {
            validator.validateAndTranslate(buildFullRequestWithMissingFieldGPConsultant("patientName", "\"not 'allowed\""));
            fail("should have thrown a format error on patient surname name");

        } catch (InvalidJsonException e) {
            assertThat("expected json format exception", e.getMessage(), containsString("contains invalid characters"));
        }
    }

    @Test
    public void confirmThatIvalidPatientSurnameLength() {
        try {
            validator.validateAndTranslate(buildFullRequestWithMissingFieldGPConsultant("patientName", "\"firstname aaaaaaaaaabbbbbbbbbbccccccccccdddddd\""));
            fail("should have thrown a length error on patient surname name");

        } catch (InvalidJsonException e) {
            assertThat("expected json format exception", e.getMessage(), containsString("maximum allowable length"));
        }
    }

    @Test
    public void confirmThatIvalidPatientFirstNameLength() {
        try {
            validator.validateAndTranslate(buildFullRequestWithMissingFieldGPConsultant("patientName", "\"firstname aaaaaaaaaabbbbbbbbbbccccccccccddddddddddeeeeeeeeeeffffffffffggggggggggii\""));
            fail("should have thrown a length error on patient surname name");

        } catch (InvalidJsonException e) {
            assertThat("expected json length exception", e.getMessage(), containsString("content validation checks"));
        }
    }

    @Test
    public void confirmSmallGMCNumberFromGPConsultantIsOK() throws InvalidJsonException {
        DSForm dsForm = validator.validateAndTranslate(buildFullRequestWithMissingFieldGPConsultant("gmcNumber", "\"123456\""));
        assertThat("GMC Number mismatch", dsForm.getGmcNumber(), is(123456));
    }

    @Test
    public void confirmSmallGMCNegativeNumberFromGPConsultantFails() {
        try {
            validator.validateAndTranslate(buildFullRequestWithMissingFieldGPConsultant("gmcNumber", "\"-1\""));
            fail("A negative gmc number is impossible");
        } catch (InvalidJsonException e) {
            assertThat("error caught", e.getMessage(), containsString("'gmcNumber' must be a positive"));
        }
    }

    @Test
    public void confirmSmallGMCZeroNumberFromGPConsultantFails() {
        try {
            validator.validateAndTranslate(buildFullRequestWithMissingFieldGPConsultant("gmcNumber", "\"0\""));
            fail("A negative gmc number is impossible");
        } catch (InvalidJsonException e) {
            assertThat("error caught", e.getMessage(), containsString("'gmcNumber' must be a positive"));
        }
    }

    @Test
    public void confirmValidGPPhonenumberIsAccepted() throws InvalidJsonException {
        DSForm dsForm = validator.validateAndTranslate(buildFullRequestWithMissingFieldGPConsultant("gpPhone", "\"07917 267922\""));
        assertThat("Declarer phone mismatch", dsForm.getDeclarerPhoneNumber(), is("07917 267922"));
    }

    @Test
    public void confirmInvalidGPPhoneNumberFails() {
        try {
            validator.validateAndTranslate(buildFullRequestWithMissingFieldGPConsultant("gpPhone", "\"invalid phone number\""));
            fail("should have thrown an error as GP phone number is invald");
        } catch (InvalidJsonException e) {
            assertThat("error caught", e.getMessage(), containsString("Invalid format for GP Phone number"));
        }
    }

    @Test
    public void confirmGPPhoneNumberWithLessThan9CharactersIsInvalidAndFails() {
        try {
            validator.validateAndTranslate(buildFullRequestWithMissingFieldGPConsultant("gpPhone", "\"12345678\""));
            fail("should have thrown an error as GP phone number is invald");
        } catch (InvalidJsonException e) {
            assertThat("error caught", e.getMessage(), containsString("Invalid format for GP Phone number"));
        }
    }

    @Test
    public void confirmGPPhoneNumberWithMoreThan16CharactersIsInvalidAndFails() throws InvalidJsonException {
        try {
            validator.validateAndTranslate(buildFullRequestWithMissingFieldGPConsultant("gpPhone", "\"0123456789123456\""));
            fail("should have thrown an error as GP phone number is invald");
        } catch (InvalidJsonException e) {
            assertThat("error caught", e.getMessage(), containsString("Invalid format for GP Phone number"));
        }
    }

    @Test
    public void confirmLargeGMCNumberFromGPConsultantFails() {
        try {
            validator.validateAndTranslate(buildFullRequestWithMissingFieldGPConsultant("gmcNumber", "\"12345678\""));
            fail("should have thrown an error");

        } catch (InvalidJsonException e) {
            assertThat(e.getMessage(), containsString("'gmcNumber' must be a positive 7 digit number"));
        }
    }

    @Test
    public void confirmValidJsonIsTransformedForOtherDeclarer() throws InvalidJsonException {

        DSForm dsForm = validator.validateAndTranslate(buildFullRequestWithMissingFieldForOtherDeclaration("", null));
        assertThat("Surname mismatch", dsForm.getSurname(), is("MAN"));
        assertThat("Other name mismatch", dsForm.getOtherNames(), is("FAKE"));
        assertThat("Address line 1 mismatch", dsForm.getAddress()[0], is("23 Fake Road"));
        assertThat("Address line 2 mismatch", dsForm.getAddress()[1], is("Fake Park"));
        assertThat("Address line 3 mismatch", dsForm.getAddress()[2], is("Fake"));
        assertThat("Postcode mismatch", dsForm.getPostcode(), is("S2 2RZ"));
        assertThat("DOB mismatch", dsForm.getDateOfBirth(), is("15/02/1972"));
        assertThat("Diagnosis date mismatch", dsForm.getDiagnosisDate(), is("05/2015"));
        assertThat("NINO mismatch", dsForm.getnINumber(), is("AA370773A"));
        assertThat("Diagnosis mismatch", dsForm.getDiagnosis(), is("Extreme grumpiness"));
        assertThat("Other diagnosis mismatch", dsForm.getOtherDiagnosis(), is("Mild allergies to dub step"));
        assertThat("Patient aware mismatch", dsForm.getPatientAware(), is("Yes"));
        assertThat("Form requestor mismatch", dsForm.getFormRequestor(), is("Representative"));
        assertThat("Representative line 1 mismatch", dsForm.getRepresentative1(), is("Representative name"));
        assertThat("Representative line 2 mismatch", dsForm.getRepresentative2(), is("21 Representative Road"));
        assertThat("Representative line 3 mismatch", dsForm.getRepresentative3(), is("NW1 3ER"));
        assertThat("Clinical features mismatch", dsForm.getClinicalFeatures(), is("Mr Coupe's clinical features"));
        assertThat("Treatment mismatch", dsForm.getTreatment(), is("Beer, loud music, fluffy kittens"));
        assertThat("Other treatment mismatch", dsForm.getOtherTreatment(), is("None whatsoever"));
        assertThat("Declaration mismatch", dsForm.getDeclaration(), is("Other"));
        assertThat("Other mismatch", dsForm.getOther(), is("Fred Bloggs"));
        assertThat("Declarer name mismatch", dsForm.getDeclarerName(), is("Dr. Michael Hfuhruhurr"));
        assertThat("Declarer address mismatch", dsForm.getDeclarerAddress(), is("Porter Brook Medical Centre"));
        assertThat("Declarer phone mismatch", dsForm.getDeclarerPhoneNumber(), is("0114 258 8520"));
        assertThat("Declaration date mismatch", dsForm.getDeclarationDate(), is(not(nullValue())));
    }

    @Test
    public void confirmPartialJsonPayloadIsTransformed() throws InvalidJsonException {
        String partialJsonResponse = "{\n" + "  \"patientName\":\"Fake Man\",\n" + getDefaultResponseAfterName();
        DSForm dsForm = validator.validateAndTranslate(partialJsonResponse);
        assertThat("Surname mismatch", dsForm.getSurname(), is("MAN"));
        assertThat("Forename mismatch", dsForm.getOtherNames(), is("FAKE"));
        assertThat("Address line 1 mismatch", dsForm.getAddress()[0], is("23 Fake Road"));
        assertThat("Address line 2 mismatch", dsForm.getAddress()[1], is("Fake Park"));
        assertThat("Address line 3 mismatch", dsForm.getAddress()[2], is("Fake"));
        assertThat("PostCode mismatch", dsForm.getPostcode(), is("S2 2RZ"));
        assertThat("DOB mismatch", dsForm.getDateOfBirth(), is("15/02/1972"));
        assertThat("Diagnosis mismatch", dsForm.getDiagnosisDate(), is("05/2015"));
        assertThat("Diagnosis mismatch", dsForm.getDiagnosis(), is("Extreme grumpiness"));
        assertThat("Other diagnosis mismatch", dsForm.getOtherDiagnosis(), is("Mild allergies to dub step"));
        assertThat("Patient Aware mismatch", dsForm.getPatientAware(), is("Yes"));
        assertThat("Form requestor mismatch", dsForm.getFormRequestor(), is("Patient"));
        assertThat("Clinical features mismatch", dsForm.getClinicalFeatures(), is("Mr Coupe's clinical features"));
        assertThat("Treatment mismatch", dsForm.getTreatment(), is("Beer, loud music, fluffy kittens"));
        assertThat("Other treatment mismatch", dsForm.getOtherTreatment(), is("None whatsoever"));
        assertThat("Declaration mismatch", dsForm.getDeclaration(), is("General Practitioner"));
        assertThat("Other declaration person mismatch", dsForm.getOther(), is(""));
        assertThat("Declarer name mismatch", dsForm.getDeclarerName(), is("Dr. Michael Hfuhruhurr"));
        assertThat("Declarer address mismatch", dsForm.getDeclarerAddress(), is("Porter Brook Medical Centre"));
        assertThat("Declarer phone mismatch", dsForm.getDeclarerPhoneNumber(), is("0114 258 8520"));
        assertThat("Declaration date mismatch", dsForm.getDeclarationDate(), is(not(nullValue())));
        assertThat("GMC Number mismatch", dsForm.getGmcNumber(), is(1234567));
    }

    @Test
    public void confirmDiagnosisDateIsSentWithoutDayValue() throws InvalidJsonException {
        String partialJsonResponse = "{\n" + "  \"patientName\":\"Paul Michael Coupe\",\n" + getDefaultResponseAfterName();
        DSForm dsForm = validator.validateAndTranslate(partialJsonResponse);
        assertThat("Diagnosis date mismatch", dsForm.getDiagnosisDate(), is("05/2015"));
    }

    @Test
    public void confirmNameSplitIsCorrectForOneName() throws InvalidJsonException {
        String partialJsonResponse = "{\n" +
            "  \"patientName\":\"Pele\",\n" +
            "  \"patientAddress\":\"23 Fake Road\\nFake Park\\nFake\",\n" +
            "  \"patientPostcode\":\"S2 2RZ\",\n" +
            "  \"patientDateOfBirth-day\":\"15\",\n" +
            "  \"patientDateOfBirth-month\":\"02\",\n" +
            "  \"patientDateOfBirth-year\":\"1972\",\n" +
            "  \"diagnosis\":\"Extreme grumpiness\",\n" +
            "  \"dateOfDiagnosis-month\":\"05\",\n" +
            "  \"dateOfDiagnosis-year\":\"2015\",\n" +
            getReponseForOtherDiagnosesOnwards();
        DSForm dsForm = validator.validateAndTranslate(partialJsonResponse);
        assertThat(dsForm.getSurname(), is("PELE"));
        assertThat(dsForm.getOtherNames(), is(""));
    }

    @Test
    public void confirmRequestIsRejectedIfOtherDeclarerButHasNoAdditionalDetails() {
        validateAndCatchException(buildFullRequestWithMissingFieldForOtherDeclaration("declarationAdditionalDetail", null), "invalidJsonException should have been thrown");
    }

    @Test
    public void confirmMandatoryFieldsAreCheckedForGPConsultant() {
        buildAndCheckRequestForMissingFieldOfGPConsultant("patientName");
        buildAndCheckRequestForMissingFieldOfGPConsultant("patientAddress");
        buildAndCheckRequestForMissingFieldOfGPConsultant("patientPostcode");
        buildAndCheckRequestForMissingFieldOfGPConsultant("patientDobDay");
        buildAndCheckRequestForMissingFieldOfGPConsultant("patientDobMonth");
        buildAndCheckRequestForMissingFieldOfGPConsultant("patientDobYear");
        buildAndCheckRequestForMissingFieldOfGPConsultant("diagnosis");
        buildAndCheckRequestForMissingFieldOfGPConsultant("diagnosisMonth");
        buildAndCheckRequestForMissingFieldOfGPConsultant("diagnosisYear");
        buildAndCheckRequestForMissingFieldOfGPConsultant("declaration");
        buildAndCheckRequestForMissingFieldOfGPConsultant("gpName");
        buildAndCheckRequestForMissingFieldOfGPConsultant("gpAddress");
        buildAndCheckRequestForMissingFieldOfGPConsultant("gpPhone");
        buildAndCheckRequestForMissingFieldOfGPConsultant("treatment");
        buildAndCheckRequestForMissingFieldOfGPConsultant("clinicalFeatures");
        buildAndCheckRequestForMissingFieldOfGPConsultant("formRequester");
        buildAndCheckRequestForMissingFieldOfGPConsultant("patientAware");
        buildAndCheckRequestForMissingFieldOfGPConsultant("gmcNumber");
    }

    @Test
    public void confirmMandatoryFieldsAreCheckedForOther() {
        buildAndCheckRequestForMissingFieldOfOther("patientName");
        buildAndCheckRequestForMissingFieldOfOther("patientAddress");
        buildAndCheckRequestForMissingFieldOfOther("patientPostcode");
        buildAndCheckRequestForMissingFieldOfOther("patientDobDay");
        buildAndCheckRequestForMissingFieldOfOther("patientDobMonth");
        buildAndCheckRequestForMissingFieldOfOther("patientDobYear");
        buildAndCheckRequestForMissingFieldOfOther("diagnosis");
        buildAndCheckRequestForMissingFieldOfOther("diagnosisMonth");
        buildAndCheckRequestForMissingFieldOfOther("diagnosisYear");
        buildAndCheckRequestForMissingFieldOfOther("declaration");
        buildAndCheckRequestForMissingFieldOfOther("gpName");
        buildAndCheckRequestForMissingFieldOfOther("gpAddress");
        buildAndCheckRequestForMissingFieldOfOther("gpPhone");
        buildAndCheckRequestForMissingFieldOfOther("treatment");
        buildAndCheckRequestForMissingFieldOfOther("clinicalFeatures");
        buildAndCheckRequestForMissingFieldOfOther("formRequester");
        buildAndCheckRequestForMissingFieldOfOther("patientAware");
    }

    @Test
    public void confirmMandatoryFieldsAreCheckedForEmptyStringGPConsultant() {
        buildAndCheckRequestForEmptyFieldOfGPConsultant("patientName");
        buildAndCheckRequestForEmptyFieldOfGPConsultant("patientAddress");
        buildAndCheckRequestForEmptyFieldOfGPConsultant("patientPostcode");
        buildAndCheckRequestForEmptyFieldOfGPConsultant("patientDobDay");
        buildAndCheckRequestForEmptyFieldOfGPConsultant("patientDobMonth");
        buildAndCheckRequestForEmptyFieldOfGPConsultant("patientDobYear");
        buildAndCheckRequestForEmptyFieldOfGPConsultant("diagnosis");
        buildAndCheckRequestForEmptyFieldOfGPConsultant("diagnosisMonth");
        buildAndCheckRequestForEmptyFieldOfGPConsultant("diagnosisYear");
        buildAndCheckRequestForEmptyFieldOfGPConsultant("declaration");
        buildAndCheckRequestForEmptyFieldOfGPConsultant("gpName");
        buildAndCheckRequestForEmptyFieldOfGPConsultant("gpAddress");
        buildAndCheckRequestForEmptyFieldOfGPConsultant("gpPhone");
        buildAndCheckRequestForEmptyFieldOfGPConsultant("treatment");
        buildAndCheckRequestForEmptyFieldOfGPConsultant("clinicalFeatures");
        buildAndCheckRequestForEmptyFieldOfGPConsultant("formRequester");
        buildAndCheckRequestForEmptyFieldOfGPConsultant("patientAware");
        buildAndCheckRequestForEmptyFieldOfGPConsultant("gmcNumber");
    }

    @Test
    public void confirmMandatoryFieldsAreCheckedForEmptyStringOther() {
        buildAndCheckRequestForEmptyFieldOfOther("patientName");
        buildAndCheckRequestForEmptyFieldOfOther("patientAddress");
        buildAndCheckRequestForEmptyFieldOfOther("patientPostcode");
        buildAndCheckRequestForEmptyFieldOfOther("patientDobDay");
        buildAndCheckRequestForEmptyFieldOfOther("patientDobMonth");
        buildAndCheckRequestForEmptyFieldOfOther("patientDobYear");
        buildAndCheckRequestForEmptyFieldOfOther("diagnosis");
        buildAndCheckRequestForEmptyFieldOfOther("diagnosisMonth");
        buildAndCheckRequestForEmptyFieldOfOther("diagnosisYear");
        buildAndCheckRequestForEmptyFieldOfOther("declaration");
        buildAndCheckRequestForEmptyFieldOfOther("gpName");
        buildAndCheckRequestForEmptyFieldOfOther("gpAddress");
        buildAndCheckRequestForEmptyFieldOfOther("gpPhone");
        buildAndCheckRequestForEmptyFieldOfOther("treatment");
        buildAndCheckRequestForEmptyFieldOfOther("clinicalFeatures");
        buildAndCheckRequestForEmptyFieldOfOther("formRequester");
        buildAndCheckRequestForEmptyFieldOfOther("patientAware");
    }

    @Test
    public void confirmMandatoryFieldsAreCheckedForLargeStringGPConsultant() {
        buildAndCheckRequestForLargeFieldOfGPConsultant("patientName");
        buildAndCheckRequestForLargeFieldOfGPConsultant("patientAddress");
        buildAndCheckRequestForLargeFieldOfGPConsultant("patientPostcode");
        buildAndCheckRequestForLargeFieldOfGPConsultant("patientDobDay");
        buildAndCheckRequestForLargeFieldOfGPConsultant("patientDobMonth");
        buildAndCheckRequestForLargeFieldOfGPConsultant("patientDobYear");
        buildAndCheckRequestForLargeFieldOfGPConsultant("diagnosis");
        buildAndCheckRequestForLargeFieldOfGPConsultant("otherDiagnoses");
        buildAndCheckRequestForLargeFieldOfGPConsultant("otherIntervention");
        buildAndCheckRequestForLargeFieldOfGPConsultant("diagnosisMonth");
        buildAndCheckRequestForLargeFieldOfGPConsultant("diagnosisYear");
        buildAndCheckRequestForLargeFieldOfGPConsultant("declaration");
        buildAndCheckRequestForLargeFieldOfGPConsultant("gpName");
        buildAndCheckRequestForLargeFieldOfGPConsultant("gpAddress");
        buildAndCheckRequestForLargeFieldOfGPConsultant("gpPhone");
        buildAndCheckRequestForLargeFieldOfGPConsultant("treatment");
        buildAndCheckRequestForLargeFieldOfGPConsultant("clinicalFeatures");
        buildAndCheckRequestForLargeFieldOfGPConsultant("formRequester");
        buildAndCheckRequestForLargeFieldOfGPConsultant("patientAware");
        buildAndCheckRequestForEmptyFieldOfGPConsultant("gmcNumber");
    }

    @Test
    public void confirmMandatoryFieldsAreCheckedForLargeStringOther() {
        buildAndCheckRequestForLargeFieldOfOther("patientName");
        buildAndCheckRequestForLargeFieldOfOther("patientAddress");
        buildAndCheckRequestForLargeFieldOfOther("patientPostcode");
        buildAndCheckRequestForLargeFieldOfOther("patientDobDay");
        buildAndCheckRequestForLargeFieldOfOther("patientDobMonth");
        buildAndCheckRequestForLargeFieldOfOther("patientDobYear");
        buildAndCheckRequestForLargeFieldOfOther("diagnosis");
        buildAndCheckRequestForLargeFieldOfOther("otherDiagnoses");
        buildAndCheckRequestForLargeFieldOfOther("otherIntervention");
        buildAndCheckRequestForLargeFieldOfOther("diagnosisMonth");
        buildAndCheckRequestForLargeFieldOfOther("diagnosisYear");
        buildAndCheckRequestForLargeFieldOfOther("declaration");
        buildAndCheckRequestForLargeFieldOfOther("gpName");
        buildAndCheckRequestForLargeFieldOfOther("gpAddress");
        buildAndCheckRequestForLargeFieldOfOther("gpPhone");
        buildAndCheckRequestForLargeFieldOfOther("treatment");
        buildAndCheckRequestForLargeFieldOfOther("clinicalFeatures");
        buildAndCheckRequestForLargeFieldOfOther("formRequester");
        buildAndCheckRequestForLargeFieldOfOther("patientAware");
    }

    @Test
    public void confirmMandatoryFieldsAreCheckedForInvalidStringGPConsultant() {
        buildAndCheckRequestForInvalidFieldOfGPConsultant("patientName");
        buildAndCheckRequestForInvalidFieldOfGPConsultant("patientAddress");
        buildAndCheckRequestForInvalidFieldOfGPConsultant("patientPostcode");
        buildAndCheckRequestForInvalidFieldOfGPConsultant("patientDobDay");
        buildAndCheckRequestForInvalidFieldOfGPConsultant("patientDobMonth");
        buildAndCheckRequestForInvalidFieldOfGPConsultant("patientDobYear");
        buildAndCheckRequestForInvalidFieldOfGPConsultant("diagnosis");
        buildAndCheckRequestForInvalidFieldOfGPConsultant("otherDiagnoses");
        buildAndCheckRequestForInvalidFieldOfGPConsultant("otherIntervention");
        buildAndCheckRequestForInvalidFieldOfGPConsultant("diagnosisMonth");
        buildAndCheckRequestForInvalidFieldOfGPConsultant("diagnosisYear");
        buildAndCheckRequestForInvalidFieldOfGPConsultant("declaration");
        buildAndCheckRequestForInvalidFieldOfGPConsultant("gpName");
        buildAndCheckRequestForInvalidFieldOfGPConsultant("gpAddress");
        buildAndCheckRequestForInvalidFieldOfGPConsultant("gpPhone");
        buildAndCheckRequestForInvalidFieldOfGPConsultant("treatment");
        buildAndCheckRequestForInvalidFieldOfGPConsultant("clinicalFeatures");
        buildAndCheckRequestForInvalidFieldOfGPConsultant("formRequester");
        buildAndCheckRequestForInvalidFieldOfGPConsultant("patientAware");
        buildAndCheckRequestForEmptyFieldOfGPConsultant("gmcNumber");
    }

    @Test
    public void confirmMandatoryFieldsAreCheckedForInvalidStringOther() {
        buildAndCheckRequestForInvalidFieldOfOther("patientName");
        buildAndCheckRequestForInvalidFieldOfOther("patientAddress");
        buildAndCheckRequestForInvalidFieldOfOther("patientPostcode");
        buildAndCheckRequestForInvalidFieldOfOther("patientDobDay");
        buildAndCheckRequestForInvalidFieldOfOther("patientDobMonth");
        buildAndCheckRequestForInvalidFieldOfOther("patientDobYear");
        buildAndCheckRequestForInvalidFieldOfOther("diagnosis");
        buildAndCheckRequestForInvalidFieldOfOther("otherDiagnoses");
        buildAndCheckRequestForInvalidFieldOfOther("otherIntervention");
        buildAndCheckRequestForInvalidFieldOfOther("diagnosisMonth");
        buildAndCheckRequestForInvalidFieldOfOther("diagnosisYear");
        buildAndCheckRequestForInvalidFieldOfOther("declaration");
        buildAndCheckRequestForInvalidFieldOfOther("gpName");
        buildAndCheckRequestForInvalidFieldOfOther("gpAddress");
        buildAndCheckRequestForInvalidFieldOfOther("gpPhone");
        buildAndCheckRequestForInvalidFieldOfOther("treatment");
        buildAndCheckRequestForInvalidFieldOfOther("clinicalFeatures");
        buildAndCheckRequestForInvalidFieldOfOther("formRequester");
        buildAndCheckRequestForInvalidFieldOfOther("patientAware");
    }

    @Test
    public void confirmInvalidDateOfBirthIsRejected() {
        checkAndConfirmRequestWithInvalidDOB("30", "02", "1972");
        checkAndConfirmRequestWithInvalidDOB("32", "01", "1972");
        checkAndConfirmRequestWithInvalidDOB("29", "02", "1973");
    }


    @Test
    public void tommorowsDateFailsValidationForDOB() {
        Date today = new Date();
        Date date = DateUtils.addDays(today, 1);
        String day = getDayFromDate(date);
        String month = getMonthFromDate(date);
        String year = getYearFromDate(date);
        checkAndConfirmRequestWithInvalidDOB(day, month, year);

    }

    @Test
    public void confirmInvalidDiagnosisDateIsRejected() {
        checkAndConfirmRequestWithInvalidDiagnosisDate("15", "1972");
        checkAndConfirmRequestWithInvalidDiagnosisDate("16", "1972");
        checkAndConfirmRequestWithInvalidDiagnosisDate("17", "1973");
    }

    @Test
    public void confirmDOBBefore1890CausesFailure() {
        checkAndConfirmRequestWithInvalidDOB("31", "12", "1889");
    }

    @Test
    public void confirmDiagnosisDateBefore1890CausesFailure() {
        checkAndConfirmRequestWithInvalidDiagnosisDate("12", "1889");
    }

    @Test
    public void confirmNameSplitIsCorrectForThreeNames() throws InvalidJsonException {
        String partialJsonResponse = "{\n" +
            "  \"patientName\":\"Fake Michael Man\",\n" +
            getDefaultResponseAfterName();
        DSForm dsForm = validator.validateAndTranslate(partialJsonResponse);
        assertThat(dsForm.getSurname(), is("MAN"));
        assertThat(dsForm.getOtherNames(), is("FAKE MICHAEL"));
    }

    private String getDayFromDate(Date date) {
        SimpleDateFormat dayFormat = new SimpleDateFormat("dd");
        return dayFormat.format(date);
    }

    private String getMonthFromDate(Date date) {
        SimpleDateFormat monthFormat = new SimpleDateFormat("MM");
        return monthFormat.format(date);
    }

    private String buildFullRequest() {
        return buildFullRequestWithMissingFieldGPConsultant("", null);
    }

    private String getYearFromDate(Date date) {
        SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy");
        return yearFormat.format(date);
    }

    private String getDefaultResponseAfterName() {
        return "  \"patientAddress\":\"23 Fake Road\\nFake Park\\nFake\",\n" +
            "  \"patientPostcode\":\"S2 2RZ\",\n" +
            "  \"patientDateOfBirth-day\":\"15\",\n" +
            "  \"patientDateOfBirth-month\":\"02\",\n" +
            "  \"patientDateOfBirth-year\":\"1972\",\n" +
            "  \"diagnosis\":\"Extreme grumpiness\",\n" +
            "  \"dateOfDiagnosis-month\":\"05\",\n" +
            "  \"dateOfDiagnosis-year\":\"2015\",\n" +
            getReponseForOtherDiagnosesOnwards();
    }

    private String buildResponseWithDobOf(String day, String month, String year) {
        return "{\n" +
            "  \"patientName\":\"Fake Michael Man\",\n" +
            "  \"patientAddress\":\"23 Fake Road\\nFake Park\\nFake\",\n" +
            "  \"patientPostcode\":\"S2 2RZ\",\n" +
            "  \"patientDateOfBirth-day\":\"" + day + "\",\n" +
            "  \"patientDateOfBirth-month\":\"" + month + "\",\n" +
            "  \"patientDateOfBirth-year\":\"" + year + "\",\n" +
            "  \"diagnosis\":\"Extreme grumpiness\",\n" +
            "  \"dateOfDiagnosis-day\":\"11\",\n" +
            "  \"dateOfDiagnosis-month\":\"05\",\n" +
            "  \"dateOfDiagnosis-year\":\"2015\",\n" +
            getReponseForOtherDiagnosesOnwards();
    }

    private String buildResponseWithDiagnosisDateOf(String month, String year) {
        return "{\n" +
            "  \"patientName\":\"Fake Michael Man\",\n" +
            "  \"patientAddress\":\"23 Fake Road\\nFake Park\\nFake\",\n" +
            "  \"patientPostcode\":\"S2 2RZ\",\n" +
            "  \"patientDateOfBirth-day\":\"01\",\n" +
            "  \"patientDateOfBirth-month\":\"01\",\n" +
            "  \"patientDateOfBirth-year\":\"1972\",\n" +
            "  \"diagnosis\":\"Extreme grumpiness\",\n" +
            "  \"dateOfDiagnosis-month\":\"" + month + "\",\n" +
            "  \"dateOfDiagnosis-year\":\"" + year + "\",\n" +
            getReponseForOtherDiagnosesOnwards();
    }

    private String getReponseForOtherDiagnosesOnwards() {
        return "  \"otherDiagnoses\":\"Mild allergies to dub step\",\n" +
            "  \"patientAware\":\"Yes\",\n" +
            "  \"formRequester\":\"Patient\",\n" +
            "  \"clinicalFeatures\":\"Mr Coupe's clinical features\",\n" +
            "  \"treatment\":\"Beer, loud music, fluffy kittens\",\n" +
            "  \"otherIntervention\":\"None whatsoever\",\n" +
            "  \"declaration\":\"General Practitioner\",\n" +
            "  \"declarationAdditionalDetail\":\"\",\n" +
            "  \"gpName\":\"Dr. Michael Hfuhruhurr\",\n" +
            "  \"gpAddress\":\"Porter Brook Medical Centre\",\n" +
            "  \"gpPhone\":\"0114 258 8520\",\n" +
            "  \"gmcNumber\":\"1234567\",\n" +
            "  \"vatRegistered\":\"Yes\",\n" +
            "  \"firstFromSurgery\":\"Yes\"}";
    }


    private void buildAndCheckRequestForMissingFieldOfGPConsultant(String missingField) {
        validateAndCatchException(buildFullRequestWithMissingFieldGPConsultant(missingField, null), "invalidJsonException should have been thrown for missing " + missingField);
    }

    private void buildAndCheckRequestForMissingFieldOfOther(String missingField) {
        validateAndCatchException(buildFullRequestWithMissingFieldForOtherDeclaration(missingField, null), "invalidJsonException should have been thrown for missing " + missingField);
    }

    private void buildAndCheckRequestForLargeFieldOfGPConsultant(String missingField) {
        validateAndCatchException(buildFullRequestWithMissingFieldGPConsultant(missingField, STRING_WITH_TOO_MANY_CHARACTERS), "invalidJsonException should have been thrown for missing " + missingField);
    }

    private void buildAndCheckRequestForLargeFieldOfOther(String missingField) {
        validateAndCatchException(buildFullRequestWithMissingFieldForOtherDeclaration(missingField, STRING_WITH_TOO_MANY_CHARACTERS), "invalidJsonException should have been thrown for missing " + missingField);
    }

    private void buildAndCheckRequestForInvalidFieldOfGPConsultant(String missingField) {
        validateAndCatchException(buildFullRequestWithMissingFieldGPConsultant(missingField, STRING_WITH_INVALID_CHARACTERS), "invalidJsonException should have been thrown for missing " + missingField);
    }

    private void buildAndCheckRequestForInvalidFieldOfOther(String missingField) {
        validateAndCatchException(buildFullRequestWithMissingFieldForOtherDeclaration(missingField, STRING_WITH_INVALID_CHARACTERS), "invalidJsonException should have been thrown for missing " + missingField);
    }

    private void buildAndCheckRequestForEmptyFieldOfGPConsultant(String missingField) {
        validateAndCatchException(buildFullRequestWithMissingFieldGPConsultant(missingField, "\"\""), "invalidJsonException should have been thrown for missing " + missingField);
    }

    private void buildAndCheckRequestForEmptyFieldOfOther(String missingField) {
        validateAndCatchException(buildFullRequestWithMissingFieldForOtherDeclaration(missingField, "\"\""), "invalidJsonException should have been thrown for missing " + missingField);
    }

    private void checkAndConfirmRequestWithInvalidDOB(String dobDay, String dobMonth, String dobYear) {
        validateAndCatchException(buildResponseWithDobOf(dobDay, dobMonth, dobYear), "Invalid date of birth should be rejected");
    }

    private void checkAndConfirmRequestWithInvalidDiagnosisDate(String dobMonth, String dobYear) {
        validateAndCatchException(buildResponseWithDiagnosisDateOf(dobMonth, dobYear), "Invalid diagnosis date should be rejected");
    }

    private void validateAndCatchException(String request, String message) {
        try {
            validator.validateAndTranslate(request);
            fail(message);
        } catch (Exception e) {
            // exception expected
        }
    }

    private String buildFullRequestWithMissingFieldGPConsultant(String missingField, String replaceString) {
        return buildRequest(
            (missingField.equals("patientName") ? replaceString : "\"Fake Man\""),
            (missingField.equals("patientAddress") ? replaceString : "\"23 Fake Road\\nFake Park\\nFake\""),
            (missingField.equals("patientPostcode") ? replaceString : "\"S2 2RZ\""),
            (missingField.equals("patientDobDay") ? replaceString : "\"15\""),
            (missingField.equals("patientDobMonth") ? replaceString : "\"02\""),
            (missingField.equals("patientDobYear") ? replaceString : "\"1972\""),
            (missingField.equals("patientNino") ? replaceString : "\"AA370773A\""),
            (missingField.equals("diagnosis") ? replaceString : "\"Extreme grumpiness\""),
            (missingField.equals("diagnosisDay") ? replaceString : "\"11\""),
            (missingField.equals("diagnosisMonth") ? replaceString : "\"05\""),
            (missingField.equals("diagnosisYear") ? replaceString : "\"2015\""),
            (missingField.equals("otherDiagnoses") ? replaceString : "\"Mild allergies to dub step\""),
            (missingField.equals("patientAware") ? replaceString : "\"Yes\""),
            (missingField.equals("formRequester") ? replaceString : "\"Representative\""),
            (missingField.equals("representativeName") ? replaceString : "\"Representative name\""),
            (missingField.equals("representativeAddress") ? replaceString : "\"21 Representative Road\""),
            (missingField.equals("representativePostcode") ? replaceString : "\"NW1 3ER\""),
            (missingField.equals("clinicalFeatures") ? replaceString : "\"Mr Coupe's clinical features\""),
            (missingField.equals("treatment") ? replaceString : "\"Beer, loud music, fluffy kittens\""),
            (missingField.equals("otherIntervention") ? replaceString : "\"None whatsoever\""),
            (missingField.equals("declaration") ? replaceString : "\"General Practitioner\""),
            (missingField.equals("declarationAdditionalDetail") ? replaceString : "\"\""),
            (missingField.equals("gpName") ? replaceString : "\"Dr. Michael Hfuhruhurr\""),
            (missingField.equals("gpAddress") ? replaceString : "\"Porter Brook Medical Centre\""),
            (missingField.equals("gpPhone") ? replaceString : "\"0114 258 8520\""),
            (missingField.equals("gmcNumber") ? replaceString : "\"1234567\""),
            (missingField.equals("vatRegistered") ? replaceString : "\"Yes\""),
            (missingField.equals("firstFromSurgery") ? replaceString : "\"Yes\""));
    }

    private String buildFullRequestWithMissingFieldForOtherDeclaration(String missingField, String replaceString) {
        return buildRequest(
            (missingField.equals("patientName") ? replaceString : "\"Fake Man\""),
            (missingField.equals("patientAddress") ? replaceString : "\"23 Fake Road\\nFake Park\\nFake\""),
            (missingField.equals("patientPostcode") ? replaceString : "\"S2 2RZ\""),
            (missingField.equals("patientDobDay") ? replaceString : "\"15\""),
            (missingField.equals("patientDobMonth") ? replaceString : "\"02\""),
            (missingField.equals("patientDobYear") ? replaceString : "\"1972\""),
            (missingField.equals("patientNino") ? replaceString : "\"AA370773A\""),
            (missingField.equals("diagnosis") ? replaceString : "\"Extreme grumpiness\""),
            (missingField.equals("diagnosisDay") ? replaceString : "\"11\""),
            (missingField.equals("diagnosisMonth") ? replaceString : "\"05\""),
            (missingField.equals("diagnosisYear") ? replaceString : "\"2015\""),
            (missingField.equals("otherDiagnoses") ? replaceString : "\"Mild allergies to dub step\""),
            (missingField.equals("patientAware") ? replaceString : "\"Yes\""),
            (missingField.equals("formRequester") ? replaceString : "\"Representative\""),
            (missingField.equals("representativeName") ? replaceString : "\"Representative name\""),
            (missingField.equals("representativeAddress") ? replaceString : "\"21 Representative Road\""),
            (missingField.equals("representativePostcode") ? replaceString : "\"NW1 3ER\""),
            (missingField.equals("clinicalFeatures") ? replaceString : "\"Mr Coupe's clinical features\""),
            (missingField.equals("treatment") ? replaceString : "\"Beer, loud music, fluffy kittens\""),
            (missingField.equals("otherIntervention") ? replaceString : "\"None whatsoever\""),
            (missingField.equals("declaration") ? replaceString : "\"Other\""),
            (missingField.equals("declarationAdditionalDetail") ? replaceString : "\"Fred Bloggs\""),
            (missingField.equals("gpName") ? replaceString : "\"Dr. Michael Hfuhruhurr\""),
            (missingField.equals("gpAddress") ? replaceString : "\"Porter Brook Medical Centre\""),
            (missingField.equals("gpPhone") ? replaceString : "\"0114 258 8520\""),
            null,
            null,
            null);
    }

    private String buildRequest(final String patientName,
                                final String patientAddress,
                                final String patientPostcode,
                                final String dobDay,
                                final String dobMonth,
                                final String dobYear,
                                final String nino,
                                final String diagnosis,
                                final String diagnosisDay,
                                final String diagnosisMonth,
                                final String diagnosisYear,
                                final String otherDiagnosis,
                                final String patientAware,
                                final String formRequestor,
                                final String representativeName,
                                final String representativeAddress,
                                final String representativePostcode,
                                final String clinicalFeatures,
                                final String treatment,
                                final String intervention,
                                final String declarer,
                                final String declarerAdditional,
                                final String gpName,
                                final String gpAddress,
                                final String gpPhone,
                                final String gmcNumber,
                                final String vatRegistered,
                                final String firstFromSurgery) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{\n");
        if (patientName != null) {
            stringBuilder.append("  \"patientName\":").append(patientName).append(",\n");
        }
        if (patientAddress != null) {
            stringBuilder.append("  \"patientAddress\":").append(patientAddress).append(",\n");
        }
        if (patientPostcode != null) {
            stringBuilder.append("  \"patientPostcode\":").append(patientPostcode).append(",\n");
        }
        if (dobDay != null) {
            stringBuilder.append("  \"patientDateOfBirth-day\":").append(dobDay).append(",\n");
        }
        if (dobMonth != null) {
            stringBuilder.append("  \"patientDateOfBirth-month\":").append(dobMonth).append(",\n");
        }
        if (dobYear != null) {
            stringBuilder.append("  \"patientDateOfBirth-year\":").append(dobYear).append(",\n");
        }
        if (nino != null) {
            stringBuilder.append("  \"patientNino\":").append(nino).append(",\n");
        }
        if (diagnosis != null) {
            stringBuilder.append("  \"diagnosis\":").append(diagnosis).append(",\n");
        }
        if (diagnosisDay != null) {
            stringBuilder.append("  \"dateOfDiagnosis-day\":").append(diagnosisDay).append(",\n");
        }
        if (diagnosisMonth != null) {
            stringBuilder.append("  \"dateOfDiagnosis-month\":").append(diagnosisMonth).append(",\n");
        }
        if (diagnosisYear != null) {
            stringBuilder.append("  \"dateOfDiagnosis-year\":").append(diagnosisYear).append(",\n");
        }
        if (otherDiagnosis != null) {
            stringBuilder.append("  \"otherDiagnoses\":").append(otherDiagnosis).append(",\n");
        }
        if (patientAware != null) {
            stringBuilder.append("  \"patientAware\":").append(patientAware).append(",\n");
        }
        if (formRequestor != null) {
            stringBuilder.append("  \"formRequester\":").append(formRequestor).append(",\n");
        }
        if (representativeName != null) {
            stringBuilder.append("  \"representativeName\":").append(representativeName).append(",\n");
        }
        if (representativeAddress != null) {
            stringBuilder.append("  \"representativeAddress\":").append(representativeAddress).append(",\n");
        }
        if (representativePostcode != null) {
            stringBuilder.append("  \"representativePostcode\":").append(representativePostcode).append(",\n");
        }
        if (clinicalFeatures != null) {
            stringBuilder.append("  \"clinicalFeatures\":").append(clinicalFeatures).append(",\n");
        }
        if (treatment != null) {
            stringBuilder.append("  \"treatment\":").append(treatment).append(",\n");
        }
        if (intervention != null) {
            stringBuilder.append("  \"otherIntervention\":").append(intervention).append(",\n");
        }
        if (declarer != null) {
            stringBuilder.append("  \"declaration\":").append(declarer).append(",\n");
        }
        if (declarerAdditional != null) {
            stringBuilder.append("  \"declarationAdditionalDetail\":").append(declarerAdditional).append(",\n");
        }
        if (gpName != null) {
            stringBuilder.append("  \"gpName\":").append(gpName).append(",\n");
        }
        if (gpAddress != null) {
            stringBuilder.append("  \"gpAddress\":").append(gpAddress).append(",\n");
        }
        if (gpPhone != null) {
            stringBuilder.append("  \"gpPhone\":").append(gpPhone).append(",\n");
        }
        if (gmcNumber != null) {
            stringBuilder.append("  \"gmcNumber\":").append(gmcNumber).append(",\n");
        }
        if (vatRegistered != null) {
            stringBuilder.append("  \"vatRegistered\":").append(vatRegistered).append(",\n");
        }
        if (firstFromSurgery != null) {
            stringBuilder.append("  \"firstFromSurgery\":").append(firstFromSurgery).append(",\n");
        }
        stringBuilder.append("   \"json\":\"\"}");
        return stringBuilder.toString();
    }
}
