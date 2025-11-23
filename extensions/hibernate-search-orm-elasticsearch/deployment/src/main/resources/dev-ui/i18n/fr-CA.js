import { str } from '@lit/localize';

export const templates = {
    'quarkus-hibernate-search-orm-elasticsearch-meta-description': 'Indexez automatiquement vos entités Hibernate dans Elasticsearch.',
    'quarkus-hibernate-search-orm-elasticsearch-persistence-unit': 'Unité de Persistance',
    'quarkus-hibernate-search-orm-elasticsearch-reindex-started': str`Demande de réindexation de ${0} types d'entités pour l'unité de persistance '${1}'.`,
    'quarkus-hibernate-search-orm-elasticsearch-reindex-success': str`Réindexé avec succès ${0} types d'entités pour l'unité de persistance '${1}'.`,
    'quarkus-hibernate-search-orm-elasticsearch-reindex-error': str`Une erreur s'est produite lors de la réindexation des types d'entités ${0} pour l'unité de persistance '${1}' :\n${2}`
};
