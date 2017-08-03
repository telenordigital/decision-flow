package com.telenordigital.decisionflow;

import com.telenordigital.decisionflow.Decision.OnAttributesCallback;
import com.telenordigital.decisionflow.DecisionFlowDescriber.Callback;
import com.telenordigital.decisionflow.DecisionFlowDescriber.ElementDescriptor;
import com.telenordigital.decisionflow.DecisionFlowDescriber.ElementType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

public class DecisionFlow<C, P> implements DecisionMachine<C, P> {

    private InitialNode initialNode = null;
    private final Map<String, AbstractNode> nodeMap = new HashMap<>();

    private DecisionFlow(final DecisionFlowDescriber describer) {
        load(describer);
    }

    public static <C, P> DecisionFlow<C, P> getInstance(final DecisionFlowDescriber describer) {
        return new DecisionFlow<>(describer);
    }

    @Override
    public Decision<P> getDecision(final C context) {
        final List<Decision<P>> decisions = getDecisions(context, true);
        return decisions.isEmpty() ? null : decisions.get(0);
    }

    @Override
    public List<Decision<P>> getDecisions(final C context) {
        return getDecisions(context, false);
    }

    public Decision<P> continueFrom(final Decision<P> decision, final C context) {
        return continueFrom(context, decision);
    }

    public Decision<P> continueFrom(final String decisionId, final C context) {
        final AbstractNode node = nodeMap.get(decisionId);
        if (node == null) {
            throw new DecisionFlowException("Node not found.");
        }
        if (!(node instanceof Target)) {
            throw new DecisionFlowException("Not a decision.");
        }
        final List<Decision<P>> decisions = new ArrayList<>();
        final List<ElementDescriptor> descriptors = new ArrayList<>();

        final Decision<P> fakeDecision = new Decision<P>() {

            @Override
            public String getId() {
                return node.getId();
            }

            @Override
            public String getName() {
                return node.getName();
            }

            @Override
            public ElementType getType() {
                return node.getType();
            }

            @Override
            public Map<String, ?> getAttributes() {
                return node.getAttributes();
            }

            @Override
            public String getExpression() {
                return node.getExpression();
            }

            @Override
            public String getSourceNodeId() {
                return node.getSourceNodeId();
            }

            @Override
            public String getDestinationNodeId() {
                return node.getDestinationNodeId();
            }

            @Override
            public boolean isDefault() {
                return node.isDefault();
            }

            @Override
            public boolean isObligatory() {
                return node.isObligatory();
            }

            @Override
            public P getPayload() {
                return null;
            }

            @Override
            public List<ElementDescriptor> getDecisionPath() {
                return descriptors;
            }

            @Override
            public List<Decision<P>> getDecisions() {
                return decisions;
            }
        };
        return continueFrom(context, fakeDecision);
    }

    private Decision<P> continueFrom(
            final C context,
            final Decision<P> decision) {

        final AbstractNode node = nodeMap.get(decision.getId());
        if (node == null) {
            throw new DecisionFlowException("Node not found.");
        }
        if (node.getArrows().size() == 0) {
            return null;
        }

        if (node.getArrows().size() > 1) {
            throw new DecisionFlowException("Multiple paths found to continue from.");
        }

        final int oldSize = decision.getDecisions().size();
        getDecisions(context, node.getArrows().get(0).getDestination(),
                decision.getDecisions(), decision.getDecisionPath(), true);
        final int newSize = decision.getDecisions().size();
        return (oldSize < newSize)
                ? decision.getDecisions().get(newSize - 1)
                : null;
    }

