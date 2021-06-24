package org.jboss.resteasy.reactive.common.headers;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.ext.RuntimeDelegate;
import org.jboss.resteasy.reactive.common.util.CookieParser;
import org.jboss.resteasy.reactive.common.util.DateUtil;
import org.jboss.resteasy.reactive.common.util.MediaTypeHelper;
import org.jboss.resteasy.reactive.common.util.WeightedLanguage;

/**
 * These work for MultivaluedMap with String and Object
 */
public class HeaderUtil {

    private static final ClassValue<RuntimeDelegate.HeaderDelegate> HEADER_DELEGATE_CACHE = new ClassValue() {
        @Override
        protected RuntimeDelegate.HeaderDelegate<?> computeValue(Class type) {
            return RuntimeDelegate.getInstance().createHeaderDelegate(type);
        }
    };

    public static String headerToString(Object obj) {
        if (obj instanceof String) {
            return (String) obj;
        } else {
            return HEADER_DELEGATE_CACHE.get(obj.getClass()).toString(obj);
        }
    }

    public static Set<String> getAllowedMethods(MultivaluedMap<String, ? extends Object> headers) {
        List<? extends Object> allowed = headers.get(HttpHeaders.ALLOW);
        if ((allowed == null) || allowed.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> allowedMethods = new HashSet<>();
        for (Object header : allowed) {
            if (header instanceof String) {
                String stringHeader = ((String) header);
                if (stringHeader.indexOf(',') != -1) {
                    for (String str : stringHeader.split(",")) {
                        String trimmed = str.trim();
                        if (!trimmed.isEmpty()) {
                            allowedMethods.add(trimmed.toUpperCase());
                        }
                    }
                } else {
                    allowedMethods.add(stringHeader.trim().toUpperCase());
                }
            } else {
                allowedMethods.add(HeaderUtil.headerToString(header).toUpperCase());
            }
        }
        return allowedMethods;
    }

    public static Date getDate(MultivaluedMap<String, ? extends Object> headers) {
        return firstHeaderToDate(HttpHeaders.DATE, headers);
    }

    public static Date getLastModified(MultivaluedMap<String, ? extends Object> headers) {
        return firstHeaderToDate(HttpHeaders.LAST_MODIFIED, headers);
    }

    private static Date firstHeaderToDate(String date, MultivaluedMap<String, ? extends Object> headers) {
        Object d = headers.getFirst(date);
        if (d == null)
            return null;
        if (d instanceof Date)
            return (Date) d;
        return DateUtil.parseDate(d.toString());
    }

    public static URI getLocation(MultivaluedMap<String, ? extends Object> headers) {
        Object uri = headers.getFirst(HttpHeaders.LOCATION);
        if (uri == null) {
            return null;
        }
        if (uri instanceof URI) {
            return (URI) uri;
        }
        String str = null;
        if (uri instanceof String) {
            str = (String) uri;
        } else {
            str = HeaderUtil.headerToString(uri);
        }
        return URI.create(str);
    }

    public static MediaType getMediaType(MultivaluedMap<String, ? extends Object> headers) {
        Object first = headers.getFirst(HttpHeaders.CONTENT_TYPE);
        if (first instanceof String) {
            String contentType = (String) first;
            return MediaType.valueOf(contentType);
        } else {
            return (MediaType) first;
        }
    }

    public static Locale getLanguage(MultivaluedMap<String, ? extends Object> headers) {
        Object obj = headers.getFirst(HttpHeaders.CONTENT_LANGUAGE);
        if (obj == null) {
            return null;
        }
        if (obj instanceof Locale) {
            return (Locale) obj;
        }
        return Locale.forLanguageTag(HeaderUtil.headerToString(obj));
    }

    public static int getLength(MultivaluedMap<String, ? extends Object> headers) {
        Object obj = headers.getFirst(HttpHeaders.CONTENT_LENGTH);
        if (obj == null) {
            return -1;
        }
        if (obj instanceof Integer) {
            return (Integer) obj;
        }
        return Integer.parseInt(HeaderUtil.headerToString(obj));
    }

    public static Map<String, Cookie> getCookies(MultivaluedMap<String, ? extends Object> headers) {
        List list = headers.get(HttpHeaders.COOKIE);
        if (list == null)
            return Collections.emptyMap();
        Map<String, Cookie> cookies = new HashMap<String, Cookie>();
        for (Object obj : list) {
            if (obj instanceof Cookie) {
                Cookie cookie = (Cookie) obj;
                cookies.put(cookie.getName(), cookie);
            } else {
                String str = headerToString(obj);
                for (Cookie cookie : CookieParser.parseCookies(str)) {
                    cookies.put(cookie.getName(), cookie);
                }
            }
        }
        return cookies;
    }

    public static Map<String, NewCookie> getNewCookies(MultivaluedMap<String, ? extends Object> headers) {
        List<?> list = headers.get(HttpHeaders.SET_COOKIE);
        if ((list == null) || list.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, NewCookie> cookies = new HashMap<>();
        for (Object obj : list) {
            if (obj instanceof NewCookie) {
                NewCookie cookie = (NewCookie) obj;
                cookies.put(cookie.getName(), cookie);
            } else {
                String str = HeaderUtil.headerToString(obj);
                NewCookie cookie = NewCookie.valueOf(str);
                cookies.put(cookie.getName(), cookie);
            }
        }
        return cookies;
    }

    public static EntityTag getEntityTag(MultivaluedMap<String, ? extends Object> headers) {
        Object d = headers.getFirst(HttpHeaders.ETAG);
        if (d == null) {
            return null;
        }
        if (d instanceof EntityTag) {
            return (EntityTag) d;
        }
        return EntityTag.valueOf(HeaderUtil.headerToString(d));
    }

    public static String getHeaderString(MultivaluedMap<String, ? extends Object> headers, String name) {
        List<? extends Object> list = headers.get(name);
        if (list == null) {
            return null;
        }
        if (list.size() == 1) {
            return headerToString(list.get(0));
        }
        StringBuilder sb = new StringBuilder();
        for (Object s : list) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(headerToString(s));
        }
        return sb.toString();
    }

    @SuppressWarnings(value = "unchecked")
    public static void setAllow(MultivaluedMap headers, String[] methods) {
        if (methods == null) {
            headers.remove("Allow");
            return;
        }
        StringBuilder builder = new StringBuilder();
        boolean isFirst = true;
        for (String l : methods) {
            if (isFirst) {
                isFirst = false;
            } else {
                builder.append(", ");
            }
            builder.append(l);
        }
        headers.putSingle("Allow", builder.toString());
    }

    @SuppressWarnings(value = "unchecked")
    public static void setAllow(MultivaluedMap headers, Set<String> methods) {
        if (methods == null) {
            headers.remove("Allow");
            return;
        }
        StringBuilder builder = new StringBuilder();
        boolean isFirst = true;
        for (String l : methods) {
            if (isFirst) {
                isFirst = false;
            } else {
                builder.append(", ");
            }
            builder.append(l);
        }
        headers.putSingle("Allow", builder.toString());
    }

    public static boolean isContentLengthZero(MultivaluedMap httpHeaders) {
        if (httpHeaders == null)
            return false;
        @SuppressWarnings(value = "unchecked")
        String contentLength = (String) httpHeaders.getFirst(HttpHeaders.CONTENT_LENGTH);
        if (contentLength != null) {
            long length = Long.parseLong(contentLength);
            if (length == 0)
                return true;
        }
        return false;
    }

    public static List<MediaType> getAcceptableMediaTypes(MultivaluedMap<String, ? extends Object> headers) {
        List<?> accepts = headers.get(HttpHeaders.ACCEPT);
        if (accepts == null || accepts.isEmpty()) {
            return Collections.singletonList(MediaType.WILDCARD_TYPE);
        }
        List<MediaType> list = new ArrayList<MediaType>();
        for (Object obj : accepts) {
            if (obj instanceof MediaType) {
                list.add((MediaType) obj);
                continue;
            }
            String accept = null;
            if (obj instanceof String) {
                accept = (String) obj;
            } else {
                accept = headerToString(obj);
            }
            if (accept.indexOf(',') != -1) {
                StringTokenizer tokenizer = new StringTokenizer(accept, ",");
                while (tokenizer.hasMoreElements()) {
                    String item = tokenizer.nextToken().trim();
                    list.add(MediaType.valueOf(item));
                }
            } else {
                list.add(MediaType.valueOf(accept.trim()));
            }
        }
        MediaTypeHelper.sortByWeight(list);
        return list;
    }

    public static List<Locale> getAcceptableLanguages(MultivaluedMap<String, ? extends Object> headers) {
        List<?> accepts = headers.get(HttpHeaders.ACCEPT_LANGUAGE);
        if (accepts == null || accepts.isEmpty())
            return Collections.emptyList();
        List<WeightedLanguage> languages = new ArrayList<WeightedLanguage>();
        for (Object obj : accepts) {
            if (obj instanceof Locale) {
                languages.add(new WeightedLanguage((Locale) obj, 1.0F));
                continue;
            }
            String accept = headerToString(obj);
            if (accept.indexOf(',') != -1) {
                StringTokenizer tokenizer = new StringTokenizer(accept, ",");
                while (tokenizer.hasMoreElements()) {
                    String item = tokenizer.nextToken().trim();
                    languages.add(WeightedLanguage.parse(item));
                }
            } else {
                languages.add(WeightedLanguage.parse(accept.trim()));
            }
        }
        Collections.sort(languages);
        List<Locale> list = new ArrayList<Locale>(languages.size());
        for (WeightedLanguage language : languages)
            list.add(language.getLocale());
        return list;
    }

    /**
     * Extracts a quoted value from a header that has a given key. For instance if the header is
     * <p>
     * content-disposition=form-data; name="my field"
     * and the key is name then "my field" will be returned without the quotes.
     *
     *
     * @param header The header
     * @param key The key that identifies the token to extract
     * @return The token, or null if it was not found
     */
    public static String extractQuotedValueFromHeader(final String header, final String key) {

        int keypos = 0;
        int pos = -1;
        boolean whiteSpace = true;
        boolean inQuotes = false;
        for (int i = 0; i < header.length() - 1; ++i) { //-1 because we need room for the = at the end
            //TODO: a more efficient matching algorithm
            char c = header.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    inQuotes = false;
                }
            } else {
                if (key.charAt(keypos) == c && (whiteSpace || keypos > 0)) {
                    keypos++;
                    whiteSpace = false;
                } else if (c == '"') {
                    keypos = 0;
                    inQuotes = true;
                    whiteSpace = false;
                } else {
                    keypos = 0;
                    whiteSpace = c == ' ' || c == ';' || c == '\t';
                }
                if (keypos == key.length()) {
                    if (header.charAt(i + 1) == '=') {
                        pos = i + 2;
                        break;
                    } else {
                        keypos = 0;
                    }
                }
            }

        }
        if (pos == -1) {
            return null;
        }

