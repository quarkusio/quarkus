package org.jboss.resteasy.reactive.common.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 */
@SuppressWarnings(value = "rawtypes")
public class MediaTypeHelper {
    public static final MediaTypeComparator Q_COMPARATOR = new MediaTypeComparator("q");
    public static final MediaTypeComparator QS_COMPARATOR = new MediaTypeComparator("qs");

    private static float getQTypeWithParamInfo(MediaType type, String parameterName) {
        if (type.getParameters() != null) {
            String val = type.getParameters().get(parameterName);
            try {
                if (val != null) {
                    float rtn = Float.parseFloat(val);
                    if (rtn > 1.0F)
                        throw new WebApplicationException(
                                String.format("Media type %s greater than 1: %s", parameterName, type),
                                Response.Status.BAD_REQUEST);
                    return rtn;
                }
            } catch (NumberFormatException e) {
                throw new RuntimeException(String.format("Media type %s value must be a float: %s", parameterName, type), e);
            }
        }
        return 2.0f;
    }

    public static float getQWithParamInfo(MediaType type) {
        return getQTypeWithParamInfo(type, "q");
    }

    /**
     * subtypes like application/*+xml
     *
     * @param subtype subtype
     * @return true if subtype is composite
     */
    public static boolean isCompositeWildcardSubtype(String subtype) {
        return subtype.startsWith("*+");
    }

    /**
     * subtypes like application/*+xml
     *
     * @param subtype subtype
     * @return true if subtype is wildcard composite
     */
    public static boolean isWildcardCompositeSubtype(String subtype) {
        return subtype.endsWith("+*");
    }

    public static boolean isComposite(String subtype) {
        return (isCompositeWildcardSubtype(subtype) || isWildcardCompositeSubtype(subtype));
    }

    public static class MediaTypeComparator implements Comparator<MediaType>, Serializable {

        private static final long serialVersionUID = -5828700121582498092L;

        private final String parameterName;

        public MediaTypeComparator(String parameterName) {
            this.parameterName = parameterName;
        }

        public int compare(MediaType mediaType2, MediaType mediaType) {
            float q = getQTypeWithParamInfo(mediaType, parameterName);
            boolean wasQ = q != 2.0f;
            if (q == 2.0f)
                q = 1.0f;

            float q2 = getQTypeWithParamInfo(mediaType2, parameterName);
            boolean wasQ2 = q2 != 2.0f;
            if (q2 == 2.0f)
                q2 = 1.0f;

            if (q < q2)
                return -1;
            if (q > q2)
                return 1;

            if (mediaType.isWildcardType() && !mediaType2.isWildcardType())
                return -1;
            if (!mediaType.isWildcardType() && mediaType2.isWildcardType())
                return 1;
            if (mediaType.isWildcardSubtype() && !mediaType2.isWildcardSubtype())
                return -1;
            if (!mediaType.isWildcardSubtype() && mediaType2.isWildcardSubtype())
                return 1;
            if (isComposite(mediaType.getSubtype()) && !isComposite(mediaType2.getSubtype()))
                return -1;
            if (!isComposite(mediaType.getSubtype()) && isComposite(mediaType2.getSubtype()))
                return 1;
            if (isCompositeWildcardSubtype(mediaType.getSubtype()) && !isCompositeWildcardSubtype(mediaType2.getSubtype()))
                return -1;
            if (!isCompositeWildcardSubtype(mediaType.getSubtype()) && isCompositeWildcardSubtype(mediaType2.getSubtype()))
                return 1;
            if (isWildcardCompositeSubtype(mediaType.getSubtype()) && !isWildcardCompositeSubtype(mediaType2.getSubtype()))
                return -1;
            if (!isWildcardCompositeSubtype(mediaType.getSubtype()) && isWildcardCompositeSubtype(mediaType2.getSubtype()))
                return 1;

            int numNonQ = 0;
            if (mediaType.getParameters() != null) {
                numNonQ = mediaType.getParameters().size();
                if (wasQ)
                    numNonQ--;
            }

            int numNonQ2 = 0;
            if (mediaType2.getParameters() != null) {
                numNonQ2 = mediaType2.getParameters().size();
                if (wasQ2)
                    numNonQ2--;
            }

            if (numNonQ < numNonQ2)
                return -1;
            if (numNonQ > numNonQ2)
                return 1;

            return 0;
        }
    }

