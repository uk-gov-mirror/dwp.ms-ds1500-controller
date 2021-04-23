package uk.gov.dwp.health.ds1500controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import org.junit.Test;
import uk.gov.dwp.health.ds1500controller.domain.DSForm;
import uk.gov.dwp.health.ds1500controller.domain.Ds1500Metadata;
import uk.gov.dwp.regex.InvalidNinoException;
import uk.gov.dwp.regex.NinoValidator;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

@SuppressWarnings({"squid:S1192", "squid:S00107"}) // string literals allowed & method params
public class MetadataBuilderTest {

    MetadataBuilder builderUnderTest = new MetadataBuilder();

    @Test
    public void confirmClaimRefNumberIsCreated() throws InvalidNinoException {
        Ds1500Metadata payload = builderUnderTest.buildPayload(new DSForm(), LocalDate.of(1996, 10, 15));
        validateClaimRef(payload.getClaimRef());
    }

    @Test
    public void confirmMetadataIsAddedToPayload() throws IOException, InvalidNinoException {
        String surname = "Fake";
        String otherNames = "Man";
        String postcode = "LS1 1AR";
        String niPrefix = "AA370773";
        String niSuffix = "A";
        String nINumber = niPrefix + niSuffix;
        String dateOfBirth = "6/07/1993";
        int dateOfBirthAsNumber = 19930706;
        String businessUnitID = "29";
        int benefitType = 23;
        DSForm form = buildForm(surname, otherNames, postcode, nINumber, dateOfBirth);

        Ds1500Metadata payload = builderUnderTest.buildPayload(form, LocalDate.of(1996, 10, 15));

        checkMetadataFields(surname, otherNames, postcode, niPrefix, niSuffix, dateOfBirthAsNumber, businessUnitID, benefitType, new ObjectMapper().writeValueAsString(payload));
        validateClaimRef(payload.getClaimRef());
    }


    @Test
    public void checkDateOfBirthWithoutLeadingZeroes() throws IOException, InvalidNinoException {
        DSForm form = buildForm("Fake", "Man", "LS1 1AR", "AA370773A", "1/7/1993");
        ObjectMapper mapper = new ObjectMapper();

        String metadata = mapper.writeValueAsString(builderUnderTest.buildPayload(form, LocalDate.of(2016, 7, 25)));

        JsonNode jsonNode = mapper.readTree(metadata);
        checkFieldValue(jsonNode, "dateOfBirth", 19930701);
        checkFieldValue(jsonNode, "businessUnitID", "20");
        checkFieldValue(jsonNode, "benefitType", 7);
    }

    @Test
    public void confirmMetadataIsAddedToPayloadWhenNinoIsInvalid() throws IOException, InvalidNinoException {
        String surname = "Fake";
        String otherNames = "Man";
        String postcode = "LS1 1AR";
        String nINumber = "a";
        String dateOfBirth = "16/07/1943";
        int dateOfBirthAsNumber = 19430716;
        DSForm form = buildForm(surname, otherNames, postcode, nINumber, dateOfBirth);

        Ds1500Metadata payload = builderUnderTest.buildPayload(form, LocalDate.of(2016, 7, 25));

        ObjectMapper mapper = new ObjectMapper();
        JsonNode metadata = mapper.readTree(mapper.writeValueAsString(payload));

        checkStaticValues(metadata);

        checkFieldValue(metadata, "surname", surname);
        checkFieldValue(metadata, "forename", otherNames);
        checkFieldValue(metadata, "postCode", postcode);
        checkFieldValue(metadata, "dateOfBirth", dateOfBirthAsNumber);
        checkFieldValue(metadata, "businessUnitID", "34");
        checkFieldValue(metadata, "benefitType", 36);
        assertThat(metadata.get("nino").getClass(), is(equalTo(NullNode.class)));
        validateClaimRef(payload.getClaimRef());
    }

