package com.telenordigital.decisionflow;

import com.telenordigital.decisionflow.DecisionFlowDescriber.ElementDescriptor;
import java.util.List;

public interface Decision<P> extends ElementDescriptor {
    P getPayload();
    List<ElementDescriptor> getDecisionPath();
    List<Decision<P>> getDecisions();
}
