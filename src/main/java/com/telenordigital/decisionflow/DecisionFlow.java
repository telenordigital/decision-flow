package com.telenordigital.decisionflow;

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
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

public class DecisionFlow<C, P> implements DecisionMachine<C, P> {

    private InitialNode initialNode = null;
    private final Map<String, AbstractNode> nodeMap = new HashMap<>();
    private boolean skipLoopDetection = false;

    private DecisionFlow(final DecisionFlowDescriber describer) {
        load(describer);
    }

    private DecisionFlow(final DecisionFlowDescriber describer, boolean skipLoopDetection) {
        this.skipLoopDetection = skipLoopDetection;
        load(describer);
    }

    public static <C, P> DecisionFlow<C, P> getInstance(final DecisionFlowDescriber describer) {
        return new DecisionFlow<>(describer);
    }

    public static <C, P> DecisionFlow<C, P> getInstance(
            final DecisionFlowDescriber describer,
            final boolean skipLoopDetection) {
        return new DecisionFlow<>(describer, skipLoopDetection);
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
            public Map<String, Object> getAttributes() {
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
            Collections.sort(node.arrows, new Comparator<Arrow>() {
                @Override
                public int compare(Arrow o1, Arrow o2) {
                    return o1.getArrowType().ordinal() - o2.getArrowType().ordinal();
                }
            });
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

        if (!skipLoopDetection && accPath.contains(currentNode)) {
            throw new DecisionFlowException(
                    String.format("Loops detected in the decision flow (%s)",
                            currentNode.getName()));
        }
        accPath.add(currentNode);
        if (currentNode instanceof Target) {
            final List<ElementDescriptor> snapshotPath = new ArrayList<>(accPath);
            final List<Decision<P>> snapshotDecisions = new ArrayList<>(accDecisions);
            final Decision<P> decision = new Decision<P>() {

                @Override
                public String getName() {
                    return currentNode.getName();
                }

                @Override
                public Map<String, Object> getAttributes() {
                    return evalAttributes(context, currentNode.getAttributes());
                }

                @Override
                public List<ElementDescriptor> getDecisionPath() {
                    return snapshotPath;
                }

                @Override
                @SuppressWarnings("unchecked")
                public P getPayload() {
                    return (P) eval(context, ((Target) currentNode).getExpressionHolder());
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
            switchExprResult = eval(context, ((Switch) currentNode).getExpressionHolder());
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
                final Object arrowExprResult = eval(context, arrow.getExpressionHolder());
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

    private Map<String, Object> evalAttributes(
            final C context,
            final Map<String, Object> originalAttributes) {

        final Map<String, Object> attributes = new HashMap<>();
        for (final String key: originalAttributes.keySet()) {
            final String value = (String) originalAttributes.get(key);
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

    private Object eval(C context, ExpressionHolder expressionHolder) {
        return expressionHolder.getParsedExpression().getValue(context);
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
        public Map<String, Object> getAttributes() {
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
        AbstractNode(final ElementDescriptor elementDescriptor) {
            super(elementDescriptor);
        }

        public List<Arrow> getArrows() {
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

        private final String expression;
        private final Expression parsedExpression;
        ExpressionHolder(final String expression) {
            this.expression = expression;
            parsedExpression = EXPRESSION_PARSER.parseExpression(expression);
        }

        public String getExpression() {
            return expression;
        }

        public Expression getParsedExpression() {
            return parsedExpression;
        }
    }

    private static class Switch extends AbstractNode implements ElementWithExpressionHolder {
        private final ExpressionHolder expressionHolder;
        Switch(final ElementDescriptor elementDescriptor) {
            super(elementDescriptor);
            this.expressionHolder = new ExpressionHolder(elementDescriptor.getExpression());
        }

        @Override
        public ExpressionHolder getExpressionHolder() {
            return expressionHolder;
        }
    }

    private static class RandomSwitch extends Switch {
        RandomSwitch(final ElementDescriptor elementDescriptor) {
            super(elementDescriptor);
        }
    }

    private enum ArrowType {DEFAULT, OBLIGATORY, ORDINARY};
    private static class Arrow extends AbstractElement implements ElementWithExpressionHolder{
        private final ExpressionHolder expressionHolder;
        private final AbstractNode destination;
        private final ArrowType arrowType;
        Arrow(final ElementDescriptor elementDescriptor, final AbstractNode destination) {
            super(elementDescriptor);
            this.arrowType =
                    elementDescriptor.isObligatory()
                        ? ArrowType.OBLIGATORY
                        : (elementDescriptor.isDefault() ? ArrowType.DEFAULT : ArrowType.ORDINARY);
            this.destination = destination;
            this.expressionHolder = new ExpressionHolder(elementDescriptor.getExpression());
        }

        AbstractNode getDestination() {
            return destination;
        }
        @Override
        public ExpressionHolder getExpressionHolder() {
            return expressionHolder;
        }

        public ArrowType getArrowType() {
            return arrowType;
        }
    }

    private static class Target extends AbstractNode implements ElementWithExpressionHolder {
        private final ExpressionHolder expressionHolder;
        Target(final ElementDescriptor elementDescriptor) {
            super(elementDescriptor);
            this.expressionHolder = new ExpressionHolder(elementDescriptor.getExpression());
        }

        @Override
        public ExpressionHolder getExpressionHolder() {
            return expressionHolder;
        }
    }
}
