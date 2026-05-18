package core;

import input.KeyHandler;
import screens.*;
import util.DialogueManager;
import util.SoundPlayer;
import javax.swing.*;

public class ScreenManager {
    // כל החלונות
    private GameWindow gameWindow;
    private MenuPanel menuPanel;
    private HowToPlayPanel howToPlayPanel;
    private GamePanel gamePanel;
    private ShootingGamePanel shootingGamePanel;

    private SoundPlayer musicPlayer; // טוען את המוזיקה
    public KeyHandler keyH = new KeyHandler(); // מאזין למקשים
    private GameState currentState;
    private DialogueManager dialogueManager;

    public ScreenManager() {
        // טעינת מדיה
        musicPlayer = new SoundPlayer();
        dialogueManager = new DialogueManager(musicPlayer);

        // חלונות - אתחול כל הפאנלים
        gameWindow = new GameWindow();
        menuPanel = new MenuPanel(this);
        howToPlayPanel = new HowToPlayPanel(this);
        gamePanel = new GamePanel(this, this.keyH);
        shootingGamePanel = new ShootingGamePanel(this);

        showScreen(GameState.MENU);
        gameWindow.showWindow();
    }
    //מגדיר את הגטים וסטים
    public SoundPlayer getMusicPlayer() { return musicPlayer; }
    public DialogueManager getDialogueManager() { return dialogueManager; }
    public GameState getCurrentState() { return currentState; }
    public void setCurrentState(GameState state) { this.currentState = state; }
    //גטים לפנלים
    public JPanel getPanel(GameState state) {
        if (state == GameState.MENU) return menuPanel;
        if (state == GameState.HOW_TO_PLAY) return howToPlayPanel;
        if (state == GameState.GAME) return gamePanel;
        if (state == GameState.MINI_GAME) return shootingGamePanel; // החזרת הפאנל החדש
        return null;
    }

    public void showScreen(GameState state) {
        this.currentState = state;
        JPanel target = null;
        keyH.resetKeys();

        if (state == GameState.MENU) {
            target = menuPanel;
            musicPlayer.playBackgroundMusic("game/resources/sounds/wild_west_menuPanel.wav", 1.0f);
        }
        else if (state == GameState.HOW_TO_PLAY) {
            target = howToPlayPanel;
        }
        else if (state == GameState.GAME) {
            musicPlayer.playBackgroundMusic("game/resources/sounds/game_theme.wav", 0.8f);
            target = gamePanel;
        }
        else if (state == GameState.DIALOGUE) {
            target = gamePanel;
        }
        else if (state == GameState.MINI_GAME) {
            musicPlayer.stopMusic();
            target = shootingGamePanel;
            shootingGamePanel.start();
        }
        if (target != null) {
            gameWindow.addPanel(target);
            target.requestFocusInWindow();
        }
    }
}