package com.telenordigital.decisionflow;

import java.util.List;

public interface DecisionMachine<C, P> {
    Decision<P> getDecision(C context);
    List<Decision<P>> getDecisions(C context);
}
