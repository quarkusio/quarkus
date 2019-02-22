package io.quarkus.hibernate.validator.runtime.jaxrs;

import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.api.validation.ResteasyViolationException;
import org.jboss.resteasy.core.ResteasyContext;
import org.jboss.resteasy.spi.ResteasyConfiguration;
import org.jboss.resteasy.spi.validation.ConstraintTypeUtil;

public class ResteasyViolationExceptionImpl extends ResteasyViolationException {
    private static final long serialVersionUID = 657697354453281559L;

    public ResteasyViolationExceptionImpl(final Set<? extends ConstraintViolation<?>> constraintViolations,
            final List<MediaType> accept) {
        super(constraintViolations, accept);
    }

    @Override
    public ConstraintTypeUtil getConstraintTypeUtil() {
        return new ConstraintTypeUtil20();
    }

    @Override
    protected ResteasyConfiguration getResteasyConfiguration() {
        return ResteasyContext.getContextData(ResteasyConfiguration.class);
    }
}
