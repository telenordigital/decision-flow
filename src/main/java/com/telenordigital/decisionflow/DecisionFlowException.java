package com.telenordigital.decisionflow;

public class DecisionFlowException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DecisionFlowException(final String message) {
        super(message);
    }

    public DecisionFlowException(final String message, final Exception e) {
        super(message, e);
    }
}
