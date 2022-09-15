package org.jboss.resteasy.reactive.server.core.request;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.MediaType;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author Pascal S. de Kloe
 */
public class AcceptHeaders {

    /**
     * Gets the strings from a comma-separated list.
     * All "*" entries are replaced with {@code null} keys.
     *
     * @param header the header value.
     * @return the listed items in order of appearance or {@code null} if the header didn't contain any entries.
     */
    public static Map<String, QualityValue> getStringQualityValues(String header) {
        if (header == null) {
            return null;
        }
        header = header.trim();
        if (header.length() == 0) {
            return null;
        }
        Map<String, QualityValue> result = new LinkedHashMap<String, QualityValue>();

        int offset = 0;
        while (true) {
            int endIndex = header.indexOf(',', offset);
            String content;
            if (endIndex < 0) {
                content = header.substring(offset);
            } else {
                content = header.substring(offset, endIndex);
            }

            QualityValue qualityValue = QualityValue.DEFAULT;
            int qualityIndex = content.indexOf(';');
            if (qualityIndex >= 0) {
                String parameter = content.substring(qualityIndex + 1);
                content = content.substring(0, qualityIndex);

                int equalsIndex = parameter.indexOf('=');
                if (equalsIndex < 0) {
                    throw new BadRequestException("Malformed parameter: " + parameter);
                }
                String name = parameter.substring(0, equalsIndex).trim();
                if (!"q".equals(name)) {
                    throw new BadRequestException("Unsupported parameter: " + parameter);
                }
                String value = parameter.substring(equalsIndex + 1).trim();
                qualityValue = QualityValue.valueOf(value);
            }

            content = content.trim();
            if (content.length() == 0) {
                throw new BadRequestException("Empty Field in header: " + header);
            }
            if (content.equals("*")) {
                result.put(null, qualityValue);
            } else {
                result.put(content, qualityValue);
            }

            if (endIndex < 0) {
                break;
            }
            offset = endIndex + 1;
        }

        return result;
    }

    /**
     * Gets the locales from a comma-separated list.
     * Any "*" entries are replaced with {@code null} keys.
     *
     * @param header the header value.
     * @return the listed items in order of appearance or {@code null} if the header didn't contain any entries.
     */
    public static Map<Locale, QualityValue> getLocaleQualityValues(String header) {
        Map<String, QualityValue> stringResult = getStringQualityValues(header);
        if (stringResult == null)
            return null;
        Map<Locale, QualityValue> result = new LinkedHashMap<Locale, QualityValue>(stringResult.size() * 2);

        for (Entry<String, QualityValue> entry : stringResult.entrySet()) {
            QualityValue quality = entry.getValue();
            Locale locale = null;
            String value = entry.getKey();
            if (value != null) {
                int length = value.length();
                if (length == 2) {
                    locale = new Locale(value);
                } else if (length == 5 && value.charAt(2) == '-') {
                    String language = value.substring(0, 2);
                    String country = value.substring(3, 5);
                    locale = new Locale(language, country);
                } else {
                    //LogMessages.LOGGER.ignoringUnsupportedLocale(value);
                    continue;
                }
            }
            result.put(locale, quality);
        }

        //LogMessages.LOGGER.debug(result.toString());
        return result;
    }

    /**
     * Gets the media types from a comma-separated list.
     *
     * @param header the header value.
     * @return the listed items in order of appearance or {@code null} if the header didn't contain any entries.
     */
    public static Map<MediaType, QualityValue> getMediaTypeQualityValues(String header) {
        if (header == null)
            return null;
        header = header.trim();
        if (header.length() == 0)
            return null;
        Map<MediaType, QualityValue> result = new LinkedHashMap<MediaType, QualityValue>();

        int offset = 0;
        while (offset >= 0) {
            int slashIndex = header.indexOf('/', offset);
            if (slashIndex < 0)
                throw new BadRequestException("Malformed media type: " + header);
            String type = header.substring(offset, slashIndex);
            String subtype;
            Map<String, String> parameters = null;
            QualityValue qualityValue = QualityValue.DEFAULT;

            offset = slashIndex + 1;
            int parameterStartIndex = header.indexOf(';', offset);
            int itemEndIndex = header.indexOf(',', offset);
            if (parameterStartIndex == itemEndIndex) {
                assert itemEndIndex == -1;
                subtype = header.substring(offset);
                offset = -1;
            } else if (itemEndIndex < 0 || (parameterStartIndex >= 0 && parameterStartIndex < itemEndIndex)) {
                subtype = header.substring(offset, parameterStartIndex);
                offset = parameterStartIndex + 1;
                parameters = new LinkedHashMap<String, String>();
                offset = parseParameters(parameters, header, offset);
                qualityValue = evaluateAcceptParameters(parameters);
            } else {
                subtype = header.substring(offset, itemEndIndex);
                offset = itemEndIndex + 1;
            }
            result.put(new MediaType(type.trim(), subtype.trim(), parameters), qualityValue);
        }

        //LogMessages.LOGGER.debug(result.toString());
        return result;
    }

