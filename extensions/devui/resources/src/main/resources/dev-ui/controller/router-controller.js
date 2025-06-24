import { Router } from '@vaadin/router';

let pageNode = document.querySelector('#page');
pageNode.textContent = '';

export class RouterController {

    static router = new Router(pageNode);
    static pageMap = new Map(); // We use this to lookup page for a path
    static namespaceMap = new Map(); // We use this to lookup all pages for a namespace
    static componentMap = new Map(); // We use this to lookup page for a component
    static guardedComponents = new Set(); // We use this to allow component to guard against away navigation in case of changes
    static _skipNextGuard = false;

    static patchForUnsavedChanges() {
        if (RouterController._patched) return;
        RouterController._patched = true;

        const originalGo = RouterController.router.constructor.go;
        RouterController.router.constructor.go = async (to) => {
            if (RouterController._skipNextGuard) {
                RouterController._skipNextGuard = false;
                return originalGo.call(RouterController.router.constructor, to);
            }

            const shouldProceed = await RouterController._checkUnsavedChanges();
            if (!shouldProceed) {
                return;
            }

            RouterController._skipNextGuard = true;
            return originalGo.call(RouterController.router.constructor, to);
        };

        document.addEventListener('click', async (e) => {
            
            if(RouterController.guardedComponents.size < 1){
                return;
            }
            
            var location = RouterController.router.location;
            if (location.route && location.route.path) {
                let p = RouterController.pageMap.get(location.route.path);
                let currentComponentName = p.componentName;
                // Now check if it's guarded
                const isCurrentComponentGuarded = Array.from(RouterController.guardedComponents).some(
                    (el) => el.tagName.toLowerCase() === currentComponentName
                );

                if(!isCurrentComponentGuarded){
                    return;
                }
                const anchor = e.composedPath().find(el => el.tagName === 'A' && el.href);
                if (anchor && anchor.href.startsWith(window.location.origin)) {
                    const url = new URL(anchor.href);
                    const relativePath = url.pathname + url.search + url.hash;

                    try {
                        const matchingRoute = await RouterController.router.resolve(relativePath);

                        // If no matching route, let the browser handle it (likely REST or external link)
                        if (!matchingRoute || !matchingRoute.route || !matchingRoute.route.component) {
                            return;
                        }

                        // Intercept SPA route
                        e.preventDefault();
                        e.stopImmediatePropagation();

                        const shouldProceed = await RouterController._checkUnsavedChanges();
                        if (shouldProceed) {
                            RouterController._skipNextGuard = true;
                            RouterController.router.constructor.go(relativePath);
                        }
                    } catch (err) {
                        return;
                    }
                }
            }
        }, true);

    }
    
    static async _checkUnsavedChanges() {
        try {
            for (const component of RouterController.guardedComponents) {
                if (component?.shouldConfirmAwayNavigation?.()) {
                    const shouldLeave = await RouterController.showConfirmDialog();
                    return shouldLeave;
                }
            }
            return true;
        } finally {
            RouterController._skipNextGuard = false;
        }
    }


    static async showConfirmDialog() {
        let dialog = document.querySelector('#router-confirm-dialog');

        if (!dialog) {
            dialog = document.createElement('vaadin-confirm-dialog');
            dialog.setAttribute('id', 'router-confirm-dialog');
            dialog.setAttribute('header', 'Unsaved changes');
            dialog.setAttribute('confirm-text', 'Leave');
            dialog.setAttribute('reject-button-visible', '');
            dialog.setAttribute('reject-text', 'Stay');
            dialog.setAttribute('theme', 'warning');
            dialog.innerHTML = `You have unsaved changes. Do you want to leave this page?`;

            document.body.appendChild(dialog);
        }

        return new Promise((resolve) => {
            const onConfirm = () => {
                cleanup();
                resolve(true);
            };

            const onReject = () => {
                cleanup();
                resolve(false);
            };

            const cleanup = () => {
                dialog.removeEventListener('confirm', onConfirm);
                dialog.removeEventListener('reject', onReject);
            };

            dialog.addEventListener('confirm', onConfirm);
            dialog.addEventListener('reject', onReject);
            dialog.opened = true;
        });
    }

    static registerGuardedComponent(component) {
        RouterController.guardedComponents.add(component);
    }

    static unregisterGuardedComponent(component) {
        RouterController.guardedComponents.delete(component);
    }

    _host;

    constructor(host){
        this._host = host;
        // Ensure it's only patched once
        if (!RouterController._patched) {
            RouterController.patchForUnsavedChanges();
            RouterController._patched = true;
        }
    }

    goHome(){
        let firstPage = this.getFirstPageUrl();
        Router.go({pathname: firstPage});
    }

    go(page){
        let pageRef = this.getPageUrlFor(page);
        Router.go({pathname: pageRef});
    }

    navigate(pageRef){
        Router.go(pageRef);
    }

    getFirstPageUrl(){
        for (let entry of RouterController.pageMap) {
            let value = entry[1];
            if(value.includeInMenu){
                return entry[0];
            }
        }
        return null;
    }

    getCurrentRoutePath(){
        var location = RouterController.router.location;
        if (location.route) {
            return location.route.path;
        }
        return null;
    }

    getCurrentPage(){
        let currentRoutePath = this.getCurrentRoutePath();
        if (currentRoutePath) {
            let p = RouterController.pageMap.get(currentRoutePath);
            if(p){
                return p;
            }
        }
        return null;
    }

