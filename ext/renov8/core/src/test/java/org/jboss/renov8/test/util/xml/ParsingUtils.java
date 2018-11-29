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

import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

import java.util.Map;
import java.util.Set;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Eduardo Martins
 */
public class ParsingUtils {

    public static String getNextElement(XMLStreamReader reader, String name, Map<String, String> attributes, boolean getElementText) throws XMLStreamException {
        if (!reader.hasNext()) {
            throw new XMLStreamException("Expected more elements", reader.getLocation());
        }
        int type = reader.next();
        while (reader.hasNext() && type != START_ELEMENT) {
            type = reader.next();
        }
        if (reader.getEventType() != START_ELEMENT) {
            throw new XMLStreamException("No <" + name + "> found");
        }
        if (!reader.getLocalName().equals("" + name + "")) {
            throw new XMLStreamException("<" + name + "> expected", reader.getLocation());
        }

        if (attributes != null) {
            for (int i = 0 ; i < reader.getAttributeCount() ; i++) {
                String attr = reader.getAttributeLocalName(i);
                if (!attributes.containsKey(attr)) {
                    throw new XMLStreamException("Unexpected attribute " + attr, reader.getLocation());
                }
                attributes.put(attr, reader.getAttributeValue(i));
            }
        }

        return getElementText ? reader.getElementText() : null;
    }

    public static void parseNoContent(final XMLStreamReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                default: {
                    throw unexpectedContent(reader);
                }
            }
        }
        throw endOfDocument(reader.getLocation());
    }

    public static void parseNoAttributes(final XMLStreamReader reader) throws XMLStreamException {
        if(reader.getAttributeCount() != 0) {
            throw ParsingUtils.unexpectedContent(reader);
        }
    }

    public static XMLStreamException unexpectedContent(final XMLStreamReader reader) {
        final String kind;
        switch (reader.getEventType()) {
            case XMLStreamConstants.ATTRIBUTE:
                kind = "attribute";
                break;
            case XMLStreamConstants.CDATA:
                kind = "cdata";
                break;
            case XMLStreamConstants.CHARACTERS:
                kind = "characters";
                break;
            case XMLStreamConstants.COMMENT:
                kind = "comment";
                break;
            case XMLStreamConstants.DTD:
                kind = "dtd";
                break;
            case XMLStreamConstants.END_DOCUMENT:
                kind = "document end";
                break;
            case XMLStreamConstants.END_ELEMENT:
                kind = "element end";
                break;
            case XMLStreamConstants.ENTITY_DECLARATION:
                kind = "entity declaration";
                break;
            case XMLStreamConstants.ENTITY_REFERENCE:
                kind = "entity ref";
                break;
            case XMLStreamConstants.NAMESPACE:
                kind = "namespace";
                break;
            case XMLStreamConstants.NOTATION_DECLARATION:
                kind = "notation declaration";
                break;
            case XMLStreamConstants.PROCESSING_INSTRUCTION:
                kind = "processing instruction";
                break;
            case XMLStreamConstants.SPACE:
                kind = "whitespace";
                break;
            case XMLStreamConstants.START_DOCUMENT:
                kind = "document start";
                break;
            case XMLStreamConstants.START_ELEMENT:
                kind = "element start";
                break;
            default:
                kind = "unknown";
                break;
        }

        return new XMLStreamException("unexpected content: " + kind + (reader.hasName() ? reader.getName() : null) +
                (reader.hasText() ? reader.getText() : null), reader.getLocation());
    }

    public static XMLStreamException endOfDocument(final Location location) {
        return new XMLStreamException("Unexpected end of document ", location);
    }

    public static XMLStreamException missingAttributes(final Location location, final Set<? extends XmlNameProvider> requiredAttributes) {
        final StringBuilder b = new StringBuilder("Missing required attributes");
        for (XmlNameProvider attribute : requiredAttributes) {
            b.append(' ').append(attribute.getLocalName());
        }
        return new XMLStreamException(b.toString(), location);
    }

    public static XMLStreamException missingOneOfAttributes(final Location location, XmlNameProvider... attrs) {
        final StringBuilder b = new StringBuilder("Missing one of required attributes");
        for (XmlNameProvider attribute : attrs) {
            b.append(' ').append(attribute.getLocalName());
        }
        return new XMLStreamException(b.toString(), location);
    }

    public static String wildcardToJavaRegexp(String expr) {
        if (expr == null) {
            throw new IllegalArgumentException("expr is null");
        }
        String regex = expr.replaceAll("([(){}\\[\\].+^$])", "\\\\$1"); // escape regex characters
        regex = regex.replaceAll("\\*", ".*"); // replace * with .*
        regex = regex.replaceAll("\\?", "."); // replace ? with .
        return regex;
    }

    public static XMLStreamException unexpectedAttribute(final XMLExtendedStreamReader reader, final int index) {
        return new XMLStreamException(String.format("Unexpected attribute '%s' encountered", reader.getAttributeName(index)), reader.getLocation());

    }

    public static XMLStreamException expectedAtLeastOneChild(final XMLExtendedStreamReader reader, XmlNameProvider parent, XmlNameProvider... child) {
        final StringBuilder buf = new StringBuilder("The content of element '").append(parent.getLocalName()).append("' is not complete. One of ");
        XmlNameProvider c = child[0];
        buf.append('\'').append(c.getLocalName()).append('\'');
        if(child.length > 1) {
            for(int i = 1; i < child.length; ++i) {
                buf.append(", '").append(child[i].getLocalName()).append('\'');
            }
        }
        buf.append(" is expected.");
        return new XMLStreamException(buf.toString(), reader.getLocation());
    }

    public static String error(String msg, Location location) {
        return "ParseError at [row,col]:["+location.getLineNumber()+","+
                location.getColumnNumber()+"]\n"+
                "Message: "+msg;
    }

    public static XMLStreamException error(String string, Location location, Throwable e) {
        return new XMLStreamException(error(string, location), e);
    }
}
