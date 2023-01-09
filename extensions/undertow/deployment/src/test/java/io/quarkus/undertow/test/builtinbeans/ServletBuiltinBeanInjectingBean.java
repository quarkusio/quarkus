package io.quarkus.undertow.test.builtinbeans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

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
