package org.jboss.resteasy.reactive.server.vertx.test.inputstream;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.resteasy.reactive.server.vertx.test.simple.PortProviderUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.InputStream;
import java.util.function.Supplier;

/**
 * @tpSubChapter Resources
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEasy issues about inputstream
 */
@DisplayName("InputStream Resource Test")
public class InputStreamPostGetResourceTest {
    private static Logger LOG = Logger.getLogger(InputStreamPostGetResourceTest.class);

    static Client client;
    static Runtime runtime;

    @RegisterExtension
    static ResteasyReactiveUnitTest testExtension = new ResteasyReactiveUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(InputStreamPostGetResource.class,
                            PortProviderUtil.class);
                    return war;
                }
            });

    @BeforeAll
    public static void init() {
        client = ClientBuilder.newClient();
        runtime = Runtime.getRuntime();
    }

    @AfterAll
    public static void close() {
        client.close();
        client = null;
    }

    @BeforeEach
    public void releaseMemory() {
        System.gc();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, InputStreamPostGetResourceTest.class.getSimpleName());
    }

    private long getCurrentlyAllocatedMemory() {
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
    }

    /**
     * Test for Get InputStream with no issue on size nor OOME
     */
    @ParameterizedTest
    @ValueSource(ints = {1, 100, 1024})
    @DisplayName("Test Get InputStream")
    public void testGetInputStream(int mb) throws Exception {
        WebTarget base = client.target(generateURL("/inputstreamtransfer/test"));
        long size = mb * 1024 * 1024L;
        InputStream is = new FakeInputStream(size);
        long before = getCurrentlyAllocatedMemory();
        long start = System.nanoTime();
        Response response = base.request().post(Entity.entity(is, MediaType.APPLICATION_OCTET_STREAM));
        long stop = System.nanoTime();
        long after = getCurrentlyAllocatedMemory();
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        long remoteSize = response.readEntity(Long.class);
        Assertions.assertEquals(size, remoteSize);
        float time = (float) ((stop - start) / 1000000000.0);
        long usedMemory = after - before;
        LOG.infof("GET %d MB in %f s, %f MB/s using %d MB", mb, time, size / time / (1024 * 1024), usedMemory);
        response.close();
    }

    /**
     * Test for Post InputStream with no issue on size nor OOME
     */
    @ParameterizedTest
    @ValueSource(ints = {1, 100, 1024})
    @DisplayName("Test Post InputStream")
    public void testPostInputStream(int mb) throws Exception {
        int size = mb * 1024 * 1024;
        WebTarget base = client.target(generateURL("/inputstreamtransfer/test/" + size));
        long before = getCurrentlyAllocatedMemory();
        long start = System.nanoTime();
        InputStream is = base.request().get(InputStream.class);
        long len = 0;
        int read = 0;
        byte[] b = new byte[65536];
        while ((read = is.read(b)) > 0) {
            len += read;
        }
        long stop = System.nanoTime();
        long after = getCurrentlyAllocatedMemory();
        Assertions.assertEquals(size, len);
        is.close();
        float time = (float) ((stop - start) / 1000000000.0);
        long usedMemory = after - before;
        LOG.infof("POST %d MB in %f s, %f MB/s using %d MB", mb, time, size / time / (1024 * 1024), usedMemory);
    }
}
