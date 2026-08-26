import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.Queue;
import javax.swing.*;

public class Windows7Demo extends JPanel implements Runnable {

    static final int W = 600, H = 600;
    static final int TASKBAR_H = 38;
    static final int ICON_SIZE = 56;

    double iconX = 100, iconY = 100;
    double iconVX = 160, iconVY = 130;
    double totalTime = 0;

    public static void main(String[] args) {
        Windows7Demo m = new Windows7Demo();
        m.setPreferredSize(new Dimension(W, H));

        JFrame f = new JFrame();
        f.add(m);
        f.setTitle("Windows 7");
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

        BufferedImage buffer = new BufferedImage(W + 1, H + 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = buffer.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawDesktop(buffer, g2);
        drawWindow(g2, 130, 80, 340, 220);
        drawBouncingIcon(g2, iconX, iconY);
        drawTaskbar(g2);

        g.drawImage(buffer, 0, 0, null);
    }

    private void drawDesktop(BufferedImage buffer, Graphics2D g2) {
        // Windows 7 Harmony Blue Sky gradient background
        GradientPaint sky = new GradientPaint(0, 0, new Color(15, 95, 185), 0, H - TASKBAR_H, new Color(5, 45, 105));
        g2.setPaint(sky);
        g2.fillRect(0, 0, W, H - TASKBAR_H);

        // Center light radial glow
        RadialGradientPaint glow = new RadialGradientPaint(
                new Point2D.Float(W / 2f, H / 2f - 30),
                350f,
                new float[]{0.0f, 1.0f},
                new Color[]{new Color(55, 175, 245, 160), new Color(0, 0, 0, 0)}
        );
        g2.setPaint(glow);
        g2.fillRect(0, 0, W, H - TASKBAR_H);

        // Windows 7 Light Ribbons / Swooshes
        g2.setStroke(new BasicStroke(40f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D.Double ribbon1 = new Path2D.Double();
        ribbon1.moveTo(-50, H - 40);
        appendBezier(ribbon1, -50, H - 40, W * 0.3, H * 0.35, W * 0.7, H * 0.85, W + 50, H * 0.55);
        g2.setColor(new Color(255, 255, 255, 22));
        g2.draw(ribbon1);

        g2.setStroke(new BasicStroke(16f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D.Double ribbon2 = new Path2D.Double();
        ribbon2.moveTo(-20, H - 20);
        appendBezier(ribbon2, -20, H - 20, W * 0.4, H * 0.3, W * 0.6, H * 0.8, W + 20, H * 0.5);
        g2.setColor(new Color(255, 255, 255, 45));
        g2.draw(ribbon2);

        g2.setStroke(new BasicStroke(3f));
        Path2D.Double ribbon3 = new Path2D.Double();
        ribbon3.moveTo(0, H - 100);
        appendBezier(ribbon3, 0, H - 100, W * 0.35, H * 0.35, W * 0.65, H * 0.75, W, H * 0.55);
        g2.setColor(new Color(255, 255, 255, 130));
        g2.draw(ribbon3);

        // Center Windows Logo Watermark
        drawWin7CenterLogo(g2, W / 2, H / 2 - 30);
    }

    private void drawWin7CenterLogo(Graphics2D g2, int cx, int cy) {
        int s = 22;
        int off = 4;

        g2.setPaint(new GradientPaint(cx - s - off, cy - s - off, new Color(245, 95, 45, 180), cx - off, cy - off, new Color(215, 55, 25, 180)));
        g2.fill(new RoundRectangle2D.Double(cx - s - off, cy - s - off, s, s, 6, 6));

        g2.setPaint(new GradientPaint(cx + off, cy - s - off, new Color(145, 215, 55, 180), cx + s + off, cy - off, new Color(85, 175, 35, 180)));
        g2.fill(new RoundRectangle2D.Double(cx + off, cy - s - off, s, s, 6, 6));

        g2.setPaint(new GradientPaint(cx - s - off, cy + off, new Color(35, 165, 245, 180), cx - off, cy + s + off, new Color(15, 105, 205, 180)));
        g2.fill(new RoundRectangle2D.Double(cx - s - off, cy + off, s, s, 6, 6));

        g2.setPaint(new GradientPaint(cx + off, cy + off, new Color(255, 205, 35, 180), cx + s + off, cy + s + off, new Color(225, 155, 15, 180)));
        g2.fill(new RoundRectangle2D.Double(cx + off, cy + off, s, s, 6, 6));
    }

    private void appendBezier(Path2D.Double path, double x1, double y1, double x2, double y2,
                               double x3, double y3, double x4, double y4) {
        for (int i = 1; i <= 100; i++) {
            double t = i / 100.0;
            double x = Math.pow(1 - t, 3) * x1 + 3 * t * Math.pow(1 - t, 2) * x2
                    + 3 * Math.pow(t, 2) * (1 - t) * x3 + Math.pow(t, 3) * x4;
            double y = Math.pow(1 - t, 3) * y1 + 3 * t * Math.pow(1 - t, 2) * y2
                    + 3 * Math.pow(t, 2) * (1 - t) * y3 + Math.pow(t, 3) * y4;
            path.lineTo(x, y);
        }
    }

    private void drawWindow(Graphics2D g2, int x, int y, int w, int h) {
        int titleH = 32;

        // Aero Shadow
        g2.setColor(new Color(0, 0, 0, 70));
        g2.fill(new RoundRectangle2D.Double(x + 5, y + 5, w, h, 14, 14));

        // Aero Translucent Glass Frame
        g2.setColor(new Color(160, 205, 235, 150));
        g2.fill(new RoundRectangle2D.Double(x, y, w, h, 12, 12));

        // Glass reflection sheen
        GradientPaint glassGlow = new GradientPaint(x, y, new Color(255, 255, 255, 130), x, y + titleH, new Color(255, 255, 255, 20));
        g2.setPaint(glassGlow);
        g2.fill(new RoundRectangle2D.Double(x, y, w, titleH, 12, 12));

        // White client interior area
        int border = 7;
        int clientX = x + border;
        int clientY = y + titleH;
        int clientW = w - border * 2;
        int clientH = h - titleH - border;

        g2.setColor(new Color(252, 252, 252));
        g2.fillRect(clientX, clientY, clientW, clientH);
        g2.setColor(new Color(170, 195, 215));
        g2.drawRect(clientX, clientY, clientW, clientH);

        // Highlight glass edge outline
        g2.setColor(new Color(255, 255, 255, 180));
        g2.draw(new RoundRectangle2D.Double(x + 1, y + 1, w - 2, h - 2, 10, 10));

        // Window Title Text
        g2.setColor(new Color(15, 25, 40));
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        g2.drawString("Windows 7 - Aero Glass", x + 14, y + 21);

        // Windows 7 Control Buttons
        int bw = 28, bh = 18;
        int cx = x + w - bw - 6, cy = y + 1;

        // Close Button
        g2.setPaint(new GradientPaint(cx, cy, new Color(230, 90, 80, 230), cx, cy + bh, new Color(180, 40, 30, 240)));
        g2.fill(new RoundRectangle2D.Double(cx, cy, bw, bh, 4, 4));
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(cx + 10, cy + 5, cx + bw - 10, cy + bh - 5);
        g2.drawLine(cx + bw - 10, cy + 5, cx + 10, cy + bh - 5);

        // Maximize Button
        int mx = cx - bw - 2;
        g2.setPaint(new GradientPaint(mx, cy, new Color(225, 240, 250, 160), mx, cy + bh, new Color(175, 200, 220, 190)));
        g2.fill(new RoundRectangle2D.Double(mx, cy, bw, bh, 4, 4));
        g2.setColor(new Color(40, 50, 65));
        g2.drawRect(mx + 9, cy + 4, 9, 8);

        // Minimize Button
        int nx = mx - bw - 2;
        g2.setPaint(new GradientPaint(nx, cy, new Color(225, 240, 250, 160), nx, cy + bh, new Color(175, 200, 220, 190)));
        g2.fill(new RoundRectangle2D.Double(nx, cy, bw, bh, 4, 4));
        g2.setColor(new Color(40, 50, 65));
        g2.drawLine(nx + 9, cy + 11, nx + 17, cy + 11);

        g2.setStroke(new BasicStroke(1f));

        g2.setColor(new Color(40, 40, 40));
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g2.drawString("Windows 7 Aero Glass visual style loaded.", clientX + 16, clientY + 30);
    }

    private void drawBouncingIcon(Graphics2D g2, double x, double y) {
        int s = ICON_SIZE / 2;
        int arc = 12;

        g2.setColor(new Color(0, 0, 0, 50));
        g2.fill(new RoundRectangle2D.Double(x + 3, y + 3, ICON_SIZE, ICON_SIZE, arc, arc));

        g2.setPaint(new GradientPaint((float)x, (float)y, new Color(250, 95, 65), (float)x, (float)(y + s), new Color(215, 45, 25)));
        g2.fill(new RoundRectangle2D.Double(x, y, s, s, arc, arc));
        g2.setPaint(new GradientPaint((float)(x + s), (float)y, new Color(135, 215, 65), (float)(x + s), (float)(y + s), new Color(75, 165, 35)));
        g2.fill(new RoundRectangle2D.Double(x + s, y, s, s, arc, arc));
        g2.setPaint(new GradientPaint((float)x, (float)(y + s), new Color(40, 155, 245), (float)x, (float)(y + ICON_SIZE), new Color(15, 95, 205)));
        g2.fill(new RoundRectangle2D.Double(x, y + s, s, s, arc, arc));
        g2.setPaint(new GradientPaint((float)(x + s), (float)(y + s), new Color(255, 210, 45), (float)(x + s), (float)(y + ICON_SIZE), new Color(235, 165, 15)));
        g2.fill(new RoundRectangle2D.Double(x + s, y + s, s, s, arc, arc));

        g2.setPaint(new GradientPaint((float)x, (float)y, new Color(255, 255, 255, 130), (float)x, (float)(y + ICON_SIZE / 2), new Color(255, 255, 255, 0)));
        g2.fill(new RoundRectangle2D.Double(x, y, ICON_SIZE, ICON_SIZE / 2.0, arc, arc));
    }

    private void drawTaskbar(Graphics2D g2) {
        int y = H - TASKBAR_H;

        // Translucent dark Aero taskbar
        g2.setColor(new Color(15, 30, 50, 210));
        g2.fillRect(0, y, W, TASKBAR_H);

        // Glass highlight lines
        g2.setColor(new Color(255, 255, 255, 90));
        g2.fillRect(0, y, W, 1);
        g2.setColor(new Color(255, 255, 255, 25));
        g2.fillRect(0, y + 1, W, 1);

        // Windows 7 Round Start Orb
        int orbR = 17;
        int orbX = 22, orbY = y + TASKBAR_H / 2;

        RadialGradientPaint orbGrad = new RadialGradientPaint(
                new Point2D.Float(orbX, orbY - 4), orbR + 2,
                new float[]{0.0f, 0.7f, 1.0f},
                new Color[]{new Color(85, 175, 250), new Color(15, 85, 180), new Color(5, 35, 95)}
        );
        g2.setPaint(orbGrad);
        g2.fillOval(orbX - orbR, orbY - orbR, orbR * 2, orbR * 2);

        // Orb Outline
        g2.setColor(new Color(10, 30, 70, 200));
        g2.drawOval(orbX - orbR, orbY - orbR, orbR * 2, orbR * 2);

        // Flag emblem inside Start Orb
        int fs = 5;
        int fx = orbX - 5, fy = orbY - 5;
        g2.setColor(new Color(245, 95, 65)); g2.fillRect(fx, fy, fs, fs);
        g2.setColor(new Color(135, 215, 65)); g2.fillRect(fx + fs + 1, fy, fs, fs);
        g2.setColor(new Color(40, 155, 245)); g2.fillRect(fx, fy + fs + 1, fs, fs);
        g2.setColor(new Color(255, 210, 45)); g2.fillRect(fx + fs + 1, fy + fs + 1, fs, fs);

        // Glass sheen on orb
        g2.setPaint(new GradientPaint(orbX, orbY - orbR, new Color(255, 255, 255, 160), orbX, orbY, new Color(255, 255, 255, 0)));
        g2.fillOval(orbX - orbR + 2, orbY - orbR + 1, (orbR - 2) * 2, orbR);

        // Show Desktop Button (Far Right)
        g2.setColor(new Color(255, 255, 255, 35));
        g2.fillRect(W - 14, y + 2, 10, TASKBAR_H - 4);
        g2.setColor(new Color(0, 0, 0, 80));
        g2.drawRect(W - 14, y + 2, 10, TASKBAR_H - 4);

        // Clock
        int trayW = 85;
        int trayX = W - trayW - 14;
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a"));
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        FontMetrics fm = g2.getFontMetrics();
        int timeWidth = fm.stringWidth(time);
        g2.setColor(Color.WHITE);
        g2.drawString(time, trayX + (trayW - timeWidth) / 2, y + TASKBAR_H / 2 + 4);
    }

    private void plot(Graphics g, int x, int y) {
        g.fillRect(x, y, 1, 1);
    }

    private void bresenhamLine(Graphics g, int x1, int y1, int x2, int y2) {
        int dx = Math.abs(x2 - x1), dy = Math.abs(y2 - y1);
        int sx = (x1 < x2) ? 1 : -1;
        int sy = (y1 < y2) ? 1 : -1;
        boolean swap = false;
        if (dy > dx) {
            int tmp = dx; dx = dy; dy = tmp;
            swap = true;
        }
        int D = 2 * dy - dx;
        int x = x1, y = y1;
        for (int i = 1; i <= dx; i++) {
            plot(g, x, y);
            if (D >= 0) {
                if (swap) x += sx; else y += sy;
                D -= 2 * dx;
            }
            if (swap) y += sy; else x += sx;
            D += 2 * dy;
        }
    }

    private void midpointCircle(Graphics g, int xc, int yc, int r) {
        int x = 0, y = r, Dx = 0, Dy = 2 * r, D = 1 - r;
        while (x <= y) {
            plot(g, xc + x, yc + y);
            plot(g, xc - x, yc + y);
            plot(g, xc + x, yc - y);
            plot(g, xc - x, yc - y);
            plot(g, xc + y, yc + x);
            plot(g, xc - y, yc + x);
            plot(g, xc + y, yc - x);
            plot(g, xc - y, yc - x);
            x++;
            Dx += 2;
            D += Dx + 1;
            if (D >= 0) {
                y--;
                Dy -= 2;
                D -= Dy;
            }
        }
    }

    private void boundaryFill(BufferedImage img, int x, int y, int boundaryColor, int fillColor) {
        if (img.getRGB(x, y) == fillColor || img.getRGB(x, y) == boundaryColor) return;
        Queue<Point> q = new LinkedList<>();
        q.add(new Point(x, y));

        while (!q.isEmpty()) {
            Point p = q.poll();
            if (p.x < 0 || p.x >= img.getWidth() || p.y < 0 || p.y >= img.getHeight()) continue;

            int current = img.getRGB(p.x, p.y);
            if (current != boundaryColor && current != fillColor) {
                img.setRGB(p.x, p.y, fillColor);
                q.add(new Point(p.x + 1, p.y));
                q.add(new Point(p.x - 1, p.y));
                q.add(new Point(p.x, p.y + 1));
                q.add(new Point(p.x, p.y - 1));
            }
        }
    }
}