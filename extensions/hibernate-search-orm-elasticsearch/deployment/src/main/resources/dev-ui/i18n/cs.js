import { str } from '@lit/localize';

export const templates = {
    'quarkus-hibernate-search-orm-elasticsearch-meta-description': 'Automaticky indexujte své entity Hibernate v Elasticsearch.',
    'quarkus-hibernate-search-orm-elasticsearch-indexed_entity_types': 'Indexované typy entit',
    'quarkus-hibernate-search-orm-elasticsearch-loading': 'Načítání...',
    'quarkus-hibernate-search-orm-elasticsearch-no-persistence-units': 'Nebyly nalezeny žádné trvalé jednotky.',
    'quarkus-hibernate-search-orm-elasticsearch-persistence-unit': 'Jednotka trvalosti',
    'quarkus-hibernate-search-orm-elasticsearch-no-indexed-entities': 'Žádné indexované entity nebyly nalezeny.',
    'quarkus-hibernate-search-orm-elasticsearch-reindex-selected': 'Znovu indexovat vybrané',
    'quarkus-hibernate-search-orm-elasticsearch-entity-name': 'Název entity',
    'quarkus-hibernate-search-orm-elasticsearch-class-name': 'Název třídy',
    'quarkus-hibernate-search-orm-elasticsearch-index-names': 'Názvy indexů',
    'quarkus-hibernate-search-orm-elasticsearch-selected-entity-types': str`Vybrané typy entit ${0}`,
    'quarkus-hibernate-search-orm-elasticsearch-select-entity-types-to-reindex': str`Vyberte typy entit k opětovnému indexování pro trvalou jednotku '${0}'.`,
    'quarkus-hibernate-search-orm-elasticsearch-reindex-started': str`Byla požadována znovuindexace ${0} typů entit pro jednotku trvalosti '${1}'.`,
    'quarkus-hibernate-search-orm-elasticsearch-reindex-success': str`Úspěšně byly přeindexovány ${0} typy entit pro perzistentní jednotku '${1}'.`,
    'quarkus-hibernate-search-orm-elasticsearch-reindex-error': str`Při znovuindexaci typů entit ${0} pro jednotku perzistence '${1}' došlo k chybě:\n${2}`,
};
