package uk.gov.dwp.health.ds1500controller.utils;

import org.apache.commons.text.StringEscapeUtils;
import uk.gov.dwp.health.ds1500controller.domain.exceptions.InvalidCharactersException;

public class InputHelper {
  public String cleanInput(String input) throws InvalidCharactersException {
    String cleanedString;
    if (isValid(input)) {
      cleanedString = StringEscapeUtils.escapeHtml4(input);
    } else {
      throw new InvalidCharactersException(
          String.format("Provided Input (%s) fails character content validation checks", input));
    }
    return cleanedString;
  }

  private boolean isValid(String textAreaField) {
    if (textAreaField == null || textAreaField.isEmpty()) {
      return true;
    }
    for (String retval : textAreaField.split("\\s+")) {
      if (retval.length() > 58) {
        return false;
      }
    }
    return true;
  }
}
