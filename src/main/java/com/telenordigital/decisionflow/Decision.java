package com.telenordigital.decisionflow;

import java.util.List;
import java.util.Map;

public interface Decision<T> extends DecisionPathElement {
    T getPayload();
    Map<String, Object> getAttributes();
    List<DecisionPathElement> getDecisionPath();
}
