import { str } from '@lit/localize';

export const templates = {
    'quarkus-flyway-meta-description': 'Spravujte migrace schématu vaší databáze.',
    'quarkus-flyway-datasources': 'Datové zdroje',
    'quarkus-flyway-name': 'Název',
    'quarkus-flyway-action': 'Akce',
    'quarkus-flyway-clean': 'Vyčistit',
    'quarkus-flyway-migrate': 'Migrovat',
    'quarkus-flyway-clean-disabled-tooltip': 'Flyway clean byl zakázán pomocí quarkus.flyway.clean-disabled=true',
    'quarkus-flyway-update-button-title': 'Vytvořte soubor pro migraci aktualizace. Vždy ručně zkontrolujte vytvořený soubor, protože může způsobit ztrátu dat.',
    'quarkus-flyway-generate-migration-file': 'Vygenerovat migrační soubor',
    'quarkus-flyway-create-button-title': 'Nastavte základní soubory, aby migrace Flyway fungovaly. Počáteční soubor v db/migrations bude vytvořen a můžete pak přidávat další migrační soubory.',
    'quarkus-flyway-create-initial-migration-file': 'Vytvořit počáteční migrační soubor',
    'quarkus-flyway-create': 'Vytvořit',
    'quarkus-flyway-update': 'Aktualizace',
    'quarkus-flyway-create-dialog-description': 'Nastavte počáteční soubor pro generování schématu Hibernate ORM, aby fungovaly migrace Flyway.<br/>Pokud řeknete ano, bude vytvořen počáteční soubor v <code>db/migrations</code> a můžete poté přidat další migrační soubory podle dokumentace.',
    'quarkus-flyway-update-dialog-description': 'Vytvořte inkrementální migrační soubor z rozdílu schématu Hibernate ORM.<br/>Pokud řeknete ano, bude vytvořen další soubor v <code>db/migrations</code>.<br/>',
    'quarkus-flyway-cancel': 'Zrušit',
    'quarkus-flyway-clean-confirm': 'Tímto se odstraní všechny objekty (tabulky, pohledy, procedury, triggery, ...) ve nakonfigurovaném schématu. Chcete pokračovat?',
    'quarkus-flyway-datasource-title': str`${0} Datový zdroj`,
};
