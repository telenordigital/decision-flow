package com.telenordigital.decisionflow.describers;

import com.telenordigital.decisionflow.DecisionFlowDescriber;
import com.telenordigital.decisionflow.DecisionFlowException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Papyrus extends AbstractXMIDescriber {

    Papyrus(final String umlFilePath) {
        super(umlFilePath);
    }

    public static DecisionFlowDescriber getInstance(final String umlFilePath) {
        return new Papyrus(umlFilePath);
    }

    @Override
    protected Properties getLabels(final String umlFilePath) {
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
        for (final Map.Entry<Object, Object> entry : origProperties.entrySet()) {
            String[] split = entry.getKey().toString().split("__");
            properties.put(split[split.length - 1], entry.getValue().toString());
        }
        return properties;
    }

    @Override
    protected Map<String, String> getNsMap() {
        final Map<String, String> nsMap = new HashMap<>();
        nsMap.put("xmi", "http://www.omg.org/spec/XMI/20131001");
        nsMap.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        nsMap.put("ecore", "http://www.eclipse.org/emf/2002/Ecore");
        nsMap.put("uml", "http://www.eclipse.org/uml2/5.0.0/UML");
        return nsMap;
    }

    @Override
    protected void processStereotypes(
            Map<String, Map<String, Object>> attrMaps,
            Map<String, List<String>> stereotypeMap) {
        NodeList elements = null;
        try {
            elements = (NodeList) getXPath().evaluate("/xmi:XMI/*",
                    getInputSource(), XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new DecisionFlowException("Unexpected UML");
        }
        for (int i = 0; i < elements.getLength(); i++) {
            final Node node = elements.item(i);
            final String baseState = eval("@base_State", node);
            if (baseState != null && !baseState.isEmpty()) {
                final Map<String, Object> attrs = new HashMap<>();
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
            final String baseTransition = eval("@base_Transition", node);
            if (baseTransition != null && !baseTransition.isEmpty()) {
                final String nodeName = node.getNodeName();
                final String[] profileAndStereotype = nodeName.split(":");
                if (profileAndStereotype.length == 2) {
                    List<String> stereotypes = stereotypeMap.get(baseState);
                    if (stereotypes == null) {
                        stereotypes = new ArrayList<>();
                        stereotypeMap.put(baseTransition, stereotypes);
                    }
                    stereotypes.add(profileAndStereotype[1]);
                }
            }
        }
    }
}
