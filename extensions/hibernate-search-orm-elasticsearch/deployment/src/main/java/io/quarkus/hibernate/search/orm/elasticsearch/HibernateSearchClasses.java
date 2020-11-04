package io.quarkus.hibernate.search.orm.elasticsearch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.aliases.impl.IndexAliasDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.aliases.impl.IndexAliasDefinitionJsonAdapterFactory;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.AnalysisDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.AnalysisDefinitionJsonAdapterFactory;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.AnalyzerDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.AnalyzerDefinitionJsonAdapterFactory;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.CharFilterDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.NormalizerDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.NormalizerDefinitionJsonAdapterFactory;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.TokenFilterDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.TokenizerDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.AbstractTypeMapping;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DynamicTemplate;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DynamicTemplateJsonAdapterFactory;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DynamicType;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.FormatJsonAdapter;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.NamedDynamicTemplate;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.NamedDynamicTemplateJsonAdapterFactory;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMappingJsonAdapterFactory;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RootTypeMappingJsonAdapterFactory;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RoutingType;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RoutingTypeJsonAdapter;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl.Analysis;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl.IndexSettings;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMapping;
import org.jboss.jandex.DotName;

class HibernateSearchClasses {

    static final DotName INDEXED = DotName.createSimple(Indexed.class.getName());

    static final DotName PROPERTY_MAPPING_META_ANNOTATION = DotName.createSimple(
            org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMapping.class.getName());
    static final DotName TYPE_MAPPING_META_ANNOTATION = DotName.createSimple(TypeMapping.class.getName());

    static final List<DotName> GSON_CLASSES = new ArrayList<>();
    static {
        List<Class<?>> publicGsonClasses = Arrays.asList(
                AbstractTypeMapping.class,
                DynamicType.class,
                FormatJsonAdapter.class,
                RoutingTypeJsonAdapter.class,
                PropertyMapping.class,
                PropertyMappingJsonAdapterFactory.class,
                RootTypeMapping.class,
                RootTypeMappingJsonAdapterFactory.class,
                RoutingType.class,
                IndexSettings.class,
                Analysis.class,
                AnalysisDefinition.class,
                AnalyzerDefinition.class,
                AnalyzerDefinitionJsonAdapterFactory.class,
                NormalizerDefinition.class,
                NormalizerDefinitionJsonAdapterFactory.class,
                TokenizerDefinition.class,
                TokenFilterDefinition.class,
                CharFilterDefinition.class,
                AnalysisDefinitionJsonAdapterFactory.class,
                IndexAliasDefinition.class,
                IndexAliasDefinitionJsonAdapterFactory.class,
                DynamicTemplate.class,
                DynamicTemplateJsonAdapterFactory.class,
                NamedDynamicTemplate.class,
                NamedDynamicTemplateJsonAdapterFactory.class);
        for (Class<?> publicGsonClass : publicGsonClasses) {
            Class<?> currentClass = publicGsonClass;
            while (currentClass != Object.class) {
                GSON_CLASSES.add(DotName.createSimple(currentClass.getName()));
                currentClass = currentClass.getSuperclass();
            }
        }
    }
}
