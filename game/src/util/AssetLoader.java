package util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.swing.ImageIcon;
import java.awt.Image;
import javax.imageio.ImageIO;

public class AssetLoader {

    private static Font gameFont;

    public static void loadResources() {
        // טעינת הפונט מתוך ה-Resources
        try (InputStream is = AssetLoader.class.getResourceAsStream("/fonts/VCR_OSD_MONO_1.001.ttf")) {
            if (is == null) {
                throw new IOException("Font file not found in resources");
            }
            gameFont = Font.createFont(Font.TRUETYPE_FONT, is);

            // רישום הפונט במערכת הגרפית של Java
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(gameFont);

        } catch (FontFormatException | IOException e) {
            System.out.println("נכשל בטעינת הפונט, משתמש בפונט ברירת מחדל.");
            e.printStackTrace();
            // אם הטעינה נכשלת, נשתמש בפונט ברירת מחדל של המערכת
            gameFont = new Font("Arial", Font.PLAIN, 18);
        }
    }

    public static BufferedImage loadImage(String path) {
        // טעינת תמונה מתוך ה-Resources (הנתיב מתחיל ב-/)
        try (InputStream is = AssetLoader.class.getResourceAsStream("/" + path)) {
            if (is == null) {
                System.out.println("נכשל בטעינת תמונה בנתיב: " + path);
                return null;
            }
            return ImageIO.read(is);
        } catch (IOException e) {
            System.out.println("שגיאה בקריאת הקובץ בנתיב: " + path);
            e.printStackTrace();
            return null;
        }
    }

    // פונקציה לקבלת הפונט בגודל רצוי
    public static Font getFont(float size) {
        if (gameFont == null) {
            return new Font("Arial", Font.PLAIN, (int) size);
        }
        return gameFont.deriveFont(size);
    }

    // פונקציה להגדלת labels
    public static ImageIcon getScaledIcon(String path, int width, int height) {
        // משתמש ב-loadImage הפנימי כדי לוודא טעינה מה-resources
        BufferedImage img = loadImage(path);
        if (img != null) {
            Image scaled = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        }

        // גיבוי למקרה שהטעינה נכשלה
        return new ImageIcon();
    }
}