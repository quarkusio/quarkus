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

import java.io.Reader;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLMapper;

/**
 *
 * @author Alexey Loubyansky
 */
public class XmlParsers {

    private static final XmlParsers INSTANCE = new XmlParsers();

    private static final XMLInputFactory inputFactory;
    static {
        final XMLInputFactory tmpIF = XMLInputFactory.newInstance();
        setIfSupported(tmpIF, XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
        setIfSupported(tmpIF, XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        inputFactory = tmpIF;
    }

    private static void setIfSupported(final XMLInputFactory inputFactory, final String property, final Object value) {
        if (inputFactory.isPropertySupported(property)) {
            inputFactory.setProperty(property, value);
        }
    }

    public static XMLStreamReader createXMLStreamReader(Reader reader) throws XMLStreamException {
        return inputFactory.createXMLStreamReader(reader);
    }

    public static XmlParsers getInstance() {
        return INSTANCE;
    }

    public static void parse(final Reader reader, Object builder) throws XMLStreamException {
        INSTANCE.doParse(reader, builder);
    }

    private final XMLMapper mapper;

    private XmlParsers() {
        mapper = XMLMapper.Factory.create();
    }

    public void plugin(QName root, XMLElementReader<?> reader) {
        mapper.registerRootElement(root, reader);
    }

    public void doParse(final Reader reader, Object builder) throws XMLStreamException {
        mapper.parseDocument(builder, inputFactory.createXMLStreamReader(reader));
    }
}
