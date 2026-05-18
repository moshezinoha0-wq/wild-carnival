package entities;

import java.awt.*;
import java.awt.image.BufferedImage;
import util.AssetLoader;

public class Booth {
    private int x, y;
    private int size = 160; // גודל כפול מהטייל (80 * 2) כי הדוכן נראה גדול
    private BufferedImage image;

    public Booth(int x, int y) {
        this.x = x;
        this.y = y;
        this.image = AssetLoader.loadImage("images/booth.png");
    }

    public void draw(Graphics2D g2) {
        if (image != null) {
            g2.drawImage(image, x, y, size, size, null);
        }
    }

    // בדיקה אם השחקן קרוב מספיק כדי לעשות אינטראקציה
    public boolean isPlayerNear(int playerX, int playerY) {
        // חישוב המרחק בין מרכז השחקן למרכז הדוכן
        int centerX = this.x + (size / 4);
        int centerY = this.y + (size / 2);

        double distance = Math.sqrt(Math.pow(centerX - playerX, 2) + Math.pow(centerY - playerY, 2));

        return distance < 130;
    }

    public Rectangle getBounds() {
        // נניח שהחלק ה"חוסם" של הדוכן הוא רק החלק התחתון (העץ)
        // אם הדוכן הוא 160x160, נשים את התיבה רק ב-80 הפיקסלים התחתונים
        return new Rectangle(x, y + 80, size, size - 80);
    }
}