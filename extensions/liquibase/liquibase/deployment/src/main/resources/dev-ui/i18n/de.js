import { str } from '@lit/localize';

export const templates = {
    'quarkus-liquibase-meta-description': 'Verwalten Sie Ihre Datenbankschemas-Migrationen mit Liquibase.',
    'quarkus-liquibase-datasources': 'Datenquellen',
    'quarkus-liquibase-loading-datasources': 'Lade Datenquellen...',
    'quarkus-liquibase-name': 'Name',
    'quarkus-liquibase-action': 'Aktion',
    'quarkus-liquibase-clear-database': 'Datenbank zurücksetzen',
    'quarkus-liquibase-clear': 'Löschen',
    'quarkus-liquibase-clear-confirm': 'Dies wird alle Objekte (Tabellen, Ansichten, Prozeduren, Trigger, ...) im konfigurierten Schema löschen. Möchten Sie fortfahren?',
    'quarkus-liquibase-migrate': 'Migrieren',
    'quarkus-liquibase-cleared': str`Die Datenquelle ${0} wurde gelöscht.`,
    'quarkus-liquibase-migrated': str`Die Datenquelle ${0} wurde migriert.`,
};
