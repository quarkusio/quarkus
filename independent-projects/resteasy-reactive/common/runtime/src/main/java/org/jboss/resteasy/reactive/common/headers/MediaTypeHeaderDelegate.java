package org.jboss.resteasy.reactive.common.headers;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.RuntimeDelegate;
import org.jboss.resteasy.reactive.common.util.HeaderParameterParser;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 */
public class MediaTypeHeaderDelegate implements RuntimeDelegate.HeaderDelegate<MediaType> {
    public static final MediaTypeHeaderDelegate INSTANCE = new MediaTypeHeaderDelegate();
    private static final int MAX_MT_CACHE_SIZE = 200;
    private static final char[] quotedChars = "()<>@,;:\\\"/[]?= \t\r\n".toCharArray();
    private static final Map<String, MediaType> map = new ConcurrentHashMap<>();
    private static final Map<MediaType, String> reverseMap = new ConcurrentHashMap<>();

    protected static boolean isValid(String str) {
        if (str == null || str.length() == 0)
            return false;
        for (int i = 0; i < str.length(); i++) {
            switch (str.charAt(i)) {
                case '/':
                case '\\':
                case '?':
                case ':':
                case '<':
                case '>':
                case ';':
                case '(':
                case ')':
                case '@':
                case ',':
                case '[':
                case ']':
                case '=':
                case '\n':
                    return false;
                default:
                    break;
            }
        }
        return true;
    }

    public static MediaType parse(String type) {
        MediaType result = map.get(type);
        if (result == null) {
            result = internalParse(type);
            final int size = map.size();
            if (size >= MAX_MT_CACHE_SIZE) {
                map.clear();
                reverseMap.clear();
            }
            map.put(type, result);
            reverseMap.put(result, internalToString(result));
        }
        return result;
    }

    private static MediaType internalParse(String type) {
        int typeIndex = type.indexOf('/');
        int paramIndex = type.indexOf(';');
        String major = null;
        String subtype = null;
        if (typeIndex < 0) // possible "*"
        {
            major = type;
            if (paramIndex > -1) {
                major = major.substring(0, paramIndex);
            }
            if (!MediaType.MEDIA_TYPE_WILDCARD.equals(major)) {
                throw new IllegalArgumentException("Failed to parse media type " + type);
            }
            subtype = MediaType.MEDIA_TYPE_WILDCARD;
        } else {
            major = type.substring(0, typeIndex);
            if (paramIndex > -1) {
                subtype = type.substring(typeIndex + 1, paramIndex);
            } else {
                subtype = type.substring(typeIndex + 1);
            }
        }
        if (major.length() < 1 || subtype.length() < 1) {
            throw new IllegalArgumentException("Failed to parse media type " + type);
        }
        if (!isValid(major) || !isValid(subtype)) {
            throw new IllegalArgumentException("Failed to parse media type " + type);
        }
        String params = null;
        if (paramIndex > -1)
            params = type.substring(paramIndex + 1);
        if (params != null && !params.equals("")) {
            HashMap<String, String> typeParams = new HashMap<String, String>();

            int start = 0;

            while (start < params.length()) {
                start = HeaderParameterParser.setParam(typeParams, params, start);
            }
            return new MediaType(major, subtype, typeParams);
        } else {
            return new MediaType(major, subtype);
        }
    }

    public static boolean quoted(String str) {
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            for (char q : quotedChars) {
                if (c == q)
                    return true;
            }
        }
        return false;
    }

    public MediaType fromString(String type) throws IllegalArgumentException {
        if (type == null)
            throw new IllegalArgumentException("Media type was null");
        return parse(type);
    }

    public String toString(MediaType o) {
        if (o == null)
            throw new IllegalArgumentException("Param was null");
        MediaType type = o;
        String result = reverseMap.get(type);
        if (result == null) {
            result = internalToString(type);
            final int size = reverseMap.size();
            if (size >= MAX_MT_CACHE_SIZE) {
                reverseMap.clear();
                map.clear();
            }
            reverseMap.put(type, result);
            map.put(result, type);
        }
        return result;
    }

    private static String internalToString(MediaType type) {
        StringBuilder buf = new StringBuilder(type.getType().length() + type.getSubtype().length() + 1);

        buf.append(type.getType().toLowerCase()).append('/').append(type.getSubtype().toLowerCase());
        if (type.getParameters() == null || type.getParameters().size() == 0)
            return buf.toString();
        for (String name : type.getParameters().keySet()) {
            buf.append(';').append(name).append('=');
            String val = type.getParameters().get(name);
            if (quoted(val))
                buf.append('"').append(val).append('"');
            else
                buf.append(val);
        }
        return buf.toString();
    }
}
