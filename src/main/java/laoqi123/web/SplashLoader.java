package laoqi123.web;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Standalone, game-independent loading window shown while the mod engine (MCEF /
 * Chromium) and game finish initializing. It is a plain AWT/Swing window with no
 * dependency on Minecraft's GL context, so it can be shown before the game window
 * is visible and closed once everything is ready.
 *
 * <p>Lifecycle:
 * <ul>
 *   <li>{@link #start(String)} shows the window on the AWT event thread.</li>
 *   <li>{@link #setText(String)} / {@link #setProgress(int)} update the label/progress.</li>
 *   <li>{@link #finish()} fades out and disposes the window.</li>
 * </ul>
 */
public final class SplashLoader {

    private static JWindow window;
    private static JLabel statusLabel;
    private static JProgressBar progressBar;
    private static Timer spinner;
    private static int dot;
    private static boolean shown;
    private static boolean everShown;

    private SplashLoader() {
    }

    /** Show the loading window on the AWT event thread. */
    public static synchronized void start(String title) {
        if (shown) {
            return;
        }
        shown = true;
        everShown = true;
        SwingUtilities.invokeLater(() -> buildAndShow(title));
    }

    private static void buildAndShow(String title) {
        if (window != null) {
            return;
        }
        try {
            window = new JWindow();
            window.setAlwaysOnTop(true);
            window.setSize(460, 220);
            window.setLocationRelativeTo(null);

            JPanel root = new JPanel(new BorderLayout(0, 0));
            root.setBackground(new Color(0x1E1E1E));
            root.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, new Color(0x3584E4)));
            root.setPreferredSize(new Dimension(460, 220));

            JLabel logo = new JLabel("MYAU  NEXTGEN", JLabel.CENTER);
            logo.setForeground(Color.WHITE);
            logo.setFont(new Font("Segoe UI", Font.BOLD, 30));

            statusLabel = new JLabel("Initializing...", JLabel.CENTER);
            statusLabel.setForeground(new Color(0xC0C6D4));
            statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));

            progressBar = new JProgressBar(0, 100);
            progressBar.setValue(0);
            progressBar.setForeground(new Color(0x3584E4));
            progressBar.setBackground(new Color(0x0F0F14));
            progressBar.setBorderPainted(false);
            progressBar.setPreferredSize(new Dimension(380, 8));

            JPanel center = new JPanel(new GridBagLayout());
            center.setOpaque(false);
            center.add(logo);
            JPanel status = new JPanel();
            status.setOpaque(false);
            status.add(statusLabel);
            center.add(status);

            JPanel bottom = new JPanel();
            bottom.setOpaque(false);
            bottom.setBorder(BorderFactory.createEmptyBorder(0, 40, 30, 40));
            bottom.setLayout(new BorderLayout());
            bottom.add(progressBar, BorderLayout.CENTER);

            root.add(center, BorderLayout.CENTER);
            root.add(bottom, BorderLayout.SOUTH);
            window.setContentPane(root);
            window.pack();
            window.setVisible(true);

            spinner = new Timer(300, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    dot = (dot + 1) % 4;
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < dot; i++) {
                        sb.append('.');
                    }
                    String base = statusLabel.getText().replace(".", "").trim();
                    statusLabel.setText(base + sb);
                }
            });
            spinner.start();
        } catch (Throwable t) {
            System.err.println("[SplashLoader] failed to show: " + t);
        }
    }

    /** Update the status text (safe from any thread). */
    public static void setText(String text) {
        if (statusLabel != null) {
            SwingUtilities.invokeLater(() -> statusLabel.setText(text));
        }
    }

    /** Update the progress bar (0-100). */
    public static void setProgress(int pct) {
        if (progressBar != null) {
            SwingUtilities.invokeLater(() -> progressBar.setValue(Math.max(0, Math.min(100, pct))));
        }
    }

    /** Close the window. Safe to call multiple times / from any thread. */
    public static synchronized void finish() {
        if (!shown) {
            return;
        }
        shown = false;
        if (spinner != null) {
            spinner.stop();
            spinner = null;
        }
        SwingUtilities.invokeLater(() -> {
            if (window != null) {
                window.dispose();
                window = null;
            }
        });
    }

    /** True if the window is currently displayed. */
    public static synchronized boolean isShown() {
        return shown;
    }

    /** True if a splash was requested (start() was called at least once). */
    public static synchronized boolean wasShown() {
        return everShown;
    }

    /** Blocking wait on the AWT thread until the window finishes building. */
    public static void ensureShown() {
        try {
            GraphicsEnvironment.getLocalGraphicsEnvironment();
        } catch (Throwable ignored) {
        }
    }
}
