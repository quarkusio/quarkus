import { str } from '@lit/localize';

export const templates = {
    'quarkus-hibernate-search-standalone-elasticsearch-meta-description': 'Elasticsearchでエンティティを明示的にインデックス/検索する',
    'quarkus-hibernate-search-standalone-elasticsearch-indexed_entity_types': 'インデックス付きエンティティタイプ',
    'quarkus-hibernate-search-standalone-elasticsearch-loading': '読み込み中...',
    'quarkus-hibernate-search-standalone-elasticsearch-no-indexed-entities': 'インデックス付きエンティティは見つかりませんでした。',
    'quarkus-hibernate-search-standalone-elasticsearch-reindex-selected': '選択したものを再インデックス化',
    'quarkus-hibernate-search-standalone-elasticsearch-entity-name': 'エンティティ名',
    'quarkus-hibernate-search-standalone-elasticsearch-class-name': 'クラス名',
    'quarkus-hibernate-search-standalone-elasticsearch-index-names': 'インデックス名',
    'quarkus-hibernate-search-standalone-elasticsearch-select-entity-types': '再インデックスするエンティティタイプを選択してください。',
    'quarkus-hibernate-search-standalone-elasticsearch-selected-entity-types': str`選択された ${0} エンティティタイプ ${1}`,
    'quarkus-hibernate-search-standalone-elasticsearch-reindex-started': str`${0} エンティティタイプの再インデックス作成が要求されました。`,
    'quarkus-hibernate-search-standalone-elasticsearch-reindex-success': str`${0} エンティティタイプの再インデックスに成功しました。`,
    'quarkus-hibernate-search-standalone-elasticsearch-reindex-error': str`${0} エンティティタイプの再インデックス中にエラーが発生しました:\n${1}`,
};