    @Test
    public void confirmMetadataIsAddedToPayloadWhenNinoMissingSuffix() throws IOException, InvalidNinoException {
        String surname = "Fake";
        String otherNames = "Man";
        String postcode = "LS1 1AR";
        String nINumber = "AA370773";
        String dateOfBirth = "16/07/1943";
        int dateOfBirthAsNumber = 19430716;
        DSForm form = buildForm(surname, otherNames, postcode, nINumber, dateOfBirth);

        Ds1500Metadata payload = builderUnderTest.buildPayload(form, LocalDate.of(2016, 7, 25));

        ObjectMapper mapper = new ObjectMapper();
        JsonNode metadata = mapper.readTree(mapper.writeValueAsString(payload));

        checkStaticValues(metadata);

        checkFieldValue(metadata, "surname", surname);
        checkFieldValue(metadata, "forename", otherNames);
        checkFieldValue(metadata, "postCode", postcode);
        checkFieldValue(metadata, "dateOfBirth", dateOfBirthAsNumber);
        checkFieldValue(metadata, "businessUnitID", "34");
        checkFieldValue(metadata, "benefitType", 36);

        NinoValidator ninoValue = new NinoValidator(metadata.get("nino").get("ninoBody").textValue(), metadata.get("nino").get("ninoSuffix").textValue());
        assertThat(metadata.get("nino").getClass(), not(equalTo(NullNode.class)));
        assertThat(ninoValue.getNinoBody(), is(equalTo(nINumber)));
        assertThat(ninoValue.getNinoSuffix(), is(equalTo("")));
        validateClaimRef(payload.getClaimRef());
    }

    @Test
    public void invalidDOB() throws IOException, InvalidNinoException {
        DSForm form = new DSForm();
        form.setDateOfBirth("16/17/1992");

        Ds1500Metadata payload = builderUnderTest.buildPayload(form, LocalDate.now());
        ObjectMapper mapper = new ObjectMapper();
        JsonNode metadata = mapper.readTree(mapper.writeValueAsString(payload));

        checkStaticValues(metadata);
        checkFieldValue(metadata, "dateOfBirth", 0);
        validateClaimRef(payload.getClaimRef());
    }

    @Test
    public void confirmBlankFormDoesNotErrorAndSetsStaticValues() throws IOException, InvalidNinoException {
        DSForm form = new DSForm();
        Ds1500Metadata payload = builderUnderTest.buildPayload(form, LocalDate.now());
        ObjectMapper mapper = new ObjectMapper();
        JsonNode metadata = mapper.readTree(mapper.writeValueAsString(payload));
        checkStaticValues(metadata);
        validateClaimRef(payload.getClaimRef());
    }

    @Test
    public void applicantIsUnderSixteen() throws IOException, InvalidNinoException {
        DateTimeFormatter df = DateTimeFormatter.ofPattern("d/M/yyyy", Locale.ENGLISH);
        LocalDate submissionDate = LocalDate.parse("25/07/2016", df);
        String applicantDOB = "31/10/2005";
        DSForm form = new DSForm();

        form.setDateOfBirth(applicantDOB);

        Ds1500Metadata payload = builderUnderTest.buildPayload(form, submissionDate);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode metadata = mapper.readTree(mapper.writeValueAsString(payload));

        checkFieldValue(metadata, "businessUnitID", "29");
        checkFieldValue(metadata, "benefitType", 23);
        checkFieldValue(metadata, "officePostcode", "DL19QX");
        validateClaimRef(payload.getClaimRef());
    }

    @Test
    public void applicantIsSixteen() throws IOException, InvalidNinoException {
        DateTimeFormatter df = DateTimeFormatter.ofPattern("d/M/yyyy", Locale.ENGLISH);
        LocalDate submissionDate = LocalDate.parse("25/07/2016", df);
        String applicantDOB = "25/07/2000";
        DSForm form = new DSForm();

        form.setDateOfBirth(applicantDOB);

        Ds1500Metadata payload = builderUnderTest.buildPayload(form, submissionDate);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode metadata = mapper.readTree(mapper.writeValueAsString(payload));

        checkFieldValue(metadata, "businessUnitID", "20");
        checkFieldValue(metadata, "benefitType", 7);
        assertNull("office postcode should be null", payload.getOfficePostcode());
        validateClaimRef(payload.getClaimRef());
    }

    @Test
    public void applicantSixteenUnderSixtyFive() throws IOException, InvalidNinoException {
        DateTimeFormatter df = DateTimeFormatter.ofPattern("d/M/yyyy", Locale.ENGLISH);
        LocalDate submissionDate = LocalDate.parse("25/07/2016", df);
        String applicantDOB = "31/10/1973";
        DSForm form = new DSForm();

        form.setDateOfBirth(applicantDOB);

        Ds1500Metadata payload = builderUnderTest.buildPayload(form, submissionDate);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode metadata = mapper.readTree(mapper.writeValueAsString(payload));

        checkFieldValue(metadata, "businessUnitID", "20");
        checkFieldValue(metadata, "benefitType", 7);
        assertNull("office postcode should be null", payload.getOfficePostcode());
        validateClaimRef(payload.getClaimRef());
    }

