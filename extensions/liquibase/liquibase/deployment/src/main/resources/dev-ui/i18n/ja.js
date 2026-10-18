import { str } from '@lit/localize';

export const templates = {
    'quarkus-liquibase-meta-description': 'Liquibaseを使用してデータベーススキーマのマイグレーションを管理します。',
    'quarkus-liquibase-datasources': 'データソース',
    'quarkus-liquibase-loading-datasources': 'データソースを読み込んでいます...',
    'quarkus-liquibase-name': '名前',
    'quarkus-liquibase-action': 'アクション',
    'quarkus-liquibase-clear-database': 'データベースをクリアする',
    'quarkus-liquibase-clear': 'クリア',
    'quarkus-liquibase-clear-confirm': 'これにより、設定されたスキーマ内のすべてのオブジェクト（テーブル、ビュー、プロシージャ、トリガーなど）が削除されます。続行しますか？',
    'quarkus-liquibase-migrate': '移行する',
    'quarkus-liquibase-cleared': str`データソース ${0} はクリアされました。`,
    'quarkus-liquibase-migrated': str`データソース ${0} は移行されました。`,
};