    private void load(final DecisionFlowDescriber describer) {
        final Collection<ElementDescriptor> arrows = new ArrayList<>();
        describer.getElements(new Callback() {
            @Override
            public void newElement(final ElementDescriptor elementDescriptor) {
                switch (elementDescriptor.getType()) {
                    case INITIAL:
                        if (initialNode != null) {
                            throw new DecisionFlowException(
                                    "Multiple initial nodes found.");
                        }
                        final InitialNode initNode = new InitialNode(elementDescriptor);
                        initialNode = initNode;
                        nodeMap.put(initNode.getId(), initNode);
                        break;
                    case SWITCH:
                        final Switch switchNode = new Switch(elementDescriptor);
                        nodeMap.put(switchNode.getId(), switchNode);
                        break;
                    case RANDOM_SWITCH:
                        final RandomSwitch randomSwitchNode = new RandomSwitch(elementDescriptor);
                        nodeMap.put(randomSwitchNode.getId(), randomSwitchNode);
                        break;
                    case TARGET:
                        final Target targetNode = new Target(elementDescriptor);
                        nodeMap.put(targetNode.getId(), targetNode);
                        break;
                    case ARROW:
                        arrows.add(elementDescriptor);
                        break;
                    default:
                        throw new DecisionFlowException(
                                String.format(
                                        "Unsupported element type: %s",
                                        elementDescriptor.getType()));
                }
            }
        });

        if (initialNode == null) {
            throw new DecisionFlowException("No initial node found.");
        }

        for (final ElementDescriptor arrowDescriptor : arrows) {
            final AbstractNode srcNode = nodeMap.get(arrowDescriptor.getSourceNodeId());
            if (srcNode == null) {
                throw new DecisionFlowException(
                        String.format("Source node not found (%s->%s).",
                                arrowDescriptor.getName(),
                                arrowDescriptor.getSourceNodeId()));
            }
            final AbstractNode dstNode =
                    nodeMap.get(arrowDescriptor.getDestinationNodeId());
            if (dstNode == null) {
                throw new DecisionFlowException(
                        String.format("Destination node not found (%s->%s).",
                                arrowDescriptor.getName(),
                                arrowDescriptor.getDestinationNodeId()));
            }
            final Arrow arrow = new Arrow(arrowDescriptor, dstNode);
            srcNode.arrows.add(arrow);
        }
        for (AbstractNode node : nodeMap.values()) {
            Collections.sort(node.getArrows(), new Comparator<Arrow>() {
                @Override
                public int compare(Arrow o1, Arrow o2) {
                    return o1.getArrowType().ordinal() - o2.getArrowType().ordinal();
                }
            });
            if (node instanceof RandomSwitch) {
                ((RandomSwitch) node).ignite();
            }

        }
    }

    private List<Decision<P>> getDecisions(
            final C context,
            final boolean stopAtFirstFound) {
        final List<ElementDescriptor> path = new ArrayList<>();
        final List<Decision<P>> decisions = new ArrayList<>();
        getDecisions(context, initialNode, decisions, path, stopAtFirstFound);
        return decisions;
    }

    private void getDecisions(
            final C context,
            final AbstractNode currentNode,
            final List<Decision<P>> accDecisions,
            final List<ElementDescriptor> accPath,
            final boolean stopAtFirstFound) {

        if (accPath.contains(currentNode)) {
            throw new DecisionFlowException(
                    String.format("Loops detected in the decision flow (%s)",
                            currentNode.getName()));
        }
        accPath.add(currentNode);
        if (currentNode instanceof Target) {
            final List<ElementDescriptor> snapshotPath = new ArrayList<>(accPath);
            final List<Decision<P>> snapshotDecisions = new ArrayList<>(accDecisions);
            @SuppressWarnings({ "unchecked"})
            final P payload = (P) ((Target) currentNode).getExpressionHolder().eval(context);
            Map<String, ?> attributes = evalAttributes(context, currentNode.getAttributes());
            if (payload instanceof OnAttributesCallback) {
                ((OnAttributesCallback) payload).onAttributes(attributes);
            }
            final Decision<P> decision = new Decision<P>() {

                @Override
                public String getName() {
                    return currentNode.getName();
                }

                @Override
                public Map<String, ?> getAttributes() {
                    return attributes;
                }

                @Override
                public List<ElementDescriptor> getDecisionPath() {
                    return snapshotPath;
                }

                @Override
                public P getPayload() {
                    return payload;
                }

                @Override
                public String getId() {
                    return currentNode.getId();
                }

                @Override
                public ElementType getType() {
                    return currentNode.getType();
                }

                @Override
                public String getExpression() {
                    return currentNode.getExpression();
                }

                @Override
                public String getSourceNodeId() {
                    return currentNode.getSourceNodeId();
                }

                @Override
                public String getDestinationNodeId() {
                    return currentNode.getDestinationNodeId();
                }

                @Override
                public boolean isDefault() {
                    return currentNode.isDefault();
                }

                @Override
                public boolean isObligatory() {
                    return currentNode.isObligatory();
                }

                @Override
                public List<Decision<P>> getDecisions() {
                    if (!snapshotDecisions.contains(this)) {
                        snapshotDecisions.add(this);
                    }
                    return snapshotDecisions;
                }
            };

            accDecisions.add(decision);
            if (stopAtFirstFound) {
                return;
            }
        }
        Arrow defaultArrow = null;
        Object switchExprResult = null;
        if (currentNode instanceof Switch) {
            switchExprResult = ((Switch) currentNode).getExpressionHolder().eval(context);
        }
        for (Arrow arrow : currentNode.getArrows()) {
            switch (arrow.arrowType) {
            case DEFAULT:
                if (defaultArrow != null) {
                    throw new DecisionFlowException(
                            String.format(
                                    "Multiple default paths detected from node %s.",
                                    currentNode.getName()));
                }
                defaultArrow = arrow;
                break;
            case ORDINARY:
                final Object arrowExprResult = arrow.getExpressionHolder().eval(context);
                if (areEqual(switchExprResult, arrowExprResult)) {
                    accPath.add(arrow);
                    getDecisions(
                            context,
                            arrow.getDestination(),
                            accDecisions,
                            accPath,
                            stopAtFirstFound);
                    return;
                }
                break;
            case OBLIGATORY:
                accPath.add(arrow);
                getDecisions(
                        context,
                        arrow.getDestination(),
                        accDecisions,
                        accPath,
                        stopAtFirstFound);
            }
        }
        if (defaultArrow != null) {
            accPath.add(defaultArrow);
            getDecisions(context,
                         defaultArrow.getDestination(),
                         accDecisions,
                         accPath,
                         stopAtFirstFound);
        }
    }

