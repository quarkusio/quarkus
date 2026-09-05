package io.quarkus.it.jackson;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import tools.jackson.databind.ObjectMapper;

/**
 * Exercises the Jackson DOM handlers, which are only included in native executables when a DOM type
 * is reachable, as it is here.
 */
@Path("/dom")
public class DomResource {

    @Inject
    ObjectMapper objectMapper;

    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/serialize")
    public String serialize(String xml) throws ParserConfigurationException, IOException, SAXException {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new InputSource(new StringReader(xml)));
        return objectMapper.writeValueAsString(document);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/deserialize")
    public String deserialize(String json) {
        Document document = objectMapper.readValue(json, Document.class);
        return document.getDocumentElement().getTagName() + ":" + document.getDocumentElement().getTextContent();
    }
}
