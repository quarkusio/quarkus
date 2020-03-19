package io.quarkus.undertow.test;

import javax.servlet.annotation.WebServlet;

@WebServlet(urlPatterns = "/test-sub")
public class TestServletSubclass extends TestServlet {

}
