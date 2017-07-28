package com.telenordigital.decisionflow.describers;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.telenordigital.decisionflow.DecisionFlowDescriber;
import com.telenordigital.decisionflow.DecisionFlowException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JsonDescriber implements DecisionFlowDescriber {

    private static final ObjectMapper mapper = new ObjectMapper();

    private List<ElementDescriptor> elements = new ArrayList<>();

    private JsonDescriber(final DecisionFlowDescriber describer) {
        describer.getElements(new Callback() {

            @Override
            public void newElement(ElementDescriptor elementDescriptor) {
                elements.add(new ElementDescriptorImpl(elementDescriptor));
            }
        });
    }

    private JsonDescriber(final String json) {
        try {
            this.elements =
                    mapper.readValue(json,
                            TypeFactory
                            .defaultInstance()
                            .constructCollectionType(List.class, ElementDescriptorImpl.class));
        } catch (IOException e) {
            throw new DecisionFlowException("Could not deserialize json string.");
        }
    }


    public static DecisionFlowDescriber getInstance(final DecisionFlowDescriber describer) {
        return new JsonDescriber(describer);
    }

    public static DecisionFlowDescriber getInstance(final String json) {
        return new JsonDescriber(json);
    }

    public String getJson() {
        try {
            return mapper.writeValueAsString(elements);
        } catch (JsonProcessingException e) {
            throw new DecisionFlowException("Could not serialize elements.");
        }
    }

    @Override
    public void getElements(Callback callback) {
        for (ElementDescriptor element : elements) {
            callback.newElement(element);
        }
    }

    private static class ElementDescriptorImpl implements ElementDescriptor {
        private String id;
        private String name;
        private ElementType type;
        private Map<String, Object> attributes;
        private String expression;
        private String sourceNodeId;
        private String destinationNodeId;
        private boolean isDefault;
        private boolean isObligatory;

        @JsonCreator
        public ElementDescriptorImpl(
                final @JsonProperty("id") String id,
                final @JsonProperty("name") String name,
                final @JsonProperty("type") ElementType type,
                final @JsonProperty("attributes") Map<String, Object> attributes,
                final @JsonProperty("expression") String expression,
                final @JsonProperty("sourceNodeId") String sourceNodeId,
                final @JsonProperty("destinationNodeId") String destinationNodeId,
                final @JsonProperty("default") boolean isDefault,
                final @JsonProperty("obligatory") boolean isObligatory) {
            super();
            this.id = id;
            this.name = name;
            this.type = type;
            this.attributes = attributes;
            this.expression = expression;
            this.sourceNodeId = sourceNodeId;
            this.destinationNodeId = destinationNodeId;
            this.isDefault = isDefault;
            this.isObligatory = isObligatory;
        }

        public ElementDescriptorImpl(ElementDescriptor other) {
            this.id = other.getId();
            this.name = other.getName();
            this.type = other.getType();
            this.attributes = other.getAttributes();
            this.expression = other.getExpression();
            this.sourceNodeId = other.getSourceNodeId();
            this.destinationNodeId = other.getDestinationNodeId();
            this.isDefault = other.isDefault();
            this.isObligatory = other.isObligatory();
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public ElementType getType() {
            return type;
        }

        @Override
        public Map<String, Object> getAttributes() {
            return attributes;
        }

        @Override
        public String getExpression() {
            return expression;
        }

        @Override
        public String getSourceNodeId() {
            return sourceNodeId;
        }

        @Override
        public String getDestinationNodeId() {
            return destinationNodeId;
        }

        @Override
        public boolean isDefault() {
            return isDefault;
        }

        @Override
        public boolean isObligatory() {
            return isObligatory;
        }
    }
}
