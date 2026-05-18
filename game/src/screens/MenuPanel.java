package screens;

import core.GameWindow;
import core.ScreenManager;
import core.GameState;
import util.AssetLoader;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class MenuPanel extends JPanel implements Runnable { // הווספת Runnable


    private ScreenManager screenManager;
    private Image backgroundImage;
    private float alpha = 0f;
    private boolean isTransitioning = false;
    private Thread transitionThread; // ה-Thread שיחליף את ה-Timer
    private BufferedImage logo;


    public MenuPanel(ScreenManager screenManager) {
        this.screenManager = screenManager;
        backgroundImage = new ImageIcon("game/resources/images/background.png").getImage();

        setPreferredSize(new Dimension(GameWindow.WIDTH, GameWindow.HEIGHT));
        setLayout(null);

        logo = AssetLoader.loadImage("images/wild-carnival-logo.png");

        int centerX = (GameWindow.WIDTH / 2) - 250;

        JButton startButton = createMenuButton("Start Game", centerX, 400, e -> {
            if (!isTransitioning) {
                screenManager.getMusicPlayer().playSFX("game/resources/sounds/button.wav", false);
                startFadeTransition(); // הפעלת ה-Thread
            }
        });

        JButton howToPlayButton = createMenuButton("How to Play", centerX, 520, e -> {
            screenManager.getMusicPlayer().playSFX("game/resources/sounds/button.wav", false);
            screenManager.showScreen(GameState.HOW_TO_PLAY);
        });

        JButton quitButton = createMenuButton("Quit", centerX, 640, e -> {
            screenManager.getMusicPlayer().playSFX("game/resources/sounds/button.wav", false);

            // שימוש ב-Thread קצר לסגירת המשחק עם השהיה
            new Thread(() -> {
                try {
                    Thread.sleep(300);
                    System.exit(0);
                } catch (InterruptedException ex) {
                    System.exit(0);
                }
            }).start();
        });

        add(startButton);
        add(howToPlayButton);
        add(quitButton);
    }

    // פונקציה שמפעילה את ה-Thread של המעבר
    private void startFadeTransition() {
        isTransitioning = true;
        transitionThread = new Thread(this); // יצירת ה-Thread
        transitionThread.start(); // הפעלת הפונקציה run()
    }

    @Override
    public void run() {
        // לולאה שרצה עד שהמסך נהיה שחור לגמרי (alpha = 1)
        while (alpha < 1f) {
            alpha += 0.015f;

            if (alpha > 1f) alpha = 1f;

            // עדכון עוצמת המוזיקה לפי ה-alpha
            float currentVolume = Math.max(0, 1.0f - alpha);
            screenManager.getMusicPlayer().setVolume(currentVolume);

            // ציור מחדש של המסך
            repaint();

            try {
                // השהיה של 30 מילי-שניות (כמו שהיה בטיימר)
                Thread.sleep(30);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // סיום המעבר: עוברים למסך המשחק
        screenManager.getMusicPlayer().stopBackgroundMusic();
        screenManager.showScreen(GameState.GAME);
    }

    private JButton createMenuButton(String text, int x, int y, java.awt.event.ActionListener action) {
        float normalSize = 40f;
        float hoverSize = 50f;
        JButton button = new JButton(text) {
            private boolean isHovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { isHovered = true; repaint(); }
                    public void mouseExited(MouseEvent e) { isHovered = false; repaint(); }
                });
            }
            @Override
            public boolean contains(int x, int y) {
                Font font = AssetLoader.getFont(isHovered ? hoverSize : normalSize);
                FontMetrics fm = getFontMetrics(font);
                int textWidth = fm.stringWidth(getText());
                int textHeight = fm.getHeight();
                return new Rectangle((getWidth()-textWidth)/2-20, (getHeight()-textHeight)/2, textWidth+40, textHeight).contains(x, y);
            }
            @Override
            protected void paintComponent(Graphics g) {
                drawButtonWithStroke(g, this, AssetLoader.getFont(isHovered ? hoverSize : normalSize));
            }
        };
        button.setBounds(x, y, 500, 100);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.addActionListener(action);
        return button;
    }

    private void drawButtonWithStroke(Graphics g, JButton b, Font font) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g2.setFont(font);
        String text = b.getText();
        FontMetrics fm = g2.getFontMetrics();
        int x = (b.getWidth() - fm.stringWidth(text)) / 2;
        int y = ((b.getHeight() - fm.getHeight()) / 2) + fm.getAscent();
        g2.setColor(new Color(0, 0, 0, 150));
        g2.drawString(text, x + 4, y + 4);
        g2.setColor(Color.WHITE);
        g2.drawString(text, x, y);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        if (backgroundImage != null) g.drawImage(backgroundImage, 0, 0, GameWindow.WIDTH, GameWindow.HEIGHT, this);
        if (logo != null) {
            int logoWidth = 800;  // שנה את הגודל לפי הצורך
            int logoHeight = 250; // שנה את הגודל לפי הצורך

            // חישוב מרכוז אופקי
            int x = (GameWindow.WIDTH / 2) - (logoWidth / 2);
            int y = 50; // מרחק מהחלק העליון

            g2.drawImage(logo, x, y, logoWidth, logoHeight, null);
        }
    }

    @Override
    protected void paintChildren(Graphics g) {
        super.paintChildren(g);
        if (alpha > 0) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    @Override
    public void addNotify() {
        super.addNotify();
        this.alpha = 0f;
        this.isTransitioning = false;
        this.requestFocusInWindow();
    }
}