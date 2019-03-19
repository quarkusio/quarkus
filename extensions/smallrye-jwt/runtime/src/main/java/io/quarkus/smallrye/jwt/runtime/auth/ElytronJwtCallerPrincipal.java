package io.quarkus.smallrye.jwt.runtime.auth;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;
import javax.json.JsonValue;

import org.eclipse.microprofile.jwt.Claims;
import org.jboss.logging.Logger;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.wildfly.security.authz.Attributes;

import io.smallrye.jwt.auth.principal.JWTCallerPrincipal;

/**
 * An implementation of JWTCallerPrincipal that builds on the Elytron attributes
 */
public class ElytronJwtCallerPrincipal extends JWTCallerPrincipal {
    private static final String TMP = "tmp";
    private static Logger logger = Logger.getLogger(ElytronJwtCallerPrincipal.class);

    private Attributes claims;
    private JwtClaims claimsSet;
    private String customPrincipalName;

    public ElytronJwtCallerPrincipal(final String customPrincipalName, final Attributes claims) {
        super(getRawToken(getClaimsSet(claims)), "JWT");
        this.claims = claims;
        this.customPrincipalName = customPrincipalName;
        this.claimsSet = getClaimsSet(claims);
        fixJoseTypes();
    }

    public ElytronJwtCallerPrincipal(final String customPrincipalName, final JwtClaims claimsSet) {
        this(customPrincipalName, new ClaimAttributes(claimsSet));
    }

    public ElytronJwtCallerPrincipal(Attributes claims) {
        this(null, claims);
    }

    private static String getRawToken(JwtClaims claimsSet) {
        Object rawToken = claimsSet.getClaimValue(Claims.raw_token.name());
        return rawToken != null ? rawToken.toString() : null;
    }

    public Attributes getClaims() {
        return claims;
    }

    private static JwtClaims getClaimsSet(Attributes claims) {
        if (!(claims instanceof ClaimAttributes)) {
            throw new IllegalStateException(
                    "ElytronJwtCallerPrincipal requires Attributes to be a: " + ClaimAttributes.class.getName());
        }
        return ((ClaimAttributes) claims).getClaimsSet();
    }

    @Override
    public String getName() {
        return customPrincipalName != null ? customPrincipalName : super.getName();
    }

    //TODO: Synchronize all the code below with smallrye/smallrye-jwt and eventually remove from this class.
    // Specifically, nearly the indentical code is already present in DefaultJWTCallerPrincipal and JWTCallerPrincipal

    @Override
    public Set<String> getAudience() {
        Set<String> audSet = null;
        try {
            if (claimsSet.hasClaim(Claims.aud.name())) {
                List<String> audList = claimsSet.getStringListClaimValue("aud");
                audSet = new HashSet<>(audList);
            }
        } catch (MalformedClaimException e) {
            try {
                // Not sent as an array, try a single value
                String aud = claimsSet.getStringClaimValue("aud");
                audSet = new HashSet<>();
                audSet.add(aud);
            } catch (MalformedClaimException e1) {
            }
        }
        return audSet;
    }

    @Override
    public Set<String> getGroups() {
        HashSet<String> groups = new HashSet<>();
        try {
            List<String> globalGroups = claimsSet.getStringListClaimValue("groups");
            if (globalGroups != null) {
                groups.addAll(globalGroups);
            }
        } catch (MalformedClaimException e) {
            e.printStackTrace();
        }
        return groups;
    }

    @Override
    protected Collection<String> doGetClaimNames() {
        return claimsSet.getClaimNames();
    }

    @Override
    protected Object getClaimValue(String claimName) {
        Claims claimType = Claims.UNKNOWN;
        Object claim = null;
        try {
            claimType = Claims.valueOf(claimName);
        } catch (IllegalArgumentException e) {
        }
        // Handle the jose4j NumericDate types and
        switch (claimType) {
            case exp:
            case iat:
            case auth_time:
            case nbf:
            case updated_at:
                try {
                    claim = claimsSet.getClaimValue(claimType.name(), Long.class);
                    if (claim == null) {
                        claim = new Long(0);
                    }
                } catch (MalformedClaimException e) {
                }
                break;
            case groups:
                claim = getGroups();
                break;
            case aud:
                claim = getAudience();
                break;
            case UNKNOWN:
                // This has to be a Json type
                claim = claimsSet.getClaimValue(claimName);
                if (!(claim instanceof JsonStructure)) {
                    claim = wrapClaimValue(claim);
                }
                break;
            default:
                claim = claimsSet.getClaimValue(claimType.name());
        }
        return claim;
    }

