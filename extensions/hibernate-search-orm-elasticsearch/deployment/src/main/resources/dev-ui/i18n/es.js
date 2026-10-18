import { str } from '@lit/localize';

export const templates = {
    'quarkus-hibernate-search-orm-elasticsearch-meta-description': 'Indexar automáticamente tus entidades de Hibernate en Elasticsearch',
    'quarkus-hibernate-search-orm-elasticsearch-indexed_entity_types': 'Tipos de Entidades Indexadas',
    'quarkus-hibernate-search-orm-elasticsearch-loading': 'Cargando...',
    'quarkus-hibernate-search-orm-elasticsearch-no-persistence-units': 'No se encontraron unidades de persistencia.',
    'quarkus-hibernate-search-orm-elasticsearch-persistence-unit': 'Unidad de Persistencia',
    'quarkus-hibernate-search-orm-elasticsearch-no-indexed-entities': 'No se encontraron entidades indexadas.',
    'quarkus-hibernate-search-orm-elasticsearch-reindex-selected': 'Reindexar seleccionado',
    'quarkus-hibernate-search-orm-elasticsearch-entity-name': 'Nombre de la entidad',
    'quarkus-hibernate-search-orm-elasticsearch-class-name': 'Nombre de la clase',
    'quarkus-hibernate-search-orm-elasticsearch-index-names': 'Nombres de índices',
    'quarkus-hibernate-search-orm-elasticsearch-selected-entity-types': str`Tipos de entidad seleccionados: ${0}`,
    'quarkus-hibernate-search-orm-elasticsearch-select-entity-types-to-reindex': str`Seleccione los tipos de entidad para reindexar para la unidad de persistencia '${0}'.`,
    'quarkus-hibernate-search-orm-elasticsearch-reindex-started': str`Solicitada la reindexación de ${0} tipos de entidad para la unidad de persistencia '${1}'.`,
    'quarkus-hibernate-search-orm-elasticsearch-reindex-success': str`Se han reindexado correctamente ${0} tipos de entidad para la unidad de persistencia '${1}'.`,
    'quarkus-hibernate-search-orm-elasticsearch-reindex-error': str`Se produjo un error al reindexar los tipos de entidad ${0} para la unidad de persistencia '${1}':\n${2}`,
};
