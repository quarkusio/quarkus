import { str } from '@lit/localize';

export const templates = {
    'quarkus-flyway-meta-description': 'Gérez vos migrations de schéma de base de données',
    'quarkus-flyway-datasources': 'Sources de données',
    'quarkus-flyway-name': 'Nom',
    'quarkus-flyway-action': 'Action',
    'quarkus-flyway-clean': 'Nettoyer',
    'quarkus-flyway-migrate': 'Migrer',
    'quarkus-flyway-clean-disabled-tooltip': 'La commande Flyway clean a été désactivée via quarkus.flyway.clean-disabled=true.',
    'quarkus-flyway-update-button-title': 'Créer un fichier de migration de mise à jour. Vérifiez toujours manuellement le fichier créé car cela peut entraîner une perte de données.',
    'quarkus-flyway-generate-migration-file': 'Générer le fichier de migration',
    'quarkus-flyway-create-button-title': 'Configurer les fichiers de base pour que les migrations Flyway fonctionnent. Le fichier initial dans db/migrations sera créé et vous pourrez ensuite ajouter des fichiers de migration supplémentaires.',
    'quarkus-flyway-create-initial-migration-file': 'Créer le fichier de migration initiale',
    'quarkus-flyway-create': 'Créer',
    'quarkus-flyway-update': 'Mettre à jour',
    'quarkus-flyway-create-dialog-description': 'Configurer un fichier initial pour que la génération de schéma Hibernate ORM fonctionne avec les migrations Flyway.<br/>Si vous dites oui, un fichier initial dans <code>db/migrations</code> sera <br/>créé et vous pourrez ensuite ajouter des fichiers de migration supplémentaires comme documenté.',
    'quarkus-flyway-update-dialog-description': 'Créer un fichier de migration incrémentielle à partir de la différence de schéma Hibernate ORM.<br/>Si vous dites oui, un fichier supplémentaire dans <code>db/migrations</code> sera <br/>créé.',
    'quarkus-flyway-cancel': 'Annuler',
    'quarkus-flyway-clean-confirm': 'Ceci supprimera tous les objets (tables, vues, procédures, déclencheurs, ...) dans le schéma configuré. Voulez-vous continuer ?',
    'quarkus-flyway-datasource-title': str`${0} Source de données`,
};