    private static int parseParameters(Map<String, String> parameters, String header, int offset) {
        while (true) {
            int equalsIndex = header.indexOf('=', offset);
            if (equalsIndex < 0)
                throw new BadRequestException("Malformed parameters: " + header);
            String name = header.substring(offset, equalsIndex).trim();
            offset = equalsIndex + 1;
            if (header.charAt(offset) == '"') {
                int end = offset;
                ++offset;
                do {
                    end = header.indexOf('"', ++end);
                    if (end < 0)
                        throw new BadRequestException("Unclosed quotes:" + header);
                } while (header.charAt(end - 1) == '\\');
                String value = header.substring(offset, end);
                parameters.put(name, value);
                offset = end + 1;

                int parameterEndIndex = header.indexOf(';', offset);
                int itemEndIndex = header.indexOf(',', offset);
                if (parameterEndIndex == itemEndIndex) {
                    assert itemEndIndex == -1;
                    if (header.substring(offset).trim().length() != 0)
                        throw new BadRequestException("Extra characters after quoted string:" + header);
                    return -1;
                } else if (parameterEndIndex < 0 || (itemEndIndex >= 0 && itemEndIndex < parameterEndIndex)) {
                    if (header.substring(offset, itemEndIndex).trim().length() != 0)
                        throw new BadRequestException("Extra characters after quoted string:" + header);
                    return itemEndIndex + 1;
                } else {
                    if (header.substring(offset, parameterEndIndex).trim().length() != 0)
                        throw new BadRequestException("Extra characters after quoted string:" + header);
                    offset = parameterEndIndex + 1;
                }
            } else {
                int parameterEndIndex = header.indexOf(';', offset);
                int itemEndIndex = header.indexOf(',', offset);
                if (parameterEndIndex == itemEndIndex) {
                    assert itemEndIndex == -1;
                    String value = header.substring(offset).trim();
                    parameters.put(name, value);
                    return -1;
                } else if (parameterEndIndex < 0 || (itemEndIndex >= 0 && itemEndIndex < parameterEndIndex)) {
                    String value = header.substring(offset, itemEndIndex).trim();
                    parameters.put(name, value);
                    return itemEndIndex + 1;
                } else {
                    String value = header.substring(offset, parameterEndIndex).trim();
                    parameters.put(name, value);
                    offset = parameterEndIndex + 1;
                }
            }
        }
    }

    /**
     * Evaluates and removes the accept parameters.
     *
     * <pre>
     * accept-params  = ";" "q" "=" qvalue *( accept-extension )
     * accept-extension = ";" token [ "=" ( token | quoted-string ) ]
     * </pre>
     *
     * @param parameters all parameters in order of appearance.
     * @return the qvalue.
     * @see "accept-params
     */
    private static QualityValue evaluateAcceptParameters(Map<String, String> parameters) {
        Iterator<String> i = parameters.keySet().iterator();
        while (i.hasNext()) {
            String name = i.next();
            if ("q".equals(name)) {
                if (i.hasNext()) {
                    //LogMessages.LOGGER.acceptExtensionsNotSupported();
                    i.remove();
                    do {
                        i.next();
                        i.remove();
                    } while (i.hasNext());
                    return QualityValue.NOT_ACCEPTABLE;
                } else {
                    String value = parameters.get(name);
                    i.remove();
                    return QualityValue.valueOf(value);
                }
            }
        }
        return QualityValue.DEFAULT;
    }

}
