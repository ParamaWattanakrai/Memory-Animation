import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javax.swing.*;

public class Windows11Demo extends JPanel implements Runnable {

    static final int W = 800, H = 500;
    static final int TASKBAR_H = 48;
    static final int ICON_SIZE = 56;

    double iconX = 140, iconY = 100;
    double iconVX = 150, iconVY = 120;
    double totalTime = 0;

    public static void main(String[] args) {
        Windows11Demo m = new Windows11Demo();
        m.setPreferredSize(new Dimension(W, H));

        JFrame f = new JFrame();
        f.add(m);
        f.setTitle("Windows 11");
        f.setResizable(false);
        f.pack();
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setLocationRelativeTo(null);
        f.setVisible(true);
        (new Thread(m)).start();
    }

    @Override
    public void run() {
        double lastTime = System.currentTimeMillis();
        while (true) {
            double now = System.currentTimeMillis();
            double elapsed = (now - lastTime) / 1000.0;
            lastTime = now;
            totalTime += elapsed;

            updateBounce(elapsed);
            repaint();

            try {
                Thread.sleep(1000 / 60);
            } catch (InterruptedException e) {
                System.err.println(e);
            }
        }
    }

    private void updateBounce(double dt) {
        iconX += iconVX * dt;
        iconY += iconVY * dt;

        double minX = 0, maxX = W - ICON_SIZE;
        double minY = 0, maxY = H - TASKBAR_H - ICON_SIZE;

        if (iconX <= minX) { iconX = minX; iconVX = Math.abs(iconVX); }
        if (iconX >= maxX) { iconX = maxX; iconVX = -Math.abs(iconVX); }
        if (iconY <= minY) { iconY = minY; iconVY = Math.abs(iconVY); }
        if (iconY >= maxY) { iconY = maxY; iconVY = -Math.abs(iconVY); }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        BufferedImage buffer = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = buffer.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        drawDesktop(g2);
        // drawWindow(g2, 190, 60, 420, 250);
        drawBouncingIcon(g2, iconX, iconY);
        drawTaskbar(g2);

        g.drawImage(buffer, 0, 0, null);
    }

    private void drawDesktop(Graphics2D g2) {
        // Windows 11 light blue background gradient
        GradientPaint bg = new GradientPaint(
                0, 0, new Color(165, 195, 225),
                W, H - TASKBAR_H, new Color(195, 215, 238)
        );
        g2.setPaint(bg);
        g2.fillRect(0, 0, W, H - TASKBAR_H);

        // Radial glow center
        RadialGradientPaint centerGlow = new RadialGradientPaint(
                new Point2D.Float(W * 0.5f, H * 0.4f), 400f,
                new float[]{0.0f, 1.0f},
                new Color[]{new Color(230, 242, 255, 180), new Color(165, 195, 225, 0)}
        );
        g2.setPaint(centerGlow);
        g2.fillRect(0, 0, W, H - TASKBAR_H);

        // Windows 11 "Bloom" Folded Petal Mesh
        drawBloomPetals(g2);
    }

    private void drawBloomPetals(Graphics2D g2) {
        int cx = W / 2;
        int bottomY = H - TASKBAR_H; // Touches the bottom desktop baseline

        // Layer 1: Dark Outer Fold
        Path2D.Double p1 = new Path2D.Double();
        p1.moveTo(cx - 250, bottomY);
        p1.curveTo(cx - 380, 20, cx - 180, 0, cx, 40);
        p1.curveTo(cx + 260, 80, cx + 320, 260, cx + 200, bottomY);
        p1.closePath();
        g2.setPaint(new GradientPaint(cx - 150, 20, new Color(10, 45, 130), cx + 150, bottomY, new Color(0, 95, 210)));
        g2.fill(p1);

        // Layer 2: Vibrant Blue Fold
        Path2D.Double p2 = new Path2D.Double();
        p2.moveTo(cx - 200, bottomY);
        p2.curveTo(cx - 300, 70, cx - 80, 30, cx + 80, 90);
        p2.curveTo(cx + 240, 160, cx + 220, 330, cx + 100, bottomY);
        p2.closePath();
        g2.setPaint(new GradientPaint(cx - 120, 50, new Color(15, 115, 235), cx + 80, bottomY, new Color(0, 60, 175)));
        g2.fill(p2);

        // Layer 3: Cyan Highlight Ribbon
        Path2D.Double p3 = new Path2D.Double();
        p3.moveTo(cx - 140, bottomY);
        p3.curveTo(cx - 220, 130, cx - 20, 70, cx + 100, 130);
        p3.curveTo(cx + 170, 200, cx + 140, 340, cx - 20, bottomY);
        p3.closePath();
        g2.setPaint(new GradientPaint(cx - 80, 80, new Color(75, 175, 255), cx + 50, bottomY, new Color(20, 110, 220)));
        g2.fill(p3);

        // Layer 4: Front Glossy Swirl
        Path2D.Double p4 = new Path2D.Double();
        p4.moveTo(cx - 80, bottomY);
        p4.curveTo(cx - 150, 200, cx + 20, 130, cx + 110, 190);
        p4.curveTo(cx + 150, 270, cx + 60, 370, cx - 10, bottomY);
        p4.closePath();
        g2.setPaint(new GradientPaint(cx - 30, 150, new Color(135, 210, 255), cx + 30, bottomY, new Color(40, 130, 240)));
        g2.fill(p4);
    }

