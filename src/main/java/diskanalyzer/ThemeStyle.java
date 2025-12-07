package diskanalyzer;

public enum ThemeStyle {
    AUTO("Auto (Balanced)"),
    CONTENT("Content (True to Image)"),
    EXPRESSIVE("Expressive (High Contrast)"),
    FIDELITY("Fidelity (Natural)"),
    FRUIT_SALAD("Fruit Salad (Vibrant)"),
    NEUTRAL("Neutral (Professional)"),
    TONAL_SPOT("Tonal Spot (Focused)"),
    MONOCHROME("Monochrome (Clean)");

    private final String displayName;

    ThemeStyle(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}