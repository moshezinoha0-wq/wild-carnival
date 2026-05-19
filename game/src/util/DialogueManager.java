package util;

import javax.sound.sampled.Clip;
import java.util.LinkedList;
import java.util.Queue;

public class DialogueManager {
    private Queue<String> dialogueQueue = new LinkedList<>();
    private String visibleText = "";
    private boolean isTyping = false;
    private SoundPlayer soundPlayer;
    private Clip currentVoiceClip;
    private boolean waitingForChoice = false;

    public DialogueManager(SoundPlayer soundPlayer) {
        this.soundPlayer = soundPlayer;
    }

    // פונקציה להזנת כל הדיאלוג מראש כמערך
    public void setDialogue(String[] lines) {
        dialogueQueue.clear();
        for (String line : lines) {
            dialogueQueue.add(line);
        }
    }

    // פונקציה שעוברת למשפט הבא
    public void nextLine() {
        if (isTyping) return;

        if (!dialogueQueue.isEmpty()) {
            startTyping(dialogueQueue.poll());
        } else {
            // אם אין יותר שורות, אנחנו במצב המתנה לבחירה
            waitingForChoice = true;
        }
    }

    private void startTyping(String text) {
        isTyping = true;
        visibleText = "";

        new Thread(() -> {
            currentVoiceClip = soundPlayer.playLoopingSFX("game/resources/sounds/booth_keeper_sound.wav");

            for (int i = 0; i <= text.length(); i++) {
                visibleText = text.substring(0, i);
                try { Thread.sleep(50); } catch (InterruptedException e) {}
            }

            // הפסקת הסאונד בדיוק כשהמשפט נגמר
            if (currentVoiceClip != null) {
                currentVoiceClip.stop();
            }
            isTyping = false;
        }).start();
    }

    public String getVisibleText() { return visibleText; }
    public boolean isTyping() { return isTyping; }
    public boolean hasMoreLines() { return !dialogueQueue.isEmpty(); }
    public boolean isWaitingForChoice() { return waitingForChoice; }
    public void resetChoice() { waitingForChoice = false; }
}