    @Test
    public void applicantIsSixtyFive() throws IOException, InvalidNinoException {
        DateTimeFormatter df = DateTimeFormatter.ofPattern("d/M/yyyy", Locale.ENGLISH);
        LocalDate submissionDate = LocalDate.parse("25/7/2016", df);
        String applicantDOB = "25/07/1951";
        DSForm form = new DSForm();

        form.setDateOfBirth(applicantDOB);

        Ds1500Metadata payload = builderUnderTest.buildPayload(form, submissionDate);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode metadata = mapper.readTree(mapper.writeValueAsString(payload));

        checkFieldValue(metadata, "businessUnitID", "34");
        checkFieldValue(metadata, "benefitType", 36);
        checkFieldValue(metadata, "officePostcode", "AA19QX");
        validateClaimRef(payload.getClaimRef());
    }

    @Test
    public void applicantOverSixtyFive() throws IOException, InvalidNinoException {
        DateTimeFormatter df = DateTimeFormatter.ofPattern("d/M/yyyy", Locale.ENGLISH);
        LocalDate submissionDate = LocalDate.parse("25/07/2016", df);
        String applicantDOB = "31/10/1945";
        DSForm form = new DSForm();

        form.setDateOfBirth(applicantDOB);

        Ds1500Metadata payload = builderUnderTest.buildPayload(form, submissionDate);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode metadata = mapper.readTree(mapper.writeValueAsString(payload));

        checkFieldValue(metadata, "businessUnitID", "34");
        checkFieldValue(metadata, "benefitType", 36);
        checkFieldValue(metadata, "officePostcode", "AA19QX");
        validateClaimRef(payload.getClaimRef());
    }

    @Test
    public void applicantOverSixtyFiveSingleFigureDate() throws IOException, InvalidNinoException {
        DateTimeFormatter df = DateTimeFormatter.ofPattern("d/M/yyyy", Locale.ENGLISH);
        LocalDate submissionDate = LocalDate.parse("25/7/2016", df);
        String applicantDOB = "8/4/1945";
        DSForm form = new DSForm();

        form.setDateOfBirth(applicantDOB);

        Ds1500Metadata payload = builderUnderTest.buildPayload(form, submissionDate);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode metadata = mapper.readTree(mapper.writeValueAsString(payload));

        checkFieldValue(metadata, "businessUnitID", "34");
        checkFieldValue(metadata, "benefitType", 36);
        checkFieldValue(metadata, "officePostcode", "AA19QX");
        validateClaimRef(payload.getClaimRef());
    }

    private void validateClaimRef(String claimRef) {
        assertNotNull(claimRef);
        assertThat(claimRef.length(), is(equalTo(30)));
    }

    private void checkMetadataFields(String surname, String otherNames, String postcode, String niPrefix, String niSuffix,
                                     int dateOfBirthAsNumber, String businessUnitID, int benefitType, String payload) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode metadata = mapper.readTree(payload);

        checkStaticValues(metadata);

        checkFieldValue(metadata, "surname", surname);
        checkFieldValue(metadata, "forename", otherNames);
        checkFieldValue(metadata, "postCode", postcode);
        checkFieldValue(metadata, "dateOfBirth", dateOfBirthAsNumber);
        JsonNode nino = metadata.get("nino");
        checkFieldValue(nino, "ninoBody", niPrefix);
        checkFieldValue(nino, "ninoSuffix", niSuffix);
        checkFieldValue(metadata, "businessUnitID", businessUnitID);
        checkFieldValue(metadata, "benefitType", benefitType);

    }

    private DSForm buildForm(String surname, String otherNames, String postcode, String nINumber, String dateOfBirth) {
        DSForm form = new DSForm();
        form.setSurname(surname);
        form.setOtherNames(otherNames);
        form.setPostcode(postcode);
        form.setnINumber(nINumber);
        form.setDateOfBirth(dateOfBirth);
        return form;
    }

    private void checkStaticValues(JsonNode node) {
        checkFieldValue(node, "classification", 0);
        checkFieldValue(node, "documentType", 1242);
        checkFieldValue(node, "documentSource", 4);
    }

    private void checkFieldValue(JsonNode node, String metaDataFieldName, int expectedValue) {
        assertThat(node.get(metaDataFieldName).asInt(), is(equalTo(expectedValue)));
    }


    private void checkFieldValue(JsonNode node, String metadataFieldName, String expectedValue) {
        assertThat(node.get(metadataFieldName).asText(), is(equalTo(expectedValue)));
    }
}
