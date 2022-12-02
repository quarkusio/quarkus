package io.quarkus.qute.deployment;

import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.i18n.MessageBundles;

final class Descriptors {

    static final MethodDescriptor TEMPLATE_INSTANCE = MethodDescriptor.ofMethod(Template.class, "instance",
            TemplateInstance.class);
    static final MethodDescriptor TEMPLATE_INSTANCE_DATA = MethodDescriptor.ofMethod(TemplateInstance.class, "data",
            TemplateInstance.class, String.class, Object.class);
    static final MethodDescriptor TEMPLATE_INSTANCE_RENDER = MethodDescriptor.ofMethod(TemplateInstance.class, "render",
            String.class);
    static final MethodDescriptor BUNDLES_GET_TEMPLATE = MethodDescriptor.ofMethod(MessageBundles.class, "getTemplate",
            Template.class, String.class);

}
