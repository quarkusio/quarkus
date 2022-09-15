package org.jboss.resteasy.reactive.common.headers;

import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.RuntimeDelegate;
import java.util.List;
import java.util.Map;
import org.jboss.resteasy.reactive.common.jaxrs.LinkBuilderImpl;
import org.jboss.resteasy.reactive.common.util.QuarkusMultivaluedHashMap;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class LinkDelegate implements RuntimeDelegate.HeaderDelegate<Link> {
    public static final LinkDelegate INSTANCE = new LinkDelegate();

    private static class Parser {
        private int curr;
        private String value;
        private Link.Builder builder;

        Parser(final String value) {
            this.value = value;
            builder = new LinkBuilderImpl();
        }

        public Link getLink() {
            return builder.build();
        }

        public void parse() {
            String href = null;
            MultivaluedMap<String, String> attributes = new QuarkusMultivaluedHashMap<String, String>();
            while (curr < value.length()) {

                char c = value.charAt(curr);
                if (c == '<') {
                    if (href != null)
                        throw new IllegalArgumentException("Too many links");
                    href = parseLink();
                } else if (c == ';' || c == ' ') {
                    curr++;
                    continue;
                } else {
                    parseAttribute(attributes);
                }
            }
            populateLink(href, attributes);

        }

        protected void populateLink(String href, MultivaluedMap<String, String> attributes) {
            builder.uri(href);
            for (String name : attributes.keySet()) {
                List<String> values = attributes.get(name);
                if (name.equals("rel")) {
                    for (String val : values)
                        builder.rel(val);
                } else if (name.equals("title")) {
                    for (String val : values)
                        builder.title(val);

                } else if (name.equals("type")) {
                    for (String val : values)
                        builder.type(val);

                } else {
                    for (String val : values)
                        builder.param(name, val);

                }
            }
        }

        public String parseLink() {
            int end = value.indexOf('>', curr);
            if (end == -1)
                throw new IllegalArgumentException("No end to link");
            String href = value.substring(curr + 1, end);
            curr = end + 1;
            return href;
        }

        public void parseAttribute(MultivaluedMap<String, String> attributes) {
            int end = value.indexOf('=', curr);
            if (end == -1 || end + 1 >= value.length())
                throw new IllegalArgumentException("No end to parameter");
            String name = value.substring(curr, end);
            name = name.trim();
            curr = end + 1;
            String val = null;
            if (curr >= value.length()) {
                val = "";
            } else {

                if (value.charAt(curr) == '"') {
                    if (curr + 1 >= value.length())
                        throw new IllegalArgumentException("No end to parameter");
                    curr++;
                    end = value.indexOf('"', curr);
                    if (end == -1)
                        throw new IllegalArgumentException("No end to parameter");
                    val = value.substring(curr, end);
                    curr = end + 1;
                } else {
                    StringBuffer buf = new StringBuffer();
                    while (curr < value.length()) {
                        char c = value.charAt(curr);
                        if (c == ',' || c == ';')
                            break;
                        buf.append(value.charAt(curr));
                        curr++;
                    }
                    val = buf.toString();
                }
            }
            attributes.add(name, val);

        }

    }

    @Override
    public Link fromString(String value) throws IllegalArgumentException {
        if (value == null)
            throw new IllegalArgumentException("value was null");
        Parser parser = new Parser(value);
        parser.parse();
        return parser.getLink();
    }

    @Override
    public String toString(Link value) throws IllegalArgumentException {
        if (value == null)
            throw new IllegalArgumentException("value was null");
        StringBuffer buf = new StringBuffer("<");
        buf.append(value.getUri().toString()).append(">");

        for (Map.Entry<String, String> entry : value.getParams().entrySet()) {
            buf.append("; ").append(entry.getKey()).append("=\"").append(entry.getValue()).append("\"");
        }

        return buf.toString();
    }
}
