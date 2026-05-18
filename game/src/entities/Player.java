package entities;

import core.GameWindow;
import input.KeyHandler;
import java.awt.*;
import java.awt.image.BufferedImage;

public class Player {

    public int x, y;
    public int speed = 4;
    private final int PLAYER_SIZE = 90;

    private BufferedImage[] upFrames, downFrames, leftFrames, rightFrames;
    private String direction = "down";
    private int spriteCounter = 0;
    private int spriteNum = 1;
    private KeyHandler keyH;

    public Player(KeyHandler keyH) {
        this.keyH = keyH;
        this.x = (GameWindow.WIDTH / 2) - (PLAYER_SIZE / 2);
        this.y = (GameWindow.HEIGHT / 2) - (PLAYER_SIZE / 2);
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getSpeed() { return speed; }

    public Rectangle getBounds(int nextX, int nextY) {
        int hitboxWidth = PLAYER_SIZE / 2;
        int hitboxHeight = PLAYER_SIZE / 3;
        int offsetX = PLAYER_SIZE / 4;
        int offsetY = (PLAYER_SIZE / 3) * 2;
        return new Rectangle(nextX + offsetX, nextY + offsetY, hitboxWidth, hitboxHeight);
    }

    public void setSprites(BufferedImage[] up, BufferedImage[] down, BufferedImage[] left, BufferedImage[] right) {
        this.upFrames = up;
        this.downFrames = down;
        this.leftFrames = left;
        this.rightFrames = right;
    }

    public void update() {
        if (keyH.upPressed || keyH.downPressed || keyH.leftPressed || keyH.rightPressed) {
            if (keyH.upPressed) direction = "up";
            else if (keyH.downPressed) direction = "down";
            else if (keyH.leftPressed) direction = "left";
            else if (keyH.rightPressed) direction = "right";

            spriteCounter++;
            if (spriteCounter > 10) {
                spriteNum++;
                if (spriteNum > 3) spriteNum = 1;
                spriteCounter = 0;
            }
        } else {
            spriteNum = 1;
        }
    }

    public void draw(Graphics2D g2) {
        BufferedImage image = null;
        switch (direction) {
            case "up": image = getImage(upFrames); break;
            case "down": image = getImage(downFrames); break;
            case "left": image = getImage(leftFrames); break;
            case "right": image = getImage(rightFrames); break;
        }

        if (image != null) {
            g2.drawImage(image, x, y, PLAYER_SIZE, PLAYER_SIZE, null);
        } else {
            g2.setColor(Color.WHITE);
            g2.fillRect(x, y, PLAYER_SIZE, PLAYER_SIZE);
        }
    }

    private BufferedImage getImage(BufferedImage[] frames) {
        if (frames == null || frames.length == 0) return null;
        int index = (spriteNum - 1 < frames.length) ? spriteNum - 1 : 0;
        return frames[index];
    }
}