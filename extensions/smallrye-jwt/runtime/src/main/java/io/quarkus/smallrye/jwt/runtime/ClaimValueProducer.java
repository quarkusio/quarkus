package io.quarkus.smallrye.jwt.runtime;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;

import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.ClaimValue;

/**
 * A producer for the ClaimValue<T> wrapper injection sites.
 * 
 * @param <T> the raw claim type
 */
public class ClaimValueProducer<T> {

    @Inject
    CommonJwtProducer util;

    @Produces
    @Claim("")
    @SuppressWarnings("unchecked")
    ClaimValue<T> produce(InjectionPoint ip) {
        ClaimValue<Optional<T>> cv = util.generalClaimValueProducer(ip);
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

}
