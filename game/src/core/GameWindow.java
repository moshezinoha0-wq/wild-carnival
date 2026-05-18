package core;

import javax.swing.*;

public class GameWindow {
    //מצהיר על החלון
    private JFrame window;
    // גודל החלון, אורך ורוחב.
    public static final int WIDTH = 1280;
    public static final int HEIGHT = 720;

    public GameWindow() {
        //יוצר את החלון
        window = new JFrame();
        //נותן לזה שם
        window.setTitle("Wild Carnival");
        //נותן לכפתור X לסגור את המשימה
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //נגדיר את היכולת של המשתמש לשנות את הגודל של החלון ל-false
        window.setResizable(false);
    }

    public void showWindow() {
        //מגדיר את הופעת החלון במרכז
        window.setLocationRelativeTo(null);
        //מגדיר את המסך כנראה
        window.setVisible(true);
    }

    //דואג להציג את הpanel על החלון הראשי
    public void addPanel(javax.swing.JPanel panel) {
        //ניגש לכל מה שמופיע על החלון ומסיר אותו
        window.getContentPane().removeAll();
        // מוסיפים את הפאנל החדש
        window.add(panel);
        // מעדכנים את התצוגה
        window.revalidate();
        window.repaint();
        // מתאימים גודל ונותנים פוקוס למקלדת
        window.pack();
        //ממקד את האינטרקציה של העכבר ובמקשים על הpanel החדש
        panel.requestFocusInWindow();
    }
}