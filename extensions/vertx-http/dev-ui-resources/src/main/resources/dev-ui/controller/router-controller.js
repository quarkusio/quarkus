import { Router } from '@vaadin/router';

let pageNode = document.querySelector('#page');
pageNode.textContent = '';

export class RouterController {
    

    static router = new Router(pageNode);
    static pathContext = new Map(); // deprecated
    static pageMap = new Map(); // deprecated
    
    /**
     * Parse the event change event
     */
    static parseLocationChangedEvent(event){
        var component = event.detail.location.route.component;
        var path = event.detail.location.route.path;
        var name = event.detail.location.route.name;
        var title = RouterController.currentTitle();
        var subMenu = RouterController.currentSubMenu();

        

        return {
            'component': component,
            'path': path,
            'name': name,
            'title': title,
            'subMenu': subMenu,
        };
    }

    /**
     * Get the header title for the current path
     */
    static currentTitle(){
        var currentRoutePath = RouterController.currentRoutePath();
        if (currentRoutePath) {
            return RouterController.titleForPath(currentRoutePath);
        }
        return null;
    }

    /**
     * Get the header title for a certain path
     */
    static titleForPath(path){
        if(path.includes('/dev-ui/')){
            var metadata = RouterController.metaDataForPath(path);
            if(metadata && metadata.extensionName){
                return metadata.extensionName;
            }else{
                var currentPage = path.substring(path.indexOf('/dev-ui/') + 8);
                if(currentPage.includes('/')){
                    // This is a submenu
                    var extension = currentPage.substring(0, currentPage.lastIndexOf("/"));
                    return RouterController.displayTitle(extension);
                }else{
                    // This is a main section
                    return RouterController.displayTitle(currentPage);
                }
            }
        }
        return "";
    }

    /**
     * Get the sub menu (if any) for the current certain path 
     */
    static currentSubMenu(){
        var currentRoutePath = RouterController.currentRoutePath();
        if (currentRoutePath) {
            return RouterController.subMenuForPath(currentRoutePath);
        }
        return null;
    }

    /**
     * Get the sub menu (if any) for a certain path 
     */
    static subMenuForPath(path){
        
        if(path.includes('/dev-ui/')){
            var currentPage = path.substring(path.indexOf('/dev-ui/') + 8);
            if(currentPage.includes('/')){
                // This is a submenu
                const links = [];
                var startOfPath = path.substring(0, path.lastIndexOf("/"));
                var routes = RouterController.router.getRoutes();

                var counter = 0;
                var index = 0;
                routes.forEach((route) => {
                    var pageLink = route.path.substring(route.path.indexOf('/dev-ui/') + 8);
                    if(pageLink.includes('/')){ // To filter out section menu items
                        if(route.path.startsWith(startOfPath)){
                            links.push(route);
                            if(route.name === RouterController.router.location.route.name){
                                index = counter;
                            }
                            counter = counter + 1;
                        }
                    }
                });

                if (links && links.length > 1) {
                    return {
                        'index': index,
                        'links': links
                    };
                }
            }
        }
        return null;
    }

    /**
     * Get the metadata for the current path
     */
    static currentMetaData() {
        var currentRoutePath = RouterController.currentRoutePath();
        if (currentRoutePath) {
            return RouterController.metaDataForPath(currentRoutePath);
        }
        return null;
    }

    static currentRoutePath(){
        var location = RouterController.router.location;
        if (location.route) {
            return location.route.path;
        }
        return null;
    }

    static currentExtensionId(){
        var metadata = RouterController.currentMetaData();
        if(metadata){
            return metadata.extensionId;
        }
        return null;
    }

    /**
     * Get all the metadata for a certain path
     */
    static metaDataForPath(path) {
        if (RouterController.existingPath(path)) {
            return RouterController.pathContext.get(path);
        }else{
            return null;
        }
    }

    /**
     * Check if we already know about this path
     */
    static existingPath(path) {
        if (RouterController.pathContext && RouterController.pathContext.size > 0 && RouterController.pathContext.has(path)) {
            return true;
        }
        return false;
    }

    /**
     * Format a title
     */
    static displayTitle(title) {
        title = title.charAt(0).toUpperCase() + title.slice(1);
        return title.split("-").join(" ");
    }

    /**
     * Creating the display Title for the Section Menu
     */
    static displayMenuItem(pageName) {
        pageName = pageName.substring(pageName.indexOf('-') + 1);
        pageName = pageName.charAt(0).toUpperCase() + pageName.slice(1);
        return pageName.replaceAll('-', ' ');
    }

    /**
     * This adds a route for Extensions (typically sub-menu pages)
     */
    static addExtensionRoute(page){
        RouterController.addRoute(page.id, page.componentName, page.title, page);
    }

    /**
     * This adds a route for the Menu section
     */
    static addMenuRoute(page, defaultSelection){
        var pageref = RouterController.pageRef(page.componentName);
        RouterController.addRoute(pageref, page.componentName, page.title, page, defaultSelection);
    }

    static basePath(){
        var base = window.location.pathname;
        return base.substring(0, base.indexOf('/dev')) + "/dev-ui";
    }

    static pageRef(pageName) {
        return pageName.substring(pageName.indexOf('-') + 1);
    }

    static pageRefWithBase(pageName){
        return RouterController.basePath() + '/' + RouterController.pageRef(pageName);
    }

    /**
     * Add a route to the routes
     */
    static addRoute(path, component, name, page, defaultRoute = false) {
        var base = RouterController.basePath();
        path = base + '/' + path;

        if (!RouterController.existingPath(path)) {
            RouterController.pathContext.set(path, page.metadata); // deprecated
            RouterController.pageMap.set(path, page)
            var routes = [];
            var route = {};
            route.path = path;
            route.component = component;
            route.name = name;

            routes.push({...route});

            RouterController.router.addRoutes(routes);
        }
        // TODO: Pass the other parameters along ?
        var currentSelection = window.location.pathname;

        var relocationRequest = RouterController.from();
        if (relocationRequest) {
            // We know and already loaded the requested location
            if (relocationRequest === path) {
                Router.go({pathname: path});
            }
        } else {
            // We know and already loaded the requested location
            if (currentSelection === path) {
                Router.go({pathname: path});
                // The default naked route  
            } else if (!RouterController.router.location.route && defaultRoute && currentSelection.endsWith('/dev-ui/')) {
                Router.go({pathname: path});
                // We do not know and have not yet loaded the requested location
            } else if (!RouterController.router.location.route && defaultRoute) {
                Router.go({
                    pathname: path,
                    search: '?from=' + currentSelection,
                });
            }
        }
    }

    static queryParameters() {
        const params = new Proxy(new URLSearchParams(window.location.search), {
            get: (searchParams, prop) => searchParams.get(prop),
        });

        return params;
    }

    static from(){
        var params = RouterController.queryParameters();
        if(params){
            return params.from;
        }else {
            return null;
        }
    }

}