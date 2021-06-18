package org.jboss.resteasy.reactive.client.processor.beanparam;

import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.BEAN_PARAM;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.COOKIE_PARAM;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.HEADER_PARAM;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.QUERY_PARAM;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

public class BeanParamParser {
    public static List<Item> parse(ClassInfo beanParamClass, IndexView index) {
        List<Item> resultList = new ArrayList<>();
        Map<DotName, List<AnnotationInstance>> annotations = beanParamClass.annotations();
        List<AnnotationInstance> queryParams = annotations.get(QUERY_PARAM);
        if (queryParams != null) {
            for (AnnotationInstance annotation : queryParams) {
                AnnotationTarget target = annotation.target();
                if (target.kind() == AnnotationTarget.Kind.FIELD) {
                    FieldInfo fieldInfo = target.asField();
                    resultList.add(new QueryParamItem(annotation.value().asString(),
                            new FieldExtractor(null, fieldInfo.name(), fieldInfo.declaringClass().name().toString()),
                            fieldInfo.type()));
                } else if (target.kind() == AnnotationTarget.Kind.METHOD) {
                    MethodInfo getterMethod = getGetterMethod(beanParamClass, target.asMethod());
                    resultList.add(new QueryParamItem(annotation.value().asString(),
                            new GetterExtractor(getterMethod), getterMethod.returnType()));
                }
            }
        }
        List<AnnotationInstance> beanParams = annotations.get(BEAN_PARAM);
        if (beanParams != null) {
            for (AnnotationInstance annotation : beanParams) {
                AnnotationTarget target = annotation.target();
                if (target.kind() == AnnotationTarget.Kind.FIELD) {
                    FieldInfo fieldInfo = target.asField();
                    Type type = fieldInfo.type();
                    if (type.kind() == Type.Kind.CLASS) {
                        List<Item> subBeanParamItems = parse(index.getClassByName(type.asClassType().name()), index);
                        resultList.add(new BeanParamItem(subBeanParamItems,
                                new FieldExtractor(null, fieldInfo.name(), fieldInfo.declaringClass().name().toString())));
                    } else {
                        throw new IllegalArgumentException("BeanParam annotation used on a field that is not an object: "
                                + beanParamClass.name() + "." + fieldInfo.name());
                    }
                } else if (target.kind() == AnnotationTarget.Kind.METHOD) {
                    // this should be getter or setter
                    MethodInfo methodInfo = target.asMethod();
                    MethodInfo getter = getGetterMethod(beanParamClass, methodInfo);
                    Type returnType = getter.returnType();
                    List<Item> items = parse(index.getClassByName(returnType.name()), index);
                    resultList.add(new BeanParamItem(items, new GetterExtractor(getter)));
                }
            }
        }

        List<AnnotationInstance> cookieParams = annotations.get(COOKIE_PARAM);
        if (cookieParams != null) {
            for (AnnotationInstance annotation : cookieParams) {
                AnnotationTarget target = annotation.target();
                if (target.kind() == AnnotationTarget.Kind.FIELD) {
                    FieldInfo fieldInfo = target.asField();
                    resultList.add(new CookieParamItem(annotation.value().asString(),
                            new FieldExtractor(null, fieldInfo.name(), fieldInfo.declaringClass().name().toString())));
                } else if (target.kind() == AnnotationTarget.Kind.METHOD) {
                    MethodInfo getterMethod = getGetterMethod(beanParamClass, target.asMethod());
                    resultList.add(new CookieParamItem(annotation.value().asString(),
                            new GetterExtractor(getterMethod)));
                }
            }
        }

        List<AnnotationInstance> headerParams = annotations.get(HEADER_PARAM);
        if (headerParams != null) {
            for (AnnotationInstance queryParamAnnotation : headerParams) {
                AnnotationTarget target = queryParamAnnotation.target();
                if (target.kind() == AnnotationTarget.Kind.FIELD) {
                    FieldInfo fieldInfo = target.asField();
                    resultList.add(new HeaderParamItem(queryParamAnnotation.value().asString(),
                            new FieldExtractor(null, fieldInfo.name(), fieldInfo.declaringClass().name().toString())));
                } else if (target.kind() == AnnotationTarget.Kind.METHOD) {
                    MethodInfo getterMethod = getGetterMethod(beanParamClass, target.asMethod());
                    resultList.add(new HeaderParamItem(queryParamAnnotation.value().asString(),
                            new GetterExtractor(getterMethod)));
                }
            }
        }

        return resultList;
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

    private BeanParamParser() {
    }
}
