package io.quarkus.micrometer.deployment.binder.mpmetrics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jboss.jandex.*;
import org.jboss.logging.Logger;

import io.quarkus.arc.processor.DotNames;

public class MetricAnnotationInfo {
    private static final Logger log = Logger.getLogger(MetricAnnotationInfo.class);

    List<AnnotationValue> output = new ArrayList<>();

    String name;
    String description;
    String unit;
    String[] tags;

    MetricAnnotationInfo(AnnotationInstance input, IndexView index, ClassInfo classInfo, MethodInfo method,
            FieldInfo fieldInfo) {
        output.add(input.valueWithDefault(index, "displayName"));

        // Remember the unit
        AnnotationValue value = input.value("unit");
        if (value != null) {
            output.add(value);
            if (!"none".equals(value.asString())) {
                unit = value.asString();
            }
        }

        // Remember absolute
        value = input.valueWithDefault(index, "absolute");
        output.add(value);
        boolean absolute = value.asBoolean();

        // Assign a name. Start with the name in the annotation...
        name = input.valueWithDefault(index, "name").asString();
        if (input.target().kind() == AnnotationTarget.Kind.FIELD) {
            String fieldName = fieldInfo.name();
            if (absolute) {
                name = name.isEmpty() ? fieldName : name;
            } else {
                name = append(classInfo.name().toString(), name.isEmpty() ? fieldName : name);
            }
        }
        if (input.target().kind() == AnnotationTarget.Kind.METHOD) {
            String methodName = method.name().replace("<init>", classInfo.simpleName());
            if (absolute) {
                name = name.isEmpty() ? methodName : name;
            } else {
                name = append(classInfo.name().toString(), name.isEmpty() ? methodName : name);
            }
        }
        if (input.target().kind() == AnnotationTarget.Kind.CLASS) {
            String methodName = method == null ? "<method>" : method.name();
            if (absolute) {
                name = append(name.isEmpty() ? classInfo.simpleName() : name, methodName);
            } else {
                DotName className = classInfo.name();
                if (name.isEmpty()) {
                    name = append(className.toString(), methodName);
                } else {
                    name = append(DotNames.packageName(className), name, methodName);
                }
            }
        }

        output.add(AnnotationValue.createStringValue("name", name));

        description = input.valueWithDefault(index, "description").asString();
        output.add(AnnotationValue.createStringValue("description", description));

        tags = createTags(input, index);
        AnnotationValue[] tagValues = new AnnotationValue[tags.length];
        for (int i = 0; i < tags.length; i++) {
            tagValues[i] = AnnotationValue.createStringValue("tags", tags[i]);
        }
        output.add(AnnotationValue.createArrayValue("tags", tagValues));

        log.debugf("%s --> name='%s', description='%s', unit='%s', tags='%s'",
                input, name, description,
                unit == null ? "none" : unit,
                Arrays.asList(tags));
    }

    static String append(String... values) {
        StringBuilder b = new StringBuilder();
        for (String s : values) {
            if (b.length() > 0 && !s.isEmpty()) {
                b.append('.');
            }
            b.append(s);
        }
        return b.toString();
    }

    static String[] createTags(AnnotationInstance annotation, IndexView index) {
        List<String> tags = new ArrayList<>();
        tags.add("scope");
        tags.add("application");

        for (String s : annotation.valueWithDefault(index, "tags").asStringArray()) {
            // separate key=value strings into two parts
            int pos = s.indexOf('=');
            if (pos > 0 && s.length() > 2) {
                tags.add(s.substring(0, pos));
                tags.add(s.substring(pos + 1));
            } else {
                tags.add(s);
            }
        }
        if (tags.size() % 2 == 1) {
            log.warnf("Problem parsing tag values from %s", annotation);
        }
        return tags.toArray(new String[tags.size()]);
    }

    public AnnotationValue[] getAnnotationValues() {
        return output.toArray(new AnnotationValue[0]);
    }
}
