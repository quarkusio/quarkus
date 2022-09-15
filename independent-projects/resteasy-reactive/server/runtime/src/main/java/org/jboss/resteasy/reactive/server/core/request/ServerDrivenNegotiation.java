package org.jboss.resteasy.reactive.server.core.request;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Variant;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * {@link Variant} selection.
 *
 * @author Pascal S. de Kloe
 * @see "RFC 2296"
 */
public class ServerDrivenNegotiation {

    private Map<MediaType, QualityValue> requestedMediaTypes = null;
    private Map<String, QualityValue> requestedCharacterSets = null;
    private Map<String, QualityValue> requestedEncodings = null;
    private Map<Locale, QualityValue> requestedLanguages = null;
    private int mediaRadix = 1;

    public ServerDrivenNegotiation() {
    }

    public void setAcceptHeaders(List<String> headerValues) {
        requestedMediaTypes = null;
        if (headerValues == null)
            return;
        Map<MediaType, QualityValue> requested = null;
        for (String headerValue : headerValues) {
            Map<MediaType, QualityValue> mapping = AcceptHeaders.getMediaTypeQualityValues(headerValue);
            if (mapping == null)
                return;
            if (requested == null)
                requested = mapping;
            else
                requested.putAll(mapping);
        }
        requestedMediaTypes = requested;
        for (Iterator<MediaType> it = requested.keySet().iterator(); it.hasNext();) {
            mediaRadix = Math.max(mediaRadix, it.next().getParameters().size());
        }
    }

    public void setAcceptCharsetHeaders(List<String> headerValues) {
        requestedCharacterSets = null;
        if (headerValues == null)
            return;
        Map<String, QualityValue> requested = null;
        for (String headerValue : headerValues) {
            Map<String, QualityValue> mapping = AcceptHeaders.getStringQualityValues(headerValue);
            if (mapping == null)
                return;
            if (requested == null)
                requested = mapping;
            else
                requested.putAll(mapping);
        }
        requestedCharacterSets = requested;
    }

    public void setAcceptEncodingHeaders(List<String> headerValues) {
        requestedEncodings = null;
        if (headerValues == null)
            return;
        Map<String, QualityValue> requested = null;
        for (String headerValue : headerValues) {
            Map<String, QualityValue> mapping = AcceptHeaders.getStringQualityValues(headerValue);
            if (mapping == null)
                return;
            if (requested == null)
                requested = mapping;
            else
                requested.putAll(mapping);
        }
        requestedEncodings = requested;
    }

    public void setAcceptLanguageHeaders(List<String> headerValues) {
        requestedLanguages = null;
        if (headerValues == null)
            return;
        Map<Locale, QualityValue> requested = null;
        for (String headerValue : headerValues) {
            Map<Locale, QualityValue> mapping = AcceptHeaders.getLocaleQualityValues(headerValue);
            if (mapping == null)
                return;
            if (requested == null)
                requested = mapping;
            else
                requested.putAll(mapping);
        }
        requestedLanguages = requested;
    }

    public Variant getBestMatch(List<Variant> available) {
        //      BigDecimal bestQuality = BigDecimal.ZERO;
        VariantQuality bestQuality = null;
        Variant bestOption = null;
        for (Variant option : available) {
            VariantQuality quality = new VariantQuality();
            if (!applyMediaType(option, quality))
                continue;
            if (!applyCharacterSet(option, quality))
                continue;
            if (!applyEncoding(option, quality))
                continue;
            if (!applyLanguage(option, quality))
                continue;

            //         BigDecimal optionQuality = quality.getOverallQuality();
            //         if (isBetterOption(bestQuality, bestOption, optionQuality, option))
            if (isBetterOption(bestQuality, bestOption, quality, option)) {
                //            bestQuality = optionQuality;
                bestQuality = quality;
                bestOption = option;
            }
        }
        return bestOption;
    }

