package uk.gov.dwp.health.ds1500controller.utils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import uk.gov.dwp.health.ds1500controller.domain.exceptions.InvalidCharactersException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@SuppressWarnings({"java:S5976"}) // Ignore requirement to use parameterised tests until upgraded to junit 5
public class InputHelperTest {
    private static final String ERROR_MSG = "This is a failure scenario which should have thrown exception";
    private final InputHelper helperUnderTest = new InputHelper();

    @Test
    public void confirmValidEntryIsNotRejected() throws InvalidCharactersException {
        String validInput = "abcdefghijklmnopqrstuvwxyzabcdef ghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz";
        assertThat(helperUnderTest.cleanInput(validInput), is(validInput));
    }

    @Test
    public void testSpecialPastedHtmlFontsAreAccepted() throws InvalidCharactersException {
        assertNotNull(helperUnderTest.cleanInput("the patient’s apostrophe is non-standard but acceptable!"));
    }

    @Test(expected = InvalidCharactersException.class)
    public void confirmLargeTextEntryOnWordTwoIsRejected() throws InvalidCharactersException {
        helperUnderTest.cleanInput("ab cdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz");
        fail(ERROR_MSG);
    }

    @Test(expected = InvalidCharactersException.class)
    public void confirmHtmlTagsWillBeRejected() throws InvalidCharactersException {
        helperUnderTest.cleanInput("abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz");
        fail(ERROR_MSG);
    }
}
