import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javax.swing.*;

public class WindowsTransitionDemo extends JPanel implements Runnable {

    static final int W = 600, H = 600;
    static final int TASKBAR_H = 34;
    static final int ICON_SIZE = 56;

    double iconX = 100, iconY = 100;
    double iconVX = 160, iconVY = 130;
    double totalTime = 0;
    
    int mouseX = 0, mouseY = 0;

    public WindowsTransitionDemo() {
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
            }
        });
    }

    public static void main(String[] args) {
        WindowsTransitionDemo m = new WindowsTransitionDemo();
        JFrame f = new JFrame();
        f.add(m);
        f.setTitle("Windows XP to Windows 7 Transition");
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

        int winX = 170, winY = 80, winW = 280, winH = 340;
        int panelX = winX + 12;
        int panelY = winY + 42;
        int panelW = winW - 24;
        int panelH = 38;
        int gridX = panelX;
        int gridY = panelY + panelH + 8;
        int gridW = panelW;
        int gridH = winH - (gridY - winY) - 12;
        int cellW = (gridW - 6) / 9;
        int cellH = (gridH - 6) / 9;
        int explosionX = gridX + 4 + 8 * cellW + cellW / 2;
        int explosionY = gridY + 4 + 4 * cellH + cellH / 2;

        if (totalTime < 5.5) {
            drawXPScene(buffer, g2, winX, winY, winW, winH);
        } else if (totalTime >= 5.5 && totalTime < 7.5) {
            double progress = (totalTime - 5.5) / 2.0;
            double maxDist = Math.hypot(W, H);
            double currentRadius = progress * maxDist;

            drawXPDesktop(buffer, g2);

            Shape oldClip = g2.getClip();
            Ellipse2D.Double revealCircle = new Ellipse2D.Double(
                explosionX - currentRadius, explosionY - currentRadius,
                currentRadius * 2, currentRadius * 2
            );
            g2.setClip(revealCircle);

            drawWin7Scene(buffer, g2);
            g2.setClip(oldClip);

            drawRetroExplosion(g2, explosionX, explosionY, progress);
        } else {
            drawWin7Scene(buffer, g2);
        }

        // Persistent debug info across all phases
        drawDebugInfo(g2);

        g.drawImage(buffer, 0, 0, null);
    }

    private void drawXPScene(BufferedImage buffer, Graphics2D g2, int winX, int winY, int winW, int winH) {
        drawXPDesktop(buffer, g2);
        drawWindow(g2, 60, 70, 310, 210);
        drawMinesweeperWindow(g2, winX, winY, winW, winH);
        drawBouncingIcon(g2, iconX, iconY);
        drawXPTaskbar(g2);
    }

    private void drawWin7Scene(BufferedImage buffer, Graphics2D g2) {
        drawWin7Desktop(buffer, g2);
        // drawWin7Window(g2, 130, 80, 340, 220);
        drawBouncingIcon(g2, iconX, iconY);
        drawWin7Taskbar(g2);
    }

    private void drawRetroExplosion(Graphics2D g2, int cx, int cy, double progress) {
        int maxRadius = (int)(progress * 650);
        
        g2.setColor(new Color(255, 255, 0, Math.max(0, 255 - (int)(progress * 255))));
        g2.fillOval(cx - maxRadius, cy - maxRadius, maxRadius * 2, maxRadius * 2);

        g2.setColor(new Color(255, 100, 0, Math.max(0, 255 - (int)(progress * 300))));
        g2.fillOval(cx - (int)(maxRadius * 0.7), cy - (int)(maxRadius * 0.7), (int)(maxRadius * 1.4), (int)(maxRadius * 1.4));

        g2.setColor(new Color(255, 255, 255, Math.max(0, 255 - (int)(progress * 400))));
        g2.fillOval(cx - (int)(maxRadius * 0.3), cy - (int)(maxRadius * 0.3), (int)(maxRadius * 0.6), (int)(maxRadius * 0.6));

        g2.setStroke(new BasicStroke(4f));
        g2.setColor(Color.YELLOW);
        for (int angle = 0; angle < 360; angle += 30) {
            double rad = Math.toRadians(angle);
            int x1 = (int)(cx + Math.cos(rad) * (maxRadius * 0.2));
            int y1 = (int)(cy + Math.sin(rad) * (maxRadius * 0.2));
            int x2 = (int)(cx + Math.cos(rad) * maxRadius);
            int y2 = (int)(cy + Math.sin(rad) * maxRadius);
            g2.drawLine(x1, y1, x2, y2);
        }
        g2.setStroke(new BasicStroke(1f));
    }

    private void drawDebugInfo(Graphics2D g2) {
        g2.setColor(Color.BLACK);
        g2.fillRect(5, 5, 120, 25);
        g2.setColor(Color.GREEN);
        g2.drawRect(5, 5, 120, 25);
        g2.setFont(new Font("Monospaced", Font.BOLD, 14));
        g2.drawString("X: " + mouseX + " Y: " + mouseY, 15, 23);
        
        g2.setColor(Color.RED);
        g2.drawLine(mouseX - 10, mouseY, mouseX + 10, mouseY);
        g2.drawLine(mouseX, mouseY - 10, mouseX, mouseY + 10);
    }

    private void drawXPDesktop(BufferedImage buffer, Graphics2D g2) {
        Color cSkyDark = new Color(58, 121, 223);
        Color cSkyMid = new Color(135, 179, 241);
        Color cCloudShadow = new Color(175, 203, 241);
        Color cCloud = new Color(233, 239, 246);
        
        Color cGrassLight = new Color(135, 186, 46);
        Color cGrassMid = new Color(102, 152, 36);
        Color cGrassDark = new Color(71, 107, 26);
        Color cGrassDeep = new Color(52, 80, 20);

        g2.setColor(cSkyMid);
        g2.fillRect(0, 0, W, H - TASKBAR_H);

        drawPoly(g2, cSkyDark, 
            new int[]{0, 110, 160, 200, 150, 120, 80, 30, 0}, 
            new int[]{0, 0,   30,  80,  120, 140, 130, 90, 70});
        drawPoly(g2, cSkyDark, 
            new int[]{220, 600, 600, 480, 450, 400, 320, 280, 250}, 
            new int[]{0,   0,   250, 280, 210, 220, 160, 100, 50});

        drawPoly(g2, cCloudShadow,
            new int[]{0, 100, 150, 200, 350, 450, 600, 600, 0},
            new int[]{220, 230, 210, 240, 250, 230, 270, 350, 350});
        drawPoly(g2, cCloudShadow,
            new int[]{350, 420, 480, 550, 600, 600, 520, 450, 400},
            new int[]{120, 110, 130, 150, 140, 200, 220, 210, 160});
        drawPoly(g2, cCloudShadow,
            new int[]{70, 180, 280, 340, 260, 120},
            new int[]{160, 140, 170, 200, 190, 180});

        drawPoly(g2, cCloud,
            new int[]{0, 60, 120, 160, 130, 80, 30, 0},
            new int[]{240, 230, 250, 280, 290, 270, 280, 260});
        drawPoly(g2, cCloud,
            new int[]{420, 480, 540, 600, 600, 560, 490, 450},
            new int[]{150, 140, 160, 180, 240, 250, 220, 190});
        drawPoly(g2, cCloud,
            new int[]{280, 320, 360, 340, 300},
            new int[]{260, 250, 270, 290, 280});
        drawPoly(g2, cCloud,
            new int[]{80, 120, 150, 110, 60},
            new int[]{40, 30, 50, 70, 60});
        drawPoly(g2, cCloud,
            new int[]{220, 290, 330, 280, 240},
            new int[]{180, 170, 200, 220, 210});

        drawPoly(g2, cGrassLight,
            new int[]{0, 150, 250, 350, 450, 550, 600, 600, 0},
            new int[]{315, 310, 318, 330, 345, 365, 385, 600, 600});

        drawPoly(g2, cGrassMid,
            new int[]{0, 100, 200, 350, 500, 600, 600, 0},
            new int[]{350, 355, 365, 390, 420, 440, 600, 600});
        drawPoly(g2, cGrassMid,
            new int[]{200, 300, 400, 500, 600, 600, 450, 300},
            new int[]{325, 335, 360, 380, 410, 440, 400, 350});

        drawPoly(g2, cGrassDark,
            new int[]{0, 150, 300, 450, 600, 600, 0},
            new int[]{420, 430, 450, 480, 500, 600, 600});
        drawPoly(g2, cGrassDark,
            new int[]{0, 120, 280, 450, 600, 600, 350, 150},
            new int[]{380, 395, 420, 440, 460, 510, 470, 430});
        drawPoly(g2, cGrassDark,
            new int[]{250, 400, 550, 600, 600, 450, 300},
            new int[]{370, 390, 410, 420, 450, 430, 390});

        drawPoly(g2, cGrassDeep,
            new int[]{0, 180, 350, 500, 600, 600, 0},
            new int[]{490, 505, 520, 540, 550, 600, 600});
        drawPoly(g2, cGrassDeep,
            new int[]{0, 250, 450, 600, 600, 0},
            new int[]{540, 555, 570, 580, 600, 600});
    }

    private void drawWin7Desktop(BufferedImage buffer, Graphics2D g2) {
        GradientPaint sky = new GradientPaint(0, 0, new Color(15, 95, 185), 0, H - TASKBAR_H, new Color(5, 45, 105));
        g2.setPaint(sky);
        g2.fillRect(0, 0, W, H - TASKBAR_H);

        RadialGradientPaint glow = new RadialGradientPaint(
                new Point2D.Float(W / 2f, H / 2f - 30),
                350f,
                new float[]{0.0f, 1.0f},
                new Color[]{new Color(55, 175, 245, 160), new Color(0, 0, 0, 0)}
        );
        g2.setPaint(glow);
        g2.fillRect(0, 0, W, H - TASKBAR_H);

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

        drawWin7CenterLogo(g2, W / 2, H / 2 - 30);
    }

    private void drawWin7CenterLogo(Graphics2D g2, int cx, int cy) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // 1. Top-Left Pane (Red / Orange)
        Path2D.Double redPane = new Path2D.Double();
        redPane.moveTo(cx - 130, cy - 65);
        redPane.curveTo(cx - 90, cy - 120, cx - 45, cy - 110, cx - 4, cy - 75); // Top wave
        redPane.lineTo(cx - 4, cy - 10);                                       // Center vertical seam
        redPane.curveTo(cx - 45, cy - 42, cx - 90, cy - 52, cx - 130, cy + 5);   // Middle wave
        redPane.closePath();

        g2.setPaint(new GradientPaint(cx - 130, cy - 120, new Color(245, 95, 40, 225), 
                                    cx - 4, cy - 10, new Color(210, 45, 25, 225)));
        g2.fill(redPane);

        // 2. Top-Right Pane (Green)
        Path2D.Double greenPane = new Path2D.Double();
        greenPane.moveTo(cx + 4, cy - 71);
        greenPane.curveTo(cx + 45, cy - 35, cx + 90, cy - 45, cx + 130, cy - 90); // Top wave
        greenPane.lineTo(cx + 130, cy - 23);                                      // Right outer edge
        greenPane.curveTo(cx + 90, cy + 22, cx + 45, cy + 32, cx + 4, cy - 4);     // Middle wave
        greenPane.closePath();

        g2.setPaint(new GradientPaint(cx + 4, cy - 71, new Color(145, 215, 50, 225), 
                                    cx + 130, cy - 23, new Color(75, 175, 30, 225)));
        g2.fill(greenPane);

        // 3. Bottom-Left Pane (Blue)
        Path2D.Double bluePane = new Path2D.Double();
        bluePane.moveTo(cx - 130, cy + 13);
        bluePane.curveTo(cx - 90, cy - 44, cx - 45, cy - 34, cx - 4, cy - 2);   // Middle wave
        bluePane.lineTo(cx - 4, cy + 63);                                      // Center vertical seam
        bluePane.curveTo(cx - 45, cy + 31, cx - 90, cy + 21, cx - 130, cy + 78); // Bottom wave
        bluePane.closePath();

        g2.setPaint(new GradientPaint(cx - 130, cy - 34, new Color(35, 170, 245, 225), 
                                    cx - 4, cy + 63, new Color(15, 100, 210, 225)));
        g2.fill(bluePane);

        // 4. Bottom-Right Pane (Yellow / Gold)
        Path2D.Double yellowPane = new Path2D.Double();
        yellowPane.moveTo(cx + 4, cy + 4);
        yellowPane.curveTo(cx + 45, cy + 40, cx + 90, cy + 30, cx + 130, cy - 15); // Middle wave
        yellowPane.lineTo(cx + 130, cy + 50);                                      // Right outer edge
        yellowPane.curveTo(cx + 90, cy + 95, cx + 45, cy + 105, cx + 4, cy + 69); // Bottom wave
        yellowPane.closePath();

        g2.setPaint(new GradientPaint(cx + 4, cy + 4, new Color(255, 205, 30, 225), 
                                    cx + 130, cy + 50, new Color(225, 150, 10, 225)));
        g2.fill(yellowPane);
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

    private void drawPoly(Graphics2D g2, Color c, int[] x, int[] y) {
        g2.setColor(c);
        g2.fillPolygon(x, y, x.length);
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

        g2.setPaint(new GradientPaint(bx + bw + gap, by, new Color(80, 160, 255), bx + bw + gap, by + bh, new Color(30, 100, 220)));
        g2.fill(new RoundRectangle2D.Double(bx + bw + gap, by, bw, bh, 4, 4));
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2f));
        g2.drawRect(bx + bw + gap + 6, by + 6, bw - 12, bh - 12);
        g2.fillRect(bx + bw + gap + 6, by + 6, bw - 12, 3);
        g2.setStroke(new BasicStroke(1f));

        int cx = bx + (bw + gap) * 2, cy = by;
        g2.setPaint(new GradientPaint(cx, cy, new Color(240, 100, 80), cx, cy + bh, new Color(210, 40, 30)));
        g2.fill(new RoundRectangle2D.Double(cx, cy, bw, bh, 4, 4));
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2f));
        g2.drawLine(cx + 7, cy + 7, cx + bw - 7, cy + bh - 7);
        g2.drawLine(cx + bw - 7, cy + 7, cx + 7, cy + bh - 7);
        g2.setStroke(new BasicStroke(1f));

        g2.setColor(new Color(0, 70, 200));
        bresenhamLine(g2, x, y + 10, x, y + h - 10);
        bresenhamLine(g2, x + w, y + 10, x + w, y + h - 10);
        bresenhamLine(g2, x + 10, y, x + w - 10, y);
        bresenhamLine(g2, x + 10, y + h, x + w - 10, y + h);

        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Tahoma", Font.PLAIN, 12));
        g2.drawString("Insert your own memory here...", x + 16, y + titleH + 30);
    }

    private void drawWin7Window(Graphics2D g2, int x, int y, int w, int h) {
        int titleH = 32;

        g2.setColor(new Color(0, 0, 0, 70));
        g2.fill(new RoundRectangle2D.Double(x + 5, y + 5, w, h, 14, 14));

        g2.setColor(new Color(160, 205, 235, 150));
        g2.fill(new RoundRectangle2D.Double(x, y, w, h, 12, 12));

        GradientPaint glassGlow = new GradientPaint(x, y, new Color(255, 255, 255, 130), x, y + titleH, new Color(255, 255, 255, 20));
        g2.setPaint(glassGlow);
        g2.fill(new RoundRectangle2D.Double(x, y, w, titleH, 12, 12));

        int border = 7;
        int clientX = x + border;
        int clientY = y + titleH;
        int clientW = w - border * 2;
        int clientH = h - titleH - border;

        g2.setColor(new Color(252, 252, 252));
        g2.fillRect(clientX, clientY, clientW, clientH);
        g2.setColor(new Color(170, 195, 215));
        g2.drawRect(clientX, clientY, clientW, clientH);

        g2.setColor(new Color(255, 255, 255, 180));
        g2.draw(new RoundRectangle2D.Double(x + 1, y + 1, w - 2, h - 2, 10, 10));

        g2.setColor(new Color(15, 25, 40));
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        g2.drawString("Windows 7 - Aero Glass", x + 14, y + 21);

        int bw = 28, bh = 18;
        int cx = x + w - bw - 6, cy = y + 1;

        g2.setPaint(new GradientPaint(cx, cy, new Color(230, 90, 80, 230), cx, cy + bh, new Color(180, 40, 30, 240)));
        g2.fill(new RoundRectangle2D.Double(cx, cy, bw, bh, 4, 4));
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(cx + 10, cy + 5, cx + bw - 10, cy + bh - 5);
        g2.drawLine(cx + bw - 10, cy + 5, cx + 10, cy + bh - 5);

        int mx = cx - bw - 2;
        g2.setPaint(new GradientPaint(mx, cy, new Color(225, 240, 250, 160), mx, cy + bh, new Color(175, 200, 220, 190)));
        g2.fill(new RoundRectangle2D.Double(mx, cy, bw, bh, 4, 4));
        g2.setColor(new Color(40, 50, 65));
        g2.drawRect(mx + 9, cy + 4, 9, 8);

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

    private void drawMinesweeperWindow(Graphics2D g2, int x, int y, int w, int h) {
        int titleH = 30;

        g2.setColor(new Color(0, 0, 0, 60));
        g2.fill(new RoundRectangle2D.Double(x + 6, y + 6, w, h, 12, 12));

        g2.setColor(new Color(192, 192, 192));
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
        g2.drawString("Minesweeper", x + 12, y + 20);

        int bw = 22, bh = 22, gap = 2;
        int bx = x + w - (bw * 3 + gap * 2) - 6, by = y + 4;

        g2.setPaint(new GradientPaint(bx, by, new Color(80, 160, 255), bx, by + bh, new Color(30, 100, 220)));
        g2.fill(new RoundRectangle2D.Double(bx, by, bw, bh, 4, 4));
        g2.setColor(Color.WHITE);
        g2.fillRect(bx + 6, by + bh - 7, bw - 12, 3);
        
        g2.setPaint(new GradientPaint(bx + bw + gap, by, new Color(80, 160, 255), bx + bw + gap, by + bh, new Color(30, 100, 220)));
        g2.fill(new RoundRectangle2D.Double(bx + bw + gap, by, bw, bh, 4, 4));
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2f));
        g2.drawRect(bx + bw + gap + 6, by + 6, bw - 12, bh - 12);
        g2.fillRect(bx + bw + gap + 6, by + 6, bw - 12, 3);
        g2.setStroke(new BasicStroke(1f));

        int cx = bx + (bw + gap) * 2, cy = by;
        g2.setPaint(new GradientPaint(cx, cy, new Color(240, 100, 80), cx, cy + bh, new Color(210, 40, 30)));
        g2.fill(new RoundRectangle2D.Double(cx, cy, bw, bh, 4, 4));
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2f));
        g2.drawLine(cx + 7, cy + 7, cx + bw - 7, cy + bh - 7);
        g2.drawLine(cx + bw - 7, cy + 7, cx + 7, cy + bh - 7);
        g2.setStroke(new BasicStroke(1f));

        g2.setColor(new Color(0, 70, 200));
        bresenhamLine(g2, x, y + 10, x, y + h - 10);
        bresenhamLine(g2, x + w, y + 10, x + w, y + h - 10);
        bresenhamLine(g2, x + 10, y, x + w - 10, y);
        bresenhamLine(g2, x + 10, y + h, x + w - 10, y + h);

        int panelX = x + 12;
        int panelY = y + 42;
        int panelW = w - 24;
        int panelH = 38;
        
        g2.setColor(new Color(128, 128, 128));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRect(panelX, panelY, panelW, panelH);
        g2.setColor(Color.WHITE);
        g2.drawRect(panelX + 2, panelY + 2, panelW - 4, panelH - 4);
        g2.setStroke(new BasicStroke(1f));
        g2.setColor(new Color(192, 192, 192));
        g2.fillRect(panelX + 2, panelY + 2, panelW - 4, panelH - 4);

        int gameStep;
        if (totalTime < 5.4) {
            gameStep = (int)(totalTime * 0.5) % 3;
        } else {
            gameStep = 3; 
        }

        g2.setColor(new Color(128, 128, 128));
        g2.drawRect(panelX + 8, panelY + 7, 40, 24);
        g2.setColor(Color.WHITE);
        g2.drawRect(panelX + 9, panelY + 8, 38, 22);
        g2.setColor(Color.BLACK);
        g2.fillRect(panelX + 10, panelY + 9, 36, 20);
        g2.setColor(Color.RED);
        g2.setFont(new Font("Monospaced", Font.BOLD, 18));
        g2.drawString("010", panelX + 11, panelY + 25);

        int faceX = panelX + panelW / 2 - 13;
        int faceY = panelY + 6;
        g2.setColor(new Color(192, 192, 192));
        g2.fillRect(faceX, faceY, 26, 26);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2f));
        g2.drawLine(faceX, faceY, faceX + 25, faceY);
        g2.drawLine(faceX, faceY, faceX, faceY + 25);
        g2.setColor(new Color(128, 128, 128));
        g2.drawLine(faceX + 25, faceY, faceX + 25, faceY + 25);
        g2.drawLine(faceX, faceY + 25, faceX + 25, faceY + 25);
        g2.setStroke(new BasicStroke(1f));
        
        g2.setColor(Color.YELLOW);
        g2.fillOval(faceX + 3, faceY + 3, 20, 20);
        g2.setColor(Color.BLACK);
        g2.drawOval(faceX + 3, faceY + 3, 20, 20);
        
        if (gameStep == 3) {
            g2.drawLine(faceX + 7, faceY + 8, faceX + 11, faceY + 12);
            g2.drawLine(faceX + 7, faceY + 12, faceX + 11, faceY + 8);
            g2.drawLine(faceX + 15, faceY + 8, faceX + 19, faceY + 12);
            g2.drawLine(faceX + 15, faceY + 12, faceX + 19, faceY + 8);
            g2.drawArc(faceX + 8, faceY + 13, 10, 8, 0, 180);
        } else {
            g2.fillRect(faceX + 8, faceY + 9, 2, 3);
            g2.fillRect(faceX + 16, faceY + 9, 2, 3);
            g2.drawArc(faceX + 8, faceY + 11, 10, 8, 0, -180);
        }

        g2.setColor(new Color(128, 128, 128));
        g2.drawRect(panelX + panelW - 48, panelY + 7, 40, 24);
        g2.setColor(Color.WHITE);
        g2.drawRect(panelX + panelW - 47, panelY + 8, 38, 22);
        g2.setColor(Color.BLACK);
        g2.fillRect(panelX + panelW - 46, panelY + 9, 36, 20);
        g2.setColor(Color.RED);
        int timeVal = (gameStep == 3) ? 43 : (int)(totalTime * 3) % 999;
        String timeStr = String.format("%03d", timeVal);
        g2.drawString(timeStr, panelX + panelW - 45, panelY + 25);

        int gridX = panelX;
        int gridY = panelY + panelH + 8;
        int gridW = panelW;
        int gridH = h - (gridY - y) - 12;

        g2.setColor(new Color(128, 128, 128));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRect(gridX, gridY, gridW, gridH);
        g2.setColor(Color.WHITE);
        g2.drawRect(gridX + 2, gridY + 2, gridW - 4, gridH - 4);
        g2.setStroke(new BasicStroke(1f));
        g2.setColor(new Color(192, 192, 192));
        g2.fillRect(gridX + 2, gridY + 2, gridW - 4, gridH - 4);

        int cols = 9;
        int rows = 9;
        int cellW = (gridW - 6) / cols;
        int cellH = (gridH - 6) / rows;

        int[][] patchStep1 = {
            {2, 0, 1, 1},
            {2, 1, 1, 2}, {3, 1, 1, 1}, {4, 1, 1, 1}, {5, 1, 1, 1}, {7, 1, 1, 1}, {8, 1, 1, 1},
            {3, 2, 1, 2}, {5, 2, 1, 1}, {7, 2, 1, 1},
            {0, 3, 1, 1}, {1, 3, 1, 2}, {3, 3, 1, 2}, {5, 3, 1, 1}, {6, 3, 1, 0}, {7, 3, 1, 2},
            {1, 4, 1, 1}, {2, 4, 1, 1}, {3, 4, 1, 1}, {4, 4, 1, 0}, {5, 4, 1, 0}, {6, 4, 1, 0}, {7, 4, 1, 2},
            {0, 5, 1, 0}, {1, 5, 1, 0}, {2, 5, 1, 0}, {3, 5, 1, 0}, {4, 5, 1, 1}, {5, 5, 1, 1}, {6, 5, 1, 1}, {7, 5, 1, 2},
            {0, 6, 1, 2}, {1, 6, 1, 2}, {2, 6, 1, 1}, {3, 6, 1, 0}, {4, 6, 1, 1}, {6, 6, 1, 1}, {7, 6, 1, 1}, {8, 6, 1, 1},
            {2, 7, 1, 1}, {3, 7, 1, 0}, {4, 7, 1, 1}, {5, 7, 1, 1}, {6, 7, 1, 1},
            {2, 8, 1, 1}, {3, 8, 1, 0}, {4, 8, 1, 0}, {5, 8, 1, 0}, {6, 8, 1, 0}, {7, 8, 1, 0}, {8, 8, 1, 0}
        };

        int[][] mines = {
            {1, 1}, {1, 2}, {4, 2}, {8, 2}, {2, 3}, {8, 4}, {8, 5}, {5, 6}, {0, 7}, {1, 7}
        };

        boolean hitBomb = (gameStep == 3);

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int cx2 = gridX + 4 + c * cellW;
                int cy2 = gridY + 4 + r * cellH;

                boolean isOpen = false;
                int cellVal = 1;

                if (gameStep >= 1) {
                    for (int i = 0; i < patchStep1.length; i++) {
                        if (patchStep1[i][0] == c && patchStep1[i][1] == r) {
                            if (c == 3 && r == 2 && patchStep1[i][3] == 2) {
                                if (gameStep >= 2) {
                                    isOpen = true;
                                    cellVal = patchStep1[i][3];
                                }
                            } else {
                                isOpen = true;
                                cellVal = patchStep1[i][3];
                            }
                            break;
                        }
                    }
                }

                boolean isMine = false;
                for (int i = 0; i < mines.length; i++) {
                    if (mines[i][0] == c && mines[i][1] == r) {
                        isMine = true;
                        break;
                    }
                }

                if (hitBomb && isMine) {
                    boolean isHitCell = (c == 8 && r == 4);
                    if (isHitCell) {
                        g2.setColor(new Color(230, 80, 80));
                    } else {
                        g2.setColor(new Color(192, 192, 192));
                    }
                    g2.fillRect(cx2, cy2, cellW, cellH);
                    g2.setColor(Color.BLACK);
                    g2.drawRect(cx2, cy2, cellW, cellH);

                    int bombCenterX = cx2 + cellW / 2;
                    int bombCenterY = cy2 + cellH / 2;
                    int bRadius = Math.min(cellW, cellH) / 2 - 3;

                    g2.setColor(Color.BLACK);
                    g2.setStroke(new BasicStroke(1.5f));
                    for (int angle = 0; angle < 360; angle += 45) {
                        double rad = Math.toRadians(angle);
                        int sx1 = (int)(bombCenterX + Math.cos(rad) * 2);
                        int sy1 = (int)(bombCenterY + Math.sin(rad) * 2);
                        int sx2 = (int)(bombCenterX + Math.cos(rad) * (bRadius + 1));
                        int sy2 = (int)(bombCenterY + Math.sin(rad) * (bRadius + 1));
                        g2.drawLine(sx1, sy1, sx2, sy2);
                    }
                    g2.setStroke(new BasicStroke(1f));

                    g2.setColor(Color.BLACK);
                    g2.fillOval(bombCenterX - bRadius + 1, bombCenterY - bRadius + 1, (bRadius - 1) * 2, (bRadius - 1) * 2);

                    g2.setColor(Color.WHITE);
                    g2.fillOval(bombCenterX - 2, bombCenterY - 2, 2, 2);

                } else if (isOpen) {
                    g2.setColor(new Color(180, 180, 180));
                    g2.fillRect(cx2, cy2, cellW, cellH);
                    g2.setColor(new Color(128, 128, 128));
                    g2.drawRect(cx2, cy2, cellW, cellH);

                    if (cellVal > 0) {
                        if (cellVal == 1) g2.setColor(Color.BLUE);
                        else if (cellVal == 2) g2.setColor(new Color(0, 128, 0));
                        else g2.setColor(Color.RED);

                        g2.setFont(new Font("Tahoma", Font.BOLD, 11));
                        g2.drawString(String.valueOf(cellVal), cx2 + 4, cy2 + cellH - 4);
                    }
                } else {
                    g2.setColor(new Color(192, 192, 192));
                    g2.fillRect(cx2, cy2, cellW, cellH);
                    
                    g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawLine(cx2, cy2, cx2 + cellW - 1, cy2);
                    g2.drawLine(cx2, cy2, cx2, cy2 + cellH - 1);
                    
                    g2.setColor(new Color(128, 128, 128));
                    g2.drawLine(cx2 + cellW - 1, cy2, cx2 + cellW - 1, cy2 + cellH - 1);
                    g2.drawLine(cx2, cy2 + cellH - 1, cx2 + cellW - 1, cy2 + cellH - 1);
                    g2.setStroke(new BasicStroke(1f));
                }
            }
        }
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

    private void drawXPTaskbar(Graphics2D g2) {
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

    private void drawWin7Taskbar(Graphics2D g2) {
        int y = H - TASKBAR_H;

        g2.setColor(new Color(15, 30, 50, 210));
        g2.fillRect(0, y, W, TASKBAR_H);

        g2.setColor(new Color(255, 255, 255, 90));
        g2.fillRect(0, y, W, 1);
        g2.setColor(new Color(255, 255, 255, 25));
        g2.fillRect(0, y + 1, W, 1);

        int orbR = 17;
        int orbX = 22, orbY = y + TASKBAR_H / 2;

        RadialGradientPaint orbGrad = new RadialGradientPaint(
                new Point2D.Float(orbX, orbY - 4), orbR + 2,
                new float[]{0.0f, 0.7f, 1.0f},
                new Color[]{new Color(85, 175, 250), new Color(15, 85, 180), new Color(5, 35, 95)}
        );
        g2.setPaint(orbGrad);
        g2.fillOval(orbX - orbR, orbY - orbR, orbR * 2, orbR * 2);

        g2.setColor(new Color(10, 30, 70, 200));
        g2.drawOval(orbX - orbR, orbY - orbR, orbR * 2, orbR * 2);

        int fs = 5;
        int fx = orbX - 5, fy = orbY - 5;
        g2.setColor(new Color(245, 95, 65)); g2.fillRect(fx, fy, fs, fs);
        g2.setColor(new Color(135, 215, 65)); g2.fillRect(fx + fs + 1, fy, fs, fs);
        g2.setColor(new Color(40, 155, 245)); g2.fillRect(fx, fy + fs + 1, fs, fs);
        g2.setColor(new Color(255, 210, 45)); g2.fillRect(fx + fs + 1, fy + fs + 1, fs, fs);

        g2.setPaint(new GradientPaint(orbX, orbY - orbR, new Color(255, 255, 255, 160), orbX, orbY, new Color(255, 255, 255, 0)));
        g2.fillOval(orbX - orbR + 2, orbY - orbR + 1, (orbR - 2) * 2, orbR);

        g2.setColor(new Color(255, 255, 255, 35));
        g2.fillRect(W - 14, y + 2, 10, TASKBAR_H - 4);
        g2.setColor(new Color(0, 0, 0, 80));
        g2.drawRect(W - 14, y + 2, 10, TASKBAR_H - 4);

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
}