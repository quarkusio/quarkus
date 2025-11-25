import { str } from '@lit/localize';

export const templates = {
    'quarkus-flyway-meta-description': 'Διαχειριστείτε τις μεταναστεύσεις του σχήματος της βάσης δεδομένων σας.',
    'quarkus-flyway-datasources': 'Πηγές δεδομένων',
    'quarkus-flyway-name': 'Όνομα',
    'quarkus-flyway-action': 'Ενέργεια',
    'quarkus-flyway-clean': 'Καθαρίστε',
    'quarkus-flyway-migrate': 'Μεταφορά',
    'quarkus-flyway-clean-disabled-tooltip': 'Η καθαριστική διαδικασία του Flyway έχει απενεργοποιηθεί μέσω του quarkus.flyway.clean-disabled=true.',
    'quarkus-flyway-update-button-title': 'Δημιουργήστε αρχείο μετανάστευσης ενημέρωσης. Ελέγξτε πάντα το δημιουργημένο αρχείο χειροκίνητα, καθώς μπορεί να προκαλέσει απώλεια δεδομένων.',
    'quarkus-flyway-generate-migration-file': 'Δημιουργία Αρχείου Μετανάστευσης',
    'quarkus-flyway-create-button-title': 'Ρυθμίστε τα βασικά αρχεία για τη λειτουργία των μεταναστεύσεων Flyway. Το αρχικό αρχείο στον φάκελο db/migrations θα δημιουργηθεί και μπορείτε στη συνέχεια να προσθέσετε επιπλέον αρχεία μετανάστευσης.',
    'quarkus-flyway-create-initial-migration-file': 'Δημιουργία Αρχείου Αρχικής Μετανάστευσης',
    'quarkus-flyway-create': 'Δημιουργία',
    'quarkus-flyway-update': 'Ενημέρωση',
    'quarkus-flyway-create-dialog-description': 'Ρυθμίστε ένα αρχικό αρχείο από την παραγωγή σχημάτων του Hibernate ORM ώστε να λειτουργούν οι μετανάστες του Flyway.<br/>Αν πείτε ναι, θα δημιουργηθεί ένα αρχικό αρχείο στο <code>db/migrations</code> <br/>και μπορείτε στη συνέχεια να προσθέσετε επιπλέον αρχεία μετανάστευσης όπως τεκμηριώνεται.',
    'quarkus-flyway-update-dialog-description': 'Δημιουργήστε ένα σταδιακό αρχείο μετανάστευσης από την διαφορά σχήματος του Hibernate ORM.<br/>Εάν απαντήσετε ναι, ένα επιπλέον αρχείο στο <code>db/migrations</code> θα <br/>δημιουργηθεί.',
    'quarkus-flyway-cancel': 'Ακύρωση',
    'quarkus-flyway-clean-confirm': 'Αυτό θα διαγράψει όλα τα αντικείμενα (πίνακες, εμφανίσεις, διαδικασίες, triggers, ...) στο ρυθμισμένο σχήμα. Θέλετε να συνεχίσετε;',
    'quarkus-flyway-datasource-title': str`${0} Πηγή Δεδομένων`,
};