    private boolean areEqual(final Object nodeExprResult, final Object arrowExprResult) {
        if (nodeExprResult == null) {
            return (arrowExprResult == null);
        }
        return nodeExprResult.equals(arrowExprResult);
    }

    private Map<String, ?> evalAttributes(final C context, final Map<String, ?> map) {

        final Map<String, Object> attributes = new HashMap<>();
        for (final String key: map.keySet()) {
            final String value = (String) map.get(key);
            if (value == null) {
                continue;
            }
            try {
                final Expression expression = ExpressionHolder.EXPRESSION_PARSER
                        .parseExpression(value);
                attributes.put(key, expression.getValue(context));
            } catch (RuntimeException e) {
                attributes.put(key, value);
            }
        }
        return attributes;
    }

    private interface ElementWithExpressionHolder {
        ExpressionHolder getExpressionHolder();
    }

    private abstract static class AbstractElement implements ElementDescriptor {
        private final ElementDescriptor elementDescriptor;

        AbstractElement(final ElementDescriptor elementDescriptor) {
            this.elementDescriptor = elementDescriptor;
        }

        @Override
        public String getId() {
            return elementDescriptor.getId();
        }

        @Override
        public String getName() {
            return elementDescriptor.getName();
        }

        @Override
        public Map<String, ?> getAttributes() {
            return elementDescriptor.getAttributes();
        }

        @Override
        public ElementType getType() {
            return elementDescriptor.getType();
        }

        @Override
        public String getExpression() {
            return elementDescriptor.getExpression();
        }

        @Override
        public String getSourceNodeId() {
            return elementDescriptor.getSourceNodeId();
        }

        @Override
        public String getDestinationNodeId() {
            return elementDescriptor.getDestinationNodeId();
        }

        @Override
        public boolean isDefault() {
            return elementDescriptor.isDefault();
        }

        @Override
        public boolean isObligatory() {
            return elementDescriptor.isObligatory();
        }

        @Override
        public String toString() {
            final String name = getName();
            String expression = null;
            if (this instanceof ElementWithExpressionHolder) {
                expression = ((ElementWithExpressionHolder) this).getExpressionHolder().getExpression();
            }
            if (name != null && name.equals(expression)) {
                expression = null;
            }
            String kind = getClass().getSimpleName();
            if (this instanceof Arrow) {
                if (ArrowType.OBLIGATORY.equals(((Arrow) this).getArrowType())) {
                    kind = kind + "(always)";
                }
            }
            return kind
                    + ((name != null) ? ":" + name : "")
                    + ((expression != null) ? ":" + expression : "");
        }
    }

    private abstract static class AbstractNode extends AbstractElement {
        private List<Arrow> arrows = new ArrayList<>();
        private AbstractNode(final ElementDescriptor elementDescriptor) {
            super(elementDescriptor);
        }

        List<Arrow> getArrows() {
            return arrows;
        }
    }

    private static class InitialNode extends AbstractNode {
        InitialNode(final ElementDescriptor elementDescriptor) {
            super(elementDescriptor);
        }
    }

    private static class ExpressionHolder {
        private static final ExpressionParser EXPRESSION_PARSER =
                new SpelExpressionParser();

