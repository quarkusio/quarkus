import { str } from '@lit/localize';

export const templates = {
    'quarkus-hibernate-search-standalone-elasticsearch-meta-description': 'Indexar/buscar entidades explícitamente en Elasticsearch',
    'quarkus-hibernate-search-standalone-elasticsearch-indexed_entity_types': 'Tipos de Entidades Indexadas',
    'quarkus-hibernate-search-standalone-elasticsearch-loading': 'Cargando...',
    'quarkus-hibernate-search-standalone-elasticsearch-no-indexed-entities': 'No se encontraron entidades indexadas.',
    'quarkus-hibernate-search-standalone-elasticsearch-reindex-selected': 'Reindexar seleccionado',
    'quarkus-hibernate-search-standalone-elasticsearch-entity-name': 'Nombre de la entidad',
    'quarkus-hibernate-search-standalone-elasticsearch-class-name': 'Nombre de clase',
    'quarkus-hibernate-search-standalone-elasticsearch-index-names': 'Nombres de índices',
    'quarkus-hibernate-search-standalone-elasticsearch-select-entity-types': 'Seleccione los tipos de entidad para reindexar.',
    'quarkus-hibernate-search-standalone-elasticsearch-selected-entity-types': str`Tipo de entidad ${0} seleccionado${1}`,
    'quarkus-hibernate-search-standalone-elasticsearch-reindex-started': str`Se solicitó la reindexación de ${0} tipos de entidades.`,
    'quarkus-hibernate-search-standalone-elasticsearch-reindex-success': str`Se volvió a indexar con éxito ${0} tipos de entidades.`,
    'quarkus-hibernate-search-standalone-elasticsearch-reindex-error': str`Se produjo un error al reindexar los tipos de entidad ${0}:\n${1}`,
};
