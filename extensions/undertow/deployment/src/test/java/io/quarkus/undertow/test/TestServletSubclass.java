package io.quarkus.undertow.test;

import jakarta.servlet.annotation.WebServlet;

@WebServlet(urlPatterns = "/test-sub")
public class TestServletSubclass extends TestServlet {

}