    public static int compareWeight(MediaType one, MediaType two) {
        return Q_COMPARATOR.compare(one, two);
    }

    public static void sortByWeight(List<MediaType> types) {
        if (hasAtMostOneItem(types)) {
            return;
        }
        types.sort(Q_COMPARATOR);
    }

    public static void sortByQSWeight(List<MediaType> types) {
        if (hasAtMostOneItem(types)) {
            return;
        }
        types.sort(QS_COMPARATOR);
    }

    private static boolean hasAtMostOneItem(List<MediaType> types) {
        return types == null || types.size() <= 1;
    }

    /**
     * Finds the best match according to the weight of the media types
     * The parameters needs to be sorted, so a copy of these is made if necessary
     * in order to avoid altering the input
     */
    public static MediaType getBestMatch(List<MediaType> desired, List<MediaType> provided) {
        if (!hasAtMostOneItem(desired)) {
            desired = new ArrayList<>(desired);
            sortByWeight(desired);
        }
        if (!hasAtMostOneItem(provided)) {
            provided = new ArrayList<>(provided);
            sortByWeight(provided);
        }
        return getFirstMatch(desired, provided);
    }

    public static MediaType getFirstMatch(List<MediaType> desired, List<MediaType> provided) {
        boolean emptyDesired = desired == null || desired.size() == 0;
        boolean emptyProvided = provided == null || provided.size() == 0;

        if (emptyDesired && emptyProvided)
            return null;
        if (emptyDesired)
            return provided.get(0);
        if (emptyProvided)
            return desired.get(0);

        for (int i = 0; i < desired.size(); i++) {
            for (int j = 0; j < provided.size(); j++) {
                MediaType provide = provided.get(j);
                if (provide.isCompatible(desired.get(i))) {
                    return provide;
                }
            }
        }
        return null;
    }

    public static List<MediaType> parseHeader(String header) {
        ArrayList<MediaType> types = new ArrayList<>();
        String[] medias = header.split(",");
        for (String media : medias) {
            types.add(MediaType.valueOf(media.trim()));
        }
        return types;
    }

    public static boolean equivalent(MediaType m1, MediaType m2) {
        if (m1 == m2)
            return true;

        if (!m1.getType().equals(m2.getType()))
            return false;
        if (!m1.getSubtype().equals(m2.getSubtype()))
            return false;

        return equivalentParams(m1, m2);
    }

    public static boolean equivalentParams(MediaType m1, MediaType m2) {
        Map<String, String> params1 = m1.getParameters();
        Map<String, String> params2 = m2.getParameters();

        if (params1 == params2)
            return true;
        if (params1 == null || params2 == null)
            return false;
        if (params1.size() == 0 && params2.size() == 0)
            return true;
        int numParams1 = params1.size();
        if (params1.containsKey("q"))
            numParams1--;
        int numParams2 = params2.size();
        if (params2.containsKey("q"))
            numParams2--;

        if (numParams1 != numParams2)
            return false;
        if (numParams1 == 0)
            return true;

        for (Map.Entry<String, String> entry : params1.entrySet()) {
            String key = entry.getKey();
            if (key.equals("q"))
                continue;
            String value = entry.getValue();
            String value2 = params2.get(key);
            if (value == value2)
                continue; // both null
            if (value == null || value2 == null)
                return false;
            if (value.equals(value2) == false)
                return false;
        }
        return true;
    }

    public static boolean isTextLike(MediaType mediaType) {
        return "text".equalsIgnoreCase(mediaType.getType())
                || ("application".equalsIgnoreCase(mediaType.getType())
                        && mediaType.getSubtype().toLowerCase().startsWith("xml"));
    }

    public static boolean isUnsupportedWildcardSubtype(MediaType mediaType) {
        if (mediaType.isWildcardSubtype()) {
            return !mediaType.isWildcardType() && !"application".equals(mediaType.getType());
        }
        return false;
    }

    /**
     * If the supplied media type contains a suffix in the subtype, then this returns a new media type
     * that uses the suffix as the subtype
     */
    public static MediaType withSuffixAsSubtype(MediaType mediaType) {
        int plusIndex = mediaType.getSubtype().indexOf('+');
        if ((plusIndex > -1) && (plusIndex < mediaType.getSubtype().length() - 1)) {
            mediaType = new MediaType(mediaType.getType(),
                    mediaType.getSubtype().substring(plusIndex + 1),
                    mediaType.getParameters());
        }
        return mediaType;
    }
}
