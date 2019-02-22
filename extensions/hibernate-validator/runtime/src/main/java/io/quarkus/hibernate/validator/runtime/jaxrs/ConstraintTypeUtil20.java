package io.quarkus.hibernate.validator.runtime.jaxrs;

import java.util.Iterator;

import javax.validation.ConstraintViolation;
import javax.validation.ElementKind;
import javax.validation.Path.Node;

import org.jboss.resteasy.api.validation.ConstraintType;
import org.jboss.resteasy.resteasy_jaxrs.i18n.Messages;
import org.jboss.resteasy.spi.validation.ConstraintTypeUtil;

public class ConstraintTypeUtil20 implements ConstraintTypeUtil {

    @Override
    public ConstraintType.Type getConstraintType(Object o) {
        if (!(o instanceof ConstraintViolation)) {
            throw new RuntimeException(Messages.MESSAGES.unknownObjectPassedAsConstraintViolation(o));
        }
        ConstraintViolation<?> v = ConstraintViolation.class.cast(o);

        Iterator<Node> nodes = v.getPropertyPath().iterator();
        Node firstNode = nodes.next();

        switch (firstNode.getKind()) {
            case BEAN:
                return ConstraintType.Type.CLASS;
            case CONSTRUCTOR:
            case METHOD:
                Node secondNode = nodes.next();

                if (secondNode.getKind() == ElementKind.PARAMETER || secondNode.getKind() == ElementKind.CROSS_PARAMETER) {
                    return ConstraintType.Type.PARAMETER;
                } else if (secondNode.getKind() == ElementKind.RETURN_VALUE) {
                    return ConstraintType.Type.RETURN_VALUE;
                } else {
                    throw new RuntimeException(Messages.MESSAGES.unexpectedPathNodeViolation(secondNode.getKind()));
                }
            case PROPERTY:
                return ConstraintType.Type.PROPERTY;
            case CROSS_PARAMETER:
            case PARAMETER:
            case RETURN_VALUE:
            case CONTAINER_ELEMENT: // we shouldn't encounter these element types at the root
            default:
                throw new RuntimeException(Messages.MESSAGES.unexpectedPathNode(firstNode.getKind()));
        }
    }
}
