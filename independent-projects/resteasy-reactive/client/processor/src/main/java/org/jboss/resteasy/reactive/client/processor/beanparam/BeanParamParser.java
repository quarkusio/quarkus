package org.jboss.resteasy.reactive.client.processor.beanparam;

import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.BEAN_PARAM;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.COOKIE_PARAM;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.ENCODED;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.FORM_PARAM;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.HEADER_PARAM;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.PATH_PARAM;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.QUERY_PARAM;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.REST_COOKIE_PARAM;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.REST_FORM_PARAM;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.REST_HEADER_PARAM;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.REST_PATH_PARAM;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.REST_QUERY_PARAM;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.MediaType;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.resteasy.reactive.common.processor.AsmUtil;
import org.jboss.resteasy.reactive.common.processor.JandexUtil;
import org.jboss.resteasy.reactive.common.processor.JavaBeanUtil;
import org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames;
import org.jboss.resteasy.reactive.common.processor.StringUtil;

public class BeanParamParser {

    public static List<Item> parse(ClassInfo beanParamClass, IndexView index) {
        Set<ClassInfo> processedBeanParamClasses = Collections.newSetFromMap(new IdentityHashMap<>());
        return parseInternal(beanParamClass, index, processedBeanParamClasses);
    }

    private static List<Item> parseInternal(ClassInfo beanParamClass, IndexView index,
            Set<ClassInfo> processedBeanParamClasses) {
        if (!processedBeanParamClasses.add(beanParamClass)) {
            throw new IllegalArgumentException("Cycle detected in BeanParam annotations; already processed class "
                    + beanParamClass.name());
        }

        try {
            List<Item> resultList = new ArrayList<>();

            // Parse class tree recursively
            if (!JandexUtil.DOTNAME_OBJECT.equals(beanParamClass.superName())) {
                resultList
                        .addAll(parseInternal(index.getClassByName(beanParamClass.superName()), index,
                                processedBeanParamClasses));
            }

            resultList.addAll(paramItemsForFieldsAndMethods(beanParamClass, QUERY_PARAM,
                    (annotationValue, fieldInfo) -> new QueryParamItem(fieldInfo.name(), annotationValue,
                            fieldInfo.hasDeclaredAnnotation(ENCODED),
                            new FieldExtractor(null, fieldInfo.name(), fieldInfo.declaringClass().name().toString()),
                            fieldInfo.type()),
                    (annotationValue, getterMethod) -> new QueryParamItem(getterMethod.name(), annotationValue,
                            getterMethod.hasDeclaredAnnotation(ENCODED),
                            new GetterExtractor(getterMethod),
                            getterMethod.returnType())));

            resultList.addAll(paramItemsForFieldsAndMethods(beanParamClass, REST_QUERY_PARAM,
                    (annotationValue, fieldInfo) -> new QueryParamItem(fieldInfo.name(),
                            annotationValue != null ? annotationValue : fieldInfo.name(),
                            fieldInfo.hasDeclaredAnnotation(ENCODED),
                            new FieldExtractor(null, fieldInfo.name(), fieldInfo.declaringClass().name().toString()),
                            fieldInfo.type()),
                    (annotationValue, getterMethod) -> new QueryParamItem(getterMethod.name(),
                            annotationValue != null ? annotationValue : getterName(getterMethod),
                            getterMethod.hasDeclaredAnnotation(ENCODED),
                            new GetterExtractor(getterMethod),
                            getterMethod.returnType())));

            resultList.addAll(paramItemsForFieldsAndMethods(beanParamClass, BEAN_PARAM,
                    (annotationValue, fieldInfo) -> {
                        Type type = fieldInfo.type();
                        if (type.kind() == Type.Kind.CLASS) {
                            DotName beanParamClassName = type.asClassType().name();
                            List<Item> subBeanParamItems = parseInternal(index.getClassByName(beanParamClassName), index,
                                    processedBeanParamClasses);
                            return new BeanParamItem(fieldInfo.name(), subBeanParamItems, beanParamClassName.toString(),
                                    new FieldExtractor(null, fieldInfo.name(), fieldInfo.declaringClass().name().toString()));
                        } else {
                            throw new IllegalArgumentException("BeanParam annotation used on a field that is not an object: "
                                    + beanParamClass.name() + "." + fieldInfo.name());
                        }
                    },
                    (annotationValue, getterMethod) -> {
                        Type returnType = getterMethod.returnType();
                        List<Item> items = parseInternal(index.getClassByName(returnType.name()), index,
                                processedBeanParamClasses);
                        return new BeanParamItem(getterMethod.name(), items, beanParamClass.name().toString(),
                                new GetterExtractor(getterMethod));
                    }));

            resultList.addAll(paramItemsForFieldsAndMethods(beanParamClass, COOKIE_PARAM,
                    (annotationValue, fieldInfo) -> new CookieParamItem(fieldInfo.name(), annotationValue,
                            new FieldExtractor(null, fieldInfo.name(),
                                    fieldInfo.declaringClass().name().toString()),
                            fieldInfo.type().name().toString()),
                    (annotationValue, getterMethod) -> new CookieParamItem(getterMethod.name(), annotationValue,
                            new GetterExtractor(getterMethod), getterMethod.returnType().name().toString())));

            resultList.addAll(paramItemsForFieldsAndMethods(beanParamClass, REST_COOKIE_PARAM,
                    (annotationValue, fieldInfo) -> new CookieParamItem(fieldInfo.name(),
                            annotationValue != null ? annotationValue : fieldInfo.name(),
                            new FieldExtractor(null, fieldInfo.name(),
                                    fieldInfo.declaringClass().name().toString()),
                            fieldInfo.type().name().toString()),
                    (annotationValue, getterMethod) -> new CookieParamItem(getterMethod.name(),
                            annotationValue != null ? annotationValue : getterName(getterMethod),
                            new GetterExtractor(getterMethod), getterMethod.returnType().name().toString())));

            resultList.addAll(paramItemsForFieldsAndMethods(beanParamClass, HEADER_PARAM,
                    (annotationValue, fieldInfo) -> new HeaderParamItem(fieldInfo.name(), annotationValue,
                            new FieldExtractor(null, fieldInfo.name(), fieldInfo.declaringClass().name().toString()),
                            fieldInfo.type()),
                    (annotationValue, getterMethod) -> new HeaderParamItem(getterMethod.name(), annotationValue,
                            new GetterExtractor(getterMethod), getterMethod.returnType())));

            // @RestHeader with no explicit value are hyphenated
            resultList.addAll(paramItemsForFieldsAndMethods(beanParamClass, REST_HEADER_PARAM,
                    (annotationValue, fieldInfo) -> new HeaderParamItem(fieldInfo.name(),
                            annotationValue != null ? annotationValue
                                    : StringUtil.hyphenateWithCapitalFirstLetter(fieldInfo.name()),
                            new FieldExtractor(null, fieldInfo.name(), fieldInfo.declaringClass().name().toString()),
                            fieldInfo.type()),
                    (annotationValue, getterMethod) -> new HeaderParamItem(getterMethod.name(),
                            annotationValue != null ? annotationValue
                                    : StringUtil.hyphenateWithCapitalFirstLetter(getterName(getterMethod)),
                            new GetterExtractor(getterMethod), getterMethod.returnType())));

            resultList.addAll(paramItemsForFieldsAndMethods(beanParamClass, PATH_PARAM,
                    (annotationValue, fieldInfo) -> new PathParamItem(fieldInfo.name(), annotationValue,
                            fieldInfo.type().name().toString(), fieldInfo.hasDeclaredAnnotation(ENCODED),
                            new FieldExtractor(null, fieldInfo.name(), fieldInfo.declaringClass().name().toString())),
                    (annotationValue, getterMethod) -> new PathParamItem(getterMethod.name(), annotationValue,
                            getterMethod.returnType().name().toString(), getterMethod.hasDeclaredAnnotation(ENCODED),
                            new GetterExtractor(getterMethod))));

            resultList.addAll(paramItemsForFieldsAndMethods(beanParamClass, REST_PATH_PARAM,
                    (annotationValue, fieldInfo) -> new PathParamItem(fieldInfo.name(),
                            annotationValue != null ? annotationValue : fieldInfo.name(), fieldInfo.type().name().toString(),
                            fieldInfo.hasDeclaredAnnotation(ENCODED),
                            new FieldExtractor(null, fieldInfo.name(), fieldInfo.declaringClass().name().toString())),
                    (annotationValue, getterMethod) -> new PathParamItem(getterMethod.name(),
                            annotationValue != null ? annotationValue : getterName(getterMethod),
                            getterMethod.returnType().name().toString(), getterMethod.hasDeclaredAnnotation(ENCODED),
                            new GetterExtractor(getterMethod))));

            resultList.addAll(paramItemsForFieldsAndMethods(beanParamClass, FORM_PARAM,
                    (annotationValue, fieldInfo) -> new FormParamItem(fieldInfo.name(), annotationValue,
                            fieldInfo.type(), AsmUtil.getSignature(fieldInfo.type()),
                            fieldInfo.name(),
                            partType(fieldInfo), fileName(fieldInfo), fieldInfo.hasDeclaredAnnotation(ENCODED),
                            new FieldExtractor(null, fieldInfo.name(), fieldInfo.declaringClass().name().toString())),
                    (annotationValue, getterMethod) -> new FormParamItem(getterMethod.name(), annotationValue,
                            getterMethod.returnType(),
                            AsmUtil.getSignature(getterMethod.returnType()),
                            getterMethod.name(),
                            partType(getterMethod), fileName(getterMethod), getterMethod.hasDeclaredAnnotation(ENCODED),
                            new GetterExtractor(getterMethod))));

            resultList.addAll(paramItemsForFieldsAndMethods(beanParamClass, REST_FORM_PARAM,
                    (annotationValue, fieldInfo) -> new FormParamItem(fieldInfo.name(),
                            annotationValue != null ? annotationValue : fieldInfo.name(),
                            fieldInfo.type(), AsmUtil.getSignature(fieldInfo.type()),
                            fieldInfo.name(),
                            partType(fieldInfo), fileName(fieldInfo), fieldInfo.hasDeclaredAnnotation(ENCODED),
                            new FieldExtractor(null, fieldInfo.name(), fieldInfo.declaringClass().name().toString())),
                    (annotationValue, getterMethod) -> new FormParamItem(getterMethod.name(),
                            annotationValue != null ? annotationValue : getterName(getterMethod),
                            getterMethod.returnType(),
                            AsmUtil.getSignature(getterMethod.returnType()),
                            getterMethod.name(),
                            partType(getterMethod), fileName(getterMethod), getterMethod.hasDeclaredAnnotation(ENCODED),
                            new GetterExtractor(getterMethod))));

            return resultList;

        } finally {
            processedBeanParamClasses.remove(beanParamClass);
        }
    }