    /**
     * Tests whether {@code option} is preferable over the current {@code bestOption}.
     */
    //   private static boolean isBetterOption(BigDecimal bestQuality, Variant best,
    //                                         BigDecimal optionQuality, Variant option)
    private static boolean isBetterOption(VariantQuality bestQuality, Variant best,
            VariantQuality optionQuality, Variant option) {
        if (best == null)
            return true;

        // Compare overall quality.
        int signum = bestQuality.getOverallQuality().compareTo(optionQuality.getOverallQuality());
        if (signum != 0)
            return signum < 0;

        // Overall quality is the same.
        // Assuming the request has an Accept header, a VariantQuality has a non-null
        // requestMediaType if and only if it the corresponding Variant has a non-null mediaType.
        // If bestQuality and optionQuality both have a non-null requestMediaType, we compare them
        // for specificity.
        MediaType bestRequestMediaType = bestQuality.getRequestMediaType();
        MediaType optionRequestMediaType = optionQuality.getRequestMediaType();
        if (bestRequestMediaType != null && optionRequestMediaType != null) {
            if (bestRequestMediaType.getType().equals(optionRequestMediaType.getType())) {
                if (bestRequestMediaType.getSubtype().equals(optionRequestMediaType.getSubtype())) {
                    int bestCount = bestRequestMediaType.getParameters().size();
                    int optionCount = optionRequestMediaType.getParameters().size();
                    if (optionCount > bestCount) {
                        return true; // more matching parameters
                    } else if (optionCount < bestCount) {
                        return false; // less matching parameters
                    }
                } else if (bestRequestMediaType.getSubtype().equals("*")) {
                    return true;
                } else if (optionRequestMediaType.getSubtype().equals("*")) {
                    return false;
                }
            } else if (bestRequestMediaType.getType().equals("*")) {
                return true;
            } else if (optionRequestMediaType.getType().equals("*")) {
                return false;
            }
        }

        // Compare variant media types for specificity.
        MediaType bestType = best.getMediaType();
        MediaType optionType = option.getMediaType();
        if (bestType != null && optionType != null) {
            if (bestType.getType().equals(optionType.getType())) {
                // Same type
                if (bestType.getSubtype().equals(optionType.getSubtype())) {
                    // Same subtype
                    int bestCount = bestType.getParameters().size();
                    int optionCount = optionType.getParameters().size();
                    if (optionCount > bestCount)
                        return true; // more matching parameters
                    else if (optionCount < bestCount)
                        return false; // less matching parameters
                } else if ("*".equals(bestType.getSubtype())) {
                    return true; // more specific subtype
                } else if ("*".equals(optionType.getSubtype())) {
                    return false; // less specific subtype
                }
            } else if ("*".equals(bestType.getType())) {
                return true; // more specific type
            } else if ("*".equals(optionType.getType())) {
                return false; // less specific type;
            }
        }

        // Finally, compare specificity of the variants.
        return getExplicitness(best) < getExplicitness(option);
    }

    private static int getExplicitness(Variant variant) {
        int explicitness = 0;
        if (variant.getMediaType() != null) {
            ++explicitness;
        }
        if (variant.getEncoding() != null) {
            ++explicitness;
        }
        if (variant.getLanguage() != null) {
            ++explicitness;
        }
        return explicitness;
    }

    private boolean applyMediaType(Variant option, VariantQuality quality) {
        if (requestedMediaTypes == null)
            return true;
        MediaType mediaType = option.getMediaType();
        if (mediaType == null)
            return true;

        String type = mediaType.getType();
        if ("*".equals(type)) {
            type = null;
        }
        String subtype = mediaType.getSubtype();
        if ("*".equals(subtype)) {
            subtype = null;
        }
        Map<String, String> parameters = mediaType.getParameters();
        if (parameters.isEmpty())
            parameters = null;

        QualityValue bestQuality = QualityValue.NOT_ACCEPTABLE;
        int bestMatchCount = -1;
        MediaType bestRequestMediaType = null;

        for (MediaType requested : requestedMediaTypes.keySet()) {
            int matchCount = 0;
            if (type != null) {
                String requestedType = requested.getType();
                if (requestedType.equals(type)) {
                    matchCount += mediaRadix * 100;
                } else if (!"*".equals(requestedType)) {
                    continue;
                }
            }
            if (subtype != null) {
                String requestedSubtype = requested.getSubtype();
                if (requestedSubtype.equals(subtype)) {
                    matchCount += mediaRadix * 10;
                } else if (!"*".equals(requestedSubtype)) {
                    continue;
                }
            }
            Map<String, String> requestedParameters = requested.getParameters();
            if (requestedParameters != null && requestedParameters.size() > 0) {
                if (!hasRequiredParameters(requestedParameters, parameters))
                    continue;
                matchCount += requestedParameters.size();
            }

            if (matchCount > bestMatchCount) {
                bestMatchCount = matchCount;
                bestQuality = requestedMediaTypes.get(requested);
                bestRequestMediaType = requested;
            } else if (matchCount == bestMatchCount) {
                QualityValue qualityValue = requestedMediaTypes.get(requested);
                if (bestQuality.compareTo(qualityValue) < 0) {
                    bestQuality = qualityValue;
                    bestRequestMediaType = requested;
                }
            }
        }

        if (!bestQuality.isAcceptable()) {
            return false;
        }

        quality.setMediaTypeQualityValue(bestQuality)
                .setRequestMediaType(bestRequestMediaType);
        return true;
    }

