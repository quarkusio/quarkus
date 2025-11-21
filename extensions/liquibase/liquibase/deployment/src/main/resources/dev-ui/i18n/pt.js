import { str } from '@lit/localize';

export const templates = {
    'quarkus-liquibase-meta-description': 'Gerencie suas migrações de esquema de banco de dados com Liquibase.',
    'quarkus-liquibase-datasources': 'Fontes de dados',
    'quarkus-liquibase-loading-datasources': 'Carregando fontes de dados...',
    'quarkus-liquibase-name': 'Nome',
    'quarkus-liquibase-action': 'Ação',
    'quarkus-liquibase-clear-database': 'Limpar Banco de Dados',
    'quarkus-liquibase-clear': 'Limpar',
    'quarkus-liquibase-clear-confirm': 'Isso irá excluir todos os objetos (tabelas, visões, procedimentos, gatilhos, ...) no esquema configurado. Você deseja continuar?',
    'quarkus-liquibase-migrate': 'Migrar',
    'quarkus-liquibase-cleared': str`A fonte de dados ${0} foi limpa.`,
    'quarkus-liquibase-migrated': str`A fonte de dados ${0} foi migrada.`,
};
