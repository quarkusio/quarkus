import { str } from '@lit/localize';

export const templates = {
    'quarkus-flyway-meta-description': 'データベーススキーマのマイグレーションを処理する',
    'quarkus-flyway-datasources': 'データソース',
    'quarkus-flyway-name': '名前',
    'quarkus-flyway-action': 'アクション',
    'quarkus-flyway-clean': 'クリーン',
    'quarkus-flyway-migrate': '移行する',
    'quarkus-flyway-clean-disabled-tooltip': 'Flyway クリーンは quarkus.flyway.clean-disabled=true により無効化されました。',
    'quarkus-flyway-update-button-title': '更新マイグレーションファイルを作成します。作成されたファイルは手動で確認してください。データ損失を引き起こす可能性があります。',
    'quarkus-flyway-generate-migration-file': 'マイグレーションファイルを生成する',
    'quarkus-flyway-create-button-title': 'Flywayマイグレーションが機能するための基本ファイルをセットアップします。db/migrationsに初期ファイルが作成され、その後追加のマイグレーションファイルを追加できます。',
    'quarkus-flyway-create-initial-migration-file': '初期マイグレーションファイルを作成する',
    'quarkus-flyway-create': '作成する',
    'quarkus-flyway-update': '更新',
    'quarkus-flyway-create-dialog-description': 'Flyway移行が機能するためのHibernate ORMスキーマ生成から初期ファイルを設定します。<br/>はいと答えると、<code>db/migrations</code>に初期ファイルが<br/>作成され、その後は文書に記載されている追加の移行ファイルを追加できます。',
    'quarkus-flyway-update-dialog-description': 'Hibernate ORMスキーマ差分から増分マイグレーションファイルを作成します。<br/>はいと言うと、<code>db/migrations</code>に追加ファイルが<br/>作成されます。',
    'quarkus-flyway-cancel': 'キャンセル',
    'quarkus-flyway-clean-confirm': 'これにより、設定されたスキーマ内のすべてのオブジェクト（テーブル、ビュー、プロシージャ、トリガーなど）が削除されます。続行しますか？',
    'quarkus-flyway-datasource-title': str`${0} データソース`,
};
