package io.quarkus.qute.deployment.asyncdata;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusUnitTest;

public class AsyncDataTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Client.class)
                    .addAsResource(new StringAsset("{@io.quarkus.qute.deployment.asyncdata.Client client}"
                            + "{#for token in client.tokens}"
                            + "{token}:{/for}"), "templates/test1.html")
                    .addAsResource(new StringAsset("{@io.quarkus.qute.deployment.asyncdata.Client client}"
                            + "{#for token in tokens}"
                            + "{token}:{/for}"), "templates/test2.html")
                    .addAsResource(new StringAsset("{token}"), "templates/test3.html"));

    @Inject
    Template test1;

    @Inject
    Template test2;

    @Inject
    Template test3;

    @Test
    public void testAsyncData() {
        assertEquals("alpha:bravo:delta:", test1.data("client", new Client()).render());
        assertEquals("alpha:bravo:delta:", test2.data("tokens", new Client().getTokens()).render());
        assertEquals("alpha", test3.data("token", CompletableFuture.completedFuture("alpha")).render());
    }

}
