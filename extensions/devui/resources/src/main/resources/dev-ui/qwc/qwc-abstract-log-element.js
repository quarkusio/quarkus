import { QwcHotReloadElement, html, css} from 'qwc-hot-reload-element';
export * from 'lit';

/**
 * This is an abstract component that handle some common event for logs
 */
class QwcAbstractLogElement extends QwcHotReloadElement {
    
    constructor() {
        super();
    }

    connectedCallback() {
        super.connectedCallback();
        this.setAttribute('tabindex', '0');
        this.bindKeyHandler = this._handleKeyPress.bind(this);
        this.bindScrollHandler = this._handleScroll.bind(this);
        this.addEventListener('keydown', this.bindKeyHandler);
        this.addEventListener('wheel', this.bindScrollHandler, { passive: false });
    }
      
    disconnectedCallback() {
        this.removeEventListener('keydown', this.bindKeyHandler);
        this.removeEventListener('wheel', this.bindScrollHandler);
        super.disconnectedCallback();
    }

    _handleScroll(event){
        if (event.ctrlKey) {
            // Prevent the default zoom action when Ctrl + scroll is detected
            event.preventDefault();
            if (event.deltaY < 0) {
              this._handleZoomIn(event);
            } else if (event.deltaY > 0) {
              this._handleZoomOut(event);
            }
          }
    }
    
    _handleKeyPress(event) {
        throw new Error("Method '_handleKeyPress(event)' must be implemented.");
    }
    
    _handleZoomIn(event){
        throw new Error("Method '_handleZoomIn(event)' must be implemented.");
    }
    
    _handleZoomOut(event){
        throw new Error("Method '_handleZoomOut(event)' must be implemented.");
    }

}

export { QwcAbstractLogElement };
