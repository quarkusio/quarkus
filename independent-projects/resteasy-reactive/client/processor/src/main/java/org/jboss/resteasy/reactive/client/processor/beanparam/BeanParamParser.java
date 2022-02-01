package org.jboss.resteasy.reactive.client.processor.beanparam;

import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.BEAN_PARAM;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.COOKIE_PARAM;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.FORM_PARAM;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.HEADER_PARAM;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.PATH_PARAM;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.QUERY_PARAM;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.resteasy.reactive.common.processor.JandexUtil;

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
                    (annotationValue, fieldInfo) -> new QueryParamItem(annotationValue,
                            new FieldExtractor(null, fieldInfo.name(), fieldInfo.declaringClass().name().toString()),
                            fieldInfo.type()),
                    (annotationValue, getterMethod) -> new QueryParamItem(annotationValue, new GetterExtractor(getterMethod),
                            getterMethod.returnType())));

            resultList.addAll(paramItemsForFieldsAndMethods(beanParamClass, BEAN_PARAM,
                    (annotationValue, fieldInfo) -> {
                        Type type = fieldInfo.type();
                        if (type.kind() == Type.Kind.CLASS) {
                            List<Item> subBeanParamItems = parseInternal(index.getClassByName(type.asClassType().name()), index,
                                    processedBeanParamClasses);
                            return new BeanParamItem(subBeanParamItems,
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
                        return new BeanParamItem(items, new GetterExtractor(getterMethod));
                    }));

            resultList.addAll(paramItemsForFieldsAndMethods(beanParamClass, COOKIE_PARAM,
                    (annotationValue, fieldInfo) -> new CookieParamItem(annotationValue,
                            new FieldExtractor(null, fieldInfo.name(),
                                    fieldInfo.declaringClass().name().toString()),
                            fieldInfo.type().name().toString()),
                    (annotationValue, getterMethod) -> new CookieParamItem(annotationValue,
                            new GetterExtractor(getterMethod), getterMethod.returnType().name().toString())));

            resultList.addAll(paramItemsForFieldsAndMethods(beanParamClass, HEADER_PARAM,
                    (annotationValue, fieldInfo) -> new HeaderParamItem(annotationValue,
                            new FieldExtractor(null, fieldInfo.name(), fieldInfo.declaringClass().name().toString()),
                            fieldInfo.type().name().toString()),
                    (annotationValue, getterMethod) -> new HeaderParamItem(annotationValue,
                            new GetterExtractor(getterMethod), getterMethod.returnType().name().toString())));

            resultList.addAll(paramItemsForFieldsAndMethods(beanParamClass, PATH_PARAM,
                    (annotationValue, fieldInfo) -> new PathParamItem(annotationValue, fieldInfo.type().name().toString(),
                            new FieldExtractor(null, fieldInfo.name(), fieldInfo.declaringClass().name().toString())),
                    (annotationValue, getterMethod) -> new PathParamItem(annotationValue,
                            getterMethod.returnType().name().toString(),
                            new GetterExtractor(getterMethod))));

            resultList.addAll(paramItemsForFieldsAndMethods(beanParamClass, FORM_PARAM,
                    (annotationValue, fieldInfo) -> new FormParamItem(annotationValue,
                            fieldInfo.type().name().toString(),
                            new FieldExtractor(null, fieldInfo.name(), fieldInfo.declaringClass().name().toString())),
                    (annotationValue, getterMethod) -> new FormParamItem(annotationValue,
                            getterMethod.returnType().name().toString(),
                            new GetterExtractor(getterMethod))));

            return resultList;

        } finally {
            processedBeanParamClasses.remove(beanParamClass);
        }
    }

    private static MethodInfo getGetterMethod(ClassInfo beanParamClass, MethodInfo methodInfo) {
        MethodInfo getter = null;
        if (methodInfo.parameters().size() > 0) { // should be setter
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

            List<AnnotationInstance> relevantAnnotations = beanParamClass.annotations().get(parameterType);
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
