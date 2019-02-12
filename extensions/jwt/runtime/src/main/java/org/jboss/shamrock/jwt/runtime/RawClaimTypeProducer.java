package org.jboss.shamrock.jwt.runtime;

import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;

import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.ClaimValue;
import org.eclipse.microprofile.jwt.Claims;
import org.jboss.logging.Logger;

public class RawClaimTypeProducer {
    private static Logger log = Logger.getLogger(RawClaimTypeProducer.class);

    @Produces
    @Claim("")
    Set<String> getClaimAsSet(InjectionPoint ip) {
        log.debugf("getValue(%s)", ip);
        String name = getName(ip);
        ClaimValue<Optional<Set<String>>> cv = MPJWTProducer.generalClaimValueProducer(name);
        Optional<Set<String>> value = cv.getValue();
        Set<String> returnValue = value.orElse(null);
        return returnValue;
    }

    @Produces
    @Claim("")
    String getClaimAsString(InjectionPoint ip) {
        log.debugf("getValue(%s)", ip);
        String name = getName(ip);
        ClaimValue<Optional<String>> cv = MPJWTProducer.generalClaimValueProducer(name);
        Optional<String> value = cv.getValue();
        String returnValue = value.orElse(null);
        return returnValue;
    }

    @Produces
    @Claim("")
    Long getClaimAsLong(InjectionPoint ip) {
        log.debugf("getValue(%s)", ip);
        String name = getName(ip);
        ClaimValue<Optional<Long>> cv = MPJWTProducer.generalClaimValueProducer(name);
        Optional<Long> value = cv.getValue();
        Long returnValue = value.orElse(null);
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
