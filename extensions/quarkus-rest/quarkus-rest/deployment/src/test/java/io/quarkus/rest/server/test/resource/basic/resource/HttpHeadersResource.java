package io.quarkus.rest.server.test.resource.basic.resource;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.jboss.logging.Logger;

@Path(value = "/HeadersTest")
public class HttpHeadersResource {
    private static Logger logger = Logger.getLogger(HttpHeadersResource.class);

    @GET
    @Path("/headers")
    public String headersGet(@Context HttpHeaders hs) {
        StringBuffer sb = new StringBuffer();
        List<String> myHeaders = Arrays.asList("Accept", "Content-Type");

        try {
            MultivaluedMap<String, String> rqhdrs = hs.getRequestHeaders();
            Set<String> keys = rqhdrs.keySet();
            sb.append("getRequestHeaders= ");
            for (String header : myHeaders) {
                if (keys.contains(header)) {
                    sb.append("Found " + header + ": " +
                            hs.getRequestHeader(header) + "; ");
                }
            }
        } catch (Throwable ex) {
            sb.append("Unexpected exception thrown in getRequestHeaders: " +
                    ex.getMessage());
            StringWriter errors = new StringWriter();
            ex.printStackTrace(new PrintWriter(errors));
            logger.error(errors.toString());
        }
        return sb.toString();
    }

    @GET
    @Path("/acl")
    public String aclGet(@Context HttpHeaders hs) {
        StringBuffer sb = new StringBuffer();
        try {
            sb.append("Accept-Language");

            List<Locale> acl = hs.getAcceptableLanguages();
            sb.append("getLanguage= ");
            for (Locale tmp : acl) {
                sb.append(tmp.toString() + "; ");
            }
        } catch (Throwable ex) {
            sb.append("Unexpected exception thrown in getAcceptableLanguages: " +
                    ex.getMessage());
            StringWriter errors = new StringWriter();
            ex.printStackTrace(new PrintWriter(errors));
            logger.error(errors.toString());
        }
        return sb.toString();
    }

    @GET
    @Path("/amt")
    public String amtGet(@Context HttpHeaders hs) {
        StringBuffer sb = new StringBuffer();
        try {
            sb.append("getAcceptableMediaTypes");
            List<MediaType> acmts = hs.getAcceptableMediaTypes();

            for (MediaType mt : acmts) {
                sb.append(mt.getType());
                sb.append("/");
                sb.append(mt.getSubtype());
            }
        } catch (Throwable ex) {
            sb.append("Unexpected exception thrown: " + ex.getMessage());
            StringWriter errors = new StringWriter();
            ex.printStackTrace(new PrintWriter(errors));
            logger.error(errors.toString());
        }
        return sb.toString();
    }

    @GET
    @Path("/mt")
    public String mtGet(@Context HttpHeaders hs) {
        StringBuffer sb = new StringBuffer();

        try {
            sb.append("getMediaType");
            MediaType mt = hs.getMediaType();
            if (mt != null) {
                sb.append(mt.getType());
                sb.append("/");
                sb.append(mt.getSubtype());
                sb.append(" ");

                Map<String, String> pmap = mt.getParameters();

                sb.append("MediaType size=" + pmap.size());

                for (Map.Entry<String, String> entry : pmap.entrySet()) {
                    sb.append("Key " + entry.getKey() + "; Value " + entry.getValue());
                }

                sb.append(mt.toString());

                sb.append("MediaType= " + mt.toString() + "; ");
            } else {
                sb.append("MediaType= null; ");
            }
        } catch (Throwable ex) {
            sb.append("Unexpected exception thrown: " + ex.getMessage());
            StringWriter errors = new StringWriter();
            ex.printStackTrace(new PrintWriter(errors));
            logger.error(errors.toString());
        }
        return sb.toString();
    }

    @GET
    @Path("/cookie")
    public String cookieGet(@Context HttpHeaders hs) {
        StringBuffer sb = new StringBuffer();

        try {
            sb.append("getCookies= ");
            Map<String, Cookie> cookies = hs.getCookies();
            sb.append("Cookie Size=" + cookies.size());

            for (Map.Entry<String, Cookie> tmp : cookies.entrySet()) {
                sb.append(tmp.getKey() + ": " + tmp.getValue() + "; ");
                Cookie c = cookies.get("name1");
                sb.append("Cookie Name=" + c.getName());
                sb.append("Cookie Value=" + c.getValue());
                sb.append("Cookie Path=" + c.getPath());
                sb.append("Cookie Domain=" + c.getDomain());
                sb.append("Cookie Version=" + c.getVersion());

            }
        } catch (Throwable ex) {
            sb.append("Unexpected exception thrown: " + ex.getMessage());
            StringWriter errors = new StringWriter();
            ex.printStackTrace(new PrintWriter(errors));
            logger.error(errors.toString());
        }

        return sb.toString();
    }

    @PUT
    public String headersPlainPut(@Context HttpHeaders hs) {
        StringBuffer sb = new StringBuffer();
        sb.append("Content-Language");
        sb.append(hs.getLanguage());
        return sb.toString();
    }
}
