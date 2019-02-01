package org.jboss.shamrock.jwt.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;

import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.ClaimValue;
import org.eclipse.microprofile.jwt.Claims;

/**
 * A producer for the ClaimValue<T> wrapper injection sites.
 * @param <T> the raw claim type
 */
public class ClaimValueProducer<T> {

    @Produces
    @Claim("")
    ClaimValue<T> produce(InjectionPoint ip) {
        String name = getName(ip);
        ClaimValue<Optional<T>> cv = MPJWTProducer.generalClaimValueProducer(name);
        ClaimValue<T> returnValue = (ClaimValue<T>) cv;
        Optional<T> value = cv.getValue();
        // Pull out the ClaimValue<T> T type,
        Type matchType = ip.getType();
        Type actualType = Object.class;
        boolean isOptional = false;
        if (matchType instanceof ParameterizedType) {
            actualType = ((ParameterizedType) matchType).getActualTypeArguments()[0];
            isOptional = matchType.getTypeName().equals(Optional.class.getTypeName());
            if (isOptional) {
                actualType = ((ParameterizedType) matchType).getActualTypeArguments()[0];
            }
        }

        if (!actualType.getTypeName().startsWith(Optional.class.getTypeName())) {
            T nestedValue = value.orElse(null);
            ClaimValueWrapper<T> wrapper = new ClaimValueWrapper<>(cv.getName());
            wrapper.setValue(nestedValue);
            returnValue = wrapper;
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