    private void drawWindow(Graphics2D g2, int x, int y, int w, int h) {
        int titleH = 36;
        int cornerArc = 16;

        // Windows 11 Soft Ambient Drop Shadow
        g2.setColor(new Color(0, 0, 0, 25));
        g2.fill(new RoundRectangle2D.Double(x + 2, y + 6, w, h, cornerArc, cornerArc));

        // Window Frame (Mica Material)
        g2.setColor(new Color(243, 243, 243, 245));
        g2.fill(new RoundRectangle2D.Double(x, y, w, h, cornerArc, cornerArc));

        // Outline border
        g2.setColor(new Color(210, 210, 210));
        g2.setStroke(new BasicStroke(1f));
        g2.draw(new RoundRectangle2D.Double(x, y, w - 1, h - 1, cornerArc, cornerArc));

        // Title Bar Area
        g2.setColor(new Color(248, 248, 248, 220));
        g2.fill(new RoundRectangle2D.Double(x + 1, y + 1, w - 2, titleH, cornerArc, cornerArc));
        g2.fillRect(x + 1, y + titleH - 5, w - 2, 6);

        // Title Text
        g2.setColor(new Color(30, 30, 30));
        g2.setFont(new Font("Segoe UI Variable", Font.PLAIN, 12));
        g2.drawString("Windows 11 - Fluent Desktop Engine", x + 16, y + 22);

        // Client Interior Surface
        int border = 8;
        int clientX = x + border;
        int clientY = y + titleH;
        int clientW = w - border * 2;
        int clientH = h - titleH - border;

        g2.setColor(Color.WHITE);
        g2.fill(new RoundRectangle2D.Double(clientX, clientY, clientW, clientH, 8, 8));
        g2.setColor(new Color(230, 230, 230));
        g2.draw(new RoundRectangle2D.Double(clientX, clientY, clientW, clientH, 8, 8));

        g2.setColor(new Color(50, 50, 50));
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        g2.drawString("Windows 11 Fluent visual style rendered.", clientX + 16, clientY + 32);

        // Windows 11 Clean Window Control Buttons
        int btnW = 46, btnH = titleH - 2;
        int closeX = x + w - btnW - 4;

        // Close 'X'
        g2.setColor(new Color(80, 80, 80));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawLine(closeX + 18, y + 13, closeX + 28, y + 23);
        g2.drawLine(closeX + 28, y + 13, closeX + 18, y + 23);

        // Maximize Snap Box
        int maxX = closeX - btnW;
        g2.drawRect(maxX + 18, y + 13, 10, 10);

        // Minimize Line
        int minX = maxX - btnW;
        g2.drawLine(minX + 18, y + 18, minX + 28, y + 18);
    }

    private void drawBouncingIcon(Graphics2D g2, double x, double y) {
        int s = ICON_SIZE / 2;

        g2.setColor(new Color(0, 0, 0, 30));
        g2.fill(new RoundRectangle2D.Double(x + 2, y + 4, ICON_SIZE, ICON_SIZE, 12, 12));

        // Modern 4-Square Windows 11 Logo
        Color winBlue = new Color(0, 120, 215);
        g2.setColor(winBlue);
        g2.fill(new RoundRectangle2D.Double(x, y, s - 1, s - 1, 4, 4));
        g2.fill(new RoundRectangle2D.Double(x + s + 1, y, s - 1, s - 1, 4, 4));
        g2.fill(new RoundRectangle2D.Double(x, y + s + 1, s - 1, s - 1, 4, 4));
        g2.fill(new RoundRectangle2D.Double(x + s + 1, y + s + 1, s - 1, s - 1, 4, 4));
    }

