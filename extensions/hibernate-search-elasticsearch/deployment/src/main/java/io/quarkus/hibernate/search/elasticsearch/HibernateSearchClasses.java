package io.quarkus.hibernate.search.elasticsearch;

import java.util.Arrays;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.analysis.model.impl.esnative.AbstractCompositeAnalysisDefinition;
import org.hibernate.search.backend.elasticsearch.analysis.model.impl.esnative.AnalysisDefinition;
import org.hibernate.search.backend.elasticsearch.analysis.model.impl.esnative.AnalysisDefinitionJsonAdapterFactory;
import org.hibernate.search.backend.elasticsearch.analysis.model.impl.esnative.AnalyzerDefinition;
import org.hibernate.search.backend.elasticsearch.analysis.model.impl.esnative.AnalyzerDefinitionJsonAdapterFactory;
import org.hibernate.search.backend.elasticsearch.analysis.model.impl.esnative.CharFilterDefinition;
import org.hibernate.search.backend.elasticsearch.analysis.model.impl.esnative.NormalizerDefinition;
import org.hibernate.search.backend.elasticsearch.analysis.model.impl.esnative.NormalizerDefinitionJsonAdapterFactory;
import org.hibernate.search.backend.elasticsearch.analysis.model.impl.esnative.TokenFilterDefinition;
import org.hibernate.search.backend.elasticsearch.analysis.model.impl.esnative.TokenizerDefinition;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.AbstractTypeMapping;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.AbstractTypeMappingJsonAdapterFactory;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.DataType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.DynamicType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.ElasticsearchFormatJsonAdapter;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.ElasticsearchRoutingTypeJsonAdapter;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMappingJsonAdapterFactory;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.RootTypeMappingJsonAdapterFactory;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.RoutingType;
import org.hibernate.search.backend.elasticsearch.index.settings.impl.esnative.Analysis;
import org.hibernate.search.backend.elasticsearch.index.settings.impl.esnative.IndexSettings;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration.PropertyBridgeMapping;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration.TypeBridgeMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.jboss.jandex.DotName;

class HibernateSearchClasses {

    static final List<DotName> FIELD_ANNOTATIONS = Arrays.asList(
            DotName.createSimple(DocumentId.class.getName()),
            DotName.createSimple(GenericField.class.getName()),
            DotName.createSimple(FullTextField.class.getName()),
            DotName.createSimple(KeywordField.class.getName()),
            DotName.createSimple(IndexedEmbedded.class.getName()));

    static final DotName PROPERTY_BRIDGE_DECLARATION_ANNOTATION = DotName
            .createSimple(PropertyBridgeMapping.class.getName());

    static final DotName TYPE_BRIDGE_DECLARATION_ANNOTATION = DotName
            .createSimple(TypeBridgeMapping.class.getName());

    static final List<DotName> SCHEMA_MAPPING_CLASSES = Arrays.asList(
            DotName.createSimple(AbstractTypeMapping.class.getName()),
            DotName.createSimple(AbstractTypeMappingJsonAdapterFactory.class.getName()),
            DotName.createSimple(DataType.class.getName()),
            DotName.createSimple(DynamicType.class.getName()),
            DotName.createSimple(ElasticsearchFormatJsonAdapter.class.getName()),
            DotName.createSimple(ElasticsearchRoutingTypeJsonAdapter.class.getName()),
            DotName.createSimple(PropertyMapping.class.getName()),
            DotName.createSimple(PropertyMappingJsonAdapterFactory.class.getName()),
            DotName.createSimple(RootTypeMapping.class.getName()),
            DotName.createSimple(RootTypeMappingJsonAdapterFactory.class.getName()),
            DotName.createSimple(RoutingType.class.getName()),
            DotName.createSimple(IndexSettings.class.getName()),
            DotName.createSimple(Analysis.class.getName()),
            DotName.createSimple(AnalysisDefinition.class.getName()),
            DotName.createSimple(AbstractCompositeAnalysisDefinition.class.getName()),
            DotName.createSimple(AnalyzerDefinition.class.getName()),
            DotName.createSimple(AnalyzerDefinitionJsonAdapterFactory.class.getName()),
            DotName.createSimple(NormalizerDefinition.class.getName()),
            DotName.createSimple(NormalizerDefinitionJsonAdapterFactory.class.getName()),
            DotName.createSimple(TokenizerDefinition.class.getName()),
            DotName.createSimple(TokenFilterDefinition.class.getName()),
            DotName.createSimple(CharFilterDefinition.class.getName()),
            DotName.createSimple(AnalysisDefinitionJsonAdapterFactory.class.getName()));

    static final DotName INDEXED = DotName.createSimple(Indexed.class.getName());
}
