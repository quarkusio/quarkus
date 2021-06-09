package io.quarkus.qute.deployment;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

import org.jboss.jandex.DotName;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.i18n.Localized;
import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;
import io.quarkus.qute.i18n.MessageParam;
import io.smallrye.mutiny.Uni;

final class Names {

    static final DotName BUNDLE = DotName.createSimple(MessageBundle.class.getName());
    static final DotName MESSAGE = DotName.createSimple(Message.class.getName());
    static final DotName MESSAGE_PARAM = DotName.createSimple(MessageParam.class.getName());
    static final DotName LOCALIZED = DotName.createSimple(Localized.class.getName());
    static final DotName TEMPLATE = DotName.createSimple(Template.class.getName());
    static final DotName ITERABLE = DotName.createSimple(Iterable.class.getName());
    static final DotName ITERATOR = DotName.createSimple(Iterator.class.getName());
    static final DotName STREAM = DotName.createSimple(Stream.class.getName());
    static final DotName MAP = DotName.createSimple(Map.class.getName());
    static final DotName MAP_ENTRY = DotName.createSimple(Entry.class.getName());
    static final DotName COLLECTION = DotName.createSimple(Collection.class.getName());
    static final DotName TEMPLATE_INSTANCE = DotName.createSimple(TemplateInstance.class.getName());
    static final DotName COMPLETION_STAGE = DotName.createSimple(CompletionStage.class.getName());
    static final DotName UNI = DotName.createSimple(Uni.class.getName());
    static final DotName LOCATION = DotName.createSimple(Location.class.getName());
    static final DotName CHECKED_TEMPLATE = DotName.createSimple(io.quarkus.qute.CheckedTemplate.class.getName());

    private Names() {
    }

}
