import core.ScreenManager;
import util.AssetLoader;
public class Main {
    public static void main(String[] args) {
        // טוענים את הפונט והמשאבים
        AssetLoader.loadResources();

        new ScreenManager();
    }
}