    private static String getterName(MethodInfo getterMethod) {
        return JavaBeanUtil.getPropertyNameFromGetter(getterMethod.name());
    }

    private static String partType(FieldInfo annotated) {
        return partType(annotated.annotation(ResteasyReactiveDotNames.PART_TYPE_NAME));
    }

    private static String partType(MethodInfo annotated) {
        return partType(annotated.annotation(ResteasyReactiveDotNames.PART_TYPE_NAME));
    }

    private static String partType(AnnotationInstance annotation) {
        if (annotation == null || annotation.value() == null)
            return null;
        String mimeType = annotation.value().asString();
        // nullify default value
        if (!mimeType.equals(MediaType.TEXT_PLAIN))
            return mimeType;
        return null;
    }

    private static String fileName(FieldInfo annotated) {
        return fileName(annotated.annotation(ResteasyReactiveDotNames.PART_FILE_NAME));
    }

    private static String fileName(MethodInfo annotated) {
        return fileName(annotated.annotation(ResteasyReactiveDotNames.PART_FILE_NAME));
    }

    private static String fileName(AnnotationInstance annotation) {
        if (annotation == null || annotation.value() == null)
            return null;
        return annotation.value().asString();
    }

    private static MethodInfo getGetterMethod(ClassInfo beanParamClass, MethodInfo methodInfo) {
        MethodInfo getter = null;
        if (methodInfo.parametersCount() > 0) { // should be setter
            // find the corresponding getter:
            String setterName = methodInfo.name();
            if (setterName.startsWith("set")) {
                getter = beanParamClass.method(setterName.replace("^set", "^get"));
            }
        } else if (methodInfo.name().startsWith("get")) {
            getter = methodInfo;
        }

        if (getter == null) {
            throw new IllegalArgumentException(
                    "No getter corresponding to " + methodInfo.declaringClass().name() + "#" + methodInfo.name() + " found");
        }
        return getter;
    }

