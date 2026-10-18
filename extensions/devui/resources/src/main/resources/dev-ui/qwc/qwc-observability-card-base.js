import { QwcHotReloadElement } from 'qwc-hot-reload-element';
import { notifier } from 'notifier';

/**
 * Shared base for Observability signal components. Provides common shell affordances
 * (currently CSV export) so each signal component reuses them. Registered in the Dev UI
 * import map by core devui as the bare specifier 'observability-card-base'.
 *
 * Extends QwcHotReloadElement so signal components can react to dev-mode live reloads:
 * when the JSON-RPC connection is re-established after a reload, the framework invokes
 * their hotReload() so they can re-seed and re-subscribe (the previous websocket is gone).
 */
export class ObservabilityCardBase extends QwcHotReloadElement {

    /**
     * Export an array of flat row objects to a downloaded CSV file.
     * @param {Array<Object>} rows
     * @param {string} filename
     */
    exportCsv(rows, filename) {
        if (!rows || rows.length === 0) {
            notifier.showWarningMessage('Nothing to export');
            return;
        }
        const headers = Object.keys(rows[0]);
        const escape = (v) => {
            const s = v === null || v === undefined ? '' : String(v);
            return '"' + s.replace(/"/g, '""') + '"';
        };
        const lines = [headers.join(',')];
        for (const row of rows) {
            lines.push(headers.map(h => escape(row[h])).join(','));
        }
        const blob = new Blob([lines.join('\n')], { type: 'text/csv' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = filename;
        link.click();
        URL.revokeObjectURL(url);
        notifier.showInfoMessage('Exported ' + rows.length + ' rows');
    }
}
