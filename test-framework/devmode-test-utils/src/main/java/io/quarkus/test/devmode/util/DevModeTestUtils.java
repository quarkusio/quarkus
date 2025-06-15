package io.quarkus.test.devmode.util;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * @deprecated Use {@link DevModeClient} instead (the methods on that class are non-static to allow ports to be
 *             specified).
 */
@Deprecated(since = "3.3", forRemoval = true)
public class DevModeTestUtils {

    private static final DevModeClient devModeClient = new DevModeClient();

    public static List<ProcessHandle> killDescendingProcesses() {
        return DevModeClient.killDescendingProcesses();
    }

    public static void filter(File input, Map<String, String> variables) throws IOException {
        DevModeClient.filter(input, variables);
    }

    public static void awaitUntilServerDown() {
        devModeClient.awaitUntilServerDown();
    }

    public static String getHttpResponse() {
        return devModeClient.getHttpResponse();
    }

    public static String getHttpResponse(Supplier<String> brokenReason) {
        return devModeClient.getHttpResponse(brokenReason);
    }

    public static String getHttpErrorResponse() {
        return devModeClient.getHttpErrorResponse();
    }

    public static String getHttpErrorResponse(Supplier<String> brokenReason) {
        return devModeClient.getHttpErrorResponse(brokenReason);
    }

    public static String getHttpResponse(String path) {
        return devModeClient.getHttpResponse(path);
    }

    public static String getHttpResponse(String path, Supplier<String> brokenReason) {
        return devModeClient.getHttpResponse(path, brokenReason);
    }

    public static String getHttpResponse(String path, boolean allowError) {
        return devModeClient.getHttpResponse(path, allowError);
    }

    public static String getHttpResponse(String path, boolean allowError, Supplier<String> brokenReason) {
        return devModeClient.getHttpResponse(path, allowError, brokenReason);
    }

    public static String getHttpResponse(String path, boolean allowError, Supplier<String> brokenReason, long timeout,
            TimeUnit tu) {
        return devModeClient.getHttpResponse(path, allowError, brokenReason, timeout, tu);
    }

    public static boolean getHttpResponse(String path, int expectedStatus) {
        return devModeClient.getHttpResponse(path, expectedStatus);
    }

    public static boolean getHttpResponse(String path, int expectedStatus, long timeout, TimeUnit tu) {
        return devModeClient.getHttpResponse(path, expectedStatus, timeout, tu);
    }

    // will fail if it receives any http response except the expected one
    public static boolean getStrictHttpResponse(String path, int expectedStatus) {
        return devModeClient.getStrictHttpResponse(path, expectedStatus);
    }

    public static String get() throws IOException {
        return devModeClient.get();
    }

    public static String get(String urlStr) throws IOException {
        return devModeClient.get(urlStr);
    }

    public static boolean isCode(String path, int code) {
        return devModeClient.isCode(path, code);
    }
}
