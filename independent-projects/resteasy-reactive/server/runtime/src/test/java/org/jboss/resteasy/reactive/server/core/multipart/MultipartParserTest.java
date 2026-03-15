package org.jboss.resteasy.reactive.server.core.multipart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.jboss.resteasy.reactive.common.util.CaseInsensitiveMap;
import org.jboss.resteasy.reactive.server.core.multipart.MultipartParser.PartHandler;
import org.junit.jupiter.api.Test;

public class MultipartParserTest {
    @Test
    public void testEmpty() throws IOException {
        var boundary = "----geckoformboundary781efa46e46556855951be27ee89788e";
        var content = "\r\n------geckoformboundary781efa46e46556855951be27ee89788e--\r\n";
        var handler = new MultipartParser.PartHandler() {
            public void beginPart(final CaseInsensitiveMap<String> headers) {
            }

            public void data(final ByteBuffer buffer) throws IOException {
            }

            public void endPart() {
            }
        };
        var parser = MultipartParser.beginParse(handler, boundary.getBytes(StandardCharsets.US_ASCII), "UTF-8");
        parser.parse(ByteBuffer.wrap(content.getBytes(StandardCharsets.UTF_8)));
        assertTrue(parser.isComplete());
    }

    @Test
    public void testSimple() throws IOException {
        var boundary = "----geckoformboundary5ced44e8f9bd18901d8eff2729601699";
        var content = "\r\n------geckoformboundary5ced44e8f9bd18901d8eff2729601699\r\n"
                + "Content-Disposition: form-data; name=\"param-a\"\r\n"
                + "\r\n"
                + "Sample A\r\n"
                + "------geckoformboundary5ced44e8f9bd18901d8eff2729601699\r\n"
                + "Content-Disposition: form-data; name=\"param-b\"\r\n"
                + "\r\n"
                + "Sample B\r\n"
                + "------geckoformboundary5ced44e8f9bd18901d8eff2729601699--\r\n";
        var allHeaders = new ArrayList<CaseInsensitiveMap<String>>();
        var allData = new ArrayList<String>();
        var handler = new PartHandler() {
            public void beginPart(final CaseInsensitiveMap<String> headers) {
                allHeaders.add(headers);
            }

            public void data(final ByteBuffer buffer) throws IOException {
                allData.add(StandardCharsets.UTF_8.decode(buffer).toString());
            }

            public void endPart() {
            }
        };
        var parser = MultipartParser.beginParse(handler, boundary.getBytes(StandardCharsets.US_ASCII), "UTF-8");
        parser.parse(ByteBuffer.wrap(content.getBytes(StandardCharsets.UTF_8)));
        assertTrue(parser.isComplete());
        assertTrue(allHeaders.size() == 2);
        assertTrue(allData.size() == 2);

        assertEquals(allHeaders.get(0).get("Content-Disposition").get(0), "form-data; name=\"param-a\"");
        assertEquals(allHeaders.get(1).get("Content-Disposition").get(0), "form-data; name=\"param-b\"");

        assertEquals(allData.get(0), "Sample A");
        assertEquals(allData.get(1), "Sample B");
    }
}
