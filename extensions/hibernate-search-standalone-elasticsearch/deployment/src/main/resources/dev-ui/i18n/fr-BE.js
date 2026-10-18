import { str } from '@lit/localize';

export const templates = {
    'quarkus-hibernate-search-standalone-elasticsearch-meta-description': 'Indexer/rechercher explicitement des entités dans Elasticsearch',
    'quarkus-hibernate-search-standalone-elasticsearch-indexed_entity_types': 'Types d\'entités indexés',
    'quarkus-hibernate-search-standalone-elasticsearch-reindex-selected': 'Réindexer sélectionné',
    'quarkus-hibernate-search-standalone-elasticsearch-reindex-started': str`Reindexation demandée pour ${0} types d'entités.`,
    'quarkus-hibernate-search-standalone-elasticsearch-reindex-success': str`Types d'entités ${0} réindexés avec succès.`,
    'quarkus-hibernate-search-standalone-elasticsearch-reindex-error': str`Une erreur s'est produite lors de la réindexation des types d'entités ${0} :\n${1}`
};
