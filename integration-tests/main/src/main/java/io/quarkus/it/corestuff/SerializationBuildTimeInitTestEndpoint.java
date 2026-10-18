package io.quarkus.it.corestuff;

import java.io.IOException;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import io.quarkus.it.corestuff.serialization.SerializationChildBuildTimeInit;
import io.quarkus.it.corestuff.serialization.SerializationParentBuildTimeInit;

/**
 * Ensure that classes registered for serialization are build-time-initialized
 */
@WebServlet(name = "CoreSerializationBuildTimeInitTestEndpoint", urlPatterns = "/core/serialization-build-time-init")
public class SerializationBuildTimeInitTestEndpoint extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.getWriter().write(SerializationParentBuildTimeInit.value + "\t" + SerializationChildBuildTimeInit.value);
    }

}
