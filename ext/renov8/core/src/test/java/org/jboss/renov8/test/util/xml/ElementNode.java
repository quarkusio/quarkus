/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.renov8.test.util.xml;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ElementNode extends Node {

    private final ElementNode parent;
    private final String name;
    private final String namespace;
    private final Map<String, AttributeValue> attributes = new LinkedHashMap<String, AttributeValue>();
    private List<Node> children = new ArrayList<Node>();

    public ElementNode(final ElementNode parent, final String name) {
        this(parent, name, parent.getNamespace());
    }

    public ElementNode(final ElementNode parent, final String name, final String namespace) {
        this.parent = parent;
        this.name = name;
        this.namespace = namespace == null ? namespace : namespace.isEmpty() ? null : namespace;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getName() {
        return name;
    }

    public void addAttribute(String name, AttributeValue value) {
        attributes.put(name, value);
    }

    public void addChild(Node child) {
        children.add(child);
    }

    public void addElementText(String text) {
        children.add(new TextNode(text));
    }

    public Iterator<Node> getChildren() {
        return children.iterator();
    }

    public ElementNode getParent() {
        return parent;
    }

    public Iterator<Node> iterateChildren(){
        return children.iterator();
    }

    public String getAttributeValue(String name) {
        AttributeValue av = attributes.get(name);
        if (av == null) {
            return null;
        }
        return av.getValue();
    }

    public String getAttributeValue(String name, String defaultValue) {
        String s = getAttributeValue(name);
        if (s == null) {
            return defaultValue;
        }
        return s;
    }

    @Override
    public void marshall(XMLStreamWriter writer) throws XMLStreamException {
        // boolean empty = false;//children.isEmpty()
        boolean empty = isEmpty();
//        if (empty && name.equals("package")) {
//            if (attributes.size() == 1) {
//                AttributeValue next = attributes.values().iterator().next();
//                if (next.getValue().equals("modules")) {
//                    throw new XMLStreamException("package modules is empty");
//                }
//            }
//        }
        String prefix = writer.getNamespaceContext().getPrefix(namespace);
        if (prefix == null) {
            // Unknown namespace; it becomes default
            writer.setDefaultNamespace(namespace);
            if (empty) {
                writer.writeEmptyElement(name);
            } else {
                writer.writeStartElement(name);
            }
            writer.writeNamespace(null, namespace);
        } else if (empty) {
            writer.writeEmptyElement(namespace, name);
        } else {
            writer.writeStartElement(namespace, name);
        }

        for (Map.Entry<String, AttributeValue> attr : attributes.entrySet()) {
            writer.writeAttribute(attr.getKey(), attr.getValue().getValue());
        }

        if (!empty) {
            for (Node child : children) {
                child.marshall(writer);
            }

            try {
                writer.writeEndElement();
            } catch (XMLStreamException e) {
                // TODO REMOVE THIS
                throw e;
            }
        }
    }

    private boolean isEmpty() {
        if (children.isEmpty()) {
            return true;
        }
        for (Node child : children) {
            if (child.hasContent()) {
                return false;
            }
        }
        return true;
    }

    public String toString() {
        return "Element(name=" + name + ",ns=" + namespace + ")";
    }
}
