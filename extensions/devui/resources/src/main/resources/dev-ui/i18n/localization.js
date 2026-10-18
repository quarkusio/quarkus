import {configureLocalization, updateWhenLocaleChanges, msg, str} from '@lit/localize';
import { devuiState } from 'devui-state';
import { locales } from 'devui-locales-data';

const slugify = (s) =>
  String(s)
    .normalize('NFKD')
    .replace(/[\u0300-\u036f]/g, '')
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '_')
    .replace(/^_+|_+$/g, '');

export const {getLocale, setLocale} = configureLocalization({
    sourceLocale: 'x-src',
    targetLocales: locales.map(l => l.code),
    loadLocale: loadAndMergeLocale
});

async function loadAndMergeLocale(locale) {
    // Core Dev UI
    const baseLang = String(locale).split(/[-_]/)[0];
    const coreBaseUrl = new URL(`./${baseLang}.js`, import.meta.url).href;
    const coreLocaleUrl = new URL(`./${locale}.js`, import.meta.url).href;
    const coreBaseMod = await tryImport(coreBaseUrl);
    const coreMod = await tryImport(coreLocaleUrl);

    // Also load extensions (namespace/i18n/<locale>.js)
    let extIds = getExtensionsIds();
    const extMods = [];
    for (const ns of extIds) {
        let extBaseUrl = new URL(`../${ns}/i18n/${baseLang}.js`, import.meta.url).href;
        let extLocaleUrl = new URL(`../${ns}/i18n/${locale}.js`, import.meta.url).href;
        let baseMod = await tryImport(extBaseUrl);
        if (baseMod) extMods.push(baseMod);
        let mod = await tryImport(extLocaleUrl);
        if (mod) extMods.push(mod);
    }
    
    const merged = { templates: {}, messages: {} };
    const apply = (mod) => {
        if (!mod) return;
        if (mod.templates && typeof mod.templates === 'object') {
            Object.assign(merged.templates, mod.templates);
        }
        if (mod.messages && typeof mod.messages === 'object') {
            Object.assign(merged.messages, mod.messages);
        }
    };
    apply(coreBaseMod);
    apply(coreMod);
    for (const m of extMods) apply(m);

    return merged;
}

async function tryImport(href) {
    try {
        const res = await fetch(href, { method: 'GET' });

        if (!res.ok) {
            return null;
        }

        const contentType = res.headers.get('content-type') || '';

        if (
            !contentType.includes('javascript') &&
            !contentType.includes('ecmascript') &&
            !contentType.includes('module')
        ) {
            return null;
        }

        const code = await res.text();
        const blob = new Blob([code], { type: 'text/javascript' });
        const blobUrl = URL.createObjectURL(blob);

        try {
            return await import(blobUrl);
        } finally {
            URL.revokeObjectURL(blobUrl);
        }
    } catch {
        return null;
    }
}

function getExtensionsIds() {
    let extensions = [...devuiState.cards.active,...devuiState.cards.inactive];
    return extensions
        .filter(obj => obj.namespace)
        .map(obj => obj.namespace);
}

export function dynamicMsg(pre, original) {
    const id = `${pre}-${slugify(original)}`;
    return msg(original, { id });
}

export {updateWhenLocaleChanges, msg, str};
