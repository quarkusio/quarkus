package io.quarkus.qute.deployment;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

import org.jboss.jandex.DotName;

import io.quarkus.qute.EngineConfiguration;
import io.quarkus.qute.Locate;
import io.quarkus.qute.Locate.Locates;
import io.quarkus.qute.Location;
import io.quarkus.qute.NamespaceResolver;
import io.quarkus.qute.ParserHook;
import io.quarkus.qute.SectionHelperFactory;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateContents;
import io.quarkus.qute.TemplateEnum;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.TemplateLocator;
import io.quarkus.qute.ValueResolver;
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
    static final DotName TEMPLATE_LOCATOR = DotName.createSimple(TemplateLocator.class.getName());
    static final DotName COMPLETION_STAGE = DotName.createSimple(CompletionStage.class.getName());
    static final DotName UNI = DotName.createSimple(Uni.class.getName());
    static final DotName LOCATION = DotName.createSimple(Location.class.getName());
    static final DotName LOCATE = DotName.createSimple(Locate.class.getName());
    static final DotName LOCATES = DotName.createSimple(Locates.class.getName());
    static final DotName CHECKED_TEMPLATE = DotName.createSimple(io.quarkus.qute.CheckedTemplate.class.getName());
    static final DotName TEMPLATE_ENUM = DotName.createSimple(TemplateEnum.class.getName());
    static final DotName ENGINE_CONFIGURATION = DotName.createSimple(EngineConfiguration.class.getName());
    static final DotName SECTION_HELPER_FACTORY = DotName.createSimple(SectionHelperFactory.class.getName());
    static final DotName VALUE_RESOLVER = DotName.createSimple(ValueResolver.class.getName());
    static final DotName NAMESPACE_RESOLVER = DotName.createSimple(NamespaceResolver.class.getName());
    static final DotName PARSER_HOOK = DotName.createSimple(ParserHook.class);
    static final DotName TEMPLATE_CONTENTS = DotName.createSimple(TemplateContents.class);

    private Names() {
    }

}
