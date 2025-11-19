// This is the default
import { str } from '@lit/localize';

export const templates = {
    // Metadata
    'quarkus-hibernate-search-orm-elasticsearch-meta-description':'Automatically index your Hibernate entities in Elasticsearch',
    // Pages
    'quarkus-hibernate-search-orm-elasticsearch-indexed_entity_types':'Indexed Entity Types',
    // General
    'quarkus-hibernate-search-orm-elasticsearch-loading': 'Loading...',
    'quarkus-hibernate-search-orm-elasticsearch-no-persistence-units':
        'No persistence units were found.',
    'quarkus-hibernate-search-orm-elasticsearch-persistence-unit':
        'Persistence Unit',
    'quarkus-hibernate-search-orm-elasticsearch-no-indexed-entities':
        'No indexed entities were found.',
    'quarkus-hibernate-search-orm-elasticsearch-selected-entity-types':
        str`Selected ${0} entity types`,
    'quarkus-hibernate-search-orm-elasticsearch-reindex-selected':
        'Reindex selected',
    'quarkus-hibernate-search-orm-elasticsearch-entity-name':
        'Entity name',
    'quarkus-hibernate-search-orm-elasticsearch-class-name':
        'Class name',
    'quarkus-hibernate-search-orm-elasticsearch-index-names':
        'Index names',
    'quarkus-hibernate-search-orm-elasticsearch-select-entity-types-to-reindex':
        str`Select entity types to reindex for persistence unit '${0}'.`,
    'quarkus-hibernate-search-orm-elasticsearch-reindex-started':
        str`Requested reindexing of ${0} entity types for persistence unit '${1}'.`,
    'quarkus-hibernate-search-orm-elasticsearch-reindex-success':
        str`Successfully reindexed ${0} entity types for persistence unit '${1}'.`,
    'quarkus-hibernate-search-orm-elasticsearch-reindex-error':
        str`An error occurred while reindexing ${0} entity types for persistence unit '${1}':\n${2}`
};
