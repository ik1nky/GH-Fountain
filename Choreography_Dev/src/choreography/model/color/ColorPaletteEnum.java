package choreography.model.color;

/**
 * Enumerated type definition for the 16 default colors.
 *
 * @author Danny Selgo
 */


/**
 * These colors are the "Legacy Colors"  They are the
 * first 16 colors of the color Palette and are not supposed
 * to be changed
 */
public enum ColorPaletteEnum {
	
    RED ("#FF0000"), 
    ORANGE ("#FF7700"),
    YELLOW ("#FFC800"), 
    GREEN ("#00F000"), 
    BLUE ("#0000FF"), 
    VIOLET ("#EF2BFF"),
    LIGHTRED ("#FF7272"),
    LIGHTORANGE ("#F59745"),
    LIGHTYELLOW ("#FFFF08"),
    LIGHTGREEN ("#3BFF00"), 
    LIGHTBLUE ("#8F8FFF"),
    LIGHTVIOLET ("#9646FF"), 
    MAGENTA ("#D100C5"), 
    CYAN ("#0096EF"), 
    OFF ("#000000"), 
    WHITE ("#FFFFFF");
    
    private String color;
    
    private ColorPaletteEnum(String color) {
        this.color = color;
    }
    
    public String getColor() {
        return color;
    }
}