    private static <T extends Item> List<T> paramItemsForFieldsAndMethods(ClassInfo beanParamClass, DotName parameterType,
            BiFunction<String, FieldInfo, T> fieldExtractor, BiFunction<String, MethodInfo, T> methodExtractor) {
        return ParamTypeAnnotations.of(beanParamClass, parameterType).itemsForFieldsAndMethods(fieldExtractor, methodExtractor);
    }

    private BeanParamParser() {
    }

    private static class ParamTypeAnnotations {
        private final ClassInfo beanParamClass;
        private final List<AnnotationInstance> annotations;

        private ParamTypeAnnotations(ClassInfo beanParamClass, DotName parameterType) {
            this.beanParamClass = beanParamClass;

            List<AnnotationInstance> relevantAnnotations = beanParamClass.annotationsMap().get(parameterType);
            this.annotations = relevantAnnotations == null
                    ? Collections.emptyList()
                    : relevantAnnotations.stream().filter(this::isFieldOrMethodAnnotation).collect(Collectors.toList());
        }

        private static ParamTypeAnnotations of(ClassInfo beanParamClass, DotName parameterType) {
            return new ParamTypeAnnotations(beanParamClass, parameterType);
        }

        private <T extends Item> List<T> itemsForFieldsAndMethods(BiFunction<String, FieldInfo, T> itemFromFieldExtractor,
                BiFunction<String, MethodInfo, T> itemFromMethodExtractor) {
            return annotations.stream()
                    .map(annotation -> toItem(annotation, itemFromFieldExtractor, itemFromMethodExtractor))
                    .collect(Collectors.toList());
        }

        private <T extends Item> T toItem(AnnotationInstance annotation,
                BiFunction<String, FieldInfo, T> itemFromFieldExtractor,
                BiFunction<String, MethodInfo, T> itemFromMethodExtractor) {
            String annotationValue = annotation.value() == null ? null : annotation.value().asString();

            return annotation.target().kind() == AnnotationTarget.Kind.FIELD
                    ? itemFromFieldExtractor.apply(annotationValue, annotation.target().asField())
                    : itemFromMethodExtractor.apply(annotationValue,
                            getGetterMethod(beanParamClass, annotation.target().asMethod()));
        }

        private boolean isFieldOrMethodAnnotation(AnnotationInstance annotation) {
            return annotation.target().kind() == AnnotationTarget.Kind.FIELD
                    || annotation.target().kind() == AnnotationTarget.Kind.METHOD;
        }
    }
}
