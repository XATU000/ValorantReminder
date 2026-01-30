package main.java.com.valorant.reminder;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class ValorantReminder {
    private static final long CHECK_INTERVAL = TimeUnit.HOURS.toMillis(1);
    private static final String REMIND_MSG = "该打瓦啦！已经1小时没启动无畏契约了，瓦学弟该上线练枪啦～";
    private static final String PROCESS_NAME_WIN = "VALORANT.exe";

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        setupSystemTray();

        Thread checkThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    if (!isProcessRunning()) {
                        showReminder();
                    }
                    // 解决“忙等待”警告：使用标准的休眠
                    Thread.sleep(CHECK_INTERVAL);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println("Detection error: " + e.getMessage());
                }
            }
        });

        checkThread.setDaemon(true);
        checkThread.start();

        // 解决“死循环”警告：使用 join 阻塞主线程，优雅且不占资源
        try {
            checkThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static boolean isProcessRunning() {
        String os = System.getProperty("os.name").toLowerCase();
        // 解决 exec 弃用问题：使用 ProcessBuilder
        ProcessBuilder pb = os.contains("win")
                ? new ProcessBuilder("tasklist", "/NH", "/FI", "IMAGENAME eq " + PROCESS_NAME_WIN)
                : new ProcessBuilder("ps", "-ef");

        Process p = null;
        try {
            p = pb.start();
            // 解决 AutoCloseable 兼容性问题：仅在 try 中包裹流
            try (BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = input.readLine()) != null) {
                    if (line.contains(PROCESS_NAME_WIN)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Process check failed: " + e.getMessage());
        } finally {
            if (p != null) p.destroy();
        }
        return false;
    }

    private static void showReminder() {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < 2; i++) {
                Toolkit.getDefaultToolkit().beep();
            }
            JFrame frame = new JFrame();
            frame.setAlwaysOnTop(true);
            JOptionPane.showMessageDialog(frame, REMIND_MSG, "瓦学弟提醒 ⚡", JOptionPane.WARNING_MESSAGE);
            frame.dispose();
        });
    }

    private static void setupSystemTray() {
        if (!SystemTray.isSupported()) return;
        try {
            SystemTray tray = SystemTray.getSystemTray();
            java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_RGB);
            Graphics g = image.getGraphics();
            g.setColor(Color.RED);
            g.fillRect(0, 0, 16, 16);
            g.dispose();

            PopupMenu menu = new PopupMenu();
            MenuItem exitItem = new MenuItem("Exit");
            exitItem.addActionListener(e -> System.exit(0));
            menu.add(exitItem);

            TrayIcon trayIcon = new TrayIcon(image, "Valorant Reminder", menu);
            trayIcon.setImageAutoSize(true);
            tray.add(trayIcon);
        } catch (Exception e) {
            System.err.println("Tray failure: " + e.getMessage());
        }
    }
}