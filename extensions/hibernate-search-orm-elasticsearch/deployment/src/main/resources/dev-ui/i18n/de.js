import { str } from '@lit/localize';

export const templates = {
    'quarkus-hibernate-search-orm-elasticsearch-meta-description': 'Indizieren Sie Ihre Hibernate-Entitäten automatisch in Elasticsearch.',
    'quarkus-hibernate-search-orm-elasticsearch-indexed_entity_types': 'Indizierte Entitätstypen',
    'quarkus-hibernate-search-orm-elasticsearch-loading': 'Laden...',
    'quarkus-hibernate-search-orm-elasticsearch-no-persistence-units': 'Es wurden keine Persistenzeinheiten gefunden.',
    'quarkus-hibernate-search-orm-elasticsearch-persistence-unit': 'Persistenzeinheit',
    'quarkus-hibernate-search-orm-elasticsearch-no-indexed-entities': 'Es wurden keine indexierten Entitäten gefunden.',
    'quarkus-hibernate-search-orm-elasticsearch-reindex-selected': 'Ausgewählte neu indizieren',
    'quarkus-hibernate-search-orm-elasticsearch-entity-name': 'Entitätsname',
    'quarkus-hibernate-search-orm-elasticsearch-class-name': 'Klassenname',
    'quarkus-hibernate-search-orm-elasticsearch-index-names': 'Indexnamen',
    'quarkus-hibernate-search-orm-elasticsearch-selected-entity-types': str`Ausgewählte ${0} Entitätstypen`,
    'quarkus-hibernate-search-orm-elasticsearch-select-entity-types-to-reindex': str`Wählen Sie Entitätstypen zum Reindexieren für die Persistenzeinheit '${0}' aus.`,
    'quarkus-hibernate-search-orm-elasticsearch-reindex-started': str`Anforderung der Reindizierung von ${0} Entitätstypen für das Persistenzeinheit '${1}'.`,
    'quarkus-hibernate-search-orm-elasticsearch-reindex-success': str`Erfolgreich ${0} Entitätstypen für die Persistenzeinheit '${1}' neu indiziert.`,
    'quarkus-hibernate-search-orm-elasticsearch-reindex-error': str`Ein Fehler ist aufgetreten beim Re-Indexieren von ${0} Entitätstypen für die Persistenzeinheit '${1}':\n${2}`,
};