    /**
     * Convert the types jose4j uses for address, sub_jwk, and jwk
     */
    private void fixJoseTypes() {
        if (claimsSet.hasClaim(Claims.address.name())) {
            replaceMap(Claims.address.name());
        }
        if (claimsSet.hasClaim(Claims.jwk.name())) {
            replaceMap(Claims.jwk.name());
        }
        if (claimsSet.hasClaim(Claims.sub_jwk.name())) {
            replaceMap(Claims.sub_jwk.name());
        }
        // Handle custom claims
        Set<String> customClaimNames = filterCustomClaimNames(claimsSet.getClaimNames());
        for (String name : customClaimNames) {
            Object claimValue = claimsSet.getClaimValue(name);
            Class claimType = claimValue.getClass();
            if (claimValue instanceof List) {
                replaceList(name);
            } else if (claimValue instanceof Map) {
                replaceMap(name);
            } else if (claimValue instanceof Number) {
                replaceNumber(name);
            }
        }
    }

    /**
     * Determine the custom claims in the set
     *
     * @param claimNames - the current set of claim names in this token
     * @return the possibly empty set of names for non-Claims claims
     */
    private Set<String> filterCustomClaimNames(Collection<String> claimNames) {
        HashSet<String> customNames = new HashSet<>(claimNames);
        for (Claims claim : Claims.values()) {
            customNames.remove(claim.name());
        }
        return customNames;
    }

    /**
     * Replace the jose4j Map<String,Object> with a JsonObject
     *
     * @param name - claim name
     */
    private void replaceMap(String name) {
        try {
            Map<String, Object> map = claimsSet.getClaimValue(name, Map.class);
            JsonObject jsonObject = replaceMapClaims(map);
            claimsSet.setClaim(name, jsonObject);
        } catch (MalformedClaimException e) {
            logger.warn("replaceMap failure for: " + name, e);
        }
    }

    private JsonObject replaceMapClaims(Map<String, Object> map) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object entryValue = entry.getValue();
            if (entryValue instanceof Map) {
                JsonObject entryJsonObject = replaceMapClaims((Map<String, Object>) entryValue);
                builder.add(entry.getKey(), entryJsonObject);
            } else if (entryValue instanceof List) {
                JsonArray array = (JsonArray) wrapClaimValue(entryValue);
                builder.add(entry.getKey(), array);
            } else if (entryValue instanceof Long || entryValue instanceof Integer) {
                long lvalue = ((Number) entryValue).longValue();
                builder.add(entry.getKey(), lvalue);
            } else if (entryValue instanceof Double || entryValue instanceof Float) {
                double dvalue = ((Number) entryValue).doubleValue();
                builder.add(entry.getKey(), dvalue);
            } else if (entryValue instanceof Boolean) {
                boolean flag = ((Boolean) entryValue).booleanValue();
                builder.add(entry.getKey(), flag);
            } else if (entryValue instanceof String) {
                builder.add(entry.getKey(), entryValue.toString());
            }
        }
        return builder.build();
    }

    JsonValue wrapClaimValue(Object value) {
        JsonValue jsonValue = null;
        if (value instanceof JsonValue) {
            // This may already be a JsonValue
            jsonValue = (JsonValue) value;
        } else if (value instanceof String) {
            jsonValue = Json.createObjectBuilder()
                    .add(TMP, value.toString())
                    .build()
                    .getJsonString(TMP);
        } else if (value instanceof Number) {
            Number number = (Number) value;
            if ((number instanceof Long) || (number instanceof Integer)) {
                jsonValue = Json.createObjectBuilder()
                        .add(TMP, number.longValue())
                        .build()
                        .getJsonNumber(TMP);
            } else {
                jsonValue = Json.createObjectBuilder()
                        .add(TMP, number.doubleValue())
                        .build()
                        .getJsonNumber(TMP);
            }
        } else if (value instanceof Boolean) {
            Boolean flag = (Boolean) value;
            jsonValue = flag ? JsonValue.TRUE : JsonValue.FALSE;
        } else if (value instanceof Collection) {
            JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
            Collection list = (Collection) value;
            for (Object element : list) {
                if (element instanceof String) {
                    arrayBuilder.add(element.toString());
                } else {
                    JsonValue jvalue = wrapClaimValue(element);
                    arrayBuilder.add(jvalue);
                }
            }
            jsonValue = arrayBuilder.build();
        } else if (value instanceof Map) {
            jsonValue = replaceMapClaims((Map) value);
        }
        return jsonValue;
    }

    /**
     * Replace the jose4j List<?> with a JsonArray
     *
     * @param name - claim name
     */
    private void replaceList(String name) {
        try {
            List list = claimsSet.getClaimValue(name, List.class);
            JsonArray array = (JsonArray) wrapClaimValue(list);
            claimsSet.setClaim(name, array);
        } catch (MalformedClaimException e) {
            logger.warn("replaceList failure for: " + name, e);
        }
    }

    private void replaceNumber(String name) {
        try {
            Number number = claimsSet.getClaimValue(name, Number.class);
            JsonNumber jsonNumber = (JsonNumber) wrapClaimValue(number);
            claimsSet.setClaim(name, jsonNumber);
        } catch (MalformedClaimException e) {
            logger.warn("replaceNumber failure for: " + name, e);
        }
    }
}
