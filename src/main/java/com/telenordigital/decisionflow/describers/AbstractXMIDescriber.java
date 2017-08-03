package com.telenordigital.decisionflow.describers;

import com.telenordigital.decisionflow.DecisionFlowDescriber;
import com.telenordigital.decisionflow.DecisionFlowException;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public abstract class AbstractXMIDescriber implements DecisionFlowDescriber {

    private final String umlFilePath;

    private final XPath xPath = createXPath();
    final InputSource inputSource;

    protected AbstractXMIDescriber(final String umlFilePath) {
        if (!new File(umlFilePath).exists()) {
            throw new DecisionFlowException(String.format("No such file: %s", umlFilePath));
        }
        this.umlFilePath = umlFilePath;
        this.inputSource = new InputSource(umlFilePath);

    }

    @Override
    public void getElements(final Callback callback) {
        final Properties properties = getLabels(umlFilePath);
        Map<String, Map<String, Object>> attrMaps = new HashMap<>();
        Map<String, List<String>> stereotypeMap = new HashMap<>();
        processStereotypes(attrMaps, stereotypeMap);
        final String query = "//subvertex | //transition";
        final List<String> namesWithLabels = new ArrayList<>();
        NodeList elements = null;
        try {
            elements = (NodeList) getXPath().evaluate(query, inputSource, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new DecisionFlowException("Unexpected UML");
        }
        for (int i = 0; i < elements.getLength(); i++) {
            final Node node = elements.item(i);
            ElementType elementType = null;

            if ("transition".equals(node.getNodeName())) {
                elementType = ElementType.ARROW;
            } else if ("subvertex".equals(node.getNodeName())) {
                String nodeType = eval("@xmi:type", node);
                if ("uml:State".equals(nodeType)) {
                    elementType = ElementType.TARGET;
                } else if ("uml:Pseudostate".equals(nodeType)) {
                    String nodeKind = eval("@kind", node);
                    if ("choice".equals(nodeKind)) {
                        final List<String> stereotypes = stereotypeMap.get(eval("@xmi:id", node));
                        if (stereotypes != null &&
                                (stereotypes.contains("Random") || stereotypes.contains("random"))) {
                            elementType = ElementType.RANDOM_SWITCH;
                        } else {
                            elementType = ElementType.SWITCH;
                        }
                    } else if (nodeKind == null || "initial".equals(nodeKind)) {
                        elementType = ElementType.INITIAL;
                    }
                }
            }

            if (elementType == null) {
                continue;
            }

            final String name = eval("@name", node);
            final String label = name != null ? properties.getProperty(name) : null;
            if (label != null && !label.isEmpty()) {
                if (namesWithLabels.contains(name)) {
                    throw new PapyrusException(
                            String.format("Duplicate element name: %s", name));
                }
                namesWithLabels.add(name);
            }

            final ElementType finalElementType = elementType;
            callback.newElement(new ElementDescriptor() {

                @Override
                public ElementType getType() {
                    return finalElementType;
                }
                @Override
                public String getName() {
                    return name;
                }

                @Override
                public String getId() {
                    return eval("@xmi:id", node);
                }

                @Override
                public String getExpression() {
                    return label != null ? label : name;
                }

                @Override
                public Map<String, Object> getAttributes() {
                    return attrMaps.get(getId());
                }

                @Override
                public String getSourceNodeId() {
                    return eval("@source", node);
                }

                @Override
                public String getDestinationNodeId() {
                    return eval("@target", node);
                }
                @Override
                public boolean isDefault() {
                    if (isObligatory()) {
                        return false;
                    }
                    return getExpression() == null || getExpression().isEmpty();
                }
                @Override
                public boolean isObligatory() {
                    return (stereotypeMap.get(getId()) == null)
                            ? false
                            : stereotypeMap.get(getId()).contains("always");
                }
            });
        }
    }

    protected XPath getXPath() {
        return xPath;
    }

    protected InputSource getInputSource() {
        return inputSource;
    }

    protected abstract void processStereotypes(
            final Map<String, Map<String, Object>> attrMaps,
            final Map<String, List<String>> stereotypeMap);
    protected abstract Properties getLabels(final String umlFilePath);
    protected abstract Map<String, String> getNsMap();

    private XPath createXPath() {
        final  Map<String, String> nsMap = getNsMap();
        final NamespaceContext nsContext = new NamespaceContext() {
            @SuppressWarnings("rawtypes")
            @Override
            public Iterator getPrefixes(final String namespaceURI) {
                return null;
            }
            @Override
            public String getPrefix(final String namespaceURI) {
                return null;
            }
            @Override
            public String getNamespaceURI(final String prefix) {
                return nsMap.get(prefix);
            }
        };
        final XPath xPath = XPathFactory.newInstance().newXPath();
        xPath.setNamespaceContext(nsContext);
        return xPath;
    }

    protected String eval(final String query, final Node node) {
        try {
            String result = getXPath().evaluate(query, node);
            return (result == null) ? null : (result.isEmpty() ? null : result);
        } catch (XPathExpressionException e) {
            throw new DecisionFlowException("Unexpected UML");
        }
    }

    @SuppressWarnings("serial")
    public class PapyrusException extends DecisionFlowException {
        PapyrusException(final String message) {
            super(message);
        }
    }
}
