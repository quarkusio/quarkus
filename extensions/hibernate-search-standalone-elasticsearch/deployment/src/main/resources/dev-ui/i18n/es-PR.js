import { str } from '@lit/localize';

export const templates = {
    'quarkus-hibernate-search-standalone-elasticsearch-class-name': 'Nombre de la clase',
    'quarkus-hibernate-search-standalone-elasticsearch-select-entity-types': 'Selecciona los tipos de entidad para reindexar.',
    'quarkus-hibernate-search-standalone-elasticsearch-reindex-started': str`Reindexación solicitada de ${0} tipos de entidad.`,
    'quarkus-hibernate-search-standalone-elasticsearch-reindex-success': str`Se reindexaron exitosamente ${0} tipos de entidad.`,
    'quarkus-hibernate-search-standalone-elasticsearch-reindex-error': str`Ocurrió un error al reindexar tipos de entidad ${0}:\n${1}`,
};
