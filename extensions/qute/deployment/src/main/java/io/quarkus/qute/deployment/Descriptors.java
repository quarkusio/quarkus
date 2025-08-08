package io.quarkus.qute.deployment;

import io.quarkus.gizmo2.desc.MethodDesc;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.i18n.MessageBundles;

final class Descriptors {

    static final MethodDesc TEMPLATE_INSTANCE = MethodDesc.of(Template.class, "instance",
            TemplateInstance.class);
    static final MethodDesc TEMPLATE_INSTANCE_DATA = MethodDesc.of(TemplateInstance.class, "data",
            TemplateInstance.class, String.class, Object.class);
    static final MethodDesc TEMPLATE_INSTANCE_RENDER = MethodDesc.of(TemplateInstance.class, "render",
            String.class);
    static final MethodDesc BUNDLES_GET_TEMPLATE = MethodDesc.of(MessageBundles.class, "getTemplate",
            Template.class, String.class);

}
