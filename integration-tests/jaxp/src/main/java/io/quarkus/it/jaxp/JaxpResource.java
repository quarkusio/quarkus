package io.quarkus.it.jaxp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

@Path("/jaxp")
@ApplicationScoped
public class JaxpResource {

    @Path("/dom-builder")
    @POST
    @Consumes(MediaType.TEXT_XML)
    @Produces(MediaType.TEXT_PLAIN)
    public String domBuilder(InputStream in) throws SAXException, IOException, ParserConfigurationException {
        final DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        final Document doc = dBuilder.parse(in);
        return doc.getDocumentElement().getTextContent();
    }

    @Path("/transformer")
    @POST
    @Consumes(MediaType.TEXT_XML)
    @Produces(MediaType.TEXT_PLAIN)
    public String transformer(InputStream in) throws TransformerException {
        final Transformer transformer = TransformerFactory.newInstance().newTransformer();
        final DOMResult result = new DOMResult();
        transformer.transform(new StreamSource(in), result);
        final Document doc = (Document) result.getNode();
        return doc.getDocumentElement().getTextContent();
    }

    @Path("/xpath")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String xpath(@QueryParam("input") String input, @QueryParam("xpath") String xPathExpression)
            throws XPathExpressionException {
        final XPath xPath = XPathFactory.newInstance().newXPath();
        InputSource in = new InputSource(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
        final String result = (String) xPath.evaluate(xPathExpression, in, XPathConstants.STRING);
        return result;
    }

}
