package screens;

import core.GameWindow;
import core.ScreenManager;
import core.GameState;
import util.AssetLoader;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class HowToPlayPanel extends JPanel {

    private ScreenManager screenManager;
    private JButton backToMenuButton;
    private boolean fromGame = false;

    // רכיב חדש להצגת ה-High Score
    private JLabel highScoreLabel;
    private final String SAVE_FILE_PATH = "highscore.txt";

    public HowToPlayPanel(ScreenManager screenManager) {
        this.screenManager = screenManager;
        setPreferredSize(new Dimension(GameWindow.WIDTH, GameWindow.HEIGHT));
        setBackground(Color.DARK_GRAY);
        setLayout(null);
        setFocusable(true);

        backToMenuButton = createReturnButton("Back to Main Menu", (GameWindow.WIDTH / 2) - 250, 20, e -> {
            new Thread(() -> {
                screenManager.showScreen(GameState.MENU);
            }).start();
        });
        backToMenuButton.setVisible(false);
        add(backToMenuButton);

        setupLabels();
        setupIcons();
        setupHighScoreLabel(); // יצירת הלייבל של השיא

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    new Thread(() -> {
                        if (fromGame) {
                            screenManager.showScreen(GameState.GAME);
                        } else {
                            screenManager.showScreen(GameState.MENU);
                        }
                    }).start();
                }
            }
        });
    }

    public void setFromGame(boolean status) {
        this.fromGame = status;
        if (backToMenuButton != null) {
            backToMenuButton.setVisible(status);
        }
    }

    // מתודה חדשה ליצירת הלייבל של ה-High Score ומיקומו על המסך
    private void setupHighScoreLabel() {
        highScoreLabel = new JLabel("MINI-GAME HI-SCORE: 0.00");
        highScoreLabel.setFont(AssetLoader.getFont(30)); // משתמש בפונט של הפרויקט שלך
        highScoreLabel.setForeground(Color.YELLOW);     // צבע זהב בולט
        highScoreLabel.setBounds(400, 250, 600, 50); // מיקום בפינה השמאלית התחתונה
        add(highScoreLabel);
    }

    // מתודה שקוראת את הציון העדכני ביותר מקובץ הטקסט
    public void updateHighScoreDisplay() {
        java.io.File file = new java.io.File(SAVE_FILE_PATH);
        double highScore = 0.0;

        if (file.exists()) {
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))) {
                String line = reader.readLine();
                if (line != null) {
                    highScore = Double.parseDouble(line);
                }
            } catch (Exception e) {
                System.out.println("Error loading High Score in HowToPlay: " + e.getMessage());
            }
        }

        // עדכון הטקסט הגרפי בצורה נקייה עם 2 ספרות אחרי הנקודה
        if (highScoreLabel != null) {
            highScoreLabel.setText(String.format("MINI-GAME HI-SCORE: %.2f", highScore));
        }
    }

    private void setupLabels() {
        JLabel label1 = new JLabel(AssetLoader.getScaledIcon("images/esc_key.png", 100, 100));
        label1.setBounds(100, 5, 100, 100);
        add(label1);

        JLabel label2 = new JLabel(AssetLoader.getScaledIcon("images/esc.png", 100, 100));
        label2.setBounds(5, 5, 100, 100);
        add(label2);

        JTextArea text = new JTextArea(
                "                how to play\n\n"+
                        "Use the            keys to move around the map.\n\n"+
                        "               Interact with\n\n"+
                        "        Aim and shoot with the mouse."
        );
        text.setFont(util.AssetLoader.getFont(40));
        text.setForeground(Color.WHITE);
        text.setOpaque(false);
        text.setEditable(false);
        text.setFocusable(false);
        text.setBounds(150, 320, 1100, 600);
        add(text);
    }

    private void setupIcons() {
        JLabel label3 = new JLabel(AssetLoader.getScaledIcon("images/e_key.png", 100, 100));
        label3.setBounds(780, 400, 200, 200);
        add(label3);
        JLabel label4 =  new JLabel(AssetLoader.getScaledIcon("images/WASD.png", 200, 200));
        label4.setBounds(350, 300, 200, 200);
        add(label4);
        JLabel label5 = new JLabel(AssetLoader.getScaledIcon("images/rightClick.png", 100, 100));
        label5.setBounds(960, 470, 200, 200);
        add(label5);
    }

    private JButton createReturnButton(String text, int x, int y, java.awt.event.ActionListener action) {
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
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
                Font font = AssetLoader.getFont(isHovered ? hoverSize : normalSize);
                g2.setFont(font);

                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth(getText())) / 2;
                int ty = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();

                g2.setColor(new Color(0, 0, 0, 150));
                g2.drawString(getText(), tx + 4, ty + 4);
                g2.setColor(isHovered ? Color.YELLOW : Color.WHITE);
                g2.drawString(getText(), tx, ty);
            }
        };
        button.setBounds(x, y, 500, 100);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.addActionListener(action);
        return button;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        updateHighScoreDisplay(); // בכל פעם שהמסך עולה מחדש - קוראים מהקובץ ומעדכנים את התצוגה!
        requestFocusInWindow();
    }
}