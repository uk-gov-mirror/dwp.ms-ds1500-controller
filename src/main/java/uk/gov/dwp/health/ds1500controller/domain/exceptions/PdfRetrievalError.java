package uk.gov.dwp.health.ds1500controller.domain.exceptions;

public class PdfRetrievalError extends Exception {
  public PdfRetrievalError(String message) {
    super(message);
  }

  public PdfRetrievalError(Throwable cause) {
    super(cause);
  }
}