    private void drawTaskbar(Graphics2D g2) {
        int y = H - TASKBAR_H;

        // Windows 11 Translucent Light Acrylic Taskbar
        g2.setColor(new Color(243, 243, 243, 235));
        g2.fillRect(0, y, W, TASKBAR_H);

        // Top Border Line
        g2.setColor(new Color(225, 225, 225));
        g2.fillRect(0, y, W, 1);

        // Centered Icons Cluster
        int totalIcons = 8;
        int spacing = 38;
        int clusterW = totalIcons * spacing;
        int startX = (W - clusterW) / 2;
        int iconY = y + (TASKBAR_H - 24) / 2;

        drawWin11StartIcon(g2, startX, iconY);
        drawSearchIcon(g2, startX + spacing, iconY);
        drawTaskViewIcon(g2, startX + spacing * 2, iconY);
        drawWidgetsIcon(g2, startX + spacing * 3, iconY);
        drawTeamsIcon(g2, startX + spacing * 4, iconY);
        drawFileExplorerIcon(g2, startX + spacing * 5, iconY);
        drawEdgeIcon(g2, startX + spacing * 6, iconY);
        drawStoreIcon(g2, startX + spacing * 7, iconY);

        // Active app indicator pill under File Explorer
        g2.setColor(new Color(0, 103, 192));
        g2.fill(new RoundRectangle2D.Double(startX + spacing * 5 + 4, y + TASKBAR_H - 4, 16, 3, 2, 2));

        // System Tray
        drawSystemTray(g2, y);
    }

    private void drawWin11StartIcon(Graphics2D g2, int x, int y) {
        int s = 10;
        g2.setColor(new Color(0, 120, 215));
        g2.fill(new RoundRectangle2D.Double(x, y, s, s, 2, 2));
        g2.fill(new RoundRectangle2D.Double(x + s + 2, y, s, s, 2, 2));
        g2.fill(new RoundRectangle2D.Double(x, y + s + 2, s, s, 2, 2));
        g2.fill(new RoundRectangle2D.Double(x + s + 2, y + s + 2, s, s, 2, 2));
    }

    private void drawSearchIcon(Graphics2D g2, int x, int y) {
        g2.setColor(new Color(80, 80, 80));
        g2.setStroke(new BasicStroke(1.8f));
        g2.drawOval(x + 2, y + 2, 13, 13);
        g2.drawLine(x + 12, y + 12, x + 19, y + 19);
    }

    private void drawTaskViewIcon(Graphics2D g2, int x, int y) {
        g2.setColor(new Color(80, 80, 80));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(x + 2, y + 4, 12, 16, 3, 3);
        g2.setColor(new Color(140, 140, 140));
        g2.drawRoundRect(x + 8, y + 2, 12, 16, 3, 3);
    }

    private void drawWidgetsIcon(Graphics2D g2, int x, int y) {
        g2.setColor(new Color(0, 120, 215));
        g2.fillRoundRect(x + 2, y + 3, 9, 18, 3, 3);
        g2.setColor(new Color(0, 164, 239));
        g2.fillRoundRect(x + 13, y + 3, 9, 18, 3, 3);
    }

    private void drawTeamsIcon(Graphics2D g2, int x, int y) {
        g2.setColor(new Color(75, 70, 185));
        g2.fillOval(x + 3, y + 3, 18, 18);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
        g2.drawString("T", x + 8, y + 16);
    }

    private void drawFileExplorerIcon(Graphics2D g2, int x, int y) {
        g2.setColor(new Color(245, 180, 35));
        g2.fillRoundRect(x + 2, y + 5, 20, 14, 4, 4);
        g2.setColor(new Color(0, 120, 215));
        g2.fillRect(x + 5, y + 3, 8, 3);
    }

    private void drawEdgeIcon(Graphics2D g2, int x, int y) {
        g2.setColor(new Color(15, 140, 205));
        g2.fillOval(x + 2, y + 2, 20, 20);
        g2.setColor(new Color(40, 200, 175));
        g2.fillOval(x + 6, y + 6, 12, 12);
    }

    private void drawStoreIcon(Graphics2D g2, int x, int y) {
        g2.setColor(new Color(0, 120, 215));
        g2.fillRoundRect(x + 3, y + 6, 18, 15, 3, 3);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawArc(x + 7, y + 2, 10, 8, 0, 180);
    }

    private void drawSystemTray(Graphics2D g2, int taskbarY) {
        int trayX = W - 145;
        g2.setColor(new Color(60, 60, 60));
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));

        // Language Pill
        g2.drawString("ENG", trayX, taskbarY + 20);
        g2.drawString("DE", trayX + 2, taskbarY + 32);

        // Network / Volume
        int iconX = trayX + 30;
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawArc(iconX, taskbarY + 16, 12, 12, 45, 90);
        g2.drawArc(iconX + 2, taskbarY + 19, 8, 8, 45, 90);

        // Clock & Date Stack
        String timeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("12:11"));
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("15/10/2021"));

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2.drawString(timeStr, trayX + 55, taskbarY + 20);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        g2.drawString(dateStr, trayX + 52, taskbarY + 34);

        // Notification Badge (Far right)
        g2.setColor(new Color(0, 103, 192));
        g2.fillOval(W - 22, taskbarY + 18, 12, 12);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
        g2.drawString("3", W - 18, taskbarY + 27);
    }
}