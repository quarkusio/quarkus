export default class SchemaPage{
    constructor(containerId) {
        this.containerId = containerId;
        Object.getOwnPropertyNames(SchemaPage.prototype).forEach((key) => {
            if (key !== 'constructor') {
                this[key] = this[key].bind(this);
            }
        });
    }

    // TODO: stub. must be implemented by all pages
    open(){

    }

}