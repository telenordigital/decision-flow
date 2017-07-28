package com.telenordigital.decisionflow;
import java.util.Map;

public interface DecisionFlowDescriber {

    enum ElementType {
        INITIAL,
        SWITCH,
        RANDOM_SWITCH,
        ARROW,
        TARGET
    }

    void getElements(Callback callback);

    public interface Callback {
        void newElement(ElementDescriptor elementDescriptor);
    }

    public interface ElementDescriptor {
        String getId();
        String getName();
        ElementType getType();
        Map<String, Object> getAttributes();
        String getExpression();
        String getSourceNodeId();
        String getDestinationNodeId();
        boolean isDefault();
        boolean isObligatory();
    }
}
