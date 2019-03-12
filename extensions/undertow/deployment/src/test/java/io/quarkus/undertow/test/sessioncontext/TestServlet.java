package io.quarkus.undertow.test.sessioncontext;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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