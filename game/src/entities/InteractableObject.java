package entities;

import java.awt.*;
import java.awt.image.BufferedImage;

public class InteractableObject {
    private int x, y, size;
    private BufferedImage image;
    private String name;
    private boolean isPlayerNear = false;

    //מטודה ליצירת אובייקטים
    public InteractableObject(String name, int x, int y, int size, BufferedImage image) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.size = size;
        this.image = image;
    }

    public void update(int playerX, int playerY) {
        // חישוב מרחק פשוט בין מרכז השחקן למרכז האובייקט
        int centerX = x + size / 2;
        int centerY = y + size / 2;
        double distance = Math.sqrt(Math.pow(centerX - playerX, 2) + Math.pow(centerY - playerY, 2));

        // אם השחקן ברדיוס של 100 פיקסלים
        isPlayerNear = distance < 100;
    }

    public void draw(Graphics2D g2) {
        if (image != null) {
            g2.drawImage(image, x, y, size, size, null);
        }

        if (isPlayerNear) {
            g2.setColor(Color.YELLOW);
            g2.setFont(new Font("Arial", Font.BOLD, 20));
            g2.drawString("E", x + (size / 2) - 5, y - 10);
        }
    }

    public boolean isPlayerNear() { return isPlayerNear; }
    public String getName() { return name; }
}
