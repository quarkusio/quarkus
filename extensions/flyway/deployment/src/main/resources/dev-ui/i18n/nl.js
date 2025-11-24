import { str } from '@lit/localize';

export const templates = {
    'quarkus-flyway-meta-description': 'Beheer uw database-schema-migraties',
    'quarkus-flyway-datasources': 'Gegevensbronnen',
    'quarkus-flyway-name': 'Naam',
    'quarkus-flyway-action': 'Actie',
    'quarkus-flyway-clean': 'Schoon',
    'quarkus-flyway-migrate': 'Migreer',
    'quarkus-flyway-clean-disabled-tooltip': 'Flyway schoonmaken is uitgeschakeld via quarkus.flyway.clean-disabled=true',
    'quarkus-flyway-update-button-title': 'Maak een update-migratiebestand. Controleer altijd handmatig het gemaakte bestand, aangezien dit gegevensverlies kan veroorzaken.',
    'quarkus-flyway-generate-migration-file': 'Migratiebestand genereren',
    'quarkus-flyway-create-button-title': 'Stel basisbestanden in voor Flyway-migraties om te werken. Het initiële bestand in db/migrations zal worden gemaakt en je kunt vervolgens aanvullende migratiebestanden toevoegen.',
    'quarkus-flyway-create-initial-migration-file': 'Maak initiële migratiebestand aan',
    'quarkus-flyway-create': 'Aanmaken',
    'quarkus-flyway-update': 'Bijwerken',
    'quarkus-flyway-create-dialog-description': 'Stel een initieel bestand in voor de Hibernate ORM-schema generatie zodat Flyway-migraties kunnen werken.<br/>Als je ja zegt, wordt er een initieel bestand aangemaakt in <code>db/migrations</code> en kun je vervolgens aanvullende migratiebestanden toevoegen zoals gedocumenteerd.',
    'quarkus-flyway-update-dialog-description': 'Maak een incrementele migratiebestand op basis van het schema-diff van Hibernate ORM.<br/>Als je ja zegt, wordt er een extra bestand aangemaakt in <code>db/migrations</code>.<br/>',
    'quarkus-flyway-cancel': 'Annuleren',
    'quarkus-flyway-clean-confirm': 'Dit verwijdert alle objecten (tabellen, views, procedures, triggers, ...) in het geconfigureerde schema. Wilt u doorgaan?',
    'quarkus-flyway-datasource-title': str`${0} Gegevensbron`,
};
