package com.mastercard.developer.exception;

public class ReferenceAppGenericException extends Exception {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 1L;

  public ReferenceAppGenericException(final String msg) {
    super(msg);
  }

  public ReferenceAppGenericException(final String msg, final Throwable cause) {
    super(msg, cause);
  }

  public ReferenceAppGenericException(final Throwable cause) {
    super(cause);
  }
}
