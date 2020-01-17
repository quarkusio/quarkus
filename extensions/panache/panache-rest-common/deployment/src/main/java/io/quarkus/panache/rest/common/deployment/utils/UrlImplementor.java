package io.quarkus.panache.rest.common.deployment.utils;

import java.net.URI;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.panache.rest.common.runtime.utils.StringUtil;

public final class UrlImplementor {

    private final FieldAccessImplementor fieldAccessor;

    public UrlImplementor(FieldAccessImplementor fieldAccessor) {
        this.fieldAccessor = fieldAccessor;
    }

    public static String getCollectionUrl(String entityType) {
        String[] pieces = StringUtil.camelToHyphenated(removePackage(entityType)).split("-");
        pieces[pieces.length - 1] = StringUtil.toPlural(pieces[pieces.length - 1]);
        return String.join("-", pieces);
    }

    private static String removePackage(String className) {
        return className.substring(className.lastIndexOf(".") + 1);
    }

    public ResultHandle getEntityUrl(BytecodeCreator creator, ResultHandle entity, String entityType) {
        ResultHandle idString = creator.invokeStaticMethod(
                MethodDescriptor.ofMethod(String.class, "valueOf", String.class, Object.class),
                fieldAccessor.getId(creator, entityType, entity));
        ResultHandle entityPathSegments = creator.newArray(CharSequence.class, 2);
        creator.writeArrayValue(entityPathSegments, 0, creator.load(getCollectionUrl(entityType)));
        creator.writeArrayValue(entityPathSegments, 1, idString);

        ResultHandle entityPath = creator.invokeStaticMethod(
                MethodDescriptor.ofMethod(String.class, "join", String.class, CharSequence.class, CharSequence[].class),
                creator.load("/"), entityPathSegments);

        return creator.invokeStaticMethod(
                MethodDescriptor.ofMethod(URI.class, "create", URI.class, String.class), entityPath);
    }
}
