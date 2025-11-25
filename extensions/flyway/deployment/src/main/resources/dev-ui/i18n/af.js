import { str } from '@lit/localize';

export const templates = {
    'quarkus-flyway-meta-description': 'Hanteer jou databasis skema migrasies',
    'quarkus-flyway-datasources': 'Gegevensbronne',
    'quarkus-flyway-name': 'Naam',
    'quarkus-flyway-action': 'Aksie',
    'quarkus-flyway-clean': 'Skoonmaak',
    'quarkus-flyway-migrate': 'Migreer',
    'quarkus-flyway-clean-disabled-tooltip': 'Flyway skoonmaak is gedeaktiveer via quarkus.flyway.clean-disabled=true',
    'quarkus-flyway-update-button-title': 'Skep opdateringsmigrasielêer. Hersien altyd die geskepte lêer handmatig, aangesien dit dataverlies kan veroorsaak.',
    'quarkus-flyway-generate-migration-file': 'Genereer Migrasiefilenaam',
    'quarkus-flyway-create-button-title': 'Stel basiese lêers op vir Flyway-migrasies om te werk. \'n Aanvangslêer in db/migrations sal geskep word en jy kan dan bykomende migrasielêers toevoeg.',
    'quarkus-flyway-create-initial-migration-file': 'Skep begin migrasie-lêer',
    'quarkus-flyway-create': 'Skep',
    'quarkus-flyway-update': 'Opdateer',
    'quarkus-flyway-create-dialog-description': 'Stel \'n aanvanklike lêer op uit Hibernate ORM-skemagenerasie sodat Flyway-migrasies kan werk.<br/>As jy ja sê, sal \'n aanvanklike lêer in <code>db/migrations</code> <br/>geskep word en jy kan dan addisionele migrasielêers byvoeg soos gedokumenteer.',
    'quarkus-flyway-update-dialog-description': 'Skep \'n inkrementele migrasie-lêer vanaf Hibernate ORM schema verskil.<br/>As jy ja sê, sal \'n bykomende lêer in <code>db/migrations</code> <br/>geskep word.',
    'quarkus-flyway-cancel': 'Kanselleer',
    'quarkus-flyway-clean-confirm': 'Hierdie sal alle voorwerpe (tabelle, aansigte, prosedures, triggers, ...) in die geconfigureerde skema verwyder. Wil jy voortgaan?',
    'quarkus-flyway-datasource-title': str`${0} Gegewensbron`,
};
