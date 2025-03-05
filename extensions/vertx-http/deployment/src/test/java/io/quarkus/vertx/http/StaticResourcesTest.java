package io.quarkus.vertx.http;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class StaticResourcesTest extends AbstractStaticResourcesTest {

    @RegisterExtension
    final static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .add(new StringAsset("quarkus.http.enable-compression=true\n"),
                            "application.properties")
                    .addAsResource("static-file.html", "META-INF/resources/dir/file.txt")
                    .addAsResource("static-file.html", "META-INF/resources/l'équipe.pdf")
                    .addAsResource("static-file.html", "META-INF/resources/static file.txt")
                    .addAsResource("static-file.html", "META-INF/resources/static-file.html")
                    .addAsResource("static-file.html", "META-INF/resources/.hidden-file.html")
                    .addAsResource("static-file.html", "META-INF/resources/index.html")
                    .addAsResource("static-file.html", "META-INF/resources/image.svg"));

}
