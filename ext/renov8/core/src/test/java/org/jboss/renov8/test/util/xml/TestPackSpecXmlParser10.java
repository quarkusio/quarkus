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

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.renov8.PackLocation;
import org.jboss.renov8.config.PackConfig;
import org.jboss.renov8.test.StrVersion;
import org.jboss.renov8.test.TestPack;
import org.jboss.renov8.test.TestPack.Builder;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 *
 * @author Alexey Loubyansky
 */
public class TestPackSpecXmlParser10 implements PlugableXmlParser<TestPack.Builder> {

    public static final String NS = "urn:jboss:renov8:pack-spec:1.0";
    public static final QName ROOT = new QName(NS, Element.PACK_SPEC.getLocalName());

    public enum Element implements XmlNameProvider {

        CHANNEL("channel"),
        DEP("dep"),
        DEPS("deps"),
        FREQUENCY("frequency"),
        LOCATION("location"),
        PACK_SPEC("pack-spec"),
        PRODUCER("producer"),
        REPO_ID("repo"),
        VERSION("version"),

        // default unknown element
        UNKNOWN(null);

        private static final Map<QName, Element> elements;

        static {
            elements = new HashMap<>(10);
            elements.put(new QName(NS, CHANNEL.name), CHANNEL);
            elements.put(new QName(NS, DEP.name), DEP);
            elements.put(new QName(NS, DEPS.name), DEPS);
            elements.put(new QName(NS, FREQUENCY.name), FREQUENCY);
            elements.put(new QName(NS, LOCATION.name), LOCATION);
            elements.put(new QName(NS, PACK_SPEC.name), PACK_SPEC);
            elements.put(new QName(NS, PRODUCER.name), PRODUCER);
            elements.put(new QName(NS, REPO_ID.name), REPO_ID);
            elements.put(new QName(NS, VERSION.name), VERSION);
            elements.put(null, UNKNOWN);
        }

        static Element of(QName qName) {
            QName name;
            if (qName.getNamespaceURI().equals("")) {
                name = new QName(NS, qName.getLocalPart());
            } else {
                name = qName;
            }
            final Element element = elements.get(name);
            return element == null ? UNKNOWN : element;
        }

        private final String name;
        private final String namespace = NS;

        Element(final String name) {
            this.name = name;
        }

        /**
         * Get the local name of this element.
         *
         * @return the local name
         */
        @Override
        public String getLocalName() {
            return name;
        }

        @Override
        public String getNamespace() {
            return namespace;
        }
    }
/*
    enum Attribute implements XmlNameProvider {

        NAME("name"),
        // default unknown attribute
        UNKNOWN(null);

        private static final Map<QName, Attribute> attributes;

        static {
            attributes = new HashMap<>(2);
            attributes.put(new QName(NAME.name), NAME);
            attributes.put(null, UNKNOWN);
        }

        static Attribute of(QName qName) {
            final Attribute attribute = attributes.get(qName);
            return attribute == null ? UNKNOWN : attribute;
        }

        private final String name;

        Attribute(final String name) {
            this.name = name;
        }

        / **
         * Get the local name of this element.
         *
         * @return the local name
         * /
        @Override
        public String getLocalName() {
            return name;
        }

        @Override
        public String getNamespace() {
            return null;
        }
    }
*/
    @Override
    public QName getRoot() {
        return ROOT;
    }

    @Override
    public void readElement(XMLExtendedStreamReader reader, Builder builder) throws XMLStreamException {
        ParsingUtils.parseNoAttributes(reader);
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case LOCATION:
                            builder.setLocation(parseLocation(reader));
                            break;
                        case DEPS:
                            parseDeps(reader, builder);
                            break;
                        default:
                            throw ParsingUtils.unexpectedContent(reader);
                    }
                    break;
                }
                default: {
                    throw ParsingUtils.unexpectedContent(reader);
                }
            }
        }
        throw ParsingUtils.endOfDocument(reader.getLocation());
    }

    private static PackLocation parseLocation(XMLExtendedStreamReader reader) throws XMLStreamException {
        ParsingUtils.parseNoAttributes(reader);
        String repoType = null;
        String producer = null;
        String channel = null;
        String frequency = null;
        String version = null;
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    if(producer == null) {
                        throw new XMLStreamException("producer is missing", reader.getLocation());
                    }
                    if(version == null) {
                        throw new XMLStreamException("version is missing", reader.getLocation());
                    }
                    return new PackLocation(repoType, producer, channel, frequency, new StrVersion(version));
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case PRODUCER:
                            producer = reader.getElementText().trim();
                            break;
                        case VERSION:
                            version = reader.getElementText().trim();
                            break;
                        case CHANNEL:
                            channel = reader.getElementText().trim();
                            break;
                        case FREQUENCY:
                            frequency = reader.getElementText().trim();
                            break;
                        case REPO_ID:
                            repoType = reader.getElementText().trim();
                            break;
                        default:
                            throw ParsingUtils.unexpectedContent(reader);
                    }
                    break;
                }
                default: {
                    throw ParsingUtils.unexpectedContent(reader);
                }
            }
        }
        throw ParsingUtils.endOfDocument(reader.getLocation());
    }

    private void parseDeps(XMLExtendedStreamReader reader, Builder builder) throws XMLStreamException {
        ParsingUtils.parseNoAttributes(reader);
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case DEP:
                            builder.addDependency(PackConfig.forLocation(parseLocation(reader)));
                            break;
                        default:
                            throw ParsingUtils.unexpectedContent(reader);
                    }
                    break;
                }
                default: {
                    throw ParsingUtils.unexpectedContent(reader);
                }
            }
        }
        throw ParsingUtils.endOfDocument(reader.getLocation());
    }
}
