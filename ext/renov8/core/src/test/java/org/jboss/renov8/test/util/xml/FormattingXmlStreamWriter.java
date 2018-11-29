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


import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * An XML stream writer which nicely formats the XML for configuration files.
 * This gets rid of the attribute queue used in the org.jboss.staxmapper version which breaks the behaviour.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public final class FormattingXmlStreamWriter implements XMLStreamWriter, XMLStreamConstants, AutoCloseable {
    private static final String NO_NAMESPACE = new String();
    private final XMLStreamWriter delegate;
    private int level;
    private int state = START_DOCUMENT;
    private boolean indentEndElement = false;
    private ArrayDeque<String> unspecifiedNamespaces = new ArrayDeque<String>();


    public FormattingXmlStreamWriter(final XMLStreamWriter delegate) {
        this.delegate = delegate;
        unspecifiedNamespaces.push(NO_NAMESPACE);
    }

    private void nl() throws XMLStreamException {
        delegate.writeCharacters("\n");
    }

    private void indent() throws XMLStreamException {
        int level = this.level;
        final XMLStreamWriter delegate = this.delegate;
        for (int i = 0; i < level; i ++) {
            delegate.writeCharacters("    ");
        }
    }

    public interface ArgRunnable {
        void run(int arg) throws XMLStreamException;
    }

    private String nestUnspecifiedNamespace() {
        ArrayDeque<String> namespaces = unspecifiedNamespaces;
        String clone = namespaces.getFirst();
        namespaces.push(clone);
        return clone;
    }

    public void writeStartElement(final String localName) throws XMLStreamException {
        ArrayDeque<String> namespaces = unspecifiedNamespaces;
        String namespace = namespaces.getFirst();
        if (namespace != NO_NAMESPACE) {
            writeStartElement(namespace, localName);
            return;
        }

        unspecifiedNamespaces.push(namespace);

        // If this is a nested element flush the outer
        nl();
        indent();
        delegate.writeStartElement(localName);

        level++;
        state = START_ELEMENT;
        indentEndElement = false;
    }

    public void writeStartElement(final String namespaceURI, final String localName) throws XMLStreamException {
        nestUnspecifiedNamespace();

        // If this is a nested element flush the outer
        nl();
        indent();
        delegate.writeStartElement(namespaceURI, localName);
        level++;
        state = START_ELEMENT;
        indentEndElement = false;
    }

    public void writeStartElement(final String prefix, final String localName, final String namespaceURI) throws XMLStreamException {
        nestUnspecifiedNamespace();

        // If this is a nested element flush the outer
        nl();
        indent();
        delegate.writeStartElement(prefix, namespaceURI, localName);
        level++;
        state = START_ELEMENT;
        indentEndElement = false;
    }

    public void writeEmptyElement(final String namespaceURI, final String localName) throws XMLStreamException {
        nl();
        indent();
        delegate.writeEmptyElement(namespaceURI, localName);
        state = END_ELEMENT;
    }

    public void writeEmptyElement(final String prefix, final String localName, final String namespaceURI) throws XMLStreamException {
        nl();
        indent();
        delegate.writeEmptyElement(prefix, namespaceURI, localName);
        state = END_ELEMENT;
    }

    public void writeEmptyElement(final String localName) throws XMLStreamException {
        String namespace = unspecifiedNamespaces.getFirst();
        if (namespace != NO_NAMESPACE) {
            writeEmptyElement(namespace, localName);
            return;
        }

        nl();
        indent();
        delegate.writeEmptyElement(localName);
        state = END_ELEMENT;
    }

    public void writeEndElement() throws XMLStreamException {
        level--;
        if (state != START_ELEMENT) {
            if (state != CHARACTERS || indentEndElement) {
                nl();
                indent();
                indentEndElement = false;
            }
            delegate.writeEndElement();
        } else {
//            // Change the start element to an empty element
//            ArgRunnable start = attrQueue.poll();
//            start.run(1);
//            // Write everything else
//            runAttrQueue();
            throw new IllegalStateException("Should not happen?");
        }

        unspecifiedNamespaces.pop();
        state = END_ELEMENT;
    }

    public void writeEndDocument() throws XMLStreamException {
        delegate.writeEndDocument();
        //nl();
        state = END_DOCUMENT;
    }

    public void close() throws XMLStreamException {
        delegate.close();
        state = END_DOCUMENT;
    }

    public void flush() throws XMLStreamException {
        delegate.flush();
    }

    public void writeAttribute(final String localName, final String value) throws XMLStreamException {
        delegate.writeAttribute(localName, value);
    }

    public void writeAttribute(final String prefix, final String namespaceURI, final String localName, final String value) throws XMLStreamException {
        delegate.writeAttribute(prefix, namespaceURI, localName, value);
    }

    public void writeAttribute(final String namespaceURI, final String localName, final String value) throws XMLStreamException {
        delegate.writeAttribute(namespaceURI, localName, value);
    }

    public void writeAttribute(final String localName, final String[] values) throws XMLStreamException {
        delegate.writeAttribute(localName, join(values));
    }

    public void writeAttribute(final String prefix, final String namespaceURI, final String localName, final String[] values) throws XMLStreamException {
        delegate.writeAttribute(prefix, namespaceURI, localName, join(values));
    }

    public void writeAttribute(final String namespaceURI, final String localName, final String[] values) throws XMLStreamException {
        delegate.writeAttribute(namespaceURI, localName, join(values));
    }

    public void writeAttribute(final String localName, final Iterable<String> values) throws XMLStreamException {
        delegate.writeAttribute(localName, join(values));
    }

    public void writeAttribute(final String prefix, final String namespaceURI, final String localName, final Iterable<String> values) throws XMLStreamException {
        delegate.writeAttribute(prefix, namespaceURI, localName, join(values));
    }

    public void writeAttribute(final String namespaceURI, final String localName, final Iterable<String> values) throws XMLStreamException {
        delegate.writeAttribute(namespaceURI, localName, join(values));
    }

    public void writeNamespace(final String prefix, final String namespaceURI) throws XMLStreamException {
        delegate.writeNamespace(prefix, namespaceURI);
    }

    public void writeDefaultNamespace(final String namespaceURI) throws XMLStreamException {
        delegate.writeDefaultNamespace(namespaceURI);
    }

    public void writeComment(final String data) throws XMLStreamException {
        nl();
        indent();
        final StringBuilder b = new StringBuilder(data.length());
        final Iterator<String> i = Spliterator.over(data, '\n');
        if (! i.hasNext()) {
            return;
        } else {
            final String first = i.next();
            if (! i.hasNext()) {
                delegate.writeComment(first);
                state = COMMENT;
                return;
            } else {
                b.append('\n');
                for (int q = 0; q < level; q++) {
                    b.append("    ");
                }
                b.append("  ~ ");
                b.append(first);
                do {
                    b.append('\n');
                    for (int q = 0; q < level; q++) {
                        b.append("    ");
                    }
                    b.append("  ~ ");
                    b.append(i.next());
                } while (i.hasNext());
            }
            b.append('\n');
            for (int q = 0; q < level; q ++) {
                b.append("    ");
            }
            b.append("  ");
            delegate.writeComment(b.toString());
            state = COMMENT;
        }
    }

    public void writeProcessingInstruction(final String target) throws XMLStreamException {
        nl();
        indent();
        delegate.writeProcessingInstruction(target);
        state = PROCESSING_INSTRUCTION;
    }

    public void writeProcessingInstruction(final String target, final String data) throws XMLStreamException {
        nl();
        indent();
        delegate.writeProcessingInstruction(target, data);
        state = PROCESSING_INSTRUCTION;
    }

    public void writeCData(final String data) throws XMLStreamException {
        delegate.writeCData(data);
        state = CDATA;
    }

    public void writeDTD(final String dtd) throws XMLStreamException {
        nl();
        indent();
        delegate.writeDTD(dtd);
        state = DTD;
    }

    public void writeEntityRef(final String name) throws XMLStreamException {
        delegate.writeEntityRef(name);
        state = ENTITY_REFERENCE;
    }

    public void writeStartDocument() throws XMLStreamException {
        delegate.writeStartDocument();
        nl();
        state = START_DOCUMENT;
    }

    public void writeStartDocument(final String version) throws XMLStreamException {
        delegate.writeStartDocument(version);
        nl();
        state = START_DOCUMENT;
    }

    public void writeStartDocument(final String encoding, final String version) throws XMLStreamException {
        delegate.writeStartDocument(encoding, version);
        nl();
        state = START_DOCUMENT;
    }

    public void writeCharacters(final String text) throws XMLStreamException {
        if (state != CHARACTERS) {
            String trimmed = text.trim();
            if (trimmed.contains("\n")) {
                nl();
                indent();
            } else {
                delegate.writeCharacters(trimmed);
                indentEndElement = false;
                state = CHARACTERS;
                return;
            }
        }
        final Iterator<String> iterator = Spliterator.over(text, '\n');
        while (iterator.hasNext()) {
            final String t = iterator.next();
            delegate.writeCharacters(t);
            if (iterator.hasNext()) {
                nl();
                indent();
            }
        }
        state = CHARACTERS;
        indentEndElement = true;
    }

    public void writeCharacters(final char[] text, final int start, final int len) throws XMLStreamException {
        delegate.writeCharacters(text, start, len);
        state = CHARACTERS;
    }

    public String getPrefix(final String uri) throws XMLStreamException {
        return delegate.getPrefix(uri);
    }

    public void setPrefix(final String prefix, final String uri) throws XMLStreamException {
        delegate.setPrefix(prefix, uri);
    }

    public void setDefaultNamespace(final String uri) throws XMLStreamException {
        delegate.setDefaultNamespace(uri);
    }

    public void setNamespaceContext(final NamespaceContext context) throws XMLStreamException {
        delegate.setNamespaceContext(context);
    }

    public NamespaceContext getNamespaceContext() {
        return delegate.getNamespaceContext();
    }

    public Object getProperty(final String name) throws IllegalArgumentException {
        return delegate.getProperty(name);
    }

    private static String join(final String[] values) {
        final StringBuilder b = new StringBuilder();
        for (int i = 0, valuesLength = values.length; i < valuesLength; i++) {
            final String s = values[i];
            if (s != null) {
                if (i > 0) {
                    b.append(' ');
                }
                b.append(s);
            }
        }
        return b.toString();
    }

    private static String join(final Iterable<String> values) {
        final StringBuilder b = new StringBuilder();
        Iterator<String> iterator = values.iterator();
        while (iterator.hasNext()) {
            final String s = iterator.next();
            if (s != null) {
                b.append(s);
                if (iterator.hasNext()) b.append(' ');
            }
        }
        return b.toString();
    }

    static final class Spliterator implements Iterator<String> {
        private final String subject;
        private final char delimiter;
        private int i;

        Spliterator(final String subject, final char delimiter) {
            this.subject = subject;
            this.delimiter = delimiter;
            i = 0;
        }

        static Spliterator over(String subject, char delimiter) {
            return new Spliterator(subject, delimiter);
        }

        public boolean hasNext() {
            return i != -1;
        }

        public String next() {
            final int i = this.i;
            if (i == -1) {
                throw new NoSuchElementException();
            }
            int n = subject.indexOf(delimiter, i);
            try {
                return n == -1 ? subject.substring(i) : subject.substring(i, n);
            } finally {
                this.i = n == -1 ? -1 : n + 1;
            }
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

}