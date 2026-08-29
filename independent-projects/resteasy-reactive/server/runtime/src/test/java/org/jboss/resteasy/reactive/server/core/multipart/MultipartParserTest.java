package org.jboss.resteasy.reactive.server.core.multipart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.jboss.resteasy.reactive.common.util.CaseInsensitiveMap;
import org.jboss.resteasy.reactive.common.util.MultipartParser;
import org.jboss.resteasy.reactive.common.util.MultipartParser.PartHandler;
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

    @Test
    public void testOversizedHeaderName() {
        var boundary = "----testboundary";
        // Build a header name that exceeds the default 32KB limit
        String hugeHeaderName = "X-" + "A".repeat(33000);
        var content = "\r\n------testboundary\r\n"
                + hugeHeaderName + ": value\r\n"
                + "\r\n"
                + "body\r\n"
                + "------testboundary--\r\n";
        var parser = MultipartParser.beginParse(noopHandler(), boundary.getBytes(StandardCharsets.US_ASCII), "UTF-8");
        assertThrows(MultipartParser.HeaderTooLargeException.class,
                () -> parser.parse(ByteBuffer.wrap(content.getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    public void testOversizedHeaderValue() {
        var boundary = "----testboundary";
        String hugeValue = "V".repeat(33000);
        var content = "\r\n------testboundary\r\n"
                + "X-Pad: " + hugeValue + "\r\n"
                + "\r\n"
                + "body\r\n"
                + "------testboundary--\r\n";
        var parser = MultipartParser.beginParse(noopHandler(), boundary.getBytes(StandardCharsets.US_ASCII), "UTF-8");
        assertThrows(MultipartParser.HeaderTooLargeException.class,
                () -> parser.parse(ByteBuffer.wrap(content.getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    public void testTooManyHeaders() {
        var boundary = "----testboundary";
        StringBuilder headers = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            headers.append("X-Header-").append(i).append(": value").append(i).append("\r\n");
        }
        var content = "\r\n------testboundary\r\n"
                + headers
                + "\r\n"
                + "body\r\n"
                + "------testboundary--\r\n";
        var parser = MultipartParser.beginParse(noopHandler(), boundary.getBytes(StandardCharsets.US_ASCII), "UTF-8");
        assertThrows(MultipartParser.HeaderTooLargeException.class,
                () -> parser.parse(ByteBuffer.wrap(content.getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    public void testNormalHeadersPass() throws IOException {
        var boundary = "----testboundary";
        var content = "\r\n------testboundary\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"test.txt\"\r\n"
                + "Content-Type: text/plain\r\n"
                + "Content-Transfer-Encoding: base64\r\n"
                + "\r\n"
                + "body\r\n"
                + "------testboundary--\r\n";
        var allHeaders = new ArrayList<CaseInsensitiveMap<String>>();
        var handler = new PartHandler() {
            public void beginPart(final CaseInsensitiveMap<String> headers) {
                allHeaders.add(headers);
            }

            public void data(final ByteBuffer buffer) {
            }

            public void endPart() {
            }
        };
        var parser = MultipartParser.beginParse(handler, boundary.getBytes(StandardCharsets.US_ASCII), "UTF-8");
        parser.parse(ByteBuffer.wrap(content.getBytes(StandardCharsets.UTF_8)));
        assertTrue(parser.isComplete());
        assertEquals(1, allHeaders.size());
        assertEquals("form-data; name=\"file\"; filename=\"test.txt\"",
                allHeaders.get(0).getFirst("Content-Disposition"));
    }

    @Test
    public void testHeaderLimitsResetBetweenParts() throws IOException {
        var boundary = "----testboundary";
        // Use custom limit of 100 bytes per part header section
        // Each part's headers are under 100 bytes individually
        StringBuilder headers = new StringBuilder();
        headers.append("Content-Disposition: form-data; name=\"a\"\r\n");
        headers.append("X-Custom: ").append("x".repeat(30)).append("\r\n");

        var content = "\r\n------testboundary\r\n"
                + headers
                + "\r\n"
                + "data-a\r\n"
                + "------testboundary\r\n"
                + headers
                + "\r\n"
                + "data-b\r\n"
                + "------testboundary--\r\n";

        var partCount = new int[] { 0 };
        var handler = new PartHandler() {
            public void beginPart(final CaseInsensitiveMap<String> h) {
                partCount[0]++;
            }

            public void data(final ByteBuffer buffer) {
            }

            public void endPart() {
            }
        };
        var parser = MultipartParser.beginParse(handler, boundary.getBytes(StandardCharsets.US_ASCII), "UTF-8",
                200, 40);
        parser.parse(ByteBuffer.wrap(content.getBytes(StandardCharsets.UTF_8)));
        assertTrue(parser.isComplete());
        assertEquals(2, partCount[0]);
    }

    @Test
    public void testCustomLimits() {
        var boundary = "----testboundary";
        // Set a very small limit of 10 bytes for part headers
        var content = "\r\n------testboundary\r\n"
                + "X-Header: this-value-exceeds-ten-bytes\r\n"
                + "\r\n"
                + "body\r\n"
                + "------testboundary--\r\n";
        var parser = MultipartParser.beginParse(noopHandler(), boundary.getBytes(StandardCharsets.US_ASCII), "UTF-8",
                10, 40);
        assertThrows(MultipartParser.HeaderTooLargeException.class,
                () -> parser.parse(ByteBuffer.wrap(content.getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    public void testCustomHeaderCountLimit() {
        var boundary = "----testboundary";
        // Allow only 2 headers per part
        StringBuilder headers = new StringBuilder();
        headers.append("Content-Disposition: form-data; name=\"a\"\r\n");
        headers.append("Content-Type: text/plain\r\n");
        headers.append("X-Extra: third-header\r\n");

        var content = "\r\n------testboundary\r\n"
                + headers
                + "\r\n"
                + "body\r\n"
                + "------testboundary--\r\n";
        var parser = MultipartParser.beginParse(noopHandler(), boundary.getBytes(StandardCharsets.US_ASCII), "UTF-8",
                32768, 2);
        assertThrows(MultipartParser.HeaderTooLargeException.class,
                () -> parser.parse(ByteBuffer.wrap(content.getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    public void testChunkedDelivery() {
        var boundary = "----testboundary";
        String hugeValue = "V".repeat(33000);
        var content = "\r\n------testboundary\r\n"
                + "X-Pad: " + hugeValue + "\r\n"
                + "\r\n"
                + "body\r\n"
                + "------testboundary--\r\n";
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        var parser = MultipartParser.beginParse(noopHandler(), boundary.getBytes(StandardCharsets.US_ASCII), "UTF-8");
        // Feed in small 1024-byte chunks to simulate chunked transfer
        assertThrows(MultipartParser.HeaderTooLargeException.class, () -> {
            int chunkSize = 1024;
            for (int i = 0; i < bytes.length; i += chunkSize) {
                int end = Math.min(i + chunkSize, bytes.length);
                parser.parse(ByteBuffer.wrap(bytes, i, end - i));
            }
        });
    }

    private static PartHandler noopHandler() {
        return new PartHandler() {
            public void beginPart(final CaseInsensitiveMap<String> headers) {
            }

            public void data(final ByteBuffer buffer) {
            }

            public void endPart() {
            }
        };
    }
}
