import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.Queue;
import javax.swing.*;

public class WindowsXPDemo extends JPanel implements Runnable {

    static final int W = 600, H = 600;
    static final int TASKBAR_H = 34;
    static final int ICON_SIZE = 56;

    double iconX = 100, iconY = 100;
    double iconVX = 160, iconVY = 130;
    double totalTime = 0;

    public static void main(String[] args) {
        WindowsXPDemo m = new WindowsXPDemo();
        JFrame f = new JFrame();
        f.add(m);
        f.setTitle("Windows XP - My Memories");
        f.setSize(W, H);
        f.setResizable(false);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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
        drawWindow(g2, 140, 90, 340, 220);
        drawBouncingIcon(g2, iconX, iconY);
        drawTaskbar(g2);

        g.drawImage(buffer, 0, 0, null);
    }

    private void drawDesktop(BufferedImage buffer, Graphics2D g2) {
        GradientPaint sky = new GradientPaint(0, 0, new Color(40, 110, 210), 0, H - TASKBAR_H, new Color(190, 230, 255));
        g2.setPaint(sky);
        g2.fillRect(0, 0, W, H - TASKBAR_H);
        g2.setPaint(null);

        g2.setColor(new Color(255, 240, 150, 150));
        midpointCircle(g2, 500, 80, 50);
        
        Color sunColor = new Color(255, 236, 130);
        g2.setColor(sunColor);
        midpointCircle(g2, 500, 80, 46);
        boundaryFill(buffer, 500, 80, sunColor.getRGB(), sunColor.getRGB());

        Path2D.Double hillBack = new Path2D.Double();
        hillBack.moveTo(0, H - TASKBAR_H);
        appendBezier(hillBack, 0, 470, 200, 400, 380, 500, 600, 430);
        hillBack.lineTo(W, H - TASKBAR_H);
        hillBack.closePath();
        g2.setPaint(new GradientPaint(0, 400, new Color(120, 180, 70), 0, H - TASKBAR_H, new Color(40, 100, 30)));
        g2.fill(hillBack);

        Path2D.Double hillFront = new Path2D.Double();
        hillFront.moveTo(0, H - TASKBAR_H);
        appendBezier(hillFront, 0, 430, 150, 340, 300, 460, 600, 380);
        hillFront.lineTo(W, H - TASKBAR_H);
        hillFront.closePath();
        g2.setPaint(new GradientPaint(0, 340, new Color(150, 210, 80), 0, H - TASKBAR_H, new Color(60, 140, 40)));
        g2.fill(hillFront);
        g2.setPaint(null);
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
        int titleH = 30;

        g2.setColor(new Color(0, 0, 0, 60));
        g2.fill(new RoundRectangle2D.Double(x + 6, y + 6, w, h, 12, 12));

        g2.setColor(new Color(240, 240, 235));
        g2.fill(new RoundRectangle2D.Double(x, y, w, h, 12, 12));

        GradientPaint titleGrad = new GradientPaint(x, y, new Color(0, 88, 225), x, y + titleH, new Color(30, 110, 255));
        g2.setPaint(titleGrad);
        Path2D.Double titleBar = new Path2D.Double();
        titleBar.moveTo(x, y + titleH);
        titleBar.lineTo(x, y + 10);
        titleBar.quadTo(x, y, x + 10, y);
        titleBar.lineTo(x + w - 10, y);
        titleBar.quadTo(x + w, y, x + w, y + 10);
        titleBar.lineTo(x + w, y + titleH);
        titleBar.closePath();
        g2.fill(titleBar);
        g2.setPaint(null);

        g2.setColor(new Color(255, 255, 255, 100));
        g2.draw(new RoundRectangle2D.Double(x + 1, y + 1, w - 2, h - 2, 10, 10));

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Tahoma", Font.BOLD, 13));
        g2.drawString("My Memories.exe", x + 12, y + 20);

        int bw = 22, bh = 22, gap = 2;
        int bx = x + w - (bw * 3 + gap * 2) - 6, by = y + 4;

        g2.setPaint(new GradientPaint(bx, by, new Color(80, 160, 255), bx, by + bh, new Color(30, 100, 220)));
        g2.fill(new RoundRectangle2D.Double(bx, by, bw, bh, 4, 4));
        g2.setColor(Color.WHITE);
        g2.fillRect(bx + 6, by + bh - 7, bw - 12, 3);
        g2.setColor(new Color(255, 255, 255, 100));
        g2.drawRoundRect(bx, by, bw, bh, 4, 4);

        g2.setPaint(new GradientPaint(bx + bw + gap, by, new Color(80, 160, 255), bx + bw + gap, by + bh, new Color(30, 100, 220)));
        g2.fill(new RoundRectangle2D.Double(bx + bw + gap, by, bw, bh, 4, 4));
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2f));
        g2.drawRect(bx + bw + gap + 6, by + 6, bw - 12, bh - 12);
        g2.fillRect(bx + bw + gap + 6, by + 6, bw - 12, 3);
        g2.setStroke(new BasicStroke(1f));
        g2.setColor(new Color(255, 255, 255, 100));
        g2.drawRoundRect(bx + bw + gap, by, bw, bh, 4, 4);

        int cx = bx + (bw + gap) * 2, cy = by;
        g2.setPaint(new GradientPaint(cx, cy, new Color(240, 100, 80), cx, cy + bh, new Color(210, 40, 30)));
        g2.fill(new RoundRectangle2D.Double(cx, cy, bw, bh, 4, 4));
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2f));
        g2.drawLine(cx + 7, cy + 7, cx + bw - 7, cy + bh - 7);
        g2.drawLine(cx + bw - 7, cy + 7, cx + 7, cy + bh - 7);
        g2.setStroke(new BasicStroke(1f));
        g2.setColor(new Color(255, 255, 255, 150));
        g2.drawRoundRect(cx, cy, bw, bh, 4, 4);

        g2.setColor(new Color(0, 70, 200));
        bresenhamLine(g2, x, y + 10, x, y + h - 10);
        bresenhamLine(g2, x + w, y + 10, x + w, y + h - 10);
        bresenhamLine(g2, x + 10, y, x + w - 10, y);
        bresenhamLine(g2, x + 10, y + h, x + w - 10, y + h);

        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Tahoma", Font.PLAIN, 12));
        g2.drawString("Insert your own memory here...", x + 16, y + titleH + 30);
    }

    private void drawBouncingIcon(Graphics2D g2, double x, double y) {
        int s = ICON_SIZE / 2;
        int arc = 12;

        g2.setColor(new Color(0, 0, 0, 50));
        g2.fill(new RoundRectangle2D.Double(x + 3, y + 3, ICON_SIZE, ICON_SIZE, arc, arc));

        g2.setPaint(new GradientPaint((float)x, (float)y, new Color(250, 90, 60), (float)x, (float)(y + s), new Color(210, 40, 20)));
        g2.fill(new RoundRectangle2D.Double(x, y, s, s, arc, arc));
        g2.setPaint(new GradientPaint((float)(x + s), (float)y, new Color(130, 210, 80), (float)(x + s), (float)(y + s), new Color(70, 160, 40)));
        g2.fill(new RoundRectangle2D.Double(x + s, y, s, s, arc, arc));
        g2.setPaint(new GradientPaint((float)x, (float)(y + s), new Color(70, 150, 250), (float)x, (float)(y + ICON_SIZE), new Color(30, 90, 210)));
        g2.fill(new RoundRectangle2D.Double(x, y + s, s, s, arc, arc));
        g2.setPaint(new GradientPaint((float)(x + s), (float)(y + s), new Color(255, 230, 80), (float)(x + s), (float)(y + ICON_SIZE), new Color(230, 180, 20)));
        g2.fill(new RoundRectangle2D.Double(x + s, y + s, s, s, arc, arc));

        g2.setPaint(new GradientPaint((float)x, (float)y, new Color(255, 255, 255, 120), (float)x, (float)(y + ICON_SIZE / 2), new Color(255, 255, 255, 0)));
        g2.fill(new RoundRectangle2D.Double(x, y, ICON_SIZE, ICON_SIZE / 2.0, arc, arc));
    }

    private void drawTaskbar(Graphics2D g2) {
        int y = H - TASKBAR_H;
        
        GradientPaint bar = new GradientPaint(0, y, new Color(30, 90, 220), 0, H, new Color(15, 60, 160));
        g2.setPaint(bar);
        g2.fillRect(0, y, W, TASKBAR_H);
        
        g2.setColor(new Color(60, 140, 255));
        g2.fillRect(0, y, W, 2);
        g2.setColor(new Color(15, 50, 130));
        g2.fillRect(0, y + 2, W, 1);

        int tw = 70;
        int trayX = W - tw - 10;
        g2.setPaint(new GradientPaint(trayX, y, new Color(10, 50, 140), trayX, H, new Color(30, 100, 210)));
        g2.fillRect(trayX, y, W - trayX, TASKBAR_H);
        g2.setColor(new Color(0, 30, 100));
        g2.drawLine(trayX, y, trayX, H);

        RoundRectangle2D.Double startBtn = new RoundRectangle2D.Double(0, y, 105, TASKBAR_H, 15, 15);
        GradientPaint startGrad = new GradientPaint(0, y, new Color(80, 180, 70), 0, H, new Color(40, 120, 30));
        g2.setPaint(startGrad);
        g2.fill(startBtn);

        g2.setPaint(new GradientPaint(0, y, new Color(255, 255, 255, 100), 0, y + TASKBAR_H / 2, new Color(255, 255, 255, 0)));
        g2.fill(new RoundRectangle2D.Double(0, y, 105, TASKBAR_H / 2.0, 15, 15));

        g2.setColor(new Color(20, 80, 10));
        g2.draw(new RoundRectangle2D.Double(0, y, 105, TASKBAR_H, 15, 15));

        int fx = 12, fy = y + 10, fs = 7;
        g2.setColor(new Color(240, 80, 50)); g2.fillRect(fx, fy, fs, fs);
        g2.setColor(new Color(110, 200, 70)); g2.fillRect(fx + fs + 1, fy, fs, fs);
        g2.setColor(new Color(70, 140, 240)); g2.fillRect(fx, fy + fs + 1, fs, fs);
        g2.setColor(new Color(250, 220, 60)); g2.fillRect(fx + fs + 1, fy + fs + 1, fs, fs);

        g2.setColor(new Color(0, 0, 0, 100));
        g2.setFont(new Font("Tahoma", Font.BOLD | Font.ITALIC, 17));
        g2.drawString("start", 37, y + 23);
        g2.setColor(Color.WHITE);
        g2.drawString("start", 36, y + 22);

        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a"));
        g2.setFont(new Font("Tahoma", Font.PLAIN, 12));
        FontMetrics fm = g2.getFontMetrics();
        int timeWidth = fm.stringWidth(time);
        g2.setColor(Color.WHITE);
        g2.drawString(time, trayX + (W - trayX - timeWidth) / 2, y + TASKBAR_H / 2 + 5);
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