package uk.gov.dwp.health.ds1500controller.domain.exceptions;

public class InvalidJsonException extends Exception {

  public InvalidJsonException(String message) {
    super(message);
  }

  public InvalidJsonException(Throwable cause) {
    super(cause);
  }
}
