/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2017 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.logmanager.formatters;

import java.util.Iterator;
import java.util.NoSuchElementException;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * An XML stream writer which pretty prints the XML.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class IndentingXmlWriter implements XMLStreamWriter, XMLStreamConstants {

    private static final String SPACES = "    ";

    private final XMLStreamWriter delegate;
    private int index;
    private int state = START_DOCUMENT;
    private boolean indentEnd;

    IndentingXmlWriter(final XMLStreamWriter delegate) {
        this.delegate = delegate;
        index = 0;
        indentEnd = false;
    }

    private void indent() throws XMLStreamException {
        final int index = this.index;
        if (index > 0) {
            for (int i = 0; i < index; i++) {
                delegate.writeCharacters(SPACES);
            }
        }

    }

    private void newline() throws XMLStreamException {
        delegate.writeCharacters("\n");
    }

    @Override
    public void writeStartElement(final String localName) throws XMLStreamException {
        newline();
        indent();
        delegate.writeStartElement(localName);
        indentEnd = false;
        state = START_ELEMENT;
        index++;
    }

    @Override
    public void writeStartElement(final String namespaceURI, final String localName) throws XMLStreamException {
        newline();
        indent();
        delegate.writeStartElement(namespaceURI, localName);
        indentEnd = false;
        state = START_ELEMENT;
        index++;
    }

    @Override
    public void writeStartElement(final String prefix, final String localName, final String namespaceURI) throws XMLStreamException {
        newline();
        indent();
        delegate.writeStartElement(prefix, localName, namespaceURI);
        indentEnd = false;
        state = START_ELEMENT;
        index++;
    }

    @Override
    public void writeEmptyElement(final String namespaceURI, final String localName) throws XMLStreamException {
        newline();
        indent();
        delegate.writeEmptyElement(namespaceURI, localName);
        state = END_ELEMENT;
    }

    @Override
    public void writeEmptyElement(final String prefix, final String localName, final String namespaceURI) throws XMLStreamException {
        newline();
        indent();
        delegate.writeEmptyElement(prefix, localName, namespaceURI);
        state = END_ELEMENT;
    }

    @Override
    public void writeEmptyElement(final String localName) throws XMLStreamException {
        newline();
        indent();
        delegate.writeEmptyElement(localName);
        state = END_ELEMENT;
    }

    @Override
    public void writeEndElement() throws XMLStreamException {
        index--;
        if (state != CHARACTERS || indentEnd) {
            newline();
            indent();
            indentEnd = false;
        }
        delegate.writeEndElement();
        state = END_ELEMENT;
    }

    @Override
    public void writeEndDocument() throws XMLStreamException {
        delegate.writeEndDocument();
        state = END_DOCUMENT;
    }

    @Override
    public void close() throws XMLStreamException {
        delegate.close();
    }

    @Override
    public void flush() throws XMLStreamException {
        delegate.flush();
    }

    @Override
    public void writeAttribute(final String localName, final String value) throws XMLStreamException {
        delegate.writeAttribute(localName, value);
    }

    @Override
    public void writeAttribute(final String prefix, final String namespaceURI, final String localName, final String value) throws XMLStreamException {
        delegate.writeAttribute(prefix, namespaceURI, localName, value);
    }

    @Override
    public void writeAttribute(final String namespaceURI, final String localName, final String value) throws XMLStreamException {
        delegate.writeAttribute(namespaceURI, localName, value);
    }

    @Override
    public void writeNamespace(final String prefix, final String namespaceURI) throws XMLStreamException {
        delegate.writeNamespace(prefix, namespaceURI);
    }

    @Override
    public void writeDefaultNamespace(final String namespaceURI) throws XMLStreamException {
        delegate.writeDefaultNamespace(namespaceURI);
    }

    @Override
    public void writeComment(final String data) throws XMLStreamException {
        newline();
        indent();
        delegate.writeComment(data);
        state = COMMENT;
    }

    @Override
    public void writeProcessingInstruction(final String target) throws XMLStreamException {
        newline();
        indent();
        delegate.writeProcessingInstruction(target);
        state = PROCESSING_INSTRUCTION;
    }

    @Override
    public void writeProcessingInstruction(final String target, final String data) throws XMLStreamException {
        newline();
        indent();
        delegate.writeProcessingInstruction(target, data);
        state = PROCESSING_INSTRUCTION;
    }

    @Override
    public void writeCData(final String data) throws XMLStreamException {
        delegate.writeCData(data);
        state = CDATA;
    }

    @Override
    public void writeDTD(final String dtd) throws XMLStreamException {
        newline();
        indent();
        delegate.writeDTD(dtd);
        state = DTD;
    }

    @Override
    public void writeEntityRef(final String name) throws XMLStreamException {
        delegate.writeEntityRef(name);
        state = ENTITY_REFERENCE;
    }

    @Override
    public void writeStartDocument() throws XMLStreamException {
        delegate.writeStartDocument();
        newline();
        state = START_DOCUMENT;
    }

    @Override
    public void writeStartDocument(final String version) throws XMLStreamException {
        delegate.writeStartDocument(version);
        newline();
        state = START_DOCUMENT;
    }

    @Override
    public void writeStartDocument(final String encoding, final String version) throws XMLStreamException {
        delegate.writeStartDocument(encoding, version);
        newline();
        state = START_DOCUMENT;
    }

    @Override
    public void writeCharacters(final String text) throws XMLStreamException {
        indentEnd = false;
        boolean first = true;
        final Iterator<String> iterator = new SplitIterator(text, '\n');
        while (iterator.hasNext()) {
            final String t = iterator.next();
            // On first iteration if more than one line is required, skip to a new line and indent
            if (first && iterator.hasNext()) {
                first = false;
                newline();
                indent();
            }
            delegate.writeCharacters(t);
            if (iterator.hasNext()) {
                newline();
                indent();
                indentEnd = true;
            }
        }
        state = CHARACTERS;
    }

    @Override
    public void writeCharacters(final char[] text, final int start, final int len) throws XMLStreamException {
        delegate.writeCharacters(text, start, len);
    }

    @Override
    public String getPrefix(final String uri) throws XMLStreamException {
        return delegate.getPrefix(uri);
    }

    @Override
    public void setPrefix(final String prefix, final String uri) throws XMLStreamException {
        delegate.setPrefix(prefix, uri);
    }

    @Override
    public void setDefaultNamespace(final String uri) throws XMLStreamException {
        delegate.setDefaultNamespace(uri);
    }

    @Override
    public void setNamespaceContext(final NamespaceContext context) throws XMLStreamException {
        delegate.setNamespaceContext(context);
    }

    @Override
    public NamespaceContext getNamespaceContext() {
        return delegate.getNamespaceContext();
    }

    @Override
    public Object getProperty(final String name) throws IllegalArgumentException {
        return delegate.getProperty(name);
    }

    private static class SplitIterator implements Iterator<String> {

        private final String value;
        private final char delimiter;
        private int index;

        private SplitIterator(final String value, final char delimiter) {
            this.value = value;
            this.delimiter = delimiter;
            index = 0;
        }

        @Override
        public boolean hasNext() {
            return index != -1;
        }

        @Override
        public String next() {
            final int index = this.index;
            if (index == -1) {
                throw new NoSuchElementException();
            }
            int x = value.indexOf(delimiter, index);
            try {
                return x == -1 ? value.substring(index) : value.substring(index, x);
            } finally {
                this.index = (x == -1 ? -1 : x + 1);
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