    getCurrentTitle(){
        let p = this.getCurrentPage();
        if(p){
            if(p.namespaceLabel){
                return p.namespaceLabel;
            }else {
                let md = this.getCurrentMetaData();
                if(md && md.extensionName){
                    return md.extensionName;
                }else {
                    return p.title;
                }
            }
        }
        return null;
    }

    getCurrentSubTitle(){
        let p = this.getCurrentPage();
        if(p){
            if(!p.namespaceLabel){
                let md = this.getCurrentMetaData();
                if(md && md.extensionName){
                    return p.title;
                }
            }
        }
        return null;
    }

    getCurrentNamespace(){
        let p = this.getCurrentPage();
        if(p){
            return p.namespace;
        }
        return null;
    }

    getPagesForCurrentNamespace(){
        let ns = this.getCurrentNamespace();
        if(ns){
            return RouterController.namespaceMap.get(ns);
        }
        return null;
    }

    getCurrentSubMenu(){
        var pagesForNamespace = this.getPagesForCurrentNamespace();
        if (pagesForNamespace) {
            let selected = 0;

            if(pagesForNamespace.length>1){
                const subMenus = [];
                pagesForNamespace.forEach((pageForNamespace, index) => {
                    if(pageForNamespace.title === RouterController.router.location.route.name){
                        selected = index;
                    }

                    let pageRef = this.getPageUrlFor(pageForNamespace);

                    subMenus.push({
                        "path" : pageRef,
                        "name" : pageForNamespace.title, // deprecate ?
                        "page" : pageForNamespace
                    });
                });
                return {
                    'index': selected,
                    'links': subMenus
                };
            }
            return null;
        }
        return null;
    }

    getCurrentMetaData() {
        var p = this.getCurrentPage();
        if(p){
            return p.metadata;
        }
        return null;
    }

    static getBasePath(){
        var base = window.location.pathname;
        if(base.endsWith("/dev-ui")){
            return base.substring(0, base.lastIndexOf('/dev-ui')) + "/dev-ui";
        }else{
            return base.substring(0, base.lastIndexOf('/dev-ui/')) + "/dev-ui";
        }
    }

    getPageUrlFor(page){
        return RouterController.getBasePath() + '/' + page.id;
    }

    isExistingPath(path) {
        if (RouterController.pageMap && RouterController.pageMap.size > 0 && RouterController.pageMap.has(path)) {
            return true;
        }
        return false;
    }

    addExternalLink(page){
        let path = this.getPageUrlFor(page);
        if (!this.isExistingPath(path)) {
            RouterController.pageMap.set(path, page);
            if(RouterController.namespaceMap.has(page.namespace)){
                // Existing
                RouterController.namespaceMap.get(page.namespace).push(page);
            }else{
                // New
                let namespacePages = [];
                namespacePages.push(page);
                RouterController.namespaceMap.set(page.namespace, namespacePages);
            }
        }
    }

    addRouteForMenu(page, defaultSelection){
        this.addRoute(page.id, page.componentName, page.title, page, defaultSelection);
    }

    addRouteForExtension(page){
        this.addRoute(page.id, page.componentName, page.title, page);
    }

    addRoute(path, component, name, page, defaultRoute = false) {
        path = this.getPageUrlFor(page);
        const search = new URLSearchParams(window.location.search);
        if (!this.isExistingPath(path)) {
            RouterController.pageMap.set(path, page);
            if(RouterController.namespaceMap.has(page.namespace)){
                // Existing
                RouterController.namespaceMap.get(page.namespace).push(page);
            }else{
                // New
                let namespacePages = [];
                namespacePages.push(page);
                RouterController.namespaceMap.set(page.namespace, namespacePages);
            }
            RouterController.componentMap.set(component, page);
            var routes = [];
            var route = {};
            route.path = path;
            route.component = component;
            route.name = name;

            routes.push({...route});

            RouterController.router.addRoutes(routes);

            var currentSelection = window.location.pathname;
            const search = this.getQueryParamsWithoutFrom();

            var relocationRequest = this.getQueryParameter("from");
            if (relocationRequest) {
                // We know and already loaded the requested location
                if (relocationRequest === path) {
                    Router.go({pathname: path, search});
                }
            } else if(currentSelection === path){
                Router.go({pathname: path, search});
            } else if(!RouterController.router.location.route && currentSelection.endsWith('/dev-ui/') && defaultRoute) {
                Router.go({pathname: path, search});
                // We do not know and have not yet loaded the requested location
            } else if (!RouterController.router.location.route && defaultRoute) {
                // pass original query param
                const currentQueryString = window.location.search;
                const origSearch = currentQueryString?.length > 0 ? '&' + currentQueryString : '';

                Router.go({
                    pathname: path,
                    search: '?from=' + currentSelection + origSearch
                });
            }
        }
    }

    getQueryParamsWithoutFrom() {
        const params = new URLSearchParams(window.location.search);
        if (params) {
            const paramsWithoutFrom = [];
            params.forEach((value, key) => {
                if (key !== 'from') {
                    paramsWithoutFrom.push(key + '=' + value);
                }
            });
            if (paramsWithoutFrom.length > 0) {
                return paramsWithoutFrom.join('&');
            }
        }
        return '';
    }

    getQueryParameters() {
        const params = new Proxy(new URLSearchParams(window.location.search), {
            get: (searchParams, prop) => searchParams.get(prop)
        });

        return params;
    }

    getQueryParameter(param){
        var params = this.getQueryParameters();
        if(params){
            return params[param] || null;
        }else {
            return null;
        }
    }

}
