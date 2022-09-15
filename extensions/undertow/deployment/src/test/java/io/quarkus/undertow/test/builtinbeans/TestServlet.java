package io.quarkus.undertow.test.builtinbeans;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/foo/*", name = "testServlet")
public class TestServlet extends HttpServlet {

    @Inject
    ServletBuiltinBeanInjectingBean bean;

    @Inject
    Counter counter;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getRequestURI().contains("/request")) {
            bean.verifyRequest();
            resp.getWriter().write("foo=bar");
        }
        if (req.getRequestURI().endsWith("/session")) {
            if (req.getParameter("destroy") != null) {
                req.getSession().invalidate();
            } else {
                HttpSession session = req.getSession();
                if (session.isNew()) {
                    session.setAttribute("foo", "bar");
                }
                bean.verifySession(session.isNew());
                resp.setIntHeader("counter", counter.incrementAndGet());
                resp.getWriter().write("foo=bar");
            }
        }
        if (req.getRequestURI().endsWith("/context")) {
            req.getSession().getServletContext().setAttribute("foo", "bar");
            bean.verifyServletContext();
            resp.getWriter().write("foo=bar");
        }
    }
}
