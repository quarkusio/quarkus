import { str } from '@lit/localize';

export const templates = {
    'quarkus-hibernate-search-orm-elasticsearch-meta-description': 'Indexa automáticamente tus entidades de Hibernate en Elasticsearch.',
    'quarkus-hibernate-search-orm-elasticsearch-indexed_entity_types': 'Tipos de entidades indexadas',
    'quarkus-hibernate-search-orm-elasticsearch-selected-entity-types': str`Tipos de entidad ${0} seleccionados`,
    'quarkus-hibernate-search-orm-elasticsearch-select-entity-types-to-reindex': str`Selecciona los tipos de entidad para reindexar para la unidad de persistencia '${0}'.`,
    'quarkus-hibernate-search-orm-elasticsearch-reindex-started': str`Se solicitó la reindexación de los tipos de entidad ${0} para la unidad de persistencia '${1}'.`,
    'quarkus-hibernate-search-orm-elasticsearch-reindex-success': str`Se reindexaron exitosamente ${0} tipos de entidad para la unidad de persistencia '${1}'.`,
    'quarkus-hibernate-search-orm-elasticsearch-reindex-error': str`Ocurrió un error al reindexar los tipos de entidad ${0} para la unidad de persistencia '${1}':\n${2}`,
};
