package io.quarkus.it.corestuff;

import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "ImageIOTestEndpoint", urlPatterns = "/core/imageio")
public class ImageIOSupport extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        BufferedImage image = ImageIO.read(classLoader.getResource("META-INF/resources/1px.png"));
        resp.getWriter().write(image.getHeight() + "x" + image.getWidth());
    }

}