    private boolean hasRequiredParameters(Map<String, String> required, Map<String, String> available) {
        if (available == null) {
            return false;
        }
        for (Map.Entry<String, String> requiredEntry : required.entrySet()) {
            String name = requiredEntry.getKey();
            String value = requiredEntry.getValue();
            String availableValue = available.get(name);
            if (availableValue == null && "charset".equals(name)) {
                if (requestedCharacterSets != null
                        && !requestedCharacterSets.containsKey(null)
                        && !requestedCharacterSets.containsKey(value)) {
                    return false;
                }
            } else if (!value.equals(availableValue)) {
                return false;
            }
        }
        return true;
    }

    private boolean applyCharacterSet(Variant option, VariantQuality quality) {
        if (requestedCharacterSets == null)
            return true;
        MediaType mediaType = option.getMediaType();
        if (mediaType == null) {
            return true;
        }
        String charsetParameter = mediaType.getParameters().get("charset");
        if (charsetParameter == null) {
            return true;
        }
        QualityValue value = requestedCharacterSets.get(charsetParameter);
        if (value == null) { // try wildcard
            value = requestedCharacterSets.get(null);
        }
        if (value == null) { // no match
            return false;
        }
        if (!value.isAcceptable()) {
            return false;
        }
        quality.setCharacterSetQualityValue(value);
        return true;
    }

    private boolean applyEncoding(Variant option, VariantQuality quality) {
        if (requestedEncodings == null)
            return true;
        String encoding = option.getEncoding();
        if (encoding == null)
            return true;
        QualityValue value = requestedEncodings.get(encoding);
        if (value == null) { // try wildcard
            value = requestedEncodings.get(null);
        }
        if (value == null) { // no match
            return false;
        }
        if (!value.isAcceptable()) {
            return false;
        }
        quality.setEncodingQualityValue(value);
        return true;
    }

    private boolean hasCountry(Locale locale) {
        return locale.getCountry() != null && !"".equals(locale.getCountry().trim());
    }

    private boolean applyLanguage(Variant option, VariantQuality quality) {
        if (requestedLanguages == null) {
            return true;
        }
        Locale language = option.getLanguage();
        if (language == null) {
            return true;
        }
        QualityValue value = null;
        for (Map.Entry<Locale, QualityValue> entry : requestedLanguages.entrySet()) {
            Locale locale = entry.getKey();
            QualityValue qualityValue = entry.getValue();
            if (locale == null) {
                continue;
            }

            if (locale.getLanguage().equalsIgnoreCase(language.getLanguage())) {
                if (hasCountry(locale) && hasCountry(language)) {
                    if (locale.getCountry().equalsIgnoreCase(language.getCountry())) {
                        value = qualityValue;
                        break;
                    } else {
                        continue;
                    }
                } else if (hasCountry(locale) == hasCountry(language)) {
                    value = qualityValue;
                    break;
                } else {
                    value = qualityValue; // might be a better match so re-loop
                }
            }
        }

        if (value == null) {// try wildcard
            value = requestedLanguages.get(null);
        }
        if (value == null) {// no match
            return false;
        }
        if (!value.isAcceptable()) {
            return false;
        }
        quality.setLanguageQualityValue(value);
        return true;
    }

}
