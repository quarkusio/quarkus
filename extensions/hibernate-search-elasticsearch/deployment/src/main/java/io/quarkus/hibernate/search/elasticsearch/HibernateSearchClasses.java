package io.quarkus.hibernate.search.elasticsearch;

import java.util.Arrays;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.analysis.model.esnative.impl.AbstractCompositeAnalysisDefinition;
import org.hibernate.search.backend.elasticsearch.analysis.model.esnative.impl.AnalysisDefinition;
import org.hibernate.search.backend.elasticsearch.analysis.model.esnative.impl.AnalysisDefinitionJsonAdapterFactory;
import org.hibernate.search.backend.elasticsearch.analysis.model.esnative.impl.AnalyzerDefinition;
import org.hibernate.search.backend.elasticsearch.analysis.model.esnative.impl.AnalyzerDefinitionJsonAdapterFactory;
import org.hibernate.search.backend.elasticsearch.analysis.model.esnative.impl.CharFilterDefinition;
import org.hibernate.search.backend.elasticsearch.analysis.model.esnative.impl.NormalizerDefinition;
import org.hibernate.search.backend.elasticsearch.analysis.model.esnative.impl.NormalizerDefinitionJsonAdapterFactory;
import org.hibernate.search.backend.elasticsearch.analysis.model.esnative.impl.TokenFilterDefinition;
import org.hibernate.search.backend.elasticsearch.analysis.model.esnative.impl.TokenizerDefinition;
import org.hibernate.search.backend.elasticsearch.document.model.esnative.impl.AbstractTypeMapping;
import org.hibernate.search.backend.elasticsearch.document.model.esnative.impl.AbstractTypeMappingJsonAdapterFactory;
import org.hibernate.search.backend.elasticsearch.document.model.esnative.impl.DynamicType;
import org.hibernate.search.backend.elasticsearch.document.model.esnative.impl.ElasticsearchFormatJsonAdapter;
import org.hibernate.search.backend.elasticsearch.document.model.esnative.impl.ElasticsearchRoutingTypeJsonAdapter;
import org.hibernate.search.backend.elasticsearch.document.model.esnative.impl.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.document.model.esnative.impl.PropertyMappingJsonAdapterFactory;
import org.hibernate.search.backend.elasticsearch.document.model.esnative.impl.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.document.model.esnative.impl.RootTypeMappingJsonAdapterFactory;
import org.hibernate.search.backend.elasticsearch.document.model.esnative.impl.RoutingType;
import org.hibernate.search.backend.elasticsearch.index.settings.esnative.impl.Analysis;
import org.hibernate.search.backend.elasticsearch.index.settings.esnative.impl.IndexSettings;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration.MarkerBinding;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration.PropertyBinding;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration.RoutingKeyBinding;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration.TypeBinding;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ScaledNumberField;
import org.jboss.jandex.DotName;

class HibernateSearchClasses {

    static final DotName INDEXED = DotName.createSimple(Indexed.class.getName());

    static final List<DotName> FIELD_ANNOTATIONS = Arrays.asList(
            DotName.createSimple(DocumentId.class.getName()),
            DotName.createSimple(GenericField.class.getName()),
            DotName.createSimple(FullTextField.class.getName()),
            DotName.createSimple(KeywordField.class.getName()),
            DotName.createSimple(ScaledNumberField.class.getName()),
            DotName.createSimple(IndexedEmbedded.class.getName()),
            DotName.createSimple(AssociationInverseSide.class.getName()),
            DotName.createSimple(IndexingDependency.class.getName()));

    static final List<DotName> BINDING_DECLARATION_ANNOTATIONS_ON_PROPERTIES = Arrays.asList(
            DotName.createSimple(PropertyBinding.class.getName()),
            DotName.createSimple(MarkerBinding.class.getName()));

    static final List<DotName> BINDING_DECLARATION_ANNOTATIONS_ON_TYPES = Arrays.asList(
            DotName.createSimple(TypeBinding.class.getName()),
            DotName.createSimple(RoutingKeyBinding.class.getName()));

    static final List<DotName> SCHEMA_MAPPING_CLASSES = Arrays.asList(
            DotName.createSimple(AbstractTypeMapping.class.getName()),
            DotName.createSimple(AbstractTypeMappingJsonAdapterFactory.class.getName()),
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
}
