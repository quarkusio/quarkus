package io.quarkus.undertow.test.builtinbeans;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

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
