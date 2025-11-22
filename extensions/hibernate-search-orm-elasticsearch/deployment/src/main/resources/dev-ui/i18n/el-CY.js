import { str } from '@lit/localize';

export const templates = {
    'quarkus-hibernate-search-orm-elasticsearch-indexed_entity_types': 'Καταχωρημένοι Τύποι Οντοτήτων',
    'quarkus-hibernate-search-orm-elasticsearch-no-persistence-units': 'Δεν βρέθηκαν μονάδες επιμονής.',
    'quarkus-hibernate-search-orm-elasticsearch-reindex-selected': 'Επανακατάταξη επιλεγμένων',
    'quarkus-hibernate-search-orm-elasticsearch-class-name': 'Όνομα κατηγορίας',
    'quarkus-hibernate-search-orm-elasticsearch-index-names': 'Ονόματα ευρετηρίων',
    'quarkus-hibernate-search-orm-elasticsearch-select-entity-types-to-reindex': str`Επιλέξτε τύπους οντοτήτων για επανακατηγοριοποίηση της μονάδας επιμονής '${0}'.`,
    'quarkus-hibernate-search-orm-elasticsearch-reindex-started': str`Ζητήθηκε η επανακαταχώριση των τύπων οντότητας ${0} για τη μονάδα επίμονας δεδομένων '${1}'.`,
    'quarkus-hibernate-search-orm-elasticsearch-reindex-success': str`Επιτυχώς ανακατατάχθηκαν ${0} τύποι οντοτήτων για την μονάδα διατήρησης '${1}'.`,
    'quarkus-hibernate-search-orm-elasticsearch-reindex-error': str`Εμφανίστηκε σφάλμα κατά την εκ νέου ευρετηρίαση των τύπων οντοτήτων ${0} για τη μονάδαPersistence '${1}':\n${2}`,
};
