import { themes } from 'devui-data';
import { LitState } from 'lit-element-state';

/**
 * This keeps state of the theme (dark/light)
 */
class ThemeState extends LitState {

    constructor() {
        super();
        this._themes = themes;
    }

    static get stateVars() {
        return {
            theme: {}
        };
    }
    
    toggle(){
        if(themeState.theme.name === "dark"){
            this.changeTo("light");
        }else{
            this.changeTo("dark");
        }
    }

    changeTo(themeName){
        const newTheme = new Object();
        newTheme.name = themeName;
        
        var colorMap;
        
        if(themeName==="dark"){
            newTheme.icon = "moon";
            colorMap = this._themes.dark;
        }else{
            newTheme.icon = "sun";
            colorMap = this._themes.light;
        }
        
        for (const [key, value] of Object.entries(colorMap)) {
            document.body.style.setProperty(key, value);
            if(key === "--quarkus-blue"){
                newTheme.quarkusBlue = value;
            }else if(key === "--quarkus-red"){
                newTheme.quarkusRed = value;
            }else if(key === "--quarkus-center"){
                newTheme.quarkusCenter = value;
            }
        }
        
        themeState.theme = newTheme;
    }
}

export const themeState = new ThemeState();