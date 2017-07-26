package com.telenordigital.decisionflow;

import com.telenordigital.decisionflow.DecisionFlowDescriber.Callback;
import com.telenordigital.decisionflow.DecisionFlowDescriber.ElementDescriptor;
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
                        final InitialNode initNode = new InitialNode(
                                elementDescriptor.getId(),
                                elementDescriptor.getName(),
                                elementDescriptor.getAttributes());
                        initialNode = initNode;
                        nodeMap.put(initNode.getId(), initNode);
                        break;
                    case SWITCH:
                        final Switch switchNode = new Switch(
                                elementDescriptor.getId(),
                                elementDescriptor.getName(),
                                elementDescriptor.getAttributes(),
                                elementDescriptor.getExpression());
                        nodeMap.put(switchNode.getId(), switchNode);
                        break;
                    case RANDOM_SWITCH:
                        final RandomSwitch randomSwitchNode = new RandomSwitch(
                                elementDescriptor.getId(),
                                elementDescriptor.getName(),
                                elementDescriptor.getAttributes(),
                                elementDescriptor.getExpression());
                        nodeMap.put(randomSwitchNode.getId(), randomSwitchNode);
                        break;
                    case TARGET:
                        final Target targetNode = new Target(
                                elementDescriptor.getId(),
                                elementDescriptor.getName(),
                                elementDescriptor.getAttributes(),
                                elementDescriptor.getExpression());
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
            final String xpr = arrowDescriptor.getExpression();
            final Arrow arrow = new Arrow(
                    arrowDescriptor.getId(),
                    arrowDescriptor.isDefault()
                        ? ArrowType.DEFAULT
                        : (arrowDescriptor.isObligatory() ? ArrowType.OBLIGATORY : ArrowType.ORDINARY),
                    arrowDescriptor.getName(),
                    arrowDescriptor.getAttributes(),
                    (xpr == null || xpr.isEmpty()) ? null : xpr, dstNode);
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
        final List<DecisionPathElement> path = new ArrayList<>();
        final List<Decision<P>> decisions = new ArrayList<>();
        getDecisions(context, initialNode, decisions, path, stopAtFirstFound);
        return decisions;
    }

    private void getDecisions(
            final C context,
            final AbstractNode currentNode,
            final List<Decision<P>> accDecisions,
            final List<DecisionPathElement> accPath,
            final boolean stopAtFirstFound) {

        if (!skipLoopDetection && accPath.contains(currentNode)) {
            throw new DecisionFlowException(
                    String.format("Loops detected in the decision flow (%s)",
                            currentNode.getName()));
        }
        accPath.add(currentNode);
        if (currentNode instanceof Target) {
            final List<DecisionPathElement> snapshotPath = new ArrayList<>(accPath);
            accDecisions.add(new Decision<P>() {

                @Override
                public String getName() {
                    return currentNode.getName();
                }

                @Override
                public Map<String, Object> getAttributes() {
                    return evalAttributes(context, currentNode.getAttributes());
                }

                @Override
                public List<DecisionPathElement> getDecisionPath() {
                    return snapshotPath;
                }

                @Override
                @SuppressWarnings("unchecked")
                public P getPayload() {
                    return (P) ((Target) currentNode).getParsedExpression().getValue(context);
                }

                @Override
                public String getId() {
                    return currentNode.getId();
                }
            });
            if (stopAtFirstFound) {
                return;
            }
        }
        Arrow defaultArrow = null;
        Object switchExprResult = null;
        if (currentNode instanceof Switch) {
            switchExprResult = ((Switch) currentNode).getParsedExpression().getValue(context);
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
                final Object arrowExprResult = arrow.getParsedExpression().getValue(context);
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
            final Map<String, String> originalAttributes) {

        final Map<String, Object> attributes = new HashMap<>();
        for (final String key: originalAttributes.keySet()) {
            final String value = originalAttributes.get(key);
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

    private interface ElementWithExpression {
        String getExpression();
        Expression getParsedExpression();
    }

    private interface Node {
        List<Arrow> getArrows();
    }

    private abstract static class AbstractElement implements DecisionPathElement {
        private final String id;
        private final String name;
        private final Map<String, String> attributes = new HashMap<>();

        AbstractElement(
                final String id,
                final String name,
                final Map<String, String> attributes) {
            this.id = id;
            this.name = name;
            if (attributes != null) {
                this.attributes.putAll(attributes);
            }
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getName() {
            return name;
        }

        public Map<String, String> getAttributes() {
            return attributes;
        }

        @Override
        public String toString() {
            final String name = getName();
            String expression = null;
            if (this instanceof ElementWithExpression) {
                expression = ((ElementWithExpression) this).getExpression();
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

    private abstract static class AbstractNode extends AbstractElement implements Node {
        private List<Arrow> arrows = new ArrayList<>();
        AbstractNode(
                final String id,
                final String name,
                final Map<String, String> attributes) {
            super(id, name, attributes);
        }

        @Override
        public List<Arrow> getArrows() {
            return arrows;
        }
    }

    private static class InitialNode extends AbstractNode {
        InitialNode(
                final String id,
                final String name,
                final Map<String, String> attributes) {
            super(id, name, attributes);
        }
    }

    private static class ExpressionHolder implements ElementWithExpression {
        private static final ExpressionParser EXPRESSION_PARSER =
                new SpelExpressionParser();

        private final String expression;
        private final Expression parsedExpression;
        ExpressionHolder(final String expression) {
            this.expression = expression;
            parsedExpression = EXPRESSION_PARSER.parseExpression(expression);
        }

        @Override
        public String getExpression() {
            return expression;
        }

        @Override
        public Expression getParsedExpression() {
            return parsedExpression;
        }
    }

    private static class Switch extends AbstractNode implements ElementWithExpression {
        private final ExpressionHolder expressionHolder;
        Switch(final String id,
               final String name,
               final Map<String, String> attributes,
               final String expression) {
            super(id, name, attributes);
            this.expressionHolder = new ExpressionHolder(expression);
        }

        @Override
        public String getExpression() {
            return expressionHolder.getExpression();
        }

        @Override
        public Expression getParsedExpression() {
            return expressionHolder.getParsedExpression();
        }
    }

    private static class RandomSwitch extends Switch {
        RandomSwitch(final String id,
                     final String name,
                     final Map<String, String> attributes,
                     final String expression) {
            super(id, name, attributes, expression);
        }
    }

    private enum ArrowType {DEFAULT, OBLIGATORY, ORDINARY};
    private static class Arrow extends AbstractElement implements ElementWithExpression{
        private final ExpressionHolder expressionHolder;
        private final AbstractNode destination;
        private final ArrowType arrowType;
        Arrow(final String id,
              final ArrowType arrowType,
              final String name,
              final Map<String, String> attributes,
              final String expression,
              final AbstractNode destination) {
            super(id, name, attributes);
            this.arrowType = arrowType;
            this.destination = destination;
            this.expressionHolder = new ExpressionHolder(expression);
        }

        AbstractNode getDestination() {
            return destination;
        }
        @Override
        public String getExpression() {
            return expressionHolder.getExpression();
        }
        @Override
        public Expression getParsedExpression() {
            return expressionHolder.getParsedExpression();
        }

        public ArrowType getArrowType() {
            return arrowType;
        }
    }

    private static class Target extends AbstractNode implements ElementWithExpression {
        private final ExpressionHolder expressionHolder;
        Target(final String id,
               final String name,
               final Map<String, String> attributes,
               final String expression) {
            super(id, name, attributes);
            this.expressionHolder = new ExpressionHolder(expression);
        }

        @Override
        public String getExpression() {
            return expressionHolder.getExpression();
        }

        @Override
        public Expression getParsedExpression() {
            return expressionHolder.getParsedExpression();
        }
    }
}
