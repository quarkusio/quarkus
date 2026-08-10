/**
 * Session-scoped registry of Prod UI pages that were observed to be unavailable
 * this browser session - i.e. their component bundle failed to load, or their
 * read-only JSON-RPC data call errored. It exists so the Extensions landing can
 * keep such pages *visible* (operators still see they exist) but move them into a
 * collapsed "Unavailable" group instead of silently dropping them.
 *
 * Design: availability is only ever discovered at navigation time. Prod UI's
 * build-time page data never emits disabled or hidden pages (those are dropped
 * for security), so there is no server-provided "unavailable" flag to show up
 * front - the landing has nothing to grey out until a real failure is observed.
 * This registry therefore records only genuine, observed failures; it never
 * fabricates a state and never leaks the existence of a hidden page.
 *
 * It is strictly client-side and read-only with respect to the server: it reads
 * and writes sessionStorage only (cleared when the tab closes) and makes no
 * network calls.
 */

const STORAGE_KEY = 'pui-unavailable';

function readRegistry() {
    try {
        const raw = sessionStorage.getItem(STORAGE_KEY);
        return raw ? JSON.parse(raw) : {};
    } catch (e) {
        // sessionStorage may be unavailable (private mode / disabled) or hold
        // corrupt data - degrade to "everything available" rather than break.
        return {};
    }
}

function writeRegistry(map) {
    try {
        sessionStorage.setItem(STORAGE_KEY, JSON.stringify(map));
    } catch (e) {
        // Best-effort only; losing the hint is harmless.
    }
}

/**
 * Record that a namespace's page could not be reached, with a short reason shown
 * next to it in the collapsed group.
 */
export function markUnavailable(namespace, reason) {
    if (!namespace) {
        return;
    }
    const map = readRegistry();
    map[namespace] = reason || 'Unavailable';
    writeRegistry(map);
}

/**
 * Clear a namespace's unavailable flag once its data loads successfully again.
 */
export function clearUnavailable(namespace) {
    if (!namespace) {
        return;
    }
    const map = readRegistry();
    if (namespace in map) {
        delete map[namespace];
        writeRegistry(map);
    }
}

/** The current map of namespace -> reason for pages observed unavailable. */
export function getUnavailable() {
    return readRegistry();
}

/**
 * Pure helper: split a list of page groups into available and unavailable using
 * the supplied namespace -> reason map. Kept side-effect free so it is easy to
 * reason about (and unit test) independently of sessionStorage.
 */
export function partitionByAvailability(pages, unavailable) {
    const available = [];
    const notAvailable = [];
    const map = unavailable || {};
    for (const ext of pages || []) {
        if (ext && ext.namespace && Object.prototype.hasOwnProperty.call(map, ext.namespace)) {
            notAvailable.push({ ...ext, unavailableReason: map[ext.namespace] });
        } else {
            available.push(ext);
        }
    }
    return { available, unavailable: notAvailable };
}
