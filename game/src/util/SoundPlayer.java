package util;

import javax.sound.sampled.*;
import java.io.File;

public class SoundPlayer {
    // הסרנו את ה-static כדי למנוע דריסות בין אובייקטים
    private Clip backgroundClip;
    private String currentMusicPath = "";

    public void playBackgroundMusic(String path, float initialVolume) {
        if (path.equals(currentMusicPath) && backgroundClip != null && backgroundClip.isRunning()) {
            setVolume(initialVolume);
            return;
        }

        // הפעלה ב-Thread נפרד כדי למנוע Lag בטעינה
        new Thread(() -> {
            try {
                stopBackgroundMusic();
                File soundFile = new File(path);
                if (!soundFile.exists()) return;

                AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile);
                backgroundClip = AudioSystem.getClip();
                backgroundClip.open(audioIn);
                setVolume(initialVolume);
                backgroundClip.loop(Clip.LOOP_CONTINUOUSLY);
                backgroundClip.start();
                currentMusicPath = path;
            } catch (Exception e) {
                System.err.println("Sound Error: " + e.getMessage());
            }
        }).start();
    }

    public void setVolume(float volume) {
        if (backgroundClip != null && backgroundClip.isOpen()) {
            try {
                if (backgroundClip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl gainControl = (FloatControl) backgroundClip.getControl(FloatControl.Type.MASTER_GAIN);

                    // המרה ל-dB בצורה בטוחה יותר
                    float safeVolume = Math.max(0.0001f, Math.min(volume, 1.0f));
                    float dB = (float) (Math.log10(safeVolume) * 20.0);

                    // בדיקה שהערך בטווח המותר של הכרטיס קול
                    float min = gainControl.getMinimum();
                    float max = gainControl.getMaximum();
                    if (dB < min) dB = min;
                    if (dB > max) dB = max;

                    gainControl.setValue(dB);
                }
            } catch (Exception e) {
                // שגיאה שקטה בשינוי ווליום
            }
        }
    }

    public void playSFX(String path, boolean loop) {
        new Thread(() -> {
            try {
                File soundFile = new File(path);
                if (!soundFile.exists()) return;

                AudioInputStream rawIn = AudioSystem.getAudioInputStream(soundFile);
                AudioFormat baseFormat = rawIn.getFormat();

                // המרה לפורמט PCM סטנדרטי ש-Java תמיד מצליחה לקרוא
                AudioFormat targetFormat = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        baseFormat.getSampleRate(),
                        16,
                        baseFormat.getChannels(),
                        baseFormat.getChannels() * 2,
                        baseFormat.getSampleRate(),
                        false
                );

                AudioInputStream decodedIn = AudioSystem.getAudioInputStream(targetFormat, rawIn);
                Clip sfxClip = AudioSystem.getClip();
                sfxClip.open(decodedIn);

                if (sfxClip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl gain = (FloatControl) sfxClip.getControl(FloatControl.Type.MASTER_GAIN);
                    gain.setValue(-10.0f);
                }

                sfxClip.start();
                sfxClip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        sfxClip.close();
                    }
                });
            } catch (Exception e) {
                System.err.println("SFX Error with file: " + path);
                e.printStackTrace();
            }
        }).start();
    }

    public void stopBackgroundMusic() {
        if (backgroundClip != null) {
            try {
                backgroundClip.stop();
                backgroundClip.flush();
                backgroundClip.close();
            } catch (Exception e) {
                // התעלמות משגיאות בסגירה
            }
        }
        backgroundClip = null;
        currentMusicPath = "";
    }

    public Clip playLoopingSFX(String path) {
        try {
            File file = new File(path);
            AudioInputStream ais = AudioSystem.getAudioInputStream(file);
            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            clip.loop(Clip.LOOP_CONTINUOUSLY); // גורם לסאונד לרוץ בלופ
            clip.start();
            return clip;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void stopMusic() {
        if (backgroundClip != null && backgroundClip.isRunning()) {
            backgroundClip.stop();
            backgroundClip.close();
        }
    }
}