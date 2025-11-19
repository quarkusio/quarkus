// This is the default
import { str } from '@lit/localize';

export const templates = {
    // Metadata
    'quarkus-hibernate-search-standalone-elasticsearch-meta-description':'Explicitly index/search entities in Elasticsearch',
    // Pages
    'quarkus-hibernate-search-standalone-elasticsearch-indexed_entity_types':'Indexed Entity Types',
    // General
    'quarkus-hibernate-search-standalone-elasticsearch-loading': 'Loading...',
    'quarkus-hibernate-search-standalone-elasticsearch-no-indexed-entities': 'No indexed entities were found.',
    'quarkus-hibernate-search-standalone-elasticsearch-selected-entity-types': str`Selected ${0} entity type${1}`,
    'quarkus-hibernate-search-standalone-elasticsearch-reindex-selected': 'Reindex selected',
    'quarkus-hibernate-search-standalone-elasticsearch-entity-name': 'Entity name',
    'quarkus-hibernate-search-standalone-elasticsearch-class-name': 'Class name',
    'quarkus-hibernate-search-standalone-elasticsearch-index-names': 'Index names',
    'quarkus-hibernate-search-standalone-elasticsearch-select-entity-types': 'Select entity types to reindex.',
    'quarkus-hibernate-search-standalone-elasticsearch-reindex-started': str`Requested reindexing of ${0} entity types.`,
    'quarkus-hibernate-search-standalone-elasticsearch-reindex-success': str`Successfully reindexed ${0} entity types.`,
    'quarkus-hibernate-search-standalone-elasticsearch-reindex-error': str`An error occurred while reindexing ${0} entity types:\n${1}`
};

