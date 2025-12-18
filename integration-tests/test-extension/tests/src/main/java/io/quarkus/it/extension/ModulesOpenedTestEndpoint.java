package io.quarkus.it.extension;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.util.ArrayList;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * This test verifies that the module java.base/java.util is open:
 * this should have been requested by {@code io.quarkus.extest.deployment.ModulesCustomProcessor},
 * so we're verifying that the ModuleOpenBuildItem is working as expected.
 */
@WebServlet(name = "ModulesOpenedTestEndpoint", urlPatterns = "/core/modulesOpen")
public class ModulesOpenedTestEndpoint extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        PrintWriter writer = resp.getWriter();
        boolean testPassed;
        String errorMessage = null;
        try {
            verifyJavaUtilsWasOpened();
            testPassed = true;
        } catch (Exception e) {
            testPassed = false;
            errorMessage = e.getMessage();
        }
        if (testPassed) {
            writer.write("OK");
        } else {
            writer.write("Test Failed: " + errorMessage);
        }
    }

    public void verifyJavaUtilsWasOpened() throws Exception {
        String SOME_DATA = "anything";
        ArrayList<String> list = new ArrayList<>();
        list.add(SOME_DATA);

        // Let's try some cheeky operation which shouldn't be allowed:
        Field internalArray = ArrayList.class.getDeclaredField("elementData");

        // .. and specifically this will fail if "--add-opens=java.base/java.util=ALL-UNNAMED", hasn't been set:
        try {
            internalArray.setAccessible(true);
        } catch (InaccessibleObjectException e) {
            throw new RuntimeException("Test Failed: Module java.base/java.util is NOT open! ", e);
        }

        // Check that we can indeed read it:
        Object[] data = (Object[]) internalArray.get(list);
        if (!data[0].equals(SOME_DATA)) {
            throw new RuntimeException("Test Failed: Internal array content is not as expected!");
        }
    }

}
