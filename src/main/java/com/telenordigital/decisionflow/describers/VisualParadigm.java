package com.telenordigital.decisionflow.describers;

import com.telenordigital.decisionflow.DecisionFlowDescriber;
import com.telenordigital.decisionflow.DecisionFlowException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class VisualParadigm extends AbstractXMIDescriber {

    VisualParadigm(final String umlFilePath) {
        super(umlFilePath);
    }

    public static DecisionFlowDescriber getInstance(final String umlFilePath) {
        return new VisualParadigm(umlFilePath);
    }

    @Override
    protected Properties getLabels(String umlFilePath) {
        return new Properties();
    }

    @Override
    protected Map<String, String> getNsMap() {
        final Map<String, String> nsMap = new HashMap<>();
        nsMap.put("xmi", "http://schema.omg.org/spec/XMI/2.1");
        nsMap.put("uml", "http://schema.omg.org/spec/UML/2.0");
        return nsMap;
    }

    @Override
    protected void processStereotypes(
            Map<String, Map<String, Object>> attrMaps,
            Map<String, List<String>> stereotypeMap) {
        processStereotypeNames(stereotypeMap);
        processStereotypeValues(attrMaps);
    }

    private void processStereotypeNames(Map<String, List<String>> stereotypeMap) {
        final String query = "//appliedStereotype";
        NodeList elements = null;
        try {
            elements = (NodeList) getXPath().evaluate(query, inputSource, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new DecisionFlowException("Unexpected UML");
        }
        for (int i = 0; i < elements.getLength(); i++) {
            final Node node = elements.item(i);
            final String appliedStereotypeId = eval("@xmi:value", node);
            final String stereotypeName = getStereotypeName(appliedStereotypeId);
            if (stereotypeName != null && !stereotypeName.isEmpty()) {
                final String parentNodeId = eval("@xmi:id", node.getParentNode().getParentNode());
                if (parentNodeId != null && !parentNodeId.isEmpty()) {
                    List<String> stereotypeNames = stereotypeMap.get(parentNodeId);
                    if (stereotypeNames == null) {
                        stereotypeNames = new ArrayList<>();
                        stereotypeMap.put(parentNodeId, stereotypeNames);
                    }
                    stereotypeNames.add(stereotypeName);
                }
            }
        }
    }

    private void processStereotypeValues(final Map<String, Map<String, Object>> attrMaps) {
        final String query =
                "//xmi:Extension/properties/property[@name='taggedValues']/vpumlModel/vpumlChildModelRefs/modelRef";
        NodeList elements = null;
        try {
            elements = (NodeList) getXPath().evaluate(query, inputSource, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new DecisionFlowException("Unexpected UML");
        }
        for (int i = 0; i < elements.getLength(); i++) {
            final Node node = elements.item(i);
            final String taggedValueRef = eval("@value", node);
            final Map<String, String> keyValuePair = getTaggedValue(taggedValueRef);
            if (!keyValuePair.isEmpty()) {
                final String parentNodeId = eval("@xmi:id",
                        node
                        .getParentNode()
                        .getParentNode()
                        .getParentNode()
                        .getParentNode()
                        .getParentNode()
                        .getParentNode());
                assert parentNodeId != null;
                Map<String, Object> attrMap = attrMaps.get(parentNodeId);
                if (attrMap == null) {
                    attrMap = new HashMap<>();
                    attrMaps.put(parentNodeId, attrMap);
                }
                attrMap.putAll(keyValuePair);
            }

        }
    }

    private Map<String, String> getTaggedValue(String taggedValueRef) {
        final String query = String.format("//ownedMember[@xmi:id='%s']", taggedValueRef);
        NodeList elements = null;
        try {
            elements = (NodeList) getXPath().evaluate(query, inputSource, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new DecisionFlowException("Unexpected UML");
        }
        Map<String, String> keyValuePair = new HashMap<>();

        for (int i = 0; i < elements.getLength(); i++) {
            final String key = eval("@tag", elements.item(i));
            final String value = eval("@value", elements.item(i));
            keyValuePair.put(key, value);
            break;
        }
        return keyValuePair;
    }

    private String getStereotypeName(final String stereotypeId) {
        final String query = String.format("//ownedMember[@xmi:id='%s']", stereotypeId);
        NodeList elements = null;
        try {
            elements = (NodeList) getXPath().evaluate(query, inputSource, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new DecisionFlowException("Unexpected UML");
        }
        String stereotypeName = null;

        for (int i = 0; i < elements.getLength(); i++) {
            stereotypeName = eval("@name", elements.item(i));
            break;
        }
        return stereotypeName;
    }
}
