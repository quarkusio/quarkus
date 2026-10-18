import { str } from '@lit/localize';

export const templates = {
    'quarkus-flyway-meta-description': 'Verwalten Sie Ihre Datenbankschema-Migrationen',
    'quarkus-flyway-datasources': 'Datenquellen',
    'quarkus-flyway-name': 'Name',
    'quarkus-flyway-action': 'Aktion',
    'quarkus-flyway-clean': 'Bereinigen',
    'quarkus-flyway-migrate': 'Migrieren',
    'quarkus-flyway-clean-disabled-tooltip': 'Flyway clean wurde deaktiviert über quarkus.flyway.clean-disabled=true.',
    'quarkus-flyway-update-button-title': 'Erstellen Sie eine Aktualisierungs-Migrationsdatei. Überprüfen Sie die erstellte Datei immer manuell, da sie zu Datenverlust führen kann.',
    'quarkus-flyway-generate-migration-file': 'Migrationsdatei generieren',
    'quarkus-flyway-create-button-title': 'Richten Sie die grundlegenden Dateien ein, damit Flyway-Migrationen funktionieren. Die erste Datei im Verzeichnis db/migrations wird erstellt, und Sie können dann zusätzliche Migrationsdateien hinzufügen.',
    'quarkus-flyway-create-initial-migration-file': 'Initiale Migrationsdatei erstellen',
    'quarkus-flyway-create': 'Erstellen',
    'quarkus-flyway-update': 'Aktualisieren',
    'quarkus-flyway-create-dialog-description': 'Richten Sie eine anfängliche Datei für die Schemaerzeugung von Hibernate ORM ein, damit Flyway-Migrationen funktionieren.<br/>Wenn Sie ja sagen, wird eine anfängliche Datei in <code>db/migrations</code> erstellt und Sie können dann zusätzliche Migrationsdateien hinzufügen, wie dokumentiert.',
    'quarkus-flyway-update-dialog-description': 'Erstellen Sie eine inkrementelle Migrationsdatei aus dem Hibernate ORM-Schema-Diff.<br/>Wenn Sie ja sagen, wird eine zusätzliche Datei in <code>db/migrations</code> <br/>erzeugt.',
    'quarkus-flyway-cancel': 'Abbrechen',
    'quarkus-flyway-clean-confirm': 'Dies entfernt alle Objekte (Tabellen, Sichten, Prozeduren, Trigger, ...) im konfigurierten Schema. Möchten Sie fortfahren?',
    'quarkus-flyway-datasource-title': str`${0} Datenquelle`,
};
