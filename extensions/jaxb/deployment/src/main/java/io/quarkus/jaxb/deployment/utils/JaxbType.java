package io.quarkus.jaxb.deployment.utils;

import java.util.Locale;
import java.util.Set;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

public class JaxbType {

    private static final String DEFAULT_JAXB_ANNOTATION_VALUE = "##default";

    private final String modelName;
    private final Class<?> clazz;

    public JaxbType(Class<?> clazz) {
        this.modelName = findModelNameFromType(clazz);
        this.clazz = clazz;
    }

    public String getModelName() {
        return modelName;
    }

    public Class<?> getType() {
        return clazz;
    }

    private String findModelNameFromType(Class<?> clazz) {
        String nameFromAnnotation = DEFAULT_JAXB_ANNOTATION_VALUE;
        String namespaceFromAnnotation = DEFAULT_JAXB_ANNOTATION_VALUE;
        XmlType xmlType = clazz.getAnnotation(XmlType.class);
        if (xmlType != null) {
            nameFromAnnotation = xmlType.name();
            namespaceFromAnnotation = xmlType.namespace();
        } else {
            XmlRootElement rootElement = clazz.getAnnotation(XmlRootElement.class);
            if (rootElement != null) {
                nameFromAnnotation = rootElement.name();
                namespaceFromAnnotation = rootElement.namespace();
            }
        }

        String modelName = nameFromAnnotation;
        if (DEFAULT_JAXB_ANNOTATION_VALUE.equals(nameFromAnnotation)) {
            modelName = clazz.getSimpleName().toLowerCase(Locale.ROOT);
        }

        if (!DEFAULT_JAXB_ANNOTATION_VALUE.equals(namespaceFromAnnotation)) {
            modelName += "." + namespaceFromAnnotation;
        }

        return modelName;
    }

    public static boolean isValidType(Class<?> clazz) {
        return clazz != null && !clazz.isPrimitive() && !clazz.isArray();
    }

    public static JaxbType findExistingType(Set<JaxbType> dictionary, JaxbType jaxbType) {
        for (JaxbType existing : dictionary) {
            if (existing.modelName.equals(jaxbType.modelName)) {
                return existing;
            }
        }
        return null;
    }
}
