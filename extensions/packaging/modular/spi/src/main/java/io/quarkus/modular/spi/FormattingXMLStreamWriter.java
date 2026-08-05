package io.quarkus.modular.spi;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * A simple formatter for XML.
 */
final class FormattingXMLStreamWriter implements XMLStreamWriter {
    private final XMLStreamWriter delegate;
    boolean pendingBreak = false;
    int level = 0;

    FormattingXMLStreamWriter(final XMLStreamWriter delegate) {
        this.delegate = delegate;
    }

    public void writeStartElement(final String localName) throws XMLStreamException {
        indent();
        delegate.writeStartElement(localName);
        pendingBreak = true;
        level++;
    }

    public void writeStartElement(final String namespaceURI, final String localName) throws XMLStreamException {
        indent();
        delegate.writeStartElement(namespaceURI, localName);
        pendingBreak = true;
        level++;
    }

    public void writeStartElement(final String prefix, final String localName, final String namespaceURI)
            throws XMLStreamException {
        indent();
        delegate.writeStartElement(prefix, namespaceURI, localName);
        pendingBreak = true;
        level++;
    }

    public void writeEmptyElement(final String namespaceURI, final String localName) throws XMLStreamException {
        indent();
        delegate.writeEmptyElement(namespaceURI, localName);
        pendingBreak = true;
    }

    public void writeEmptyElement(final String prefix, final String localName, final String namespaceURI)
            throws XMLStreamException {
        indent();
        delegate.writeEmptyElement(prefix, namespaceURI, localName);
        pendingBreak = true;
    }

    public void writeEmptyElement(final String localName) throws XMLStreamException {
        indent();
        delegate.writeEmptyElement(localName);
        pendingBreak = true;
    }

    public void writeEndElement() throws XMLStreamException {
        level--;
        indent();
        delegate.writeEndElement();
        delegate.writeCharacters("\n");
    }

    public void writeEndDocument() throws XMLStreamException {
        checkBreak();
        delegate.writeEndDocument();
    }

    public void close() throws XMLStreamException {
        delegate.close();
    }

    public void flush() throws XMLStreamException {
        delegate.flush();
    }

    public void writeAttribute(final String localName, final String value) throws XMLStreamException {
        delegate.writeAttribute(localName, value);
    }

    public void writeAttribute(final String prefix, final String namespaceURI, final String localName, final String value)
            throws XMLStreamException {
        delegate.writeAttribute(prefix, namespaceURI, localName, value);
    }

    public void writeAttribute(final String namespaceURI, final String localName, final String value)
            throws XMLStreamException {
        delegate.writeAttribute(namespaceURI, localName, value);
    }

    public void writeNamespace(final String prefix, final String namespaceURI) throws XMLStreamException {
        delegate.writeNamespace(prefix, namespaceURI);
    }

    public void writeDefaultNamespace(final String namespaceURI) throws XMLStreamException {
        delegate.writeDefaultNamespace(namespaceURI);
    }

    public void writeComment(final String data) throws XMLStreamException {
        indent();
        delegate.writeComment(data);
        delegate.writeCharacters("\n");
    }

    public void writeProcessingInstruction(final String target) throws XMLStreamException {
        checkBreak();
        delegate.writeProcessingInstruction(target);
    }

    public void writeProcessingInstruction(final String target, final String data) throws XMLStreamException {
        checkBreak();
        delegate.writeProcessingInstruction(target, data);
    }

    public void writeCData(final String data) throws XMLStreamException {
        checkBreak();
        delegate.writeCData(data);
    }

    public void writeDTD(final String dtd) throws XMLStreamException {
        checkBreak();
        delegate.writeDTD(dtd);
    }

    public void writeEntityRef(final String name) throws XMLStreamException {
        checkBreak();
        delegate.writeEntityRef(name);
    }

    public void writeStartDocument() throws XMLStreamException {
        checkBreak();
        delegate.writeStartDocument();
        delegate.writeCharacters("\n\n");
    }

    public void writeStartDocument(final String version) throws XMLStreamException {
        checkBreak();
        delegate.writeStartDocument(version);
        delegate.writeCharacters("\n\n");
    }

    public void writeStartDocument(final String encoding, final String version) throws XMLStreamException {
        checkBreak();
        delegate.writeStartDocument(encoding, version);
        delegate.writeCharacters("\n\n");
    }

    public void writeCharacters(final String text) throws XMLStreamException {
        checkBreak();
        delegate.writeCharacters(text);
    }

    public void writeCharacters(final char[] text, final int start, final int len) throws XMLStreamException {
        indent();
        delegate.writeCharacters(text, start, len);
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

    private void indent() throws XMLStreamException {
        checkBreak();
        for (int i = 0; i < level; i++) {
            delegate.writeCharacters("    ");
        }
    }

    private void checkBreak() throws XMLStreamException {
        if (pendingBreak) {
            pendingBreak = false;
            delegate.writeCharacters("\n");
        }
    }
}
