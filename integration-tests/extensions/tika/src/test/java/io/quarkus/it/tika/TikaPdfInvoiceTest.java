package io.quarkus.it.tika;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.startsWith;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class TikaPdfInvoiceTest {

    @Test
    public void testGetPlainTextFromInvoice() throws Exception {
        given()
                .when().header("Content-Type", "application/pdf")
                .body(readTestFile("invoice.pdf"))
                .post("/invoice/text")
                .then()
                .statusCode(200)
                .body(allOf(startsWith("PDF Invoice Example"),
                        containsString("DEMO - Sliced Invoices Order Number 12345"),
                        new OrderCheckingMatcher()));
    }

    private byte[] readTestFile(String fileName) throws Exception {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(fileName)) {
            return readBytes(is);
        }
    }

    static byte[] readBytes(InputStream is) throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int len;
        while ((len = is.read(buffer)) != -1) {
            os.write(buffer, 0, len);
        }
        return os.toByteArray();
    }

    private static class OrderCheckingMatcher implements Matcher<String> {
        int from;
        int to;

        @Override
        public void describeTo(Description description) {
        }

        @Override
        public boolean matches(Object item) {
            String text = (String) item;
            from = text.indexOf("From:");
            to = text.indexOf("To:");
            return from > 0 && to > 0 && from < to;
        }

        @Override
        public void describeMismatch(Object item, Description mismatchDescription) {
            mismatchDescription.appendText("The invoice does not have From preceeding To");
        }

        @Override
        public void _dont_implement_Matcher___instead_extend_BaseMatcher_() {
        }
    }
}
