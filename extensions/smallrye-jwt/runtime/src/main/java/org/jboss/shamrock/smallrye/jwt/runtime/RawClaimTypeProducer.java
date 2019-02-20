package org.jboss.shamrock.smallrye.jwt.runtime;

import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.json.JsonNumber;
import javax.json.JsonString;

import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

/**
 * Producer for unwrapped native and primitive Java types of the claims
 */
public class RawClaimTypeProducer {
    private static Logger log = Logger.getLogger(RawClaimTypeProducer.class);

    @Inject
    JsonWebToken currentToken;

    @Produces
    @Claim("")
    Set<String> getClaimAsSet(InjectionPoint ip) {
        log.debugf("getValue(%s)", ip);
        String name = getName(ip);
        Optional<Set<String>> value = currentToken.claim(name);
        Set<String> returnValue = value.orElse(null);
        return returnValue;
    }

    @Produces
    @Claim("")
    String getClaimAsString(InjectionPoint ip) {
        log.debugf("getValue(%s)", ip);
        String name = getName(ip);
        Optional<Object> optValue = currentToken.claim(name);
        String returnValue = null;
        if(optValue.isPresent()) {
            Object value = optValue.get();
            if(value instanceof JsonString) {
                JsonString jsonValue = (JsonString) value;
                returnValue = jsonValue.getString();
            } else {
                returnValue = value.toString();
            }
        }
        return returnValue;
    }

    @Produces
    @Claim("")
    Long getClaimAsLong(InjectionPoint ip) {
        log.debugf("getValue(%s)", ip);
        String name = getName(ip);
        Optional<Object> optValue = currentToken.claim(name);
        Long returnValue = null;
        if(optValue.isPresent()) {
            Object value = optValue.get();
            if(value instanceof JsonNumber) {
                JsonNumber jsonValue = (JsonNumber) value;
                returnValue = jsonValue.longValue();
            } else {
                returnValue = Long.parseLong(value.toString());
            }
        }
        return returnValue;
    }

    @Produces
    @Claim("")
    Double getClaimAsDouble(InjectionPoint ip) {
        log.debugf("getValue(%s)", ip);
        String name = getName(ip);
        Optional<Object> optValue = currentToken.claim(name);
        Double returnValue = null;
        if(optValue.isPresent()) {
            Object value = optValue.get();
            if(value instanceof JsonNumber) {
                JsonNumber jsonValue = (JsonNumber) value;
                returnValue = jsonValue.doubleValue();
            } else {
                returnValue = Double.parseDouble(value.toString());
            }
        }
        return returnValue;
    }

    String getName(InjectionPoint ip) {
        String name = null;
        for (Annotation ann : ip.getQualifiers()) {
            if (ann instanceof Claim) {
                Claim claim = (Claim) ann;
                name = claim.standard() == Claims.UNKNOWN ? claim.value() : claim.standard().name();
            }
        }
        return name;
    }
}
