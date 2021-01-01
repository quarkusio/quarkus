package io.quarkus.undertow.test.builtinbeans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@Dependent
class ServletBuiltinBeanInjectingBean {

    @Inject
    HttpServletRequest request;

    @Inject
    HttpSession session;

    @Inject
    ServletContext context;

    public void verifyRequest() {
        assertEquals(true, request.getRequestURI().contains("/request"));
        assertEquals(request.getParameter("foo"), "bar");
    }

    public void verifySession(boolean isNew) {
        assertEquals(isNew, session.isNew());
        assertEquals(session.getAttribute("foo"), "bar");
        session.setMaxInactiveInterval(60);
    }

    public void verifyServletContext() {
        assertEquals(context.getAttribute("foo"), "bar");
        assertNotNull(context.getServletRegistration("testServlet"));
        assertEquals(context.getServletRegistration("testServlet").getClassName(), TestServlet.class.getName());
    }
}
