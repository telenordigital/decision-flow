package com.telenordigital.decisionflow;

import com.telenordigital.decisionflow.DecisionFlowDescriber.ElementDescriptor;
import java.util.List;

public interface Decision<T> extends ElementDescriptor {
    T getPayload();
    List<ElementDescriptor> getDecisionPath();
}
