package io.quarkus.it.tika;

import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.csv.TextAndCSVParser;
import org.apache.tika.parser.odf.OpenDocumentParser;
import org.apache.tika.parser.pdf.PDFParser;

import io.quarkus.tika.TikaParser;

@Path("/parse")
public class TikaParserResource {
    // Avoiding the injection, otherwise the recorded tika-config.xml intended for TikaPdfInvoiceTest is used
    TikaParser parser = new TikaParser(
            new AutoDetectParser(new PDFParser(), new OpenDocumentParser(), new TextAndCSVParser()), true);

    @POST
    @Path("/text")
    @Consumes({ "text/plain", "application/pdf", "application/vnd.oasis.opendocument.text" })
    @Produces(MediaType.TEXT_PLAIN)
    public String extractText(InputStream stream) {
        return parser.parse(stream).getText();
    }

    @POST
    @Path("/metadata")
    @Consumes({ "text/plain", "application/pdf", "application/vnd.oasis.opendocument.text" })
    @Produces(MediaType.TEXT_PLAIN)
    public String extractMetadata(InputStream stream) {
        return parser.getMetadata(stream).getNames().toString();
    }
}
