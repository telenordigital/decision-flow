package com.telenordigital.decisionflow;

import com.telenordigital.decisionflow.DecisionFlowDescriber.ElementDescriptor;
import java.util.List;
import java.util.Map;

public interface Decision<P> extends ElementDescriptor {
    P getPayload();
    List<ElementDescriptor> getDecisionPath();
    List<Decision<P>> getDecisions();

    public interface OnAttributesCallback {
        void onAttributes(Map<String, ?> attributes);
    }
}
