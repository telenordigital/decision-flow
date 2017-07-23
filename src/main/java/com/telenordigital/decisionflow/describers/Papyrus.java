package com.telenordigital.decisionflow.describers;

import com.telenordigital.decisionflow.DecisionFlowDescriber;
import com.telenordigital.decisionflow.DecisionFlowException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class Papyrus implements DecisionFlowDescriber {

    private final String umlFilePath;

    private Papyrus(final String umlFilePath) {
        if (!new File(umlFilePath).exists()) {
            throw new DecisionFlowException(String.format("No such file: %s", umlFilePath));
        }
        this.umlFilePath = umlFilePath;
    }

    public static DecisionFlowDescriber getInstance(final String umlFilePath) {
        return new Papyrus(umlFilePath);
    }

    @Override
    public void getElements(final Callback callback) {
        final Properties properties = getProperties(umlFilePath);
        final XPath xPath = getXPath();
        final InputSource inputSource = new InputSource(umlFilePath);
        final Map<String, Map<String, String>> attrMaps =
                getAttributeMaps(xPath, inputSource);
        final String query = "//subvertex | //transition";
        final List<String> namesWithLabels = new ArrayList<>();
        NodeList elements = null;
        try {
            elements = (NodeList) xPath.evaluate(query, inputSource, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new DecisionFlowException("Unexpected UML");
        }
        for (int i = 0; i < elements.getLength(); i++) {
            final Node node = elements.item(i);
            ElementType elementType = null;

            if ("transition".equals(node.getNodeName())) {
                elementType = ElementType.ARROW;
            } else if ("subvertex".equals(node.getNodeName())) {
                String nodeType = eval(xPath, "@xmi:type", node);
                if ("uml:State".equals(nodeType)) {
                    elementType = ElementType.TARGET;
                } else if ("uml:Pseudostate".equals(nodeType)) {
                    String nodeKind = eval(xPath, "@kind", node);
                    if ("choice".equals(nodeKind)) {
                        elementType = ElementType.SWITCH;
                    } else if (nodeKind == null) {
                        elementType = ElementType.INITIAL;
                    }
                }
            }

            if (elementType == null) {
                continue;
            }

            final String name = eval(xPath, "@name", node);
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
                    return eval(xPath, "@xmi:id", node);
                }

                @Override
                public String getExpression() {
                    return label != null ? label : name;
                }

                @Override
                public Map<String, String> getAttributes() {
                    return attrMaps.get(getId());
                }

                @Override
                public String getSourceNodeId() {
                    return eval(xPath, "@source", node);
                }

                @Override
                public String getDestinationNodeId() {
                    return eval(xPath, "@target", node);
                }
            });
        }
    }

    private Map<String, Map<String, String>> getAttributeMaps(final XPath xPath,
            final InputSource inputSource) {
        final Map<String, Map<String, String>> attrMaps = new HashMap<>();
        NodeList elements = null;
        try {
            elements = (NodeList) xPath.evaluate("/xmi:XMI/*",
                    inputSource, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new DecisionFlowException("Unexpected UML");
        }
        for (int i = 0; i < elements.getLength(); i++) {
            final Node node = elements.item(i);
            final String baseState = eval(xPath, "@base_State", node);
            if (baseState != null && !baseState.isEmpty()) {
                final Map<String, String> attrs = new HashMap<>();
                final NamedNodeMap nnm = node.getAttributes();
                for (int j = 0; j < nnm.getLength(); j++) {
                    final Node nnmNode = nnm.item(j);
                    final String nnmNodeName = nnmNode.getNodeName();
                    if (!nnmNodeName.contains(":") && !nnmNodeName.equals("base_State")) {
                        attrs.put(nnmNodeName, nnmNode.getNodeValue());
                    }
                }
                if (!attrs.isEmpty()) {
                    attrMaps.put(baseState, attrs);
                }
            }
        }
        return attrMaps;
    }

    private static Properties getProperties(final String umlFilePath) {
        final Path path = Paths.get(umlFilePath);
        final String[] fileAndExtension = path.getFileName().toString().split("\\.");
        if (fileAndExtension.length != 2 || !"uml".equals(fileAndExtension[1])) {
            throw new DecisionFlowException(
                    String.format("File name without .uml extension: %s", umlFilePath));
        }
        final String propertyFilePath = path.getParent().resolve(
                String.format("%s_en_US.properties", fileAndExtension[0])).toString();
        final Properties origProperties = new Properties();
        try {
            origProperties.load(new FileReader(propertyFilePath));
        } catch (IOException e) {
            throw new DecisionFlowException("Could not read property file.", e);
        }
        final Properties properties = new Properties();
        for (final Entry<Object, Object> entry : origProperties.entrySet()) {
            String[] split = entry.getKey().toString().split("__");
            properties.put(split[split.length - 1], entry.getValue().toString());
        }
        return properties;
    }

    private static XPath getXPath() {
        final Map<String, String> nsMap = new HashMap<>();
        nsMap.put("xmi", "http://www.omg.org/spec/XMI/20131001");
        nsMap.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        nsMap.put("ecore", "http://www.eclipse.org/emf/2002/Ecore");
        nsMap.put("uml", "http://www.eclipse.org/uml2/5.0.0/UML");
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

    private static String eval(final XPath xPath, final String query, final Node node) {
        try {
            String result = xPath.evaluate(query, node);
            return (result == null) ? null : (result.isEmpty() ? null : result);
        } catch (XPathExpressionException e) {
            throw new DecisionFlowException("Unexpected UML");
        }
    }

    public class PapyrusException extends DecisionFlowException {
        PapyrusException(final String message) {
            super(message);
        }
    }
}