        private String expression;
        private Expression parsedExpression;

        private ExpressionHolder(final String expression) {
            setExpression(expression);
        }

        private ExpressionHolder() {
        }

        private String getExpression() {
            return expression;
        }

        <C> Object eval(C context) {
            return parsedExpression.getValue(context);
        }

        private void setExpression(final String expression) {
            this.expression = expression;
            parsedExpression = EXPRESSION_PARSER.parseExpression(expression);
        }

        private void prepareExpression(final String expression) {
            parsedExpression = EXPRESSION_PARSER.parseExpression(expression);
        }
    }

    private static class Switch extends AbstractNode implements ElementWithExpressionHolder {
        private final ExpressionHolder expressionHolder;
        private Switch(final ElementDescriptor elementDescriptor) {
            super(elementDescriptor);
            this.expressionHolder = new ExpressionHolder(elementDescriptor.getExpression());
        }

        @Override
        public ExpressionHolder getExpressionHolder() {
            return expressionHolder;
        }
    }

    private static class RandomSwitch extends Switch {
        private ExpressionHolder randomisingExpressionHolder = null;

        private RandomSwitch(final ElementDescriptor elementDescriptor) {
            super(elementDescriptor);
        }

        @Override
        public ExpressionHolder getExpressionHolder() {
            if (randomisingExpressionHolder == null) {
                throw new DecisionFlowException("Random switch not initialised.");
            }
            return randomisingExpressionHolder;
        }

        private void ignite() {
            final Random random = new Random();
            final Integer sumNonNulls = getArrows()
                    .stream()
                    .filter(a -> a.getExpressionHolder().getExpression() != null)
                    .map(a -> Integer.valueOf(a.getExpressionHolder().getExpression()))
                    .reduce(0, (a, b) -> a + b);
            Integer sum = sumNonNulls;
            final List<Arrow> defaultArrows = getArrows()
                    .stream()
                    .filter(a -> a.isDefault())
                    .collect(Collectors.toList());
            if (defaultArrows.size() > 1) {
                throw new DecisionFlowException("Multiple default arrows found from a random switch.");
            }
            Arrow defaultArrow = null;
            if (defaultArrows.size() > 0) {
                defaultArrow = defaultArrows.get(0);
                sum = (sumNonNulls > 100) ? sumNonNulls : 100;
            }

            final Integer[] flags = new Integer[sum];
            int arrowIndex = 0;
            int flagIndex = 0;
            for (final Arrow arrow : getArrows()) {
                final int weight =
                        arrow.equals(defaultArrow)
                        ? 100 - sumNonNulls
                        : Integer.valueOf(arrow.getExpressionHolder().getExpression());
                arrow.getExpressionHolder().prepareExpression(String.valueOf(arrowIndex));
                for (int k = 0; k < weight; k++) {
                    flags[flagIndex++] = arrowIndex;
                }
                arrowIndex++;
            }
            randomisingExpressionHolder = new ExpressionHolder() {
                @Override
                protected Object eval(Object context) {
                    return flags[random.nextInt(flags.length)];
                }
            };
        }
    }

    private enum ArrowType {DEFAULT, OBLIGATORY, ORDINARY};
    private static class Arrow extends AbstractElement implements ElementWithExpressionHolder {
        private final ExpressionHolder expressionHolder;
        private final AbstractNode destination;
        private final ArrowType arrowType;
        private Arrow(final ElementDescriptor elementDescriptor, final AbstractNode destination) {
            super(elementDescriptor);
            this.arrowType =
                    elementDescriptor.isObligatory()
                        ? ArrowType.OBLIGATORY
                        : (elementDescriptor.isDefault() ? ArrowType.DEFAULT : ArrowType.ORDINARY);
            this.destination = destination;
            this.expressionHolder = new ExpressionHolder(elementDescriptor.getExpression());
        }

        private AbstractNode getDestination() {
            return destination;
        }
        @Override
        public ExpressionHolder getExpressionHolder() {
            return expressionHolder;
        }

        private ArrowType getArrowType() {
            return arrowType;
        }
    }

    private static class Target extends AbstractNode implements ElementWithExpressionHolder {
        private final ExpressionHolder expressionHolder;
        private Target(final ElementDescriptor elementDescriptor) {
            super(elementDescriptor);
            this.expressionHolder = new ExpressionHolder(elementDescriptor.getExpression());
        }

        @Override
        public ExpressionHolder getExpressionHolder() {
            return expressionHolder;
        }
    }
}
