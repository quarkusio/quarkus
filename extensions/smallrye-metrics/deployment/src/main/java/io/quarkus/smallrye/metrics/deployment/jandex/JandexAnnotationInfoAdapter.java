package io.quarkus.smallrye.metrics.deployment.jandex;

import org.eclipse.microprofile.metrics.annotation.Gauge;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.smallrye.metrics.elementdesc.AnnotationInfo;
import io.smallrye.metrics.elementdesc.RawAnnotationInfo;
import io.smallrye.metrics.elementdesc.adapter.AnnotationInfoAdapter;

public class JandexAnnotationInfoAdapter implements AnnotationInfoAdapter<AnnotationInstance> {

    private final IndexView indexView;

    public JandexAnnotationInfoAdapter(IndexView indexView) {
        this.indexView = indexView;
    }

    @Override
    public AnnotationInfo convert(AnnotationInstance input) {
        boolean reusable = !input.name().equals(DotName.createSimple(Gauge.class.getName()))
                && input.valueWithDefault(indexView, "reusable").asBoolean();
        return new RawAnnotationInfo(
                input.valueWithDefault(indexView, "name").asString(),
                input.valueWithDefault(indexView, "absolute").asBoolean(),
                input.valueWithDefault(indexView, "tags").asStringArray(),
                input.valueWithDefault(indexView, "unit").asString(),
                input.valueWithDefault(indexView, "description").asString(),
                input.valueWithDefault(indexView, "displayName").asString(),
                reusable,
                input.name().toString());
    }

}
