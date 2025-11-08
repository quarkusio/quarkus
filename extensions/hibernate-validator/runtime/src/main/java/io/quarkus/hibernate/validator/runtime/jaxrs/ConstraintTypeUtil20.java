package io.quarkus.hibernate.validator.runtime.jaxrs;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ElementKind;
import jakarta.validation.Path.Node;

import org.hibernate.validator.path.RandomAccessPath;
import org.jboss.resteasy.api.validation.ConstraintType;
import org.jboss.resteasy.resteasy_jaxrs.i18n.Messages;
import org.jboss.resteasy.spi.validation.ConstraintTypeUtil;

public class ConstraintTypeUtil20 implements ConstraintTypeUtil {

    public static final ConstraintTypeUtil INSTANCE = new ConstraintTypeUtil20();

    private ConstraintTypeUtil20() {
    }

    @Override
    public ConstraintType.Type getConstraintType(Object o) {
        if (o instanceof ConstraintViolation<?> v) {
            if (v.getPropertyPath() instanceof RandomAccessPath randomAccessPath) {
                switch (randomAccessPath.getRootNode().getKind()) {
                    case BEAN:
                        return ConstraintType.Type.CLASS;
                    case CONSTRUCTOR:
                    case METHOD:
                        Node secondNode = randomAccessPath.getNode(1);

                        if (secondNode.getKind() == ElementKind.PARAMETER
                                || secondNode.getKind() == ElementKind.CROSS_PARAMETER) {
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
                        throw new RuntimeException(
                                Messages.MESSAGES.unexpectedPathNode(randomAccessPath.getRootNode().getKind()));
                }
            }
        }
        throw new RuntimeException(Messages.MESSAGES.unknownObjectPassedAsConstraintViolation(o));
    }
}
