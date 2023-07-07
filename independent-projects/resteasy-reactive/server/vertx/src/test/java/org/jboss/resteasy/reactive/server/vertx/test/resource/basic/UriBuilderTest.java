package org.jboss.resteasy.reactive.server.vertx.test.resource.basic;

import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.UriBuilder;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.common.jaxrs.UriBuilderImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @tpSubChapter Util tests
 * @tpChapter Unit tests
 * @tpTestCaseDetails Test for UriBuilder class.
 * @tpSince RESTEasy 3.0.16
 */
public class UriBuilderTest {
    private static final String ERROR_MSG = "UriBuilder works incorrectly";
    protected static final Logger logger = Logger.getLogger(UriBuilderTest.class.getName());

    private static final Pattern uriPattern = Pattern.compile("^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?");

    protected static final String URL = "http://cts.tck:888/resource";
    private static final String ENCODED = "%42%5A%61%7a%2F%%21";

    private static final String ENCODED_EXPECTED_PATH = "path-rootless%2Ftest2/x%25yz/%2Fpath-absolute%2F%2525test1/fred@example.com/x%25yz";

    /**
     * @tpTestDetails Test for all important method and use cases of UriBuilder class.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void uriBuilderTest() throws Exception {
        // testExceptions
        {
            try {
                UriBuilder.fromUri(":cts:8080//tck:90090//jaxrs ");
                fail(ERROR_MSG);
            } catch (IllegalArgumentException e) {
                // ok
            }

            // testUrn
            UriBuilder.fromUri("urn:isbn:096139210x");
        }

        // testParse
        {
            String[] uris = { ":cts:8080//tck:90090//jaxrs "
            };
            for (String uri : uris) {
                printParse(uri);
            }
        }

        // testNullReplaceQuery
        {
            UriBuilder builder = UriBuilder.fromUri("/foo?a=b&bar=foo");
            builder.replaceQueryParam("bar", (Object[]) null);
            URI uri = builder.build();
            logger.info(uri.toString());
        }

        // testNullHost
        {
            UriBuilder builder = UriBuilder.fromUri("http://example.com/foo/bar");
            builder.scheme(null);
            builder.host(null);
            URI uri = builder.build();
            logger.info(uri.toString());
            Assert.assertEquals(ERROR_MSG, uri.toString(), "/foo/bar");

        }

        // testTemplate
        {
            UriBuilder builder = UriBuilder.fromUri("http://{host}/x/y/{path}?{q}={qval}");
            String template = builder.toTemplate();
            Assert.assertEquals(ERROR_MSG, template, "http://{host}/x/y/{path}?{q}={qval}");
            builder = builder.resolveTemplate("host", "localhost");
            template = builder.toTemplate();
            Assert.assertEquals(ERROR_MSG, template, "http://localhost/x/y/{path}?{q}={qval}");

            builder = builder.resolveTemplate("q", "name");
            template = builder.toTemplate();
            Assert.assertEquals(ERROR_MSG, template, "http://localhost/x/y/{path}?name={qval}");
            Map<String, Object> values = new HashMap<>();
            values.put("path", "z");
            values.put("qval", 42);
            builder = builder.resolveTemplates(values);
            template = builder.toTemplate();
            Assert.assertEquals(ERROR_MSG, template, "http://localhost/x/y/z?name=42");

            // RESTEASY-1878 - test if regex templates work
            // see jakarta.ws.rs.core.UriBuilder class description for info about regex template parameters
            builder = UriBuilder.fromUri("{id: [0-9]+}");
            Assert.assertEquals(new URI("123"), builder.build("123"));

            builder = UriBuilder.fromUri("{id: [a-z]+}");
            Assert.assertEquals(new URI("abcd"), builder.build("abcd"));

            builder = UriBuilder.fromUri("/resources/{id: [0-9]+}");
            Assert.assertEquals(new URI("/resources/123"), builder.build("123"));
            // end of RESTEASY-1878
        }

        // test587
        {
            logger.info(UriBuilder.fromPath("/{p}").build("$a"));
        }

        // test443
        {
            // test for RESTEASY-443

            UriBuilderImpl.fromUri("?param=").replaceQueryParam("otherParam", "otherValue");

        }

        // testEmoji
        {
            UriBuilder builder = UriBuilder.fromPath("/my/url");
            builder.queryParam("msg", "emoji stuff %EE%81%96%EE%90%8F");
            URI uri = builder.build();
            logger.info(uri);
            Assert.assertEquals(ERROR_MSG, "/my/url?msg=emoji+stuff+%EE%81%96%EE%90%8F", uri.toString());

        }

        // testQuery
        {
            UriBuilder builder = UriBuilder.fromPath("/foo");
            builder.queryParam("mama", "   ");
            Assert.assertEquals(ERROR_MSG, builder.build().toString(), "/foo?mama=+++");
        }

        // testQuery2
        {
            UriBuilder builder = UriBuilder.fromUri("http://localhost/test");
            builder.replaceQuery("a={b}");
            URI uri = builder.build("=");
            Assert.assertEquals(ERROR_MSG, uri.toString(), "http://localhost/test?a=%3D");
        }

        // testReplaceScheme
        {
            URI bu = UriBuilder.fromUri("http://localhost:8080/a/b/c").scheme("https").build();
            Assert.assertEquals(ERROR_MSG, URI.create("https://localhost:8080/a/b/c"), bu);
        }

        // testReplaceUserInfo
        {
            URI bu = UriBuilder.fromUri("http://bob@localhost:8080/a/b/c").userInfo("sue").build();
            Assert.assertEquals(ERROR_MSG, URI.create("http://sue@localhost:8080/a/b/c"), bu);
        }

        // testReplaceHost
        {
            URI bu = UriBuilder.fromUri("http://localhost:8080/a/b/c").host("a.com").build();
            Assert.assertEquals(ERROR_MSG, URI.create("http://a.com:8080/a/b/c"), bu);
        }

        // testReplacePort
        {
            URI bu = UriBuilder.fromUri("http://localhost:8080/a/b/c").port(9090).build();
            Assert.assertEquals(ERROR_MSG, URI.create("http://localhost:9090/a/b/c"), bu);

            bu = UriBuilder.fromUri("http://localhost:8080/a/b/c").port(-1).build();
            Assert.assertEquals(ERROR_MSG, URI.create("http://localhost/a/b/c"), bu);
        }

        // testReplacePath
        {
            URI bu = UriBuilder.fromUri("http://localhost:8080/a/b/c").replacePath("/x/y/z").build();
            Assert.assertEquals(ERROR_MSG, URI.create("http://localhost:8080/x/y/z"), bu);
        }

        // testReplaceMatrixParam
        {
            URI bu = UriBuilder.fromUri("http://localhost:8080/a/b/c;a=x;b=y").replaceMatrix("x=a;y=b").build();
            Assert.assertEquals(ERROR_MSG, URI.create("http://localhost:8080/a/b/c;x=a;y=b"), bu);
            UriBuilder builder = UriBuilder.fromUri("http://localhost:8080/a").path("/{b:B{0,10}}/c;a=x;b=y");
            builder.replaceMatrixParam("a", "1", "2");
            bu = builder.build("B");
            Assert.assertEquals(ERROR_MSG, URI.create("http://localhost:8080/a/B/c;b=y;a=1;a=2"), bu);

            // test removal

            bu = UriBuilder.fromUri("http://localhost:8080/a/b/c;a=x;b=y").replaceMatrixParam("a", (Object[]) null).build();
            Assert.assertEquals(ERROR_MSG, URI.create("http://localhost:8080/a/b/c;b=y"), bu);
        }

        // testReplaceQueryParams
        {
            URI bu = UriBuilder.fromUri("http://localhost:8080/a/b/c?a=x&b=y").replaceQuery("x=a&y=b").build();
            Assert.assertEquals(ERROR_MSG, URI.create("http://localhost:8080/a/b/c?x=a&y=b"), bu);

            UriBuilder builder = UriBuilder.fromUri("http://localhost:8080/a/b/c?a=x&b=y");
            builder.replaceQueryParam("a", "1", "2");
            bu = builder.build();
            Assert.assertEquals(ERROR_MSG, URI.create("http://localhost:8080/a/b/c?b=y&a=1&a=2"), bu);

        }

        // testReplaceFragment
        {
            URI bu = UriBuilder.fromUri("http://localhost:8080/a/b/c?a=x&b=y#frag").fragment("ment").build();
            Assert.assertEquals(ERROR_MSG, URI.create("http://localhost:8080/a/b/c?a=x&b=y#ment"), bu);
        }

        // testReplaceUri
        {
            URI u = URI.create("http://bob@localhost:8080/a/b/c?a=x&b=y#frag");

            UriBuilder uriBuilder = UriBuilder.fromUri(u);
            URI bu = uriBuilder.uri(URI.create("https://bob@localhost:8080")).build();
            Assert.assertEquals(ERROR_MSG, URI.create("https://bob@localhost:8080/a/b/c?a=x&b=y#frag"), bu);

            bu = UriBuilder.fromUri(u).uri(URI.create("https://sue@localhost:8080")).build();
            Assert.assertEquals(ERROR_MSG, URI.create("https://sue@localhost:8080/a/b/c?a=x&b=y#frag"), bu);

            bu = UriBuilder.fromUri(u).uri(URI.create("https://sue@localhost:9090")).build();
            Assert.assertEquals(ERROR_MSG, URI.create("https://sue@localhost:9090/a/b/c?a=x&b=y#frag"), bu);

            bu = UriBuilder.fromUri(u).uri(URI.create("/x/y/z")).build();
            Assert.assertEquals(ERROR_MSG, URI.create("http://bob@localhost:8080/x/y/z?a=x&b=y#frag"), bu);

            bu = UriBuilder.fromUri(u).uri(URI.create("?x=a&b=y")).build();
            Assert.assertEquals(ERROR_MSG, URI.create("http://bob@localhost:8080/a/b/c?x=a&b=y#frag"), bu);

            bu = UriBuilder.fromUri(u).uri(URI.create("#ment")).build();
            Assert.assertEquals(ERROR_MSG, URI.create("http://bob@localhost:8080/a/b/c?a=x&b=y#ment"), bu);
        }

        // testSchemeSpecificPart
        {
            URI u = URI.create("http://bob@localhost:8080/a/b/c?a=x&b=y#frag");

            URI bu = UriBuilder.fromUri(u).schemeSpecificPart("//sue@remotehost:9090/x/y/z?x=a&y=b").build();
            Assert.assertEquals(ERROR_MSG, URI.create("http://sue@remotehost:9090/x/y/z?x=a&y=b#frag"), bu);
        }

        // testAppendPath
        {
            URI bu = UriBuilder.fromUri("http://localhost:8080/a/b/c/").path("/").build();
            Assert.assertEquals(ERROR_MSG, URI.create("http://localhost:8080/a/b/c/"), bu);

            bu = UriBuilder.fromUri("http://localhost:8080/a/b/c/").path("/x/y/z").build();
            Assert.assertEquals(ERROR_MSG, URI.create("http://localhost:8080/a/b/c/x/y/z"), bu);

            bu = UriBuilder.fromUri("http://localhost:8080/a/b/c").path("/x/y/z").build();
            Assert.assertEquals(ERROR_MSG, URI.create("http://localhost:8080/a/b/c/x/y/z"), bu);

            bu = UriBuilder.fromUri("http://localhost:8080/a/b/c").path("x/y/z").build();
            Assert.assertEquals(ERROR_MSG, URI.create("http://localhost:8080/a/b/c/x/y/z"), bu);

            bu = UriBuilder.fromUri("http://localhost:8080/a/b/c").path("/").build();
            Assert.assertEquals(ERROR_MSG, URI.create("http://localhost:8080/a/b/c/"), bu);

            bu = UriBuilder.fromUri("http://localhost:8080/a/b/c").path("").build();
            Assert.assertEquals(ERROR_MSG, URI.create("http://localhost:8080/a/b/c"), bu);

            bu = UriBuilder.fromUri("http://localhost:8080/a%20/b%20/c%20").path("/x /y /z ").build();
            Assert.assertEquals(ERROR_MSG, URI.create("http://localhost:8080/a%20/b%20/c%20/x%20/y%20/z%20"), bu);
        }

        // testAppendQueryParams
        {
            URI bu = UriBuilder.fromUri("http://localhost:8080/a/b/c?a=x&b=y").queryParam("c", "z").build();
            Assert.assertEquals(ERROR_MSG, URI.create("http://localhost:8080/a/b/c?a=x&b=y&c=z"), bu);
        }

        // testQueryParamsEncoding
        {
            URI bu = UriBuilder.fromUri("http://localhost:8080/a/b/c?a=x&b=y").queryParam("c", "z=z/z").build();
            Assert.assertEquals(ERROR_MSG, URI.create("http://localhost:8080/a/b/c?a=x&b=y&c=z%3Dz%2Fz"), bu);
        }

        // testAppendMatrixParams
        {
            URI bu = UriBuilder.fromUri("http://localhost:8080/a/b/c;a=x;b=y").matrixParam("c", "z").build();
            Assert.assertEquals(ERROR_MSG, URI.create("http://localhost:8080/a/b/c;a=x;b=y;c=z"), bu);
        }

        // testResourceAppendPath
        {
            URI ub = UriBuilder.fromUri("http://localhost:8080/base").path(UriBuilderResource.class).build();
            Assert.assertEquals(ERROR_MSG, URI.create("http://localhost:8080/base/resource"), ub);

            ub = UriBuilder.fromUri("http://localhost:8080/base").path(UriBuilderResource.class, "get").build();
            Assert.assertEquals(ERROR_MSG, URI.create("http://localhost:8080/base/method"), ub);

            Method get = UriBuilderResource.class.getMethod("get");
            Method locator = UriBuilderResource.class.getMethod("locator");
            ub = UriBuilder.fromUri("http://localhost:8080/base").path(get).path(locator).build();
            Assert.assertEquals(ERROR_MSG, URI.create("http://localhost:8080/base/method/locator"), ub);
        }

        // testTemplates
        {
            URI bu = UriBuilder.fromUri("http://localhost:8080/a/b/c").path("/{foo}/{bar}/{baz}/{foo}").build("x", "y", "z");
            Assert.assertEquals(ERROR_MSG, URI.create("http://localhost:8080/a/b/c/x/y/z/x"), bu);

            Map<String, Object> m = new HashMap<>();
            m.put("foo", "x");
            m.put("bar", "y");
            m.put("baz", "z");
            bu = UriBuilder.fromUri("http://localhost:8080/a/b/c").path("/{foo}/{bar}/{baz}/{foo}").buildFromMap(m);
            Assert.assertEquals(ERROR_MSG, URI.create("http://localhost:8080/a/b/c/x/y/z/x"), bu);
        }

        // testClone
        {
            UriBuilder ub = UriBuilder.fromUri("http://user@localhost:8080/?query#fragment").path("a");
            URI full = ub.clone().path("b").build();
            URI base = ub.build();

            Assert.assertEquals(ERROR_MSG, URI.create("http://user@localhost:8080/a?query#fragment"), base);
            Assert.assertEquals(ERROR_MSG, URI.create("http://user@localhost:8080/a/b?query#fragment"), full);
        }

        // FromUriTest3 - Create an UriBuilder instance using uriBuilder.fromUri(String)
        {
            StringBuffer sb = new StringBuffer();
            URI uri;

            String[] uris = {
                    "mailto:java-net@java.sun.com",
                    "ftp://ftp.is.co.za/rfc/rfc1808.txt", "news:comp.lang.java",
                    "urn:isbn:096139210x",
                    "http://www.ietf.org/rfc/rfc2396.txt",
                    "ldap://[2001:db8::7]/c=GB?objectClass?one",
                    "tel:+1-816-555-1212",
                    "telnet://192.0.2.16:80/",
                    "foo://example.com:8042/over/there?name=ferret#nose",
            };

            int j = 0;
            while (j < 9) {
                uri = UriBuilder.fromUri(uris[j]).build();
                if (uri.toString().trim().compareToIgnoreCase(uris[j]) != 0) {
                    sb.append("Test failed for expected uri: " + uris[j] +
                            " Got " + uri.toString() + " instead");
                    throw new Exception(sb.toString());
                }
                j++;
            }
        }

        // testEncoding
        {
            HashMap<String, Object> map = new HashMap<>();

            {
                map.clear();
                UriBuilderImpl impl = (UriBuilderImpl) UriBuilder.fromPath("/foo/{id}");
                map.put("id", "something %%20something");

                URI uri = impl.buildFromMap(map);
                Assert.assertEquals(ERROR_MSG, "/foo/something%20%25%2520something", uri.toString());
            }
            {
                UriBuilderImpl impl = (UriBuilderImpl) UriBuilder.fromPath("/foo/{id}");
                map.clear();
                map.put("id", "something something");
                URI uri = impl.buildFromMap(map);
                Assert.assertEquals(ERROR_MSG, "/foo/something%20something", uri.toString());
            }
            {
                UriBuilderImpl impl = (UriBuilderImpl) UriBuilder.fromPath("/foo/{id}");
                map.clear();
                map.put("id", "something%20something");
                URI uri = impl.buildFromEncodedMap(map);
                Assert.assertEquals(ERROR_MSG, "/foo/something%20something", uri.toString());
            }

            {
                UriBuilderImpl impl = (UriBuilderImpl) UriBuilder.fromPath("/foo/{id}");

                impl.substitutePathParam("id", "something %%20something", false);
                URI uri = impl.build();
                Assert.assertEquals(ERROR_MSG, "/foo/something%20%25%20something", uri.toString());
            }
            {
                UriBuilderImpl impl = (UriBuilderImpl) UriBuilder.fromPath("/foo/{id}");

                impl.substitutePathParam("id", "something something", false);
                URI uri = impl.build();
                Assert.assertEquals(ERROR_MSG, "/foo/something%20something", uri.toString());
            }
            {
                UriBuilderImpl impl = (UriBuilderImpl) UriBuilder.fromPath("/foo/{id}");

                impl.substitutePathParam("id", "something%20something", true);
                URI uri = impl.build();
                Assert.assertEquals(ERROR_MSG, "/foo/something%20something", uri.toString());
            }
        }

        // testQueryParamSubstitution
        {
            UriBuilder.fromUri("http://localhost/test").queryParam("a", "{b}").build("c");
        }

        // testEncodedMap1 - Regression from TCK 1.1
        {
            StringBuffer sb = new StringBuffer();
            boolean pass = true;
            URI uri;

            Map<String, String> maps = new HashMap<>();
            maps.put("x", "x%20yz");
            maps.put("y", "/path-absolute/%test1");
            maps.put("z", "fred@example.com");
            maps.put("w", "path-rootless/test2");

            String expected_path = "path-rootless/test2/x%20yz//path-absolute/%25test1/fred@example.com/x%20yz";

            uri = UriBuilder.fromPath("").path("{w}/{x}/{y}/{z}/{x}").buildFromEncodedMap(maps);
            if (uri.getRawPath().compareToIgnoreCase(expected_path) != 0) {
                pass = false;
                sb.append("Test failed for expected path: " + expected_path +
                        " Got " + uri.getRawPath() + " instead\n");
            } else {
                sb.append("Got expected path: " + uri.getRawPath() + "\n");
            }

            if (!pass) {
                logger.info(sb.toString());
            }
            Assert.assertTrue(ERROR_MSG, pass);
        }

        // testEncodedMapTest3 - from TCK 1.1
        {
            Map<String, String> maps = new HashMap<>();
            maps.put("x", null);
            maps.put("y", "/path-absolute/test1");
            maps.put("z", "fred@example.com");
            maps.put("w", "path-rootless/test2");
            maps.put("u", "extra");

            try {
                UriBuilder.fromPath("").path("{w}/{x}/{y}/{z}/{x}").buildFromEncodedMap(maps);
                throw new Exception("Test Failed: expected IllegalArgumentException not thrown");
            } catch (IllegalArgumentException ex) {
            }
        }

        // testEncodedMapTest4
        {
            Map<String, String> maps = new HashMap<>();
            maps.put("x", "x%yz");
            maps.put("y", "/path-absolute/test1");
            maps.put("z", "fred@example.com");
            maps.put("w", "path-rootless/test2");
            maps.put("u", "extra");
            try {
                UriBuilder.fromPath("").path("{w}/{v}/{x}/{y}/{z}/{x}").buildFromEncodedMap(maps);
                throw new Exception("Test Failed: expected IllegalArgumentException not thrown");
            } catch (IllegalArgumentException ex) {
            }
        }

        // testBuildFromMapTest1
        {
            StringBuffer sb = new StringBuffer();
            boolean pass = true;
            URI uri;

            Map<String, String> maps = new HashMap<>();
            maps.put("x", "x%yz");
            maps.put("y", "/path-absolute/test1");
            maps.put("z", "fred@example.com");
            maps.put("w", "path-rootless/test2");

            String expected_path = "path-rootless%2Ftest2/x%25yz/%2Fpath-absolute%2Ftest1/fred@example.com/x%25yz";

            try {
                uri = UriBuilder.fromPath("").path("{w}/{x}/{y}/{z}/{x}").buildFromMap(maps);
                if (uri.getRawPath().compareToIgnoreCase(expected_path) != 0) {
                    pass = false;
                    sb.append("Test failed for expected path: " + expected_path +
                            " Got " + uri.getRawPath() + " instead\n");
                } else {
                    sb.append("Got expected path: " + uri.getRawPath() + "\n");
                }
            } catch (Exception ex) {
                pass = false;
                sb.append("Unexpected exception thrown: " + ex.getMessage() +
                        "\n");
            }

            if (!pass) {
                throw new Exception("At least one assertion failed: " + sb.toString());
            }
        }

        // testBuildFromMapTest2
        {
            StringBuffer sb = new StringBuffer();
            boolean pass = true;
            URI uri;

            Map<String, String> maps = new HashMap<>();
            maps.put("x", "x%yz");
            maps.put("y", "/path-absolute/test1");
            maps.put("z", "fred@example.com");
            maps.put("w", "path-rootless/test2");
            maps.put("u", "extra");

            String expected_path = "path-rootless%2Ftest2/x%25yz/%2Fpath-absolute%2Ftest1/fred@example.com/x%25yz";

            try {
                uri = UriBuilder.fromPath("").path("{w}/{x}/{y}/{z}/{x}").buildFromMap(maps);
                if (uri.getRawPath().compareToIgnoreCase(expected_path) != 0) {
                    pass = false;
                    sb.append("Test failed for expected path: " + expected_path +
                            " Got " + uri.getRawPath() + " instead" + "\n");
                } else {
                    sb.append("Got expected path: " + uri.getRawPath() + "\n");
                }
            } catch (Exception ex) {
                pass = false;
                sb.append("Unexpected exception thrown: " + ex.getMessage() +
                        "\n");
            }

            if (!pass) {
                throw new Exception("At least one assertion failed: " + sb.toString());
            }
        }

        // testBuildFromMapTest3
        {
            Map<String, String> maps = new HashMap<>();
            maps.put("x", null);
            maps.put("y", "/path-absolute/test1");
            maps.put("z", "fred@example.com");
            maps.put("w", "path-rootless/test2");
            maps.put("u", "extra");
            try {
                UriBuilder.fromPath("").path("{w}/{x}/{y}/{z}/{x}").buildFromMap(maps);
                throw new Exception("Test Failed: expected IllegalArgumentException not thrown");
            } catch (IllegalArgumentException ex) {
            }
        }

        // testBuildFromMapTest4
        {
            Map<String, String> maps = new HashMap<>();
            maps.put("x", "x%yz");
            maps.put("y", "/path-absolute/test1");
            maps.put("z", "fred@example.com");
            maps.put("w", "path-rootless/test2");
            maps.put("u", "extra");
            try {
                UriBuilder.fromPath("").path("{w}/{v}/{x}/{y}/{z}/{x}").buildFromMap(maps);
                throw new Exception("Test Failed: expected IllegalArgumentException not thrown");
            } catch (IllegalArgumentException ex) {
            }
        }

        // testBuildFromMapTest5
        {
            StringBuffer sb = new StringBuffer();
            boolean pass = true;
            URI uri;
            UriBuilder ub;

            Map<String, String> maps = new HashMap<>();
            maps.put("x", "x%yz");
            maps.put("y", "/path-absolute/test1");
            maps.put("z", "fred@example.com");
            maps.put("w", "path-rootless/test2");

            Map<String, String> maps1 = new HashMap<>();
            maps1.put("x", "x%20yz");
            maps1.put("y", "/path-absolute/test1");
            maps1.put("z", "fred@example.com");
            maps1.put("w", "path-rootless/test2");

            Map<String, String> maps2 = new HashMap<>();
            maps2.put("x", "x%yz");
            maps2.put("y", "/path-absolute/test1");
            maps2.put("z", "fred@example.com");
            maps2.put("w", "path-rootless/test2");
            maps2.put("v", "xyz");

            String expected_path = "path-rootless%2Ftest2/x%25yz/%2Fpath-absolute%2Ftest1/fred@example.com/x%25yz";

            String expected_path_1 = "path-rootless%2Ftest2/x%2520yz/%2Fpath-absolute%2Ftest1/fred@example.com/x%2520yz";

            String expected_path_2 = "path-rootless%2Ftest2/x%25yz/%2Fpath-absolute%2Ftest1/fred@example.com/x%25yz";

            try {
                ub = UriBuilder.fromPath("").path("{w}/{x}/{y}/{z}/{x}");

                uri = ub.buildFromMap(maps);

                if (uri.getRawPath().compareToIgnoreCase(expected_path) != 0) {
                    pass = false;
                    sb.append("Test failed for expected path: " + expected_path +
                            " Got " + uri.getRawPath() + " instead" + "\n");
                } else {
                    sb.append("Got expected path: " + uri.getRawPath() + "\n");
                }

                uri = ub.buildFromMap(maps1);

                if (uri.getRawPath().compareToIgnoreCase(expected_path_1) != 0) {
                    pass = false;
                    sb.append("Test failed for expected path: ").append(expected_path_1)
                            .append(" Got ").append(uri.getRawPath()).append(" instead")
                            .append("\n");
                } else {
                    sb.append("Got expected path: ").append(uri.getRawPath()).append("\n");
                }

                uri = ub.buildFromMap(maps2);

                if (uri.getRawPath().compareToIgnoreCase(expected_path_2) != 0) {
                    pass = false;
                    sb.append("Test failed for expected path: " + expected_path_2 +
                            " Got " + uri.getRawPath() + " instead" + "\n");
                } else {
                    sb.append("Got expected path: " + uri.getRawPath() + "\n");
                }
            } catch (Exception ex) {
                pass = false;
                sb.append("Unexpected exception thrown: " + ex.getMessage() +
                        "\n");
            }

            if (!pass) {
                throw new Exception("At least one assertion failed: " + sb.toString());
            }
        }

        // testFromEncodedTest1
        {
            StringBuffer sb = new StringBuffer();
            boolean pass = true;
            String expected_value_1 = "http://localhost:8080/a/%25/=/%25G0/%25/=";
            String expected_value_2 = "http://localhost:8080/xy/%20/%25/xy";
            URI uri = null;

            uri = UriBuilder.fromPath("http://localhost:8080").path("/{v}/{w}/{x}/{y}/{z}/{x}").buildFromEncoded("a", "%25",
                    "=", "%G0", "%", "23");

            if (uri.toString().compareToIgnoreCase(expected_value_1) != 0) {
                pass = false;
                sb.append("Incorrec URI returned: " + uri.toString() +
                        ", expecting " + expected_value_1 + "\n");
            } else {
                sb.append("Got expected return: " + expected_value_1 + "\n");
            }

            uri = UriBuilder.fromPath("http://localhost:8080").path("/{x}/{y}/{z}/{x}").buildFromEncoded("xy", " ", "%");

            if (uri.toString().compareToIgnoreCase(expected_value_2) != 0) {
                pass = false;
                sb.append("Incorrec URI returned: " + uri.toString() +
                        ", expecting " + expected_value_2 + "\n");
            } else {
                sb.append("Got expected return: " + expected_value_2 + "\n");
            }

            if (!pass) {
                throw new Exception("At least one assertion failed: " + sb.toString());
            }
        }

        // testQueryParamTest1
        {
            String name = null;

            try {
                UriBuilder.fromPath("http://localhost:8080").queryParam(name, "x",
                        "y");
                throw new Exception("Expected IllegalArgumentException Not thrown");
            } catch (IllegalArgumentException ilex) {
            }
        }

        // QueryParamTest5
        {
            Boolean pass = true;
            String name = "name";
            StringBuffer sb = new StringBuffer();
            String expected_value = "http://localhost:8080?name=x%3D&name=y%3F&name=x+y&name=%26";
            URI uri;

            try {
                uri = UriBuilder.fromPath("http://localhost:8080").queryParam(name,
                        "x=", "y?", "x y", "&").build();
                if (uri.toString().compareToIgnoreCase(expected_value) != 0) {
                    pass = false;
                    sb.append("Incorrect URI returned: " + uri.toString() +
                            ", expecting " + expected_value + "\n");
                } else {
                    sb.append("Got expected return: " + expected_value + "\n");
                }
            } catch (Exception ex) {
                pass = false;
                sb.append("Unexpected Exception thrown" + ex.getMessage());
            }

            if (!pass) {
                throw new Exception("At least one assertion failed: " + sb.toString());
            }
        }

        // testReplaceQueryTest3
        {
            Boolean pass = true;
            String name = "name";
            StringBuffer sb = new StringBuffer();
            String expected_value = "http://localhost:8080?name1=x&name2=%20&name3=x+y&name4=23&name5=x%20y";
            URI uri;

            uri = UriBuilder.fromPath("http://localhost:8080").queryParam(name,
                    "x=", "y?", "x y", "&").replaceQuery("name1=x&name2=%20&name3=x+y&name4=23&name5=x y").build();
            if (uri.toString().compareToIgnoreCase(expected_value) != 0) {
                pass = false;
                sb.append("Incorrec URI returned: " + uri.toString() +
                        ", expecting " + expected_value + "\n");
            } else {
                sb.append("Got expected return: " + expected_value + "\n");
            }
            if (!pass) {
                throw new Exception("At least one assertion failed: " + sb.toString());
            }
        }

        // testReplaceQueryParamTest2
        {
            Boolean pass = true;
            String name = "name";
            StringBuffer sb = new StringBuffer();
            String expected_value = "http://localhost:8080";
            URI uri;

            uri = UriBuilder.fromPath("http://localhost:8080").queryParam(name,
                    "x=", "y?", "x y", "&").replaceQueryParam(name, (Object[]) null).build();
            if (uri.toString().compareToIgnoreCase(expected_value) != 0) {
                pass = false;
                sb.append("Incorrec URI returned: " + uri.toString() +
                        ", expecting " + expected_value + "\n");
            } else {
                sb.append("Got expected return: " + expected_value + "\n");
            }

            if (!pass) {
                throw new Exception("At least one assertion failed: " + sb.toString());
            }
        }

        // testPathEncoding
        {
            UriBuilder builder = UriBuilder.fromUri("http://{host}");
            builder.path("{d}");

            URI uri = builder.build("A/B", "C/D");
            Assert.assertEquals(ERROR_MSG, "http://A%2FB/C%2FD", uri.toString());

            uri = builder.buildFromEncoded("A/B", "C/D");
            Assert.assertEquals(ERROR_MSG, "http://A/B/C/D", uri.toString());
            Object[] params = { "A/B", "C/D" };
            uri = builder.build(params, false);
            Assert.assertEquals(ERROR_MSG, "http://A/B/C/D", uri.toString());

            HashMap<String, Object> map = new HashMap<>();
            map.put("host", "A/B");
            map.put("d", "C/D");

            uri = builder.buildFromMap(map);
            Assert.assertEquals(ERROR_MSG, "http://A%2FB/C%2FD", uri.toString());
            uri = builder.buildFromEncodedMap(map);
            Assert.assertEquals(ERROR_MSG, "http://A/B/C/D", uri.toString());
            uri = builder.buildFromMap(map, false);
            Assert.assertEquals(ERROR_MSG, "http://A/B/C/D", uri.toString());

        }

        // testRelativize
        {
            URI from = URI.create("a/b/c");
            URI to = URI.create("a/b/c/d/e");
            URI relativized = UriBuilderImpl.relativize(from, to);
            Assert.assertEquals(ERROR_MSG, relativized.toString(), "d/e");

            from = URI.create("a/b/c");
            to = URI.create("d/e");
            relativized = UriBuilderImpl.relativize(from, to);
            Assert.assertEquals(ERROR_MSG, relativized.toString(), "../../../d/e");

            from = URI.create("a/b/c");
            to = URI.create("a/b/c");
            relativized = UriBuilderImpl.relativize(from, to);
            Assert.assertEquals(ERROR_MSG, relativized.toString(), "");

            from = URI.create("a");
            to = URI.create("d/e");
            relativized = UriBuilderImpl.relativize(from, to);
            Assert.assertEquals(ERROR_MSG, relativized.toString(), "../d/e");
        }

        // testPercentage
        {
            UriBuilder path = UriBuilder.fromUri(URL).path("{path}");
            String template = path.resolveTemplate("path", ENCODED, false).toTemplate();
            logger.info(template);
        }

        // testWithSlashTrue
        {
            Object[] s = { "path-rootless/test2", new StringBuilder("x%yz"),
                    "/path-absolute/%25test1", "fred@example.com" };
            URI uri = UriBuilder.fromPath("").path("{v}/{w}/{x}/{y}/{w}")
                    .build(new Object[] { s[0], s[1], s[2], s[3], s[1] }, true);
            logger.info(uri.getRawPath());
            logger.info(ENCODED_EXPECTED_PATH);
            Assert.assertEquals(ERROR_MSG, uri.getRawPath(), ENCODED_EXPECTED_PATH);
        }

        // testNull
        {
            String uri = null;

            try {
                UriBuilder.fromUri(uri);
                Assert.fail("expected IllegalArgumentException");
            } catch (IllegalArgumentException ilex) {
            }

        }

        // testNullMatrixParam
        {
            try {
                UriBuilder.fromPath("http://localhost:8080").matrixParam(null, "x", "y");
                Assert.fail(ERROR_MSG);
            } catch (IllegalArgumentException e) {

            }
            try {
                UriBuilder.fromPath("http://localhost:8080").matrixParam("name", (Object) null);
                Assert.fail(ERROR_MSG);
            } catch (IllegalArgumentException e) {

            }

        }

        // testReplaceMatrixParam2
        {
            String name = "name1";
            String expected = "http://localhost:8080;name=x=;name=y%3F;name=x%20y;name=&;name1=x;name1=y;name1=y%20x;name1=x%25y;name1=%20";

            URI uri = UriBuilder
                    .fromPath(
                            "http://localhost:8080;name=x=;name=y?;name=x y;name=&")
                    .replaceMatrixParam(name, "x", "y", "y x", "x%y", "%20")
                    .build();
            Assert.assertEquals(ERROR_MSG, uri.toString(), expected);

        }

        // testReplaceMatrixParam3
        {
            String name = "name";
            String expected = "http://localhost:8080;name=x;name=y;name=y%20x;name=x%25y;name=%20";

            URI uri = UriBuilder
                    .fromPath(
                            "http://localhost:8080;name=x=;name=y?;name=x y;name=&")
                    .replaceMatrixParam(name, "x", "y", "y x", "x%y", "%20")
                    .build();
            Assert.assertEquals(ERROR_MSG, uri.toString(), expected);

        }

        // testReplaceMatrixParam4
        {
            String name = "name";
            String expected1 = "http://localhost:8080;";

            URI uri = UriBuilder.fromPath("http://localhost:8080")
                    .matrixParam(name, "x=", "y?", "x y", "&")
                    .replaceMatrix(null).build();
            Assert.assertEquals(ERROR_MSG, uri.toString(), expected1);

        }

        // testReplaceMatrixParam5
        {
            String expected = "http://localhost:8080;name=x;name=y;name=y%20x;name=x%25y;name=%20";
            String value = "name=x;name=y;name=y x;name=x%y;name= ";

            URI uri = UriBuilder
                    .fromPath(
                            "http://localhost:8080;name=x=;name=y?;name=x y;name=&")
                    .replaceMatrix(value).build();
            Assert.assertEquals(ERROR_MSG, uri.toString(), expected);

        }

        // testReplaceMatrixParam6
        {
            String expected = "http://localhost:8080;name1=x;name1=y;name1=y%20x;name1=x%25y;name1=%20";
            String value = "name1=x;name1=y;name1=y x;name1=x%y;name1= ";

            URI uri = UriBuilder
                    .fromPath(
                            "http://localhost:8080;name=x=;name=y?;name=x y;name=&")
                    .replaceMatrix(value).build();
            Assert.assertEquals(ERROR_MSG, uri.toString(), expected);

        }

        // testUriReplace
        {
            String orig = "foo://example.com:8042/over/there?name=ferret#nose";
            String expected = "http://example.com:8042/over/there?name=ferret#nose";
            URI replacement = new URI("http",
                    "//example.com:8042/over/there?name=ferret", null);

            URI uri = UriBuilder.fromUri(new URI(orig))
                    .uri(replacement.toASCIIString()).build();
            Assert.assertEquals(ERROR_MSG, uri.toString(), expected);

        }

        // testUriReplace2
        {
            String orig = "tel:+1-816-555-1212";
            String expected = "tel:+1-816-555-1212";
            URI replacement = new URI("tel", "+1-816-555-1212", null);

            UriBuilder uriBuilder = UriBuilder.fromUri(new URI(orig));
            URI uri = uriBuilder
                    .uri(replacement.toASCIIString()).build();
            Assert.assertEquals(ERROR_MSG, uri.toString(), expected);

        }

        // testUriReplace3
        {
            String orig = "news:comp.lang.java";
            String expected = "http://comp.lang.java";
            URI replacement = new URI("http", "//comp.lang.java", null);

            UriBuilder uriBuilder = UriBuilder.fromUri(new URI(orig));
            URI uri = uriBuilder
                    .uri(replacement.toASCIIString()).build();
            Assert.assertEquals(ERROR_MSG, uri.toString(), expected);

        }

        // testParse2
        {
            String opaque = "mailto:bill@jboss.org";
            Matcher matcher = UriBuilderImpl.opaqueUri.matcher(opaque);
            if (matcher.matches()) {
                logger.info(matcher.group(1));
                logger.info(matcher.group(2));
            }

            String hierarchical = "http://foo.com";
            matcher = UriBuilderImpl.opaqueUri.matcher(hierarchical);
            if (matcher.matches()) {
                Assert.fail(ERROR_MSG);
            }
        }

        // testColon
        {
            UriBuilder builder = UriBuilder.fromUri("http://foo.com/runtime/org.jbpm:HR:1.0/process/hiring/start");
            builder.build();
        }

        // RESTEASY-1718 checks
        {
            Assert.assertEquals("http://foo", UriBuilder.fromUri("http://foo").build().toString());
            Assert.assertEquals("http://foo:8080", UriBuilder.fromUri("http://foo:8080").build().toString());
            Assert.assertEquals("http://[::1]", UriBuilder.fromUri("http://[::1]").build().toString());
            Assert.assertEquals("http://[::1]:8080", UriBuilder.fromUri("http://[::1]:8080").build().toString());

            Assert.assertEquals("http://[0:0:0:0:0:0:0:1]", UriBuilder.fromUri("http://[0:0:0:0:0:0:0:1]").build().toString());
            Assert.assertEquals("http://[0:0:0:0:0:0:0:1]:8080",
                    UriBuilder.fromUri("http://[0:0:0:0:0:0:0:1]:8080").build().toString());
            Assert.assertEquals("http://foo", UriBuilder.fromUri("http://{host}").build("foo").toString());
            Assert.assertEquals("http://foo:8080", UriBuilder.fromUri("http://{host}:8080").build("foo").toString());
        }

    }

    @Test
    public void additionalCheckForIPv6() throws Exception {

        Assert.assertEquals("http://[0:0:0:0:0:0:0:1]", UriBuilder.fromUri("http://[0:0:0:0:0:0:0:1]").build().toString());
        Assert.assertEquals("http://[::1]", UriBuilder.fromUri("http://[::1]").build().toString());

        // URI substitues square brackets with their escaped representation
        Assert.assertEquals("http://%5B0:0:0:0:0:0:0:1%5D",
                UriBuilder.fromUri("http://{host}").build("[0:0:0:0:0:0:0:1]").toString());
        Assert.assertEquals("http://%5B0:0:0:0:0:0:0:1%5D:8080",
                UriBuilder.fromUri("http://{host}:8080").build("[0:0:0:0:0:0:0:1]").toString());

        // inspiration from https://stackoverflow.com/a/17871737
        Assert.assertEquals("http://[1:2:3:4:5:6:7:8]", UriBuilder.fromUri("http://[1:2:3:4:5:6:7:8]").build().toString());
        Assert.assertEquals("http://[1::]", UriBuilder.fromUri("http://[1::]").build().toString());
        Assert.assertEquals("http://[1:2:3:4:5:6:7::]", UriBuilder.fromUri("http://[1:2:3:4:5:6:7::]").build().toString());
        Assert.assertEquals("http://[1::8]", UriBuilder.fromUri("http://[1::8]").build().toString());
        Assert.assertEquals("http://[1:2:3:4:5:6::8]", UriBuilder.fromUri("http://[1:2:3:4:5:6::8]").build().toString());
        Assert.assertEquals("http://[1::7:8]", UriBuilder.fromUri("http://[1::7:8]").build().toString());
        Assert.assertEquals("http://[1:2:3:4:5::7:8]", UriBuilder.fromUri("http://[1:2:3:4:5::7:8]").build().toString());
        Assert.assertEquals("http://[1:2:3:4:5::8]", UriBuilder.fromUri("http://[1:2:3:4:5::8]").build().toString());
        Assert.assertEquals("http://[1::6:7:8]", UriBuilder.fromUri("http://[1::6:7:8]").build().toString());
        Assert.assertEquals("http://[1:2:3:4::6:7:8]", UriBuilder.fromUri("http://[1:2:3:4::6:7:8]").build().toString());
        Assert.assertEquals("http://[1:2:3:4::8]", UriBuilder.fromUri("http://[1:2:3:4::8]").build().toString());
        Assert.assertEquals("http://[1::5:6:7:8]", UriBuilder.fromUri("http://[1::5:6:7:8]").build().toString());
        Assert.assertEquals("http://[1:2:3::5:6:7:8]", UriBuilder.fromUri("http://[1:2:3::5:6:7:8]").build().toString());
        Assert.assertEquals("http://[1:2:3::8]", UriBuilder.fromUri("http://[1:2:3::8]").build().toString());
        Assert.assertEquals("http://[1::4:5:6:7:8]", UriBuilder.fromUri("http://[1::4:5:6:7:8]").build().toString());
        Assert.assertEquals("http://[1:2::4:5:6:7:8]", UriBuilder.fromUri("http://[1:2::4:5:6:7:8]").build().toString());
        Assert.assertEquals("http://[1:2::8]", UriBuilder.fromUri("http://[1:2::8]").build().toString());
        Assert.assertEquals("http://[1::3:4:5:6:7:8]", UriBuilder.fromUri("http://[1::3:4:5:6:7:8]").build().toString());
        Assert.assertEquals("http://[1::8]", UriBuilder.fromUri("http://[1::8]").build().toString());
        Assert.assertEquals("http://[::2:3:4:5:6:7:8]", UriBuilder.fromUri("http://[::2:3:4:5:6:7:8]").build().toString());
        Assert.assertEquals("http://[::3:4:5:6:7:8]", UriBuilder.fromUri("http://[::3:4:5:6:7:8]").build().toString());
        Assert.assertEquals("http://[::8]", UriBuilder.fromUri("http://[::8]").build().toString());
        Assert.assertEquals("http://[::]", UriBuilder.fromUri("http://[::]").build().toString());

        // link-local format
        Assert.assertEquals("http://[fe80::7:8%eth0]", UriBuilder.fromUri("http://[fe80::7:8%eth0]").build().toString());
        Assert.assertEquals("http://[fe80::7:8%1]", UriBuilder.fromUri("http://[fe80::7:8%1]").build().toString());
        Assert.assertEquals("http://[fe80::7:8%eth0]:8080",
                UriBuilder.fromUri("http://[fe80::7:8%eth0]:8080").build().toString());
        Assert.assertEquals("http://[fe80::7:8%1]:80", UriBuilder.fromUri("http://[fe80::7:8%1]:80").build().toString());

        Assert.assertEquals("http://[::255.255.255.255]", UriBuilder.fromUri("http://[::255.255.255.255]").build().toString());
        Assert.assertEquals("http://[::ffff:255.255.255.255]",
                UriBuilder.fromUri("http://[::ffff:255.255.255.255]").build().toString());
        Assert.assertEquals("http://[::ffff:0:255.255.255.255]",
                UriBuilder.fromUri("http://[::ffff:0:255.255.255.255]").build().toString());

        Assert.assertEquals("http://[2001:db8:3:4::192.0.2.33]",
                UriBuilder.fromUri("http://[2001:db8:3:4::192.0.2.33]").build().toString());
        Assert.assertEquals("http://[64:ff9b::192.0.2.33]",
                UriBuilder.fromUri("http://[64:ff9b::192.0.2.33]").build().toString());
    }

    public void printParse(String uri) {
        logger.info("--- " + uri);
        Matcher match = uriPattern.matcher(uri);
        if (!match.matches()) {
            throw new IllegalStateException("no match found");
        }
        for (int i = 1; i < match.groupCount() + 1; i++) {
            logger.info("group[" + i + "] = '" + match.group(i) + "'");
        }

    }

    @Path("resource")
    public static class UriBuilderResource {
        @Path("method")
        @GET
        public String get() {
            return "";
        }

        @Path("locator")
        public Object locator() {
            return null;
        }
    }

    private static class Assert {

        public static void assertTrue(String message, boolean condition) {
            Assertions.assertTrue(condition, message);
        }

        public static void assertEquals(Object expected,
                Object actual) {
            Assertions.assertEquals(expected, actual);
        }

        public static void assertEquals(String message, Object expected,
                Object actual) {
            Assertions.assertEquals(expected, actual, message);
        }

        public static void fail(String message) {
            Assertions.fail(message);
        }
    }
}
