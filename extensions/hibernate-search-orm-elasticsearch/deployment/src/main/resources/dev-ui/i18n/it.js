import { str } from '@lit/localize';

export const templates = {
    'quarkus-hibernate-search-orm-elasticsearch-meta-description': 'Indicizza automaticamente le tue entità Hibernate in Elasticsearch',
    'quarkus-hibernate-search-orm-elasticsearch-indexed_entity_types': 'Tipi di Entità Indicizzati',
    'quarkus-hibernate-search-orm-elasticsearch-loading': 'Caricamento...',
    'quarkus-hibernate-search-orm-elasticsearch-no-persistence-units': 'Nessuna unità di persistenza trovata.',
    'quarkus-hibernate-search-orm-elasticsearch-persistence-unit': 'Unità di persistenza',
    'quarkus-hibernate-search-orm-elasticsearch-no-indexed-entities': 'Nessuna entità indicizzata è stata trovata.',
    'quarkus-hibernate-search-orm-elasticsearch-reindex-selected': 'Reindicizza selezionato',
    'quarkus-hibernate-search-orm-elasticsearch-entity-name': 'Nome entità',
    'quarkus-hibernate-search-orm-elasticsearch-class-name': 'Nome della classe',
    'quarkus-hibernate-search-orm-elasticsearch-index-names': 'Nomi degli indici',
    'quarkus-hibernate-search-orm-elasticsearch-selected-entity-types': str`Tipologie di entità selezionate ${0}`,
    'quarkus-hibernate-search-orm-elasticsearch-select-entity-types-to-reindex': str`Seleziona i tipi di entità da reindicizzare per l'unità di persistenza '${0}'.`,
    'quarkus-hibernate-search-orm-elasticsearch-reindex-started': str`Richiesta di reindicizzazione di ${0} tipi di entità per l'unità di persistenza '${1}'.`,
    'quarkus-hibernate-search-orm-elasticsearch-reindex-success': str`Ripristinati con successo ${0} tipi di entità per l'unità di persistenza '${1}'.`,
    'quarkus-hibernate-search-orm-elasticsearch-reindex-error': str`Si è verificato un errore durante la reindicizzazione dei tipi di entità ${0} per l'unità di persistenza '${1}':\n${2}`,
};
