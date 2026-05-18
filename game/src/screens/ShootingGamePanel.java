package screens;

import core.ScreenManager;
import core.GameWindow;
import core.GameState;
import util.AssetLoader;
import util.SoundPlayer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class ShootingGamePanel extends JPanel implements Runnable {

    private ScreenManager screenManager;
    private SoundPlayer soundPlayer;
    private Thread gameThread;
    private boolean running = false;

    private BufferedImage background, targetSprite, customCursor;
    private double score = 0.0;

    private final int TARGET_SIZE = 140;
    private final int SPAWN_INTERVAL = 700;

    private int cursorSize = 50; // הגודל הרגיל של הכוונת
    private final int BASE_CURSOR_SIZE = 50; // גודל הבסיס הקבוע

    private int timeLeft = 60;
    private long lastTimerUpdate;
    private boolean gameOver = false;

    private ArrayList<Target> targets = new ArrayList<>();
    private ArrayList<BulletHole> bulletHoles = new ArrayList<>(); // רשימת חורי ירי
    private ArrayList<FloatingText> floatingTexts = new ArrayList<>(); // רשימת טקסט צף

    private Random random = new Random();
    private long lastTargetSpawn;

    public ShootingGamePanel(ScreenManager screenManager) {
        this.screenManager = screenManager;
        this.soundPlayer = screenManager.getMusicPlayer();
        this.setPreferredSize(new Dimension(GameWindow.WIDTH, GameWindow.HEIGHT));

        loadAssets();
        hideStandardCursor();

        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!gameOver) {
                    cursorSize = 70; // מגדילים את הכוונת ל-70 פיקסלים ברגע הלחיצה
                    checkHit(e.getX(), e.getY());
                }
            }
        });
    }

    private void loadAssets() {
        try {
            background = AssetLoader.loadImage("images/shooting_bg.png");
            targetSprite = AssetLoader.loadImage("images/target_animation.png");
            customCursor = AssetLoader.loadImage("images/crosshair.png");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void hideStandardCursor() {
        BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(
                cursorImg, new Point(0, 0), "blank cursor");
        this.setCursor(blankCursor);
    }

    public void start() {
        resetGame();
        running = true;
        gameThread = new Thread(this);
        gameThread.start();
        soundPlayer.playBackgroundMusic("game/resources/sounds/minigame_theme.wav", 0.6f);
    }

    private void resetGame() {
        score = 0.0;
        timeLeft = 60;
        gameOver = false;
        targets.clear();
        bulletHoles.clear();
        floatingTexts.clear();
        lastTimerUpdate = System.currentTimeMillis();
    }

    @Override
    public void run() {
        while (running) {
            if (!gameOver) update();
            repaint();
            try { Thread.sleep(16); } catch (InterruptedException e) {}
        }
    }

    private void update() {
        if (screenManager.keyH.escPressed) {
            screenManager.keyH.escPressed = false;
            screenManager.showScreen(GameState.HOW_TO_PLAY);
            return;
        }

        if (cursorSize > BASE_CURSOR_SIZE) {
            cursorSize -= 2; // מקטין את הכוונת ב-2 פיקסלים בכל פריים עד שהיא חוזרת ל-50
        }

        if (System.currentTimeMillis() - lastTimerUpdate >= 1000) {
            timeLeft--;
            lastTimerUpdate = System.currentTimeMillis();
            if (timeLeft <= 0) endGame();
        }

        // שינוי: מטרות נוצרות רק אם המשחק לא נגמר
        if (!gameOver && System.currentTimeMillis() - lastTargetSpawn > SPAWN_INTERVAL) {
            spawnTarget();
            lastTargetSpawn = System.currentTimeMillis();
        }

        // עדכון מטרות
        Iterator<Target> it = targets.iterator();
        while (it.hasNext()) {
            Target t = it.next();
            t.update();
            if (t.isDead()) it.remove();
        }

        // עדכון טקסט צף (פייד אאוט)
        Iterator<FloatingText> ftIt = floatingTexts.iterator();
        while (ftIt.hasNext()) {
            FloatingText ft = ftIt.next();
            ft.update();
            if (ft.alpha <= 0) ftIt.remove();
        }
    }

    private void spawnTarget() {
        int maxAttempts = 15;
        for (int i = 0; i < maxAttempts; i++) {
            int x = random.nextInt(GameWindow.WIDTH - TARGET_SIZE - 40) + 20;
            int y = random.nextInt(GameWindow.HEIGHT - TARGET_SIZE - 200) + 50;
            Rectangle newRect = new Rectangle(x, y, TARGET_SIZE, TARGET_SIZE);

            boolean overlap = false;
            for (Target t : targets) {
                if (newRect.intersects(t.getBounds())) {
                    overlap = true;
                    break;
                }
            }

            if (!overlap) {
                targets.add(new Target(x, y, TARGET_SIZE, TARGET_SIZE));
                break;
            }
        }
    }

    private void checkHit(int mx, int my) {
        boolean hitSomething = false; // משתנה למעקב אחרי פגיעה

        for (Target t : targets) {
            if (t.getBounds().contains(mx, my) && t.canBeHit()) {
                // חישוב מרחק וניקוד
                double centerX = t.x + (t.width / 2.0);
                double centerY = t.y + (t.height / 2.0);
                double distance = Math.sqrt(Math.pow(mx - centerX, 2) + Math.pow(my - centerY, 2));
                double maxDistance = t.width / 2.0;

                double accuracyBonus = 10.0 * (1.0 - (distance / maxDistance));
                if (accuracyBonus < 1.0) accuracyBonus = 1.0;

                score += accuracyBonus;

                // טקסט צף רק בפגיעה
                floatingTexts.add(new FloatingText(mx, my, String.format("+%.1f", accuracyBonus)));

                t.hit();
                hitSomething = true; // סימון שהייתה פגיעה
                break;
            }
        }

        // אם עברנו על כל המטרות ולא פגענו בכלום - נוסיף חור ירי לרקע
        if (!hitSomething) {
            bulletHoles.add(new BulletHole(mx, my));
        }

        // הפעלת סאונד הירייה בכל מקרה (גם בפגיעה וגם בפספוס)
        soundPlayer.playSFX("game/resources/sounds/gunshot.wav", false);
    }

    private void endGame() {
        gameOver = true;
        // ניקוי המטרות שנותרו כדי שלא ימשיכו לגדול/להבהב
        targets.clear();

        SwingUtilities.invokeLater(() -> {
            String finalScore = String.format("%.2f", score);
            int choice = JOptionPane.showConfirmDialog(this,
                    "Game Over! Final Score: " + finalScore + "\nPlay again?",
                    "Results", JOptionPane.YES_NO_OPTION);

            if (choice == JOptionPane.YES_OPTION) {
                resetGame();
            } else {
                running = false;
                screenManager.showScreen(GameState.GAME);
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.drawImage(background, 0, 0, getWidth(), getHeight(), null);

        // ציור חורי ירי (Bullet Holes)
        g2.setColor(new Color(40, 40, 40)); // צבע שחור-פחם
        for (BulletHole hole : bulletHoles) {
            g2.fillOval(hole.x - 4, hole.y - 4, 8, 8);
        }

        for (Target t : targets) {
            t.draw(g2, targetSprite);
        }

        // ציור טקסט צף
        for (FloatingText ft : floatingTexts) {
            ft.draw(g2);
        }

        // ממשק
        g2.setColor(Color.RED);
        g2.setFont(new Font("Monospaced", Font.BOLD, 30));
        g2.drawString("TIME: " + timeLeft, GameWindow.WIDTH / 2 - 60, 40);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 35));
        g2.drawString(String.format("SCORE: %.2f", score), 30, 45);

        Point mousePos = getMousePosition();
        if (mousePos != null && customCursor != null) {
            // משתמשים ב-cursorSize גם למיקום (כדי שהיא תישאר ממורכזת) וגם לגודל
            int halfSize = cursorSize / 2;
            g2.drawImage(customCursor, mousePos.x - halfSize, mousePos.y - halfSize, cursorSize, cursorSize, null);
        }

    }

    // קלאס עזר לחורי ירי
    private class BulletHole {
        int x, y;
        public BulletHole(int x, int y) { this.x = x; this.y = y; }
    }

    // קלאס עזר לטקסט צף עם פייד
    private class FloatingText {
        int x, y;
        String text;
        float alpha = 1.0f;
        int yOffset = 0;

        public FloatingText(int x, int y, String text) {
            this.x = x; this.y = y; this.text = text;
        }

        public void update() {
            alpha -= 0.02f; // מהירות הפייד
            yOffset -= 1;   // תנועה כלפי מעלה
        }

        public void draw(Graphics2D g2) {
            if (alpha < 0) alpha = 0;
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2.setColor(Color.YELLOW);
            g2.setFont(new Font("Arial", Font.BOLD, 25));
            g2.drawString(text, x, y + yOffset);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f)); // איפוס שקיפות
        }
    }

    private class Target {
        int x, y, width, height;
        int state = 0; // 0=entry, 1=idle, 2=exit
        long stateStartTime;
        boolean isHit = false;

        public Target(int x, int y, int w, int h) {
            this.x = x; this.y = y;
            this.width = w; this.height = h;
            this.stateStartTime = System.currentTimeMillis();
        }

        public void update() {
            long elapsed = System.currentTimeMillis() - stateStartTime;
            if (state == 0 && elapsed > 300) { state = 1; stateStartTime = System.currentTimeMillis(); }
            else if (state == 1 && elapsed > 1000) { state = 2; stateStartTime = System.currentTimeMillis(); }
            else if (state == 2 && elapsed > 300) { state = 3; }
        }

        public void hit() { isHit = true; state = 2; stateStartTime = System.currentTimeMillis(); }
        public boolean isDead() { return state == 3; }
        public boolean canBeHit() { return state == 1 && !isHit; }
        public Rectangle getBounds() { return new Rectangle(x, y, width, height); }

        public void draw(Graphics2D g2, BufferedImage img) {
            float scale = 1.0f;
            if (state == 0) scale = (System.currentTimeMillis() - stateStartTime) / 300f;
            if (state == 2) scale = 1.0f - ((System.currentTimeMillis() - stateStartTime) / 300f);

            int drawW = (int)(width * Math.max(0, scale));
            int drawH = (int)(height * Math.max(0, scale));
            int offsetX = (width - drawW) / 2;
            int offsetY = (height - drawH) / 2;

            g2.drawImage(img, x + offsetX, y + offsetY, drawW, drawH, null);
        }
    }
}