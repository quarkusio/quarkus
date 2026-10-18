import { str } from '@lit/localize';

export const templates = {
    'quarkus-hibernate-search-orm-elasticsearch-meta-description': 'HibernateエンティティをElasticsearchに自動的にインデックスします',
    'quarkus-hibernate-search-orm-elasticsearch-indexed_entity_types': 'インデックス付きエンティティタイプ',
    'quarkus-hibernate-search-orm-elasticsearch-loading': '読み込み中...',
    'quarkus-hibernate-search-orm-elasticsearch-no-persistence-units': '永続性ユニットが見つかりませんでした。',
    'quarkus-hibernate-search-orm-elasticsearch-persistence-unit': '永続性ユニット',
    'quarkus-hibernate-search-orm-elasticsearch-no-indexed-entities': 'インデックスされたエンティティは見つかりませんでした。',
    'quarkus-hibernate-search-orm-elasticsearch-reindex-selected': '選択したものを再インデックス化',
    'quarkus-hibernate-search-orm-elasticsearch-entity-name': 'エンティティ名',
    'quarkus-hibernate-search-orm-elasticsearch-class-name': 'クラス名',
    'quarkus-hibernate-search-orm-elasticsearch-index-names': 'インデックス名',
    'quarkus-hibernate-search-orm-elasticsearch-selected-entity-types': str`選択された ${0} エンティティタイプ`,
    'quarkus-hibernate-search-orm-elasticsearch-select-entity-types-to-reindex': str`永続ユニット「${0}」の再インデックスのためのエンティティタイプを選択してください。`,
    'quarkus-hibernate-search-orm-elasticsearch-reindex-started': str`永続ユニット '${1}' のために ${0} エンティティタイプの再インデックス作成を要求しました。`,
    'quarkus-hibernate-search-orm-elasticsearch-reindex-success': str`永続ユニット '${1}' のために、${0} エンティティタイプを正常に再インデックスしました。`,
    'quarkus-hibernate-search-orm-elasticsearch-reindex-error': str`永続ユニット '${1}' の ${0} エンティティタイプの再インデックス中にエラーが発生しました：\n${2}`,
};
