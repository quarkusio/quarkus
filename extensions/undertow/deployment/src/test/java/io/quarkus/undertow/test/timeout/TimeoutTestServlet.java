package io.quarkus.undertow.test.timeout;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.stream.Collectors;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = "/timeout")
public class TimeoutTestServlet extends HttpServlet {

    public static final String TIMEOUT_SERVLET = "timeout-servlet";
    public static volatile boolean read = false;
    public static volatile IOException error;

    public static void reset() {
        error = null;
        read = false;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ReadTimeoutTestCase.READ_DATA = readRequestData(req);
        try {
            read = true;
            mimicProcessing(req);
            resp.getWriter().write(TIMEOUT_SERVLET);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void mimicProcessing(HttpServletRequest req) throws InterruptedException {
        String header = req.getHeader("Processing-Time");
        if (header != null) {
            long sleepTime = Long.parseLong(header);
            Thread.sleep(sleepTime);
        }
    }

    private String readRequestData(HttpServletRequest req) {
        try (InputStreamReader isReader = new InputStreamReader(req.getInputStream());
                BufferedReader reader = new BufferedReader(isReader)) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            error = e;
            throw new UncheckedIOException(e);
        } catch (UncheckedIOException e) {
            error = e.getCause();
            throw e;
        }
    }
}
