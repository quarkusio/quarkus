package io.quarkus.modular.spi;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringWriter;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link FormattingXMLStreamWriter}.
 */
class FormattingXMLStreamWriterTest {

    private FormattingXMLStreamWriter create(StringWriter sw) throws Exception {
        XMLStreamWriter delegate = XMLOutputFactory.newDefaultFactory().createXMLStreamWriter(sw);
        return new FormattingXMLStreamWriter(delegate);
    }

    @Test
    void writeStartDocumentAddsNewlines() throws Exception {
        StringWriter sw = new StringWriter();
        FormattingXMLStreamWriter xml = create(sw);
        xml.writeStartDocument();
        xml.flush();
        String output = sw.toString();
        // should end with two newlines after the XML declaration
        assertThat(output).endsWith("\n\n");
    }

    @Test
    void nestedElementsProduceCorrectIndentation() throws Exception {
        StringWriter sw = new StringWriter();
        FormattingXMLStreamWriter xml = create(sw);
        xml.writeStartElement("root");
        xml.writeStartElement("child");
        xml.writeEndElement(); // child
        xml.writeEndElement(); // root
        xml.flush();
        String output = sw.toString();
        // child should be indented by 4 spaces (level 1)
        assertThat(output).contains("    <child");
        // closing root should not be indented
        assertThat(output).contains("</root>");
    }

    @Test
    void writeEmptyElementIndentsWithoutLevelChange() throws Exception {
        StringWriter sw = new StringWriter();
        FormattingXMLStreamWriter xml = create(sw);
        xml.writeStartElement("root");
        xml.writeEmptyElement("empty");
        xml.writeStartElement("sibling");
        xml.writeEndElement(); // sibling
        xml.writeEndElement(); // root
        xml.flush();
        String output = sw.toString();
        // both empty and sibling should be at same indentation (level 1 = 4 spaces)
        assertThat(output).contains("    <empty");
        assertThat(output).contains("    <sibling");
    }

    @Test
    void writeAttributePassesThrough() throws Exception {
        StringWriter sw = new StringWriter();
        FormattingXMLStreamWriter xml = create(sw);
        xml.writeStartElement("root");
        xml.writeAttribute("name", "value");
        xml.writeEndElement();
        xml.flush();
        String output = sw.toString();
        assertThat(output).contains("name=\"value\"");
    }

    @Test
    void writeCommentIndentsAndBreaks() throws Exception {
        StringWriter sw = new StringWriter();
        FormattingXMLStreamWriter xml = create(sw);
        xml.writeStartElement("root");
        xml.writeComment(" a comment ");
        xml.writeEndElement();
        xml.flush();
        String output = sw.toString();
        assertThat(output).contains("    <!-- a comment -->");
    }

    @Test
    void fullNestedScenario() throws Exception {
        StringWriter sw = new StringWriter();
        FormattingXMLStreamWriter xml = create(sw);
        xml.writeStartDocument();
        xml.writeStartElement("module");
        xml.writeAttribute("name", "test");
        xml.writeStartElement("dependencies");
        xml.writeEmptyElement("dependency");
        xml.writeAttribute("name", "java.base");
        xml.writeEndElement(); // dependencies
        xml.writeEndElement(); // module
        xml.writeEndDocument();
        xml.flush();
        String output = sw.toString();

        // verify structure: module at level 0, dependencies at level 1, dependency at level 2
        assertThat(output).contains("<module name=\"test\">");
        assertThat(output).contains("    <dependencies>");
        assertThat(output).contains("        <dependency name=\"java.base\"/>");
        assertThat(output).contains("    </dependencies>");
        assertThat(output).contains("</module>");
    }
}
