package com.telenordigital.decisionflow;

@SuppressWarnings("serial")
public class DecisionFlowException extends RuntimeException {

    public DecisionFlowException(final String message) {
        super(message);
    }

    public DecisionFlowException(final String message, final Exception e) {
        super(message, e);
    }
}
