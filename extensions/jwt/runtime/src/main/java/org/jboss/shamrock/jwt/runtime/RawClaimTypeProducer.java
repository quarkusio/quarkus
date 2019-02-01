package org.jboss.shamrock.jwt.runtime;

import java.lang.annotation.Annotation;
import java.util.Optional;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Named;

import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.ClaimValue;
import org.eclipse.microprofile.jwt.Claims;
import org.jboss.logging.Logger;

/**
 *
 */
public class RawClaimTypeProducer {
    private static Logger log = Logger.getLogger(RawClaimTypeProducer.class);
    @Produces
    @Claim("")
    @Named("RawClaimTypeProducer#getValue")
    public Object getValue(InjectionPoint ip) {
        log.debugf("getValue(%s)", ip);
        String name = getName(ip);
        ClaimValue<Optional<Object>> cv = MPJWTProducer.generalClaimValueProducer(name);
        Optional<Object> value = cv.getValue();
        Object returnValue = value.orElse(null);
        return returnValue;
    }

    @Produces
    @Claim("")
    @Named("RawClaimTypeProducer#getOptionalValue")
    public Optional getOptionalValue(InjectionPoint ip) {
        log.debugf("getOptionalValue(%s)", ip);
        String name = getName(ip);
        ClaimValue<Optional<Object>> cv = MPJWTProducer.generalClaimValueProducer(name);
        Optional<Object> value = cv.getValue();
        return value;
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
