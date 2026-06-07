package screens;

import core.ScreenManager;
import core.GameWindow;
import core.GameState;
import entities.Player;
import entities.Booth;
import input.KeyHandler;
import util.AssetLoader;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class GamePanel extends JPanel implements Runnable {

    private ScreenManager screenManager;
    private Thread gameThread;
    private boolean running = false;

    // הגדרות לוח המשחק
    private final int TILE_SIZE = 80;
    private final int MAX_COL = 16;
    private final int MAX_ROW = 9;

    // נכסים גרפיים
    private BufferedImage sand1, sand2, sand3;
    private BufferedImage playerSheet;
    private BufferedImage boothCharacterImage;
    private float alpha = 1.0f;

    // אובייקטי משחק
    private KeyHandler keyH;
    private Player player;
    private entities.Booth carnivalBooth;
    private util.DialogueManager dialogueManager;

    private int selectedOption = 0; // 0 = YES, 1 = NO

    private int vnImageX = 700;
    private int vnImageY = 0;
    private int vnImageW = 700;
    private int vnImageH = 720;

    private final int[][] mapData = {
            {1,0,2,1,0,0,2,1,1,0,2,2,0,1,0,1},
            {2,1,0,0,1,2,1,0,2,2,1,0,1,0,2,1},
            {0,2,1,2,1,0,2,1,0,1,0,2,1,2,0,0},
            {1,0,2,1,0,2,1,1,0,2,2,0,1,0,1,2},
            {2,1,0,0,1,2,1,0,2,2,1,0,1,0,2,1},
            {0,1,2,2,1,0,2,1,0,1,0,2,1,2,0,0},
            {1,0,2,1,0,2,1,1,0,2,2,0,1,0,1,2},
            {2,1,0,0,1,2,1,0,2,2,1,0,1,0,2,1},
            {0,2,1,2,1,0,2,1,0,1,0,2,1,2,0,0}
    };
    //מהתחל את הנכסים של הפנאל
    public GamePanel(ScreenManager screenManager, KeyHandler keyH) {
        this.screenManager = screenManager;
        this.keyH = keyH;

        this.setPreferredSize(new Dimension(GameWindow.WIDTH, GameWindow.HEIGHT));
        this.setBackground(Color.DARK_GRAY);
        this.setFocusable(true);
        this.addKeyListener(this.keyH);
        this.dialogueManager = new util.DialogueManager(screenManager.getMusicPlayer());

        player = new entities.Player(this.keyH);
        carnivalBooth = new entities.Booth(1100, 50);

        loadImages();
        setupPlayer();
    }
    //טוען את הנכסים הגרפים
    private void loadImages() {
        try {
            sand1 = AssetLoader.loadImage("images/sand1.png");
            sand2 = AssetLoader.loadImage("images/sand2.png");
            sand3 = AssetLoader.loadImage("images/sand3.png");
            playerSheet = AssetLoader.loadImage("images/player_sheet.png");
            boothCharacterImage = AssetLoader.loadImage("images/booth_keeper.png");
        } catch (Exception e) {
            System.out.println("Error loading images: " + e.getMessage());
        }
    }
    //עושה את הפריימים של הדמות
    private void setupPlayer() {
        if (this.playerSheet == null) return;
        int w = playerSheet.getWidth() / 3;
        int h = playerSheet.getHeight() / 4;

        BufferedImage[] up = { playerSheet.getSubimage(0, h * 3, w, h), playerSheet.getSubimage(w, h * 3, w, h) };
        BufferedImage[] down = { playerSheet.getSubimage(0, 0, w, h), playerSheet.getSubimage(w, 0, w, h) };
        BufferedImage[] left = { playerSheet.getSubimage(0, h, w, h), playerSheet.getSubimage(w, h, w, h), playerSheet.getSubimage(w * 2, h, w, h) };
        BufferedImage[] right = { playerSheet.getSubimage(0, h * 2, w, h), playerSheet.getSubimage(w, h * 2, w, h), playerSheet.getSubimage(w * 2, h * 2, w, h) };

        player.setSprites(up, down, left, right);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        alpha = 1.0f;
        startFadeIn();
        startGameThread();
        this.requestFocusInWindow();
    }

    private void startGameThread() {
        if (gameThread == null || !running) {
            running = true;
            gameThread = new Thread(this);
            gameThread.start();
        }
    }

    @Override
    public void run() {
        double drawInterval = 1000000000 / 60;
        double nextDrawTime = System.nanoTime() + drawInterval;

        while (running) {
            update();
            repaint();

            try {
                double remainingTime = (nextDrawTime - System.nanoTime()) / 1000000;
                if (remainingTime < 0) remainingTime = 0;
                Thread.sleep((long) remainingTime);
                nextDrawTime += drawInterval;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void update() {
        // 1. לוגיקת ESC
        if (keyH.escPressed) {
            keyH.escPressed = false;
            keyH.resetKeys();
            HowToPlayPanel htp = (HowToPlayPanel) screenManager.getPanel(GameState.HOW_TO_PLAY);
            htp.setFromGame(true);
            screenManager.showScreen(GameState.HOW_TO_PLAY);
            return;
        }

        // 2. אינטראקציה [E] וניהול בחירה
        if (keyH.ePressed) {
            keyH.ePressed = false;

            if (screenManager.getCurrentState() == GameState.DIALOGUE) {
                if (!dialogueManager.isTyping()) {
                    if (dialogueManager.hasMoreLines()) {
                        dialogueManager.nextLine();
                    } else if (dialogueManager.isWaitingForChoice()) {
                        // ביצוע הפעולה לפי הבחירה
                        if (selectedOption == 0) {
                            // העברה למסך המיני-משחק החדש שחיברנו ב-ScreenManager
                            dialogueManager.resetChoice();
                            screenManager.showScreen(GameState.MINI_GAME);
                        } else {
                            // חזרה למפה הרגילה
                            dialogueManager.resetChoice();
                            screenManager.showScreen(GameState.GAME);
                        }
                    } else {
                        // אם נגמרו השורות, המנהל עובר למצב המתנה לבחירה (Yes/No)
                        dialogueManager.nextLine();
                    }
                }
            }
            else if (carnivalBooth != null && carnivalBooth.isPlayerNear(player.getX(), player.getY())) {
                screenManager.getMusicPlayer().stopMusic();
                screenManager.showScreen(core.GameState.DIALOGUE);
                screenManager.getMusicPlayer().playBackgroundMusic("game/resources/sounds/booth_ost.wav", 0.5f);

                String[] lines = {
                        "Welcome to the Carnival!",
                        "Do you want to see something strange?",
                        "Everything has a price. Do you want to play?"
                };
                dialogueManager.setDialogue(lines);
                dialogueManager.nextLine();
                selectedOption = 0; // איפוס הבחירה לברירת מחדל (YES)
            }
        }

        // ניהול מקשי חיצים לבחירת אופציה (למעלה/למטה) רק בזמן שהדיאלוג מחכה להחלטה
        if (screenManager.getCurrentState() == GameState.DIALOGUE && dialogueManager.isWaitingForChoice()) {
            if (keyH.upPressed) { selectedOption = 0; keyH.upPressed = false; }
            if (keyH.downPressed) { selectedOption = 1; keyH.downPressed = false; }
        }

        // 3. עדכון תנועת שחקן - חסום בזמן דיאלוג
        if (screenManager.getCurrentState() != GameState.DIALOGUE) {
            if (player != null && carnivalBooth != null) {
                int nextX = player.getX();
                int nextY = player.getY();
                int speed = player.getSpeed();
                int playerSize = 90;

                if (keyH.upPressed) nextY -= speed;
                if (keyH.downPressed) nextY += speed;
                if (keyH.leftPressed) nextX -= speed;
                if (keyH.rightPressed) nextX += speed;

                boolean withinBounds = nextX >= 0 &&
                        nextX <= GameWindow.WIDTH - playerSize &&
                        nextY >= 0 &&
                        nextY <= GameWindow.HEIGHT - playerSize;

                Rectangle nextBounds = player.getBounds(nextX, nextY);
                Rectangle boothBounds = carnivalBooth.getBounds();
                boolean collisionWithBooth = nextBounds.intersects(boothBounds);

                if (withinBounds && !collisionWithBooth) {
                    player.x = nextX;
                    player.y = nextY;
                }
                player.update();
            }
        }
    }

    private void startFadeIn() {
        new Thread(() -> {
            while (alpha > 0) {
                alpha -= 0.02f;
                if (alpha < 0) alpha = 0;
                repaint();
                try { Thread.sleep(30); } catch (InterruptedException e) {}
            }
        }).start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        drawMap(g2);
        if (carnivalBooth != null) carnivalBooth.draw(g2);
        if (player != null) {
            player.draw(g2);

            if (carnivalBooth != null && carnivalBooth.isPlayerNear(player.getX(), player.getY())) {
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Arial", Font.BOLD, 18));
                String hintText = "Press [E] to interact";
                int textWidth = g2.getFontMetrics().stringWidth(hintText);
                int xPos = player.getX() + (90 / 2) - (textWidth / 2);
                int yPos = player.getY() - 15;
                g2.drawString(hintText, xPos, yPos);
            }
        }

        if (screenManager.getCurrentState() == GameState.DIALOGUE) {
            drawDialogueScreen(g2);
        }
    }

    private void drawDialogueScreen(Graphics2D g2) {
        if (boothCharacterImage != null) {
            g2.drawImage(boothCharacterImage, vnImageX, vnImageY, vnImageW, vnImageH, null);
        }

        g2.setColor(new Color(0, 0, 0, 200));
        g2.fillRoundRect(50, 500, 1180, 180, 20, 20);

        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(3));
        g2.drawRoundRect(50, 500, 1180, 180, 20, 20);

        g2.setFont(new Font("Arial", Font.PLAIN, 28));

        if (dialogueManager != null) {
            if (dialogueManager.isWaitingForChoice()) {
                // ציור האופציות לבחירה
                g2.setFont(new Font("Arial", Font.BOLD, 32));

                g2.setColor(selectedOption == 0 ? Color.YELLOW : Color.WHITE);
                g2.drawString(selectedOption == 0 ? "> YES, LET'S PLAY" : "  YES, LET'S PLAY", 100, 570);

                g2.setColor(selectedOption == 1 ? Color.YELLOW : Color.WHITE);
                g2.drawString(selectedOption == 1 ? "> NO, I'M SCARED" : "  NO, I'M SCARED", 100, 630);
            } else {
                g2.setColor(Color.WHITE);
                g2.drawString(dialogueManager.getVisibleText(), 80, 550);
            }
        }
    }

    @Override
    protected void paintChildren(Graphics g) {
        super.paintChildren(g);
        if (alpha > 0) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }

    private void drawMap(Graphics2D g2) {
        for (int row = 0; row < MAX_ROW; row++) {
            for (int col = 0; col < MAX_COL; col++) {
                int x = col * TILE_SIZE;
                int y = row * TILE_SIZE;
                BufferedImage image = null;
                if (mapData[row][col] == 0) image = sand1;
                else if (mapData[row][col] == 1) image = sand2;
                else if (mapData[row][col] == 2) image = sand3;

                if (image != null) {
                    g2.drawImage(image, x, y, TILE_SIZE, TILE_SIZE, null);
                }
            }
        }
    }
}