package io.quarkus.undertow.test.sessioncontext;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/foo")
public class TestServlet extends HttpServlet {

    @Inject
    Foo foo;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getParameter("destroy") != null) {
            req.getSession().invalidate();
        } else {
            resp.getWriter().write("count=" + foo.incrementAndGet());
        }
    }
}
