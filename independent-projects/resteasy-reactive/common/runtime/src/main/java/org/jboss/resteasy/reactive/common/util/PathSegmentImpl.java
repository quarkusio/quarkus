package org.jboss.resteasy.reactive.common.util;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.PathSegment;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 */
public class PathSegmentImpl implements PathSegment {
    private String path;
    private String original;
    private MultivaluedMap<String, String> matrixParameters;
    private boolean hasMatrixParams;

    /**
     * @param segment encoded path segment
     * @param decode whether or not to decode values
     */
    public PathSegmentImpl(final String segment, final boolean decode) {
        this.original = segment;
        this.path = segment;
        int semicolon = segment.indexOf(';');
        if (semicolon >= 0) {
            matrixParameters = new QuarkusMultivaluedHashMap<String, String>();
            hasMatrixParams = true;
            if (semicolon > 0)
                this.path = segment.substring(0, semicolon);
            else
                this.path = "";
            String matrixParams = segment.substring(semicolon + 1);
            String[] params = matrixParams.split(";");
            for (String param : params) {
                String[] namevalue = param.split("=");
                if (namevalue != null && namevalue.length > 0) {
                    String name = namevalue[0];
                    if (decode)
                        name = Encode.decodePath(name);
                    String value = null;
                    if (namevalue.length > 1) {
                        value = namevalue[1];
                    }
                    if (decode && value != null)
                        value = Encode.decodePath(value);
                    matrixParameters.add(name, value);
                }
            }
        }
        if (decode)
            this.path = Encode.decodePath(this.path);
    }

    /**
     * NOTE: Used for optimization in ResteasyUriInfo
     *
     * @return true if path segment contains matrix parameters
     */
    public boolean hasMatrixParams() {
        return hasMatrixParams;
    }

    public String getOriginal() {
        return original;
    }

    public String getPath() {
        return path;
    }

    public MultivaluedMap<String, String> getMatrixParameters() {
        if (matrixParameters == null) {
            matrixParameters = new QuarkusMultivaluedHashMap<String, String>();
        }
        return matrixParameters;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        if (path != null)
            buf.append(path);
        if (matrixParameters != null) {
            for (String name : matrixParameters.keySet()) {
                for (String value : matrixParameters.get(name)) {
                    buf.append(";").append(name).append("=").append(value);
                }
            }
        }
        return buf.toString();
    }

}
