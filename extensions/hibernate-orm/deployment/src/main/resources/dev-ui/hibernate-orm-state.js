import { LitState } from 'lit-element-state';
import { StorageController } from 'storage-controller';

const _storage = new StorageController('hibernate-orm');

class HibernateOrmState extends LitState {
    static get stateVars() {
        return { selectedPersistenceUnit: null };
    }
}

export const hibernateOrmState = new HibernateOrmState();

export function restoreSelectedPU(persistenceUnits) {
    if (!persistenceUnits || persistenceUnits.length === 0) return null;
    const stored = _storage.get('selectedPU');
    const match = stored
        ? persistenceUnits.find(pu => pu.name === stored)
        : null;
    const selected = match || persistenceUnits[0];
    hibernateOrmState.selectedPersistenceUnit = selected;
    return selected;
}

export function saveSelectedPU(pu) {
    hibernateOrmState.selectedPersistenceUnit = pu;
    _storage.set('selectedPU', pu.name);
}
