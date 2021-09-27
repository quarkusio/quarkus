package io.quarkus.awt.it;

import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.main.QuarkusMainTest;

@QuarkusMainTest
public class ImageIOTestCase {

    @Test
    public void testImageRead() throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        BufferedImage image = ImageIO.read(classLoader.getResource("META-INF/resources/1px.png"));
        Assertions.assertEquals(1, image.getHeight());
        Assertions.assertEquals(1, image.getWidth());
    }

}
