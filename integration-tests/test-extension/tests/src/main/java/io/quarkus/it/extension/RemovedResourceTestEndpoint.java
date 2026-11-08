package io.quarkus.it.extension;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import io.quarkus.extest.runtime.RemovedResource;
import io.quarkus.extest.runtime.RemovedResource.ClassLoaderKind;

@WebServlet(name = "RemovedResourceTestEndpoint", urlPatterns = "/core/removed-resource")
public class RemovedResourceTestEndpoint extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        RemovedResource resource = RemovedResource.valueOf(req.getParameter("resource"));
        ClassLoaderKind clKind = ClassLoaderKind.valueOf(req.getParameter("classLoaderKind"));
        try {
            String content = resource.load(clKind);
            resp.getWriter().write(content);
        } catch (Exception e) {
            PrintWriter writer = resp.getWriter();
            e.printStackTrace(writer);
        }
    }
}