        int end;
        int start = pos;
        if (header.charAt(start) == '"') {
            start++;
            for (end = start; end < header.length(); ++end) {
                char c = header.charAt(end);
                if (c == '"') {
                    break;
                }
            }
            return header.substring(start, end);

        } else {
            //no quotes
            for (end = start; end < header.length(); ++end) {
                char c = header.charAt(end);
                if (c == ' ' || c == '\t' || c == ';') {
                    break;
                }
            }
            return header.substring(start, end);
        }
    }

    /**
     * Extracts a quoted value from a header that has a given key. For instance if the header is
     * <p>
     * content-disposition=form-data; filename*="utf-8''test.txt"
     * and the key is filename* then "test.txt" will be returned after extracting character set and language
     * (following RFC 2231) and performing URL decoding to the value using the specified encoding
     *
     * @param header The header
     * @param key The key that identifies the token to extract
     * @return The token, or null if it was not found
     */
    public static String extractQuotedValueFromHeaderWithEncoding(final String header, final String key) {
        String value = extractQuotedValueFromHeader(header, key);
        if (value != null) {
            return value;
        }
        value = extractQuotedValueFromHeader(header, key + "*");
        if (value != null) {
            int characterSetDelimiter = value.indexOf('\'');
            int languageDelimiter = value.lastIndexOf('\'', characterSetDelimiter + 1);
            String characterSet = value.substring(0, characterSetDelimiter);
            try {
                String fileNameURLEncoded = value.substring(languageDelimiter + 1);
                return URLDecoder.decode(fileNameURLEncoded, characterSet);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }
}
