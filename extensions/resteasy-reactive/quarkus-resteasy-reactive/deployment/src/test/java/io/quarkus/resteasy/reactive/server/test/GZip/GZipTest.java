package io.quarkus.resteasy.reactive.server.test.GZip;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.imageio.ImageIO;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import com.google.common.collect.ImmutableMap;
import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class GZipTest {

    private static final String APP_PROPS = "" +
            "quarkus.http.enable-compression=true\n";

    static long contentLength;
    static String longString;
    static {

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; ++i) {
            sb.append("Hello RESTEasy Reactive;");
        }
        longString = sb.toString();
    }

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addAsResource(new StringAsset(APP_PROPS), "application.properties")
                            .addClasses(TestCompression.class);
                }
            });

    @Test
    public void testServerCompression() throws Exception {

        RestAssured.given().get("/test/compression").then().statusCode(200)
                .header("content-encoding", "gzip")
                .header("content-length", Matchers.not(Matchers.equalTo(Integer.toString(longString.length()))))
                .body(Matchers.equalTo(longString));

        RestAssured.given().get("/test/nocompression").then().statusCode(200)
                .header("content-encoding", "identity")
                .header("content-length", Matchers.equalTo(contentLength))
                .body(Matchers.equalTo(createImage()));
    }

    @Path("/test")
    public static class TestCompression {

        @Path("/compression")
        @GET
        public String registerCompression() {
            return longString;
        }

        @Path("/nocompression")
        @GET
        @DisableCompression
        public BufferedImage registerNoCompression() throws IOException {
            BufferedImage image = createImage();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            outputStream.close();
            contentLength = outputStream.size();
            System.out.println(" content-length using ByteArrayOutputStream => " + contentLength );
            return image;
        }
    }

    public static BufferedImage createImage() throws IOException {
        int width = 300;
        int height = 300;

        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        // Create a graphics which can be used to draw into the buffered image
        Graphics2D graphic = bufferedImage.createGraphics();
        // fill all the image with white
        graphic.setColor(Color.white);
        graphic.fillRect(0, 0, width, height);

        // create a circle with black
        graphic.setColor(Color.black);
        graphic.fillOval(0, 0, width, height);

        // create a string with yellow
        graphic.setColor(Color.yellow);
        graphic.drawString("Quarkus RESTEasy Reactive", 50, 120);
        graphic.dispose();

        // Save as PNG
        File file = new File("src/test/resources/imageForTest.png");
        ImageIO.write(bufferedImage, "png", file);
        return bufferedImage;
    }
}
