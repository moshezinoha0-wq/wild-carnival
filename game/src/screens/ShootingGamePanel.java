package screens;

import core.ScreenManager;
import core.GameWindow;
import core.GameState;
import util.AssetLoader;
import util.SoundPlayer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class ShootingGamePanel extends JPanel implements Runnable {
    //משתנים תכנים לריצת המשחק
    private ScreenManager screenManager;
    private SoundPlayer soundPlayer;
    private Thread gameThread;
    private boolean running = false;

    // מנגנון מצב עצירה
    private boolean isPaused = false;
    private Rectangle resumeBtn, restartBtn, exitBtn;
    private int hoveredButton = 0; // 1 = Resume, 2 = Restart, 3 = Exit, 0 = None

    private BufferedImage background, targetSprite, customCursor;
    private double score = 0.0;
    //מנגנון המטרות
    private final int TARGET_SIZE = 200;
    private final int SPAWN_INTERVAL = 500;
    //מנגנון הטיימר
    private int timeLeft = 60;
    private long lastTimerUpdate;
    private boolean gameOver = false;
    //מנגנון הספירה לאחור
    private boolean isCountingDown = true;
    private int countdownValue = 3;
    private long lastCountdownUpdate;
    private float countdownScale = 1.0f; // עבור אפקט פעימה קטן למספרים

    private ArrayList<Target> targets = new ArrayList<>();
    private ArrayList<BulletHole> bulletHoles = new ArrayList<>();
    private ArrayList<FloatingText> floatingTexts = new ArrayList<>();

    private Random random = new Random();
    private long lastTargetSpawn;

    // כוונת דינמית
    private int cursorSize = 50;
    private final int BASE_CURSOR_SIZE = 50;
    //הבנאי (מכיל אצ המאזין של העכבר ושל המקשים)
    public ShootingGamePanel(ScreenManager screenManager) {
        this.screenManager = screenManager;
        this.soundPlayer = screenManager.getMusicPlayer();
        this.setPreferredSize(new Dimension(GameWindow.WIDTH, GameWindow.HEIGHT));
        this.setFocusable(true);

        loadAssets();       //טעינת התמונות של המשחק
        hideStandardCursor();       //מחביא את הסימן של בעכבר
        setupPauseButtons();

        // מאזין עכבר משולב (ליריות וללחיצה על כפתורי העצירה)
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (isPaused) {
                    if (resumeBtn.contains(e.getPoint())) {
                        isPaused = false;
                        lastTimerUpdate = System.currentTimeMillis();
                        lastTargetSpawn = System.currentTimeMillis();
                    } else if (restartBtn.contains(e.getPoint())) {
                        resetGame();
                    } else if (exitBtn.contains(e.getPoint())) {
                        running = false;
                        soundPlayer.stopBackgroundMusic();
                        screenManager.showScreen(GameState.GAME);
                    }
                } else if (!gameOver && !isCountingDown) {
                    cursorSize = 70; // אפקט רתיעה לכוונת
                    checkHit(e.getX(), e.getY());
                }
            }
        });

        // מאזין תנועת עכבר בשביל אפקט Hover לתפריט העצירה
        this.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (isPaused) {
                    if (resumeBtn.contains(e.getPoint())) hoveredButton = 1;
                    else if (restartBtn.contains(e.getPoint())) hoveredButton = 2;
                    else if (exitBtn.contains(e.getPoint())) hoveredButton = 3;
                    else hoveredButton = 0;
                }
            }
        });

        this.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    if (!gameOver && !isCountingDown) {
                        isPaused = !isPaused; // הפיכת מצב עצירה
                        if (isPaused) {
                            soundPlayer.playSFX("game/resources/sounds/button.wav", false);
                        } else {
                            lastTimerUpdate = System.currentTimeMillis();
                            lastTargetSpawn = System.currentTimeMillis();
                        }
                    }
                }
            }
        });
    }
    //הכפתורים של העצירה
    private void setupPauseButtons() {
        int menuW = 400;
        int btnW = 300;
        int btnH = 60;
        int menuX = (GameWindow.WIDTH - menuW) / 2;
        int menuY = (GameWindow.HEIGHT - 350) / 2;
        int btnX = menuX + (menuW - btnW) / 2;

        resumeBtn = new Rectangle(btnX, menuY + 90, btnW, btnH);
        restartBtn = new Rectangle(btnX, menuY + 170, btnW, btnH);
        exitBtn = new Rectangle(btnX, menuY + 250, btnW, btnH);
    }

    //טוען את התמונות
    private void loadAssets() {
        try {
            background = AssetLoader.loadImage("images/shooting_bg.png");
            targetSprite = AssetLoader.loadImage("images/target_animation.png");
            customCursor = AssetLoader.loadImage("images/crosshair.png");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
    //מסתיר את הסימן עכבר
    private void hideStandardCursor() {
        BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(
                cursorImg, new Point(0, 0), "blank cursor");
        this.setCursor(blankCursor);
    }
    //מתחיל את המשחק
    public void start() {
        resetGame();
        running = true;
        gameThread = new Thread(this);
        gameThread.start();
        //מפעיל את המוזיקה
        soundPlayer.playBackgroundMusic("game/resources/sounds/minigame_theme.wav", 0.6f);
    }
    //מאפס את הנתונים למצב התחלתי נקי
    private void resetGame() {
        score = 0.0;
        timeLeft = 60;
        gameOver = false;
        isPaused = false;

        // אתחול משתני הספירה לאחור
        isCountingDown = true;
        countdownValue = 3;
        countdownScale = 1.0f;
        lastCountdownUpdate = System.currentTimeMillis();

        targets.clear();
        bulletHoles.clear();
        floatingTexts.clear();
        lastTimerUpdate = System.currentTimeMillis();
    }
    //game loop
    @Override
    public void run() {
        while (running) {
            update();
            repaint();
            try { Thread.sleep(16); } catch (InterruptedException e) {}//60fps
        }
    }

    private void update() {
        // אם המשחק בעצירה, עוצרים כאן ולא מעדכנים שום לוגיקה או טיימרים
        if (isPaused) {
            return;
        }

        // אנימציית התכווצות הכוונת (רתיעה)
        if (cursorSize > BASE_CURSOR_SIZE) {
            cursorSize -= 2;
        }

        // לוגיקה בזמן ספירה לאחור
        if (isCountingDown) {
            long now = System.currentTimeMillis();
            long elapsed = now - lastCountdownUpdate;

            // אפקט פעימה קטן למספרים (הולכים וקטנים בכל שנייה)
            countdownScale = 1.0f - (elapsed / 1000f);
            if (countdownScale < 0.5f) countdownScale = 0.5f;

            if (elapsed >= 1000) {
                countdownValue--;
                lastCountdownUpdate = now;
                countdownScale = 1.0f;

                if (countdownValue <= 0) {
                    isCountingDown = false;
                    // מאפסים את הזמנים כדי שהמשחק יתחיל בדיוק עכשיו
                    lastTimerUpdate = System.currentTimeMillis();
                    lastTargetSpawn = System.currentTimeMillis();
                }
            }
            return; // עוצרים כאן! לא מעדכנים מטרות או טיימר ראשי בזמן הספירה
        }

        // לוגיקה רגילה של המשחק (רק אחרי שהספירה מסתיימת)
        if (!gameOver) {
            if (System.currentTimeMillis() - lastTimerUpdate >= 1000) {
                timeLeft--;
                lastTimerUpdate = System.currentTimeMillis();
                if (timeLeft <= 0) endGame();
            }

            if (System.currentTimeMillis() - lastTargetSpawn > SPAWN_INTERVAL) {
                spawnTarget();
                lastTargetSpawn = System.currentTimeMillis();
            }

            // עדכון מטרות רק כאשר המשחק לא נגמר
            Iterator<Target> it = targets.iterator();
            while (it.hasNext()) {
                Target t = it.next();
                t.update();
                if (t.isDead()) it.remove();
            }
        }

        // עדכון טקסט צף
        Iterator<FloatingText> ftIt = floatingTexts.iterator();
        while (ftIt.hasNext()) {
            FloatingText ft = ftIt.next();
            ft.update();
            if (ft.alpha <= 0) ftIt.remove();
        }
    }
    //מזמן את המטרות
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
    //בודק עם המטרה נפגעה
    private void checkHit(int mx, int my) {
        boolean hitSomething = false;
        for (Target t : targets) {
            if (t.getBounds().contains(mx, my) && t.canBeHit()) {
                double centerX = t.x + (t.width / 2.0);
                double centerY = t.y + (t.height / 2.0);

                double distance = Math.sqrt(Math.pow(mx - centerX, 2) + Math.pow(my - centerY, 2));
                double maxDistance = t.width / 2.0;

                double accuracyBonus = 10.0 * (1.0 - (distance / maxDistance));
                if (accuracyBonus < 1.0) accuracyBonus = 1.0;

                score += accuracyBonus;
                floatingTexts.add(new FloatingText(mx, my, String.format("+%.1f", accuracyBonus)));

                t.hit();
                hitSomething = true;
                break;
            }
        }

        if (!hitSomething) {
            bulletHoles.add(new BulletHole(mx, my));
        }

        soundPlayer.playSFX("game/resources/sounds/gunshot.wav", false);
    }
    //סוף המשחק
    private void endGame() {
        gameOver = true;
        // לא מוחקים כאן את המטרות מיד, כדי שהשחקן יראה את המצב האחרון ברקע כשהחלון קופץ

        SwingUtilities.invokeLater(() -> {
            String finalScore = String.format("%.2f", score);
            int choice = JOptionPane.showConfirmDialog(this,
                    "Game Over! Final Score: " + finalScore + "\nPlay again?",
                    "Results", JOptionPane.YES_NO_OPTION);

            if (choice == JOptionPane.YES_OPTION) {
                resetGame();
            } else {
                running = false;
                targets.clear(); // מנקים רק כשיוצאים סופית
                screenManager.showScreen(GameState.GAME);
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // ציור הרקע
        g2.drawImage(background, 0, 0, getWidth(), getHeight(), null);

        // ציור חורי ירי
        g2.setColor(new Color(40, 40, 40));
        for (BulletHole hole : bulletHoles) {
            g2.fillOval(hole.x - 4, hole.y - 4, 8, 8);
        }

        // ציור מטרות
        for (Target t : targets) {
            t.draw(g2, targetSprite);
        }

        // ציור טקסט צף
        for (FloatingText ft : floatingTexts) {
            ft.draw(g2);
        }

        // ממשק עליון קבוע (ניקוד וטיימר)
        g2.setColor(Color.RED);
        g2.setFont(new Font("Monospaced", Font.BOLD, 30));
        g2.drawString("TIME: " + timeLeft, GameWindow.WIDTH / 2 - 60, 40);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 35));
        g2.drawString(String.format("SCORE: %.2f", score), 30, 45);

        // ציור מסך הספירה לאחור במידה והוא פעיל
        if (isCountingDown) {
            // שכבת עמעום קלה על המסך כדי להבליט את המספרים
            g2.setColor(new Color(0, 0, 0, 100));
            g2.fillRect(0, 0, getWidth(), getHeight());

            // הגדרת פונט ענק משתנה לפי ה-scale
            int fontSize = (int) (120 * countdownScale);
            g2.setFont(new Font("Arial", Font.BOLD, fontSize));
            g2.setColor(Color.YELLOW);

            String text = String.valueOf(countdownValue);
            FontMetrics fm = g2.getFontMetrics();
            int textX = (GameWindow.WIDTH - fm.stringWidth(text)) / 2;
            int textY = (GameWindow.HEIGHT / 2) + (fm.getAscent() / 2) - 50;

            g2.drawString(text, textX, textY);
        }

        // שכבת תפריט עצירה (Pause Menu)
        if (isPaused) {
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRect(0, 0, getWidth(), getHeight());

            int menuW = 400;
            int menuH = 350;
            int menuX = (GameWindow.WIDTH - menuW) / 2;
            int menuY = (GameWindow.HEIGHT - menuH) / 2;

            g2.setColor(new Color(30, 30, 30, 230));
            g2.fillRoundRect(menuX, menuY, menuW, menuH, 20, 20);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(3));
            g2.drawRoundRect(menuX, menuY, menuW, menuH, 20, 20);

            g2.setFont(new Font("Arial", Font.BOLD, 40));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString("PAUSED", menuX + (menuW - fm.stringWidth("PAUSED")) / 2, menuY + 55);

            drawPauseButton(g2, resumeBtn, "Resume", hoveredButton == 1);
            drawPauseButton(g2, restartBtn, "Restart", hoveredButton == 2);
            drawPauseButton(g2, exitBtn, "Back to Game", hoveredButton == 3);
        }

        // כוונת עכבר דינמית
        Point mousePos = getMousePosition();
        if (mousePos != null && customCursor != null) {
            int halfSize = cursorSize / 2;
            g2.drawImage(customCursor, mousePos.x - halfSize, mousePos.y - halfSize, cursorSize, cursorSize, null);
        }
    }
    //כפתורים עצירה
    private void drawPauseButton(Graphics2D g2, Rectangle r, String text, boolean isHovered) {
        g2.setColor(isHovered ? Color.YELLOW : Color.DARK_GRAY);
        g2.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);
        g2.setColor(Color.WHITE);
        g2.drawRoundRect(r.x, r.y, r.width, r.height, 10, 10);

        g2.setFont(new Font("Arial", Font.BOLD, 25));
        g2.setColor(isHovered ? Color.BLACK : Color.WHITE);
        FontMetrics fm = g2.getFontMetrics();
        int tx = r.x + (r.width - fm.stringWidth(text)) / 2;
        int ty = r.y + ((r.height - fm.getHeight()) / 2) + fm.getAscent();
        g2.drawString(text, tx, ty);
    }
    //מבקש פוקוס
    @Override
    public void addNotify() {
        super.addNotify();
        requestFocusInWindow();
    }

    // קלאסי עזר פנימיים
    private class BulletHole {
        int x, y;
        public BulletHole(int x, int y) { this.x = x; this.y = y; }
    }
    //הטקסט שצף
    private class FloatingText {
        int x, y;
        String text;
        float alpha = 1.0f;
        int yOffset = 0;

        public FloatingText(int x, int y, String text) {
            this.x = x; this.y = y; this.text = text;
        }

        public void update() {
            alpha -= 0.02f;
            yOffset -= 1;
        }

        public void draw(Graphics2D g2) {
            if (alpha < 0) alpha = 0;
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2.setColor(Color.YELLOW);
            g2.setFont(new Font("Arial", Font.BOLD, 25));
            g2.drawString(text, x, y + yOffset);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }
    }
    //המטרות
    private class Target {
        int x, y, width, height;
        int state = 0;
        boolean isHit = false;

        // החלפנו את זמני המערכת במד-זמן מבוסס פריימים עצמאי
        private int animationTicks = 0;

        public Target(int x, int y, int w, int h) {
            this.x = x; this.y = y;
            this.width = w; this.height = h;
        }

        // העדכון מתקדם בצעדים קבועים רק כשלולאת המשחק רצה אקטיבית
        public void update() {
            animationTicks += 16; // כל פריים מוסיף כ-16 מילישניות וירטואליות

            if (state == 0 && animationTicks > 300) {
                state = 1;
                animationTicks = 0;
            } else if (state == 1 && animationTicks > 1000) {
                state = 2;
                animationTicks = 0;
            } else if (state == 2 && animationTicks > 300) {
                state = 3;
            }
        }

        public void hit() {
            isHit = true;
            state = 2;
            animationTicks = 0;
        }

        public boolean isDead() { return state == 3; }
        public boolean canBeHit() { return state == 1 && !isHit; }
        public Rectangle getBounds() { return new Rectangle(x, y, width, height); }

        public void draw(Graphics2D g2, BufferedImage img) {
            float scale = 1.0f;

            // חישוב ה-scale ישירות מתוך הצעדים שנצברו, ללא קשר לשעון המחשב הכללי
            if (state == 0) scale = animationTicks / 300f;
            if (state == 2) scale = 1.0f - (animationTicks / 300f);

            int drawW = (int)(width * Math.max(0, scale));
            int drawH = (int)(height * Math.max(0, scale));
            int offsetX = (width - drawW) / 2;
            int offsetY = (height - drawH) / 2;

            g2.drawImage(img, x + offsetX, y + offsetY, drawW, drawH, null);
        }
    }
}