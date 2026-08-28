import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javax.swing.*;

public class Assignment1_67050314 extends JPanel implements Runnable {

    private static final int TARGET_FPS = 60;
    private static final long FRAME_TIME_MS = 1000 / TARGET_FPS;

    // A frame buffer is a hidden picture we draw on first before displaying
    private final BufferedImage frameBuffer =
            new BufferedImage(Canvas.W + 1, Canvas.H + 1, BufferedImage.TYPE_INT_ARGB);

    private double totalTime = 0;

    public Assignment1_67050314() {
    }

    public static void main(String[] args) {
        Assignment1_67050314 panel = new Assignment1_67050314();
        panel.setPreferredSize(new Dimension(Canvas.W, Canvas.H));

        JFrame frame = new JFrame("Windows OS Transition Sequence");
        frame.add(panel);
        frame.setResizable(false);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        new Thread(panel).start();
    }

    @Override
    public void run() {
        long lastTimeMs = System.currentTimeMillis();
        while (!Thread.currentThread().isInterrupted()) {
            long nowMs = System.currentTimeMillis();
            // Add the time that passed since the last frame, and loop back using modulo
            totalTime = (totalTime + (nowMs - lastTimeMs) / 1000.0) % Timeline.LOOP_DURATION;
            lastTimeMs = nowMs;

            repaint();

            try {
                Thread.sleep(FRAME_TIME_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // Draws one frame to the screen
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw everything on the frame buffer first
        Graphics2D g2 = frameBuffer.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        SceneRenderer.render(g2, totalTime);
        g2.dispose();

        // Display the frame buffer
        g.drawImage(frameBuffer, 0, 0, null);
    }
}

// Canvas Properties
final class Canvas {
    static final int W = 600, H = 600;
    static final int TASKBAR_H = 34;
    static final int TITLE_BAR_H = 30;

    private Canvas() {}
}

// Stores the animation sequence
final class Timeline {
    static final double TRANSITION_DURATION = 2.0;

    static final double XP_END = 5.0;
    static final double WIN7_START = XP_END + TRANSITION_DURATION;
    static final double WIN7_TO_WIN11_START = WIN7_START + 4.0;
    static final double WIN11_START = WIN7_TO_WIN11_START + TRANSITION_DURATION;
    static final double WIN11_TO_BLACK_START = WIN11_START + 1.5;
    static final double BLACK_START = WIN11_TO_BLACK_START + TRANSITION_DURATION;
    static final double BLACK_TO_XP_START = BLACK_START + 5.5;
    static final double LOOP_DURATION = BLACK_TO_XP_START + TRANSITION_DURATION;

    static final double MINECRAFT_WINDOW_APPEAR_AT = WIN7_START + 1.0;
    static final double DIALOG_BOX_APPEAR_AT = BLACK_START + 0.8;

    private Timeline() {}
}

// One small step, one giant step
final class SceneRenderer {

    private SceneRenderer() {}

    static void render(Graphics2D g2, double t) {
        if (t < Timeline.XP_END) {
            XPScene.draw(g2, t, true);

        } else if (t < Timeline.WIN7_START) {
            Point origin = MinesweeperWidget.explosionOrigin();
            Transitions.explosive(g2, t, Timeline.XP_END, origin.x, origin.y,
                    () -> XPScene.draw(g2, t, true), () -> Win7Scene.draw(g2, t));

        } else if (t < Timeline.WIN7_TO_WIN11_START) {
            Win7Scene.draw(g2, t);

        } else if (t < Timeline.WIN11_START) {
            Point origin = Win7MinecraftWindow.explosionOrigin();
            Transitions.explosive(g2, t, Timeline.WIN7_TO_WIN11_START,
                    origin.x, origin.y,
                    () -> Win7Scene.draw(g2, t), () -> Win11Scene.draw(g2, t));

        } else if (t < Timeline.WIN11_TO_BLACK_START) {
            Win11Scene.draw(g2, t);

        } else if (t < Timeline.BLACK_START) {
            Transitions.plain(g2, t, Timeline.WIN11_TO_BLACK_START, Canvas.W / 2, Canvas.H / 2,
                    () -> Win11Scene.draw(g2, t), () -> BlackScene.draw(g2, t));

        } else if (t < Timeline.BLACK_TO_XP_START) {
            BlackScene.draw(g2, t);

        } else {
            Transitions.plain(g2, t, Timeline.BLACK_TO_XP_START, Canvas.W / 2, Canvas.H / 2,
                    () -> BlackScene.draw(g2, t), () -> XPScene.draw(g2, t, false));
        }
    }
}

// Draws transitions between scenes
final class Transitions {

    private Transitions() {}

    // Use circular wipe with an explosion as a transition
    static void explosive(Graphics2D g2, double t, double phaseStart, int originX, int originY,
                           Runnable oldScene, Runnable newScene) {
        double progress = circularWipe(g2, t, phaseStart, originX, originY, oldScene, newScene);
        Explosion.draw(g2, originX, originY, progress);
    }

    // Use plain circular wipe as a transition
    static void plain(Graphics2D g2, double t, double phaseStart, int originX, int originY,
                       Runnable oldScene, Runnable newScene) {
        circularWipe(g2, t, phaseStart, originX, originY, oldScene, newScene);
    }

    // Circular wipe logic. Figures out how big the circle should be, so the new scene appears from the center outward
    private static double circularWipe(Graphics2D g2, double t, double phaseStart, int originX, int originY,
                                        Runnable oldScene, Runnable newScene) {
        double progress = (t - phaseStart) / Timeline.TRANSITION_DURATION;
        double maxDist = Math.hypot(Canvas.W, Canvas.H);
        double radius = Math.pow(progress, 2.5) * maxDist;

        // Draw the old scene
        oldScene.run();

        // Draw the new scene, showing the part inside the circle
        Shape savedClip = g2.getClip();
        g2.setClip(new Ellipse2D.Double(originX - radius, originY - radius, radius * 2, radius * 2));
        newScene.run();
        g2.setClip(savedClip);

        return progress;
    }
}

// Draws the explosion used in transitions
final class Explosion {
    private static final int RAY_COUNT = 24;
    private static final double RAY_ANGLE_STEP_DEG = 15;
    private static final double RAY_ANGLE_JITTER_DEG = 7;

    private Explosion() {}

    static void draw(Graphics2D g2, int cx, int cy, double progress) {
        double easeOut = 1.0 - Math.pow(1.0 - progress, 3);
        int alpha = clampToByte((1.0 - progress) * 255);
        if (alpha <= 0) return;

        drawShockRing(g2, cx, cy, progress, easeOut, alpha);
        drawBlastCore(g2, cx, cy, easeOut, alpha);
        drawSparkRays(g2, cx, cy, progress, easeOut, alpha);
    }

    // Draws the ring that grows outward from the explosion
    private static void drawShockRing(Graphics2D g2, int cx, int cy, double progress, double easeOut, int alpha) {
        int ringRadius = (int) (easeOut * 800);
        g2.setStroke(new BasicStroke((float) ((1.0 - progress) * 25f)));
        g2.setColor(new Color(255, 255, 255, (int) (alpha * 0.4)));
        g2.drawOval(cx - ringRadius, cy - ringRadius, ringRadius * 2, ringRadius * 2);
        g2.setStroke(new BasicStroke(1f));
    }

    // Draws the bright center of the explosion
    private static void drawBlastCore(Graphics2D g2, int cx, int cy, double easeOut, int alpha) {
        int coreRadius = (int) (easeOut * 600);
        if (coreRadius <= 0) return;

        RadialGradientPaint blastGradient = new RadialGradientPaint(
                new Point2D.Float(cx, cy), coreRadius + 1f,
                new float[]{0.0f, 0.2f, 0.6f, 1.0f},
                new Color[]{
                        new Color(255, 255, 255, alpha),
                        new Color(255, 200, 50, alpha),
                        new Color(255, 50, 0, (int) (alpha * 0.7)),
                        new Color(50, 0, 0, 0)
                });
        g2.setPaint(blastGradient);
        g2.fillOval(cx - coreRadius, cy - coreRadius, coreRadius * 2, coreRadius * 2);
    }

    // Draws lines shooting outward like sparks
    private static void drawSparkRays(Graphics2D g2, int cx, int cy, double progress, double easeOut, int alpha) {
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < RAY_COUNT; i++) {
            double angleDeg = i * RAY_ANGLE_STEP_DEG + (i % 2 == 0 ? 0 : RAY_ANGLE_JITTER_DEG);
            double rad = Math.toRadians(angleDeg);

            double speedMultiplier = 0.5 + ((i * 7) % 10) / 10.0;
            double dist = easeOut * 700 * speedMultiplier;
            double trailLength = 20 + (1.0 - progress) * 80 * speedMultiplier;

            int x2 = (int) (cx + Math.cos(rad) * dist);
            int y2 = (int) (cy + Math.sin(rad) * dist);
            int x1 = (int) (cx + Math.cos(rad) * Math.max(0, dist - trailLength));
            int y1 = (int) (cy + Math.sin(rad) * Math.max(0, dist - trailLength));

            g2.setColor(new Color(255, (int) (200 * (1.0 - progress)), 0, alpha));
            g2.drawLine(x1, y1, x2, y2);
        }
        g2.setStroke(new BasicStroke(1f));
    }

    // Keeps the transparency value between 0 and 255
    private static int clampToByte(double value) {
        return Math.max(0, Math.min(255, (int) value));
    }
}

// Reusable drawing functions
final class DrawUtils {

    private DrawUtils() {}

    static void fillPoly(Graphics2D g2, Color color, int[] xs, int[] ys) {
        g2.setColor(color);
        g2.fillPolygon(xs, ys, xs.length);
    }

    static void fillPolyRelative(Graphics2D g2, Color color, int cx, int cy, int[] dx, int[] dy, double scale) {
        int[] xs = new int[dx.length];
        int[] ys = new int[dy.length];
        for (int i = 0; i < dx.length; i++) {
            xs[i] = cx + (int) (dx[i] * scale);
            ys[i] = cy + (int) (dy[i] * scale);
        }
        fillPoly(g2, color, xs, ys);
    }

    // Midpoint Circle Algorithm + Fill
    static void fillMidpointCircle(Graphics2D g2, int cx, int cy, int radius, Color color) {
        g2.setColor(color);
        int x = 0, y = radius, p = 1 - radius;

        fillCircleScanlines(g2, cx, cy, x, y);
        while (x < y) {
            x++;
            if (p < 0) {
                p += 2 * x + 1;
            } else {
                y--;
                p += 2 * (x - y) + 1;
            }
            fillCircleScanlines(g2, cx, cy, x, y);
        }
    }

    private static void fillCircleScanlines(Graphics2D g2, int cx, int cy, int x, int y) {
        g2.drawLine(cx - x, cy + y, cx + x, cy + y);
        g2.drawLine(cx - x, cy - y, cx + x, cy - y);
        g2.drawLine(cx - y, cy + x, cx + y, cy + x);
        g2.drawLine(cx - y, cy - x, cx + y, cy - x);
    }
}

// Draws a window frame with a title bar for Windows XP
final class WindowChrome {

    private WindowChrome() {}

    static void draw(Graphics2D g2, int x, int y, int w, int h, Color bodyColor, String title) {
        // Draw a shadow behind the window
        g2.setColor(new Color(0, 0, 0, 60));
        g2.fill(new RoundRectangle2D.Double(x + 6, y + 6, w, h, 12, 12));

        // Fill in the window's background color
        g2.setColor(bodyColor);
        g2.fill(new RoundRectangle2D.Double(x, y, w, h, 12, 12));

        drawTitleBar(g2, x, y, w);

        // Draw a light-colored outline around the window
        g2.setColor(new Color(255, 255, 255, 100));
        g2.draw(new RoundRectangle2D.Double(x + 1, y + 1, w - 2, h - 2, 10, 10));

        // Draw the window's title text
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Tahoma", Font.BOLD, 13));
        g2.drawString(title, x + 12, y + 20);

        drawControlButtons(g2, x, y, w);

        // Draw small blue lines along the window's edges
        g2.setColor(new Color(0, 70, 200));
        g2.drawLine(x, y + 10, x, y + h - 10);
        g2.drawLine(x + w, y + 10, x + w, y + h - 10);
        g2.drawLine(x + 10, y, x + w - 10, y);
        g2.drawLine(x + 10, y + h, x + w - 10, y + h);
    }

    // Draws the blue title bar on top
    private static void drawTitleBar(Graphics2D g2, int x, int y, int w) {
        GradientPaint titleGrad = new GradientPaint(
                x, y, new Color(0, 88, 225), x, y + Canvas.TITLE_BAR_H, new Color(30, 110, 255));
        g2.setPaint(titleGrad);

        Path2D.Double titleBar = new Path2D.Double();
        titleBar.moveTo(x, y + Canvas.TITLE_BAR_H);
        titleBar.lineTo(x, y + 10);
        titleBar.quadTo(x, y, x + 10, y);
        titleBar.lineTo(x + w - 10, y);
        titleBar.quadTo(x + w, y, x + w, y + 10);
        titleBar.lineTo(x + w, y + Canvas.TITLE_BAR_H);
        titleBar.closePath();
        g2.fill(titleBar);
        g2.setPaint(null);
    }

    // Draws minimize, maximize, and close
    private static void drawControlButtons(Graphics2D g2, int x, int y, int w) {
        int bw = 22, bh = 22, gap = 2;
        int minimizeX = x + w - (bw * 3 + gap * 2) - 6, buttonY = y + 4;

        g2.setPaint(new GradientPaint(minimizeX, buttonY, new Color(80, 160, 255),
                minimizeX, buttonY + bh, new Color(30, 100, 220)));
        g2.fill(new RoundRectangle2D.Double(minimizeX, buttonY, bw, bh, 4, 4));
        g2.setColor(Color.WHITE);
        g2.fillRect(minimizeX + 6, buttonY + bh - 7, bw - 12, 3);

        int maximizeX = minimizeX + bw + gap;
        g2.setPaint(new GradientPaint(maximizeX, buttonY, new Color(80, 160, 255),
                maximizeX, buttonY + bh, new Color(30, 100, 220)));
        g2.fill(new RoundRectangle2D.Double(maximizeX, buttonY, bw, bh, 4, 4));
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2f));
        g2.drawRect(maximizeX + 6, buttonY + 6, bw - 12, bh - 12);
        g2.fillRect(maximizeX + 6, buttonY + 6, bw - 12, 3);
        g2.setStroke(new BasicStroke(1f));

        int closeX = maximizeX + bw + gap;
        g2.setPaint(new GradientPaint(closeX, buttonY, new Color(240, 100, 80),
                closeX, buttonY + bh, new Color(210, 40, 30)));
        g2.fill(new RoundRectangle2D.Double(closeX, buttonY, bw, bh, 4, 4));
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2f));
        g2.drawLine(closeX + 7, buttonY + 7, closeX + bw - 7, buttonY + bh - 7);
        g2.drawLine(closeX + bw - 7, buttonY + 7, closeX + 7, buttonY + bh - 7);
        g2.setStroke(new BasicStroke(1f));
    }
}

// The Windows XP scene
final class XPScene {
    private static final Rectangle NOTES_WINDOW_1 = new Rectangle(40, 45, 290, 190);
    private static final Rectangle NOTES_WINDOW_2 = new Rectangle(80, 110, 290, 190);

    private static final double NOTES_1_APPEAR_AT = 0.5;
    private static final double NOTES_2_APPEAR_AT = 1.5;
    private static final double MINESWEEPER_APPEAR_AT = 2.5;

    private XPScene() {}

    static void draw(Graphics2D g2, double t, boolean showWindows) {
        drawDesktop(g2);

        if (showWindows) {
            if (t >= NOTES_1_APPEAR_AT) {
                drawNoteWindow(g2, NOTES_WINDOW_1, "Notes.txt", "'Don't play for too long' \u2013Mother");
            }
            if (t >= NOTES_2_APPEAR_AT) {
                drawNoteWindow(g2, NOTES_WINDOW_2, "ntoe.txt", "Helloooooooooooo");
            }
            if (t >= MINESWEEPER_APPEAR_AT) {
                MinesweeperWidget.draw(g2, t);
            }
        }

        XPTaskbar.draw(g2);
    }

    // Draws a simple text file window
    private static void drawNoteWindow(Graphics2D g2, Rectangle bounds, String title, String content) {
        WindowChrome.draw(g2, bounds.x, bounds.y, bounds.width, bounds.height, new Color(240, 240, 235), title);
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Tahoma", Font.PLAIN, 12));
        g2.drawString(content, bounds.x + 16, bounds.y + Canvas.TITLE_BAR_H + 30);
    }

    // Draws the Bliss wallpaper
    private static void drawDesktop(Graphics2D g2) {
        Color skyDark = new Color(58, 121, 223);
        Color skyMid = new Color(135, 179, 241);
        Color cloudShadow = new Color(175, 203, 241);
        Color cloud = new Color(233, 239, 246);

        Color grassLight = new Color(135, 186, 46);
        Color grassMid = new Color(102, 152, 36);
        Color grassDark = new Color(71, 107, 26);
        Color grassDeep = new Color(52, 80, 20);

        g2.setColor(skyMid);
        g2.fillRect(0, 0, Canvas.W, Canvas.H);

        DrawUtils.fillPoly(g2, skyDark,
                new int[]{0, 110, 160, 200, 150, 120, 80, 30, 0},
                new int[]{0, 0, 30, 80, 120, 140, 130, 90, 70});
        DrawUtils.fillPoly(g2, skyDark,
                new int[]{220, 600, 600, 480, 450, 400, 320, 280, 250},
                new int[]{0, 0, 250, 280, 210, 220, 160, 100, 50});

        // The sun, drawn with the midpoint circle algorithm + scanline fill
        DrawUtils.fillMidpointCircle(g2, 510, 65, 38, new Color(233, 239, 246));

        DrawUtils.fillPoly(g2, cloudShadow,
                new int[]{0, 100, 150, 200, 350, 450, 600, 600, 0},
                new int[]{220, 230, 210, 240, 250, 230, 270, 350, 350});
        DrawUtils.fillPoly(g2, cloudShadow,
                new int[]{350, 420, 480, 550, 600, 600, 520, 450, 400},
                new int[]{120, 110, 130, 150, 140, 200, 220, 210, 160});
        DrawUtils.fillPoly(g2, cloudShadow,
                new int[]{70, 180, 280, 340, 260, 120},
                new int[]{160, 140, 170, 200, 190, 180});

        DrawUtils.fillPoly(g2, cloud,
                new int[]{0, 60, 120, 160, 130, 80, 30, 0},
                new int[]{240, 230, 250, 280, 290, 270, 280, 260});
        DrawUtils.fillPoly(g2, cloud,
                new int[]{420, 480, 540, 600, 600, 560, 490, 450},
                new int[]{150, 140, 160, 180, 240, 250, 220, 190});
        DrawUtils.fillPoly(g2, cloud,
                new int[]{280, 320, 360, 340, 300},
                new int[]{260, 250, 270, 290, 280});
        DrawUtils.fillPoly(g2, cloud,
                new int[]{80, 120, 150, 110, 60},
                new int[]{40, 30, 50, 70, 60});
        DrawUtils.fillPoly(g2, cloud,
                new int[]{220, 290, 330, 280, 240},
                new int[]{180, 170, 200, 220, 210});

        DrawUtils.fillPoly(g2, grassLight,
                new int[]{0, 150, 250, 350, 450, 550, 600, 600, 0},
                new int[]{315, 310, 318, 330, 345, 365, 385, 600, 600});

        DrawUtils.fillPoly(g2, grassMid,
                new int[]{0, 100, 200, 350, 500, 600, 600, 0},
                new int[]{350, 355, 365, 390, 420, 440, 600, 600});
        DrawUtils.fillPoly(g2, grassMid,
                new int[]{200, 300, 400, 500, 600, 600, 450, 300},
                new int[]{325, 335, 360, 380, 410, 440, 400, 350});

        DrawUtils.fillPoly(g2, grassDark,
                new int[]{0, 150, 300, 450, 600, 600, 0},
                new int[]{420, 430, 450, 480, 500, 600, 600});
        DrawUtils.fillPoly(g2, grassDark,
                new int[]{0, 120, 280, 450, 600, 600, 350, 150},
                new int[]{380, 395, 420, 440, 460, 510, 470, 430});
        DrawUtils.fillPoly(g2, grassDark,
                new int[]{250, 400, 550, 600, 600, 450, 300},
                new int[]{370, 390, 410, 420, 450, 430, 390});

        DrawUtils.fillPoly(g2, grassDeep,
                new int[]{0, 180, 350, 500, 600, 600, 0},
                new int[]{490, 505, 520, 540, 550, 600, 600});
        DrawUtils.fillPoly(g2, grassDeep,
                new int[]{0, 250, 450, 600, 600, 0},
                new int[]{540, 555, 570, 580, 600, 600});
    }
}

// Draws the Windows XP taskbar
final class XPTaskbar {

    private XPTaskbar() {}

    static void draw(Graphics2D g2) {
        int y = Canvas.H - Canvas.TASKBAR_H;

        // Fill in the taskbar's background color
        GradientPaint bar = new GradientPaint(0, y, new Color(30, 90, 220), 0, Canvas.H, new Color(15, 60, 160));
        g2.setPaint(bar);
        g2.fillRect(0, y, Canvas.W, Canvas.TASKBAR_H);

        g2.setColor(new Color(60, 140, 255));
        g2.fillRect(0, y, Canvas.W, 2);
        g2.setColor(new Color(15, 50, 130));
        g2.fillRect(0, y + 2, Canvas.W, 1);

        // Fill in the background for the clock area on the right
        int trayW = 70;
        int trayX = Canvas.W - trayW - 10;
        g2.setPaint(new GradientPaint(trayX, y, new Color(10, 50, 140), trayX, Canvas.H, new Color(30, 100, 210)));
        g2.fillRect(trayX, y, Canvas.W - trayX, Canvas.TASKBAR_H);
        g2.setColor(new Color(0, 30, 100));
        g2.drawLine(trayX, y, trayX, Canvas.H);

        drawStartButton(g2, y);

        // Show the current time
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a"));
        g2.setFont(new Font("Tahoma", Font.PLAIN, 12));
        FontMetrics fm = g2.getFontMetrics();
        int timeWidth = fm.stringWidth(time);
        g2.setColor(Color.WHITE);
        g2.drawString(time, trayX + (Canvas.W - trayX - timeWidth) / 2, y + Canvas.TASKBAR_H / 2 + 5);
    }

    // Draws the green start button
    private static void drawStartButton(Graphics2D g2, int y) {
        RoundRectangle2D.Double startBtn = new RoundRectangle2D.Double(0, y, 105, Canvas.TASKBAR_H, 15, 15);
        g2.setPaint(new GradientPaint(0, y, new Color(80, 180, 70), 0, Canvas.H, new Color(40, 120, 30)));
        g2.fill(startBtn);

        g2.setPaint(new GradientPaint(0, y, new Color(255, 255, 255, 100),
                0, y + Canvas.TASKBAR_H / 2, new Color(255, 255, 255, 0)));
        g2.fill(new RoundRectangle2D.Double(0, y, 105, Canvas.TASKBAR_H / 2.0, 15, 15));

        g2.setColor(new Color(20, 80, 10));
        g2.draw(new RoundRectangle2D.Double(0, y, 105, Canvas.TASKBAR_H, 15, 15));

        int fx = 12, fy = y + 10, fs = 7;
        g2.setColor(new Color(240, 80, 50));
        g2.fillRect(fx, fy, fs, fs);
        g2.setColor(new Color(110, 200, 70));
        g2.fillRect(fx + fs + 1, fy, fs, fs);
        g2.setColor(new Color(70, 140, 240));
        g2.fillRect(fx, fy + fs + 1, fs, fs);
        g2.setColor(new Color(250, 220, 60));
        g2.fillRect(fx + fs + 1, fy + fs + 1, fs, fs);

        g2.setColor(new Color(0, 0, 0, 100));
        g2.setFont(new Font("Tahoma", Font.BOLD | Font.ITALIC, 17));
        g2.drawString("start", 37, y + 23);
        g2.setColor(Color.WHITE);
        g2.drawString("start", 36, y + 22);
    }
}

// Draws the Minesweeper game window
final class MinesweeperWidget {
    static final Rectangle WINDOW_BOUNDS = new Rectangle(295, 210, 280, 340);

    private static final int GRID_SIZE = 9;
    private static final int HEADER_PANEL_H = 38;
    private static final int NOT_REVEALED = -1;

    private static final int STEP_INTACT = 0;
    private static final int STEP_PARTIAL_REVEAL = 1;
    private static final int STEP_FULL_REVEAL = 2;
    private static final int STEP_EXPLODED = 3;

    private static final double PARTIAL_REVEAL_AT = 3.1;
    private static final double FULL_REVEAL_AT = 3.7;
    private static final double EXPLODE_AT = 4.3;

    private static final int HIT_MINE_COL = 8, HIT_MINE_ROW = 4;
    private static final int LATE_REVEAL_COL = 3, LATE_REVEAL_ROW = 2;

    private static final int[][] REVEALED_CELLS = {
            {2, 0, 1}, {3, 0, 0}, {4, 0, 0}, {5, 0, 0}, {6, 0, 0}, {7, 0, 0}, {8, 0, 0},
            {2, 1, 2}, {3, 1, 1}, {4, 1, 1}, {5, 1, 1}, {6, 1, 0}, {7, 1, 1}, {8, 1, 1},
            {3, 2, 2}, {5, 2, 1}, {6, 2, 0}, {7, 2, 1},
            {0, 3, 1}, {1, 3, 2}, {3, 3, 2}, {4, 3, 1}, {5, 3, 1}, {6, 3, 0}, {7, 3, 2},
            {0, 4, 0}, {1, 4, 1}, {2, 4, 1}, {3, 4, 1}, {4, 4, 0}, {5, 4, 0}, {6, 4, 0}, {7, 4, 2},
            {0, 5, 0}, {1, 5, 0}, {2, 5, 0}, {3, 5, 0}, {4, 5, 1}, {5, 5, 1}, {6, 5, 1}, {7, 5, 2},
            {0, 6, 2}, {1, 6, 2}, {2, 6, 1}, {3, 6, 0}, {4, 6, 1}, {6, 6, 1}, {7, 6, 1}, {8, 6, 1},
            {2, 7, 1}, {3, 7, 0}, {4, 7, 1}, {5, 7, 1}, {6, 7, 1}, {7, 7, 0}, {8, 7, 0},
            {2, 8, 1}, {3, 8, 0}, {4, 8, 0}, {5, 8, 0}, {6, 8, 0}, {7, 8, 0}, {8, 8, 0}
    };

    private static final int[][] MINE_CELLS = {
            {1, 1}, {1, 2}, {4, 2}, {8, 2}, {2, 3}, {8, 4}, {8, 5}, {5, 6}, {0, 7}, {1, 7}
    };

    private MinesweeperWidget() {}

    // Finds the position of the mine that explodes, so the transition effect can start there
    static Point explosionOrigin() {
        Grid grid = computeGrid();
        return new Point(grid.cellCenterX(HIT_MINE_COL), grid.cellCenterY(HIT_MINE_ROW));
    }

    static void draw(Graphics2D g2, double t) {
        Rectangle b = WINDOW_BOUNDS;
        WindowChrome.draw(g2, b.x, b.y, b.width, b.height, new Color(192, 192, 192), "Minesweeper");

        int gameStep = stepAt(t);
        Grid grid = computeGrid();

        drawHeaderPanel(g2, b, t, gameStep, grid);
        drawGrid(g2, grid, gameStep);
    }

    // Figures out which stage of the game to show, based on the current time
    private static int stepAt(double t) {
        if (t < PARTIAL_REVEAL_AT) return STEP_INTACT;
        if (t < FULL_REVEAL_AT) return STEP_PARTIAL_REVEAL;
        if (t < EXPLODE_AT) return STEP_FULL_REVEAL;
        return STEP_EXPLODED;
    }

    private static Grid computeGrid() {
        Rectangle b = WINDOW_BOUNDS;
        int panelX = b.x + 12;
        int panelY = b.y + 42;
        int panelW = b.width - 24;

        int gridX = panelX;
        int gridY = panelY + HEADER_PANEL_H + 8;
        int gridW = panelW;
        int gridH = b.height - (gridY - b.y) - 12;

        int cellW = (gridW - 6) / GRID_SIZE;
        int cellH = (gridH - 6) / GRID_SIZE;
        return new Grid(gridX, gridY, gridW, gridH, cellW, cellH);
    }

    // Draws the top panel with the counters and the smiley face button
    private static void drawHeaderPanel(Graphics2D g2, Rectangle b, double t, int gameStep, Grid grid) {
        int panelX = b.x + 12;
        int panelY = b.y + 42;
        int panelW = b.width - 24;

        g2.setColor(new Color(128, 128, 128));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRect(panelX, panelY, panelW, HEADER_PANEL_H);
        g2.setColor(Color.WHITE);
        g2.drawRect(panelX + 2, panelY + 2, panelW - 4, HEADER_PANEL_H - 4);
        g2.setStroke(new BasicStroke(1f));
        g2.setColor(new Color(192, 192, 192));
        g2.fillRect(panelX + 2, panelY + 2, panelW - 4, HEADER_PANEL_H - 4);

        drawDigitalCounter(g2, panelX + 8, panelY + 7, "010");

        drawFace(g2, panelX + panelW / 2 - 13, panelY + 6, gameStep == STEP_EXPLODED);

        int timerVal = (gameStep == STEP_EXPLODED) ? 43 : (int) (t * 3) % 999;
        drawDigitalCounter(g2, panelX + panelW - 48, panelY + 7, String.format("%03d", timerVal));
    }

    private static void drawDigitalCounter(Graphics2D g2, int x, int y, String value) {
        g2.setColor(new Color(128, 128, 128));
        g2.drawRect(x, y, 40, 24);
        g2.setColor(Color.WHITE);
        g2.drawRect(x + 1, y + 1, 38, 22);
        g2.setColor(Color.BLACK);
        g2.fillRect(x + 2, y + 2, 36, 20);
        g2.setColor(Color.RED);
        g2.setFont(new Font("Monospaced", Font.BOLD, 18));
        g2.drawString(value, x + 3, y + 18);
    }

    // Draws the smiley face button and dead face if the game is over
    private static void drawFace(Graphics2D g2, int faceX, int faceY, boolean exploded) {
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

        if (exploded) {
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
    }

    // Draw each cell
    private static void drawGrid(Graphics2D g2, Grid grid, int gameStep) {
        drawGridFrame(g2, grid);

        boolean exploded = (gameStep == STEP_EXPLODED);
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                int cellX = grid.x + 4 + col * grid.cellW;
                int cellY = grid.y + 4 + row * grid.cellH;

                // Draw a mine here if the game is over
                if (exploded && isMine(col, row)) {
                    boolean isHitCell = (col == HIT_MINE_COL && row == HIT_MINE_ROW);
                    drawMineCell(g2, cellX, cellY, grid.cellW, grid.cellH, isHitCell);
                    continue;
                }

                int revealed = revealedValueAt(col, row, gameStep);
                if (revealed != NOT_REVEALED) {
                    drawOpenCell(g2, cellX, cellY, grid.cellW, grid.cellH, revealed);
                } else {
                    drawClosedCell(g2, cellX, cellY, grid.cellW, grid.cellH);
                }
            }
        }
    }

    private static void drawGridFrame(Graphics2D g2, Grid grid) {
        g2.setColor(new Color(128, 128, 128));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRect(grid.x, grid.y, grid.width, grid.height);
        g2.setColor(Color.WHITE);
        g2.drawRect(grid.x + 2, grid.y + 2, grid.width - 4, grid.height - 4);
        g2.setStroke(new BasicStroke(1f));
        g2.setColor(new Color(192, 192, 192));
        g2.fillRect(grid.x + 2, grid.y + 2, grid.width - 4, grid.height - 4);
    }

    private static boolean isMine(int col, int row) {
        for (int[] mine : MINE_CELLS) {
            if (mine[0] == col && mine[1] == row) return true;
        }
        return false;
    }

    private static int revealedValueAt(int col, int row, int gameStep) {
        if (gameStep < STEP_PARTIAL_REVEAL) return NOT_REVEALED;

        for (int[] cell : REVEALED_CELLS) {
            if (cell[0] != col || cell[1] != row) continue;

            boolean isLateReveal = (col == LATE_REVEAL_COL && row == LATE_REVEAL_ROW);
            if (isLateReveal && gameStep < STEP_FULL_REVEAL) return NOT_REVEALED;
            return cell[2];
        }
        return NOT_REVEALED;
    }

    private static void drawMineCell(Graphics2D g2, int x, int y, int w, int h, boolean isHitCell) {
        g2.setColor(isHitCell ? new Color(230, 80, 80) : new Color(192, 192, 192));
        g2.fillRect(x, y, w, h);
        g2.setColor(Color.BLACK);
        g2.drawRect(x, y, w, h);

        int bombCenterX = x + w / 2;
        int bombCenterY = y + h / 2;
        int bombRadius = Math.min(w, h) / 2 - 3;

        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(1.5f));
        for (int angle = 0; angle < 360; angle += 45) {
            double rad = Math.toRadians(angle);
            int sx1 = (int) (bombCenterX + Math.cos(rad) * 2);
            int sy1 = (int) (bombCenterY + Math.sin(rad) * 2);
            int sx2 = (int) (bombCenterX + Math.cos(rad) * (bombRadius + 1));
            int sy2 = (int) (bombCenterY + Math.sin(rad) * (bombRadius + 1));
            g2.drawLine(sx1, sy1, sx2, sy2);
        }
        g2.setStroke(new BasicStroke(1f));

        g2.setColor(Color.BLACK);
        g2.fillOval(bombCenterX - bombRadius + 1, bombCenterY - bombRadius + 1, (bombRadius - 1) * 2, (bombRadius - 1) * 2);
        g2.setColor(Color.WHITE);
        g2.fillOval(bombCenterX - 2, bombCenterY - 2, 2, 2);
    }

    private static void drawOpenCell(Graphics2D g2, int x, int y, int w, int h, int value) {
        g2.setColor(new Color(180, 180, 180));
        g2.fillRect(x, y, w, h);
        g2.setColor(new Color(128, 128, 128));
        g2.drawRect(x, y, w, h);

        if (value <= 0) return;

        g2.setColor(switch (value) {
            case 1 -> Color.BLUE;
            case 2 -> new Color(0, 128, 0);
            default -> Color.RED;
        });
        g2.setFont(new Font("Tahoma", Font.BOLD, 11));
        g2.drawString(String.valueOf(value), x + 4, y + h - 4);
    }

    private static void drawClosedCell(Graphics2D g2, int x, int y, int w, int h) {
        g2.setColor(new Color(192, 192, 192));
        g2.fillRect(x, y, w, h);

        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(x, y, x + w - 1, y);
        g2.drawLine(x, y, x, y + h - 1);

        g2.setColor(new Color(128, 128, 128));
        g2.drawLine(x + w - 1, y, x + w - 1, y + h - 1);
        g2.drawLine(x, y + h - 1, x + w - 1, y + h - 1);
        g2.setStroke(new BasicStroke(1f));
    }

    // Stores the properties of the grid
    private static final class Grid {
        final int x, y, width, height, cellW, cellH;

        Grid(int x, int y, int width, int height, int cellW, int cellH) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.cellW = cellW;
            this.cellH = cellH;
        }

        int cellCenterX(int col) { return x + 4 + col * cellW + cellW / 2; }
        int cellCenterY(int row) { return y + 4 + row * cellH + cellH / 2; }
    }
}

// The Windows 7 desktop scene
final class Win7Scene {

    private Win7Scene() {}

    static void draw(Graphics2D g2, double t) {
        drawDesktop(g2);

        if (t >= Timeline.MINECRAFT_WINDOW_APPEAR_AT) {
            Win7MinecraftWindow.draw(g2, t);
        }

        Win7Taskbar.draw(g2);
    }

    // Draws the glowing blue background
    private static void drawDesktop(Graphics2D g2) {
        GradientPaint sky = new GradientPaint(0, 0, new Color(15, 95, 185), 0, Canvas.H, new Color(5, 45, 105));
        g2.setPaint(sky);
        g2.fillRect(0, 0, Canvas.W, Canvas.H);

        RadialGradientPaint glow = new RadialGradientPaint(
                new Point2D.Float(Canvas.W / 2f, Canvas.H / 2f - 30), 350f,
                new float[]{0.0f, 1.0f},
                new Color[]{new Color(55, 175, 245, 160), new Color(0, 0, 0, 0)});
        g2.setPaint(glow);
        g2.fillRect(0, 0, Canvas.W, Canvas.H);

        drawRibbons(g2);
        drawCenterLogo(g2, Canvas.W / 2, Canvas.H / 2 - 30);
    }

    // Draws soft glowing curved lines across the screen
    private static void drawRibbons(Graphics2D g2) {
        int w = Canvas.W, h = Canvas.H;

        g2.setStroke(new BasicStroke(40f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D.Double ribbon1 = new Path2D.Double();
        ribbon1.moveTo(-50, h - 40);
        ribbon1.curveTo(w * 0.3, h * 0.35, w * 0.7, h * 0.85, w + 50, h * 0.55);
        g2.setColor(new Color(255, 255, 255, 22));
        g2.draw(ribbon1);

        g2.setStroke(new BasicStroke(16f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D.Double ribbon2 = new Path2D.Double();
        ribbon2.moveTo(-20, h - 20);
        ribbon2.curveTo(w * 0.4, h * 0.3, w * 0.6, h * 0.8, w + 20, h * 0.5);
        g2.setColor(new Color(255, 255, 255, 45));
        g2.draw(ribbon2);

        g2.setStroke(new BasicStroke(3f));
        Path2D.Double ribbon3 = new Path2D.Double();
        ribbon3.moveTo(0, h - 100);
        ribbon3.curveTo(w * 0.35, h * 0.35, w * 0.65, h * 0.75, w, h * 0.55);
        g2.setColor(new Color(255, 255, 255, 130));
        g2.draw(ribbon3);

        g2.setStroke(new BasicStroke(1f));
    }

    // Draws the Windows logo
    private static void drawCenterLogo(Graphics2D g2, int cx, int cy) {
        Path2D.Double redPane = new Path2D.Double();
        redPane.moveTo(cx - 100, cy - 95);
        redPane.curveTo(cx - 65, cy - 110, cx - 35, cy - 108, cx - 7, cy - 93);
        redPane.lineTo(cx - 7, cy - 7);
        redPane.curveTo(cx - 35, cy - 22, cx - 65, cy - 24, cx - 100, cy - 9);
        redPane.closePath();
        g2.setPaint(new GradientPaint(cx - 100, cy - 110, new Color(245, 95, 40, 225),
                cx - 7, cy - 7, new Color(210, 45, 25, 225)));
        g2.fill(redPane);

        Path2D.Double greenPane = new Path2D.Double();
        greenPane.moveTo(cx + 7, cy - 93);
        greenPane.curveTo(cx + 35, cy - 78, cx + 65, cy - 80, cx + 100, cy - 98);
        greenPane.lineTo(cx + 100, cy - 14);
        greenPane.curveTo(cx + 65, cy + 4, cx + 35, cy + 6, cx + 7, cy - 7);
        greenPane.closePath();
        g2.setPaint(new GradientPaint(cx + 7, cy - 93, new Color(145, 215, 50, 225),
                cx + 100, cy - 14, new Color(75, 175, 30, 225)));
        g2.fill(greenPane);

        Path2D.Double bluePane = new Path2D.Double();
        bluePane.moveTo(cx - 100, cy + 9);
        bluePane.curveTo(cx - 65, cy - 6, cx - 35, cy - 4, cx - 7, cy + 7);
        bluePane.lineTo(cx - 7, cy + 93);
        bluePane.curveTo(cx - 35, cy + 78, cx - 65, cy + 76, cx - 100, cy + 91);
        bluePane.closePath();
        g2.setPaint(new GradientPaint(cx - 100, cy - 6, new Color(35, 170, 245, 225),
                cx - 7, cy + 93, new Color(15, 100, 210, 225)));
        g2.fill(bluePane);

        Path2D.Double yellowPane = new Path2D.Double();
        yellowPane.moveTo(cx + 7, cy + 7);
        yellowPane.curveTo(cx + 35, cy + 20, cx + 65, cy + 18, cx + 100, cy + 0);
        yellowPane.lineTo(cx + 100, cy + 84);
        yellowPane.curveTo(cx + 65, cy + 102, cx + 35, cy + 104, cx + 7, cy + 93);
        yellowPane.closePath();
        g2.setPaint(new GradientPaint(cx + 7, cy + 7, new Color(255, 205, 30, 225),
                cx + 100, cy + 84, new Color(225, 150, 10, 225)));
        g2.fill(yellowPane);
    }
}

// The see-through taskbar for the Windows 7 scene
final class Win7Taskbar {

    private Win7Taskbar() {}

    static void draw(Graphics2D g2) {
        int y = Canvas.H - Canvas.TASKBAR_H;

        g2.setColor(new Color(15, 30, 50, 210));
        g2.fillRect(0, y, Canvas.W, Canvas.TASKBAR_H);

        g2.setColor(new Color(255, 255, 255, 90));
        g2.fillRect(0, y, Canvas.W, 1);
        g2.setColor(new Color(255, 255, 255, 25));
        g2.fillRect(0, y + 1, Canvas.W, 1);

        drawStartOrb(g2, y);

        // Draw the small "Show Desktop" button in the corner
        g2.setColor(new Color(255, 255, 255, 35));
        g2.fillRect(Canvas.W - 14, y + 2, 10, Canvas.TASKBAR_H - 4);
        g2.setColor(new Color(0, 0, 0, 80));
        g2.drawRect(Canvas.W - 14, y + 2, 10, Canvas.TASKBAR_H - 4);

        drawClock(g2, y);
    }

    // Draws the round start button
    private static void drawStartOrb(Graphics2D g2, int y) {
        int orbR = 17;
        int orbX = 22, orbY = y + Canvas.TASKBAR_H / 2;

        RadialGradientPaint orbGrad = new RadialGradientPaint(
                new Point2D.Float(orbX, orbY - 4), orbR + 2,
                new float[]{0.0f, 0.7f, 1.0f},
                new Color[]{new Color(85, 175, 250), new Color(15, 85, 180), new Color(5, 35, 95)});
        g2.setPaint(orbGrad);
        g2.fillOval(orbX - orbR, orbY - orbR, orbR * 2, orbR * 2);

        g2.setColor(new Color(10, 30, 70, 200));
        g2.drawOval(orbX - orbR, orbY - orbR, orbR * 2, orbR * 2);

        int fs = 5;
        int fx = orbX - 5, fy = orbY - 5;
        g2.setColor(new Color(245, 95, 65));
        g2.fillRect(fx, fy, fs, fs);
        g2.setColor(new Color(135, 215, 65));
        g2.fillRect(fx + fs + 1, fy, fs, fs);
        g2.setColor(new Color(40, 155, 245));
        g2.fillRect(fx, fy + fs + 1, fs, fs);
        g2.setColor(new Color(255, 210, 45));
        g2.fillRect(fx + fs + 1, fy + fs + 1, fs, fs);

        g2.setPaint(new GradientPaint(orbX, orbY - orbR, new Color(255, 255, 255, 160),
                orbX, orbY, new Color(255, 255, 255, 0)));
        g2.fillOval(orbX - orbR + 2, orbY - orbR + 1, (orbR - 2) * 2, orbR);
    }

    private static void drawClock(Graphics2D g2, int y) {
        int trayW = 85;
        int trayX = Canvas.W - trayW - 14;
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a"));
        g2.setFont(new Font("Tahoma", Font.PLAIN, 12));
        FontMetrics fm = g2.getFontMetrics();
        int timeWidth = fm.stringWidth(time);
        g2.setColor(Color.WHITE);
        g2.drawString(time, trayX + (trayW - timeWidth) / 2, y + Canvas.TASKBAR_H / 2 + 4);
    }
}

// The window that shows the Minecraft scene inside it
final class Win7MinecraftWindow {
    static final Rectangle WINDOW_BOUNDS = new Rectangle(70, 40, 460, 380);
    private static final int TITLE_H = 30;
    private static final int BORDER = 7;

    private Win7MinecraftWindow() {}

    // Gives the position where the creeper explosion should happen
    static Point explosionOrigin() {
        Rectangle b = WINDOW_BOUNDS;
        int clientX = b.x + BORDER;
        int clientY = b.y + TITLE_H;
        int clientW = b.width - BORDER * 2;
        int clientH = b.height - TITLE_H - BORDER;
        return MinecraftScene.creeperOrigin(clientX, clientY, clientW, clientH);
    }

    static void draw(Graphics2D g2, double t) {
        Rectangle b = WINDOW_BOUNDS;

        g2.setColor(new Color(0, 0, 0, 80));
        g2.fill(new RoundRectangle2D.Double(b.x + 6, b.y + 6, b.width, b.height, 14, 14));

        g2.setColor(new Color(130, 185, 225, 170));
        g2.fill(new RoundRectangle2D.Double(b.x, b.y, b.width, b.height, 12, 12));

        GradientPaint glassGlow = new GradientPaint(
                b.x, b.y, new Color(255, 255, 255, 140), b.x, b.y + TITLE_H, new Color(255, 255, 255, 30));
        g2.setPaint(glassGlow);
        g2.fill(new RoundRectangle2D.Double(b.x, b.y, b.width, TITLE_H, 12, 12));

        g2.setColor(new Color(15, 25, 40));
        g2.setFont(new Font("Tahoma", Font.PLAIN, 12));
        g2.drawString("Minecraft 1.5.2 - Singleplayer", b.x + 12, b.y + 20);

        drawTitleBarButtons(g2, b);

        int clientX = b.x + BORDER;
        int clientY = b.y + TITLE_H;
        int clientW = b.width - BORDER * 2;
        int clientH = b.height - TITLE_H - BORDER;

        // Get clip to make sure nothing is drawn outside the window's edges
        Shape savedClip = g2.getClip();
        g2.clip(new Rectangle2D.Double(clientX, clientY, clientW, clientH));
        MinecraftScene.draw(g2, clientX, clientY, clientW, clientH, t);
        g2.setClip(savedClip);

        g2.setColor(new Color(0, 0, 0, 150));
        g2.drawRect(clientX - 1, clientY - 1, clientW + 1, clientH + 1);
    }

    private static void drawTitleBarButtons(Graphics2D g2, Rectangle b) {
        int bw = 26, bh = 18;
        int closeX = b.x + b.width - bw - 6, buttonY = b.y + 2;

        g2.setPaint(new GradientPaint(closeX, buttonY, new Color(230, 90, 80, 230),
                closeX, buttonY + bh, new Color(180, 40, 30, 240)));
        g2.fill(new RoundRectangle2D.Double(closeX, buttonY, bw, bh, 4, 4));
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(closeX + 9, buttonY + 5, closeX + bw - 9, buttonY + bh - 5);
        g2.drawLine(closeX + bw - 9, buttonY + 5, closeX + 9, buttonY + bh - 5);

        int maximizeX = closeX - bw - 2;
        g2.setPaint(new GradientPaint(maximizeX, buttonY, new Color(225, 240, 250, 160),
                maximizeX, buttonY + bh, new Color(175, 200, 220, 190)));
        g2.fill(new RoundRectangle2D.Double(maximizeX, buttonY, bw, bh, 4, 4));
        g2.setColor(new Color(40, 50, 65));
        g2.drawRect(maximizeX + 8, buttonY + 4, 9, 8);

        int minimizeX = maximizeX - bw - 2;
        g2.setPaint(new GradientPaint(minimizeX, buttonY, new Color(225, 240, 250, 160),
                minimizeX, buttonY + bh, new Color(175, 200, 220, 190)));
        g2.fill(new RoundRectangle2D.Double(minimizeX, buttonY, bw, bh, 4, 4));
        g2.setColor(new Color(40, 50, 65));
        g2.drawLine(minimizeX + 8, buttonY + 11, minimizeX + 16, buttonY + 11);
        g2.setStroke(new BasicStroke(1f));
    }
}

// Draws the Minecraft gameplay scene
final class MinecraftScene {

    private MinecraftScene() {}

    // Finds the position of the creeper, so the transition effect can start there
    static Point creeperOrigin(int cx, int cy, int cw, int ch) {
        int crx = cx + cw / 2 - 22;
        int cry = cy + 95;
        return new Point(crx + 20, cry + 20);
    }

    static void draw(Graphics2D g2, int cx, int cy, int cw, int ch, double t) {
        drawSky(g2, cx, cy, cw, ch);
        drawSun(g2, cx, cy, cw);
        drawDistantMountain(g2, cx, cy);
        drawHouse(g2, cx, cy, cw);
        drawSand(g2, cx, cy, cw, ch);
        drawWater(g2, cx, cy, cw, ch);
        drawCreeper(g2, cx, cy, cw, ch, t);
        drawCrosshair(g2, cx, cy, cw, ch);
        drawSword(g2, cx, cy, cw, ch);
        drawHud(g2, cx, cy, cw, ch);
    }

    private static void drawSky(Graphics2D g2, int cx, int cy, int cw, int ch) {
        g2.setColor(new Color(10, 15, 30));
        g2.fillRect(cx, cy, cw, ch);

        g2.setColor(Color.WHITE);
        g2.fillRect(cx + 30, cy + 20, 2, 2);
        g2.fillRect(cx + 120, cy + 40, 2, 2);
        g2.fillRect(cx + 250, cy + 15, 2, 2);
        g2.fillRect(cx + 380, cy + 50, 2, 2);
        g2.fillRect(cx + 90, cy + 70, 2, 2);
    }

    private static void drawSun(Graphics2D g2, int cx, int cy, int cw) {
        g2.setColor(new Color(240, 240, 220));
        g2.fillRect(cx + cw - 60, cy + 20, 24, 24);
    }

    private static void drawDistantMountain(Graphics2D g2, int cx, int cy) {
        g2.setColor(new Color(35, 25, 20));
        g2.fillPolygon(
                new int[]{cx, cx + 130, cx + 180, cx},
                new int[]{cy + 130, cy + 40, cy + 150, cy + 150}, 4);
    }

    private static void drawHouse(Graphics2D g2, int cx, int cy, int cw) {
        int hx = cx + cw - 120, hy = cy + 70;
        g2.setColor(new Color(110, 75, 35));
        g2.fillRect(hx, hy, 90, 70);
        g2.setColor(new Color(70, 50, 25));
        g2.fillRect(hx, hy, 10, 70);
        g2.fillRect(hx + 80, hy, 10, 70);
        g2.fillRect(hx, hy, 90, 8);
        g2.setColor(new Color(160, 210, 230, 180));
        g2.fillRect(hx + 35, hy + 25, 20, 20);
        g2.setColor(new Color(50, 35, 20));
        g2.drawRect(hx + 35, hy + 25, 20, 20);

        g2.setColor(new Color(90, 90, 90));
        g2.fillRect(hx - 10, hy - 12, 110, 14);
    }

    private static void drawSand(Graphics2D g2, int cx, int cy, int cw, int ch) {
        g2.setColor(new Color(210, 195, 140));
        g2.fillRect(cx, cy + 140, cw, ch - 140);

        g2.setColor(new Color(190, 175, 120));
        for (int i = 0; i < cw; i += 24) {
            g2.drawLine(cx + i, cy + 140, cx + i - 40, cy + ch);
        }
        for (int j = 140; j < ch; j += 20) {
            g2.drawLine(cx, cy + j, cx + cw, cy + j);
        }
    }

    private static void drawWater(Graphics2D g2, int cx, int cy, int cw, int ch) {
        int[] xs = {cx, cx + 150, cx + 110, cx};
        int[] ys = {cy + 170, cy + 190, cy + ch, cy + ch};

        g2.setColor(new Color(140, 140, 140));
        g2.fillPolygon(xs, ys, 4);
        g2.setColor(new Color(110, 110, 110));
        g2.drawPolygon(xs, ys, 4);
    }

    // Draws the creeper, making it flash
    private static void drawCreeper(Graphics2D g2, int cx, int cy, int cw, int ch, double t) {
        boolean flashing = (t >= Timeline.WIN7_START + 1.0) && ((int) (t * 8) % 2 == 0);
        int crx = cx + cw / 2 - 22;
        int cry = cy + 95;

        g2.setColor(new Color(0, 0, 0, 70));
        g2.fillOval(crx - 6, cry + 80, 52, 16);

        g2.setColor(flashing ? new Color(220, 255, 220) : new Color(75, 170, 60));
        g2.fillRect(crx, cry, 40, 40);

        g2.setColor(flashing ? new Color(150, 180, 150) : Color.BLACK);
        g2.fillRect(crx + 6, cry + 10, 9, 9);
        g2.fillRect(crx + 25, cry + 10, 9, 9);
        g2.fillRect(crx + 15, cry + 19, 10, 14);
        g2.fillRect(crx + 11, cry + 24, 18, 12);
        g2.fillRect(crx + 11, cry + 32, 5, 5);
        g2.fillRect(crx + 24, cry + 32, 5, 5);

        g2.setColor(flashing ? new Color(200, 245, 200) : new Color(65, 155, 50));
        g2.fillRect(crx + 6, cry + 40, 28, 36);

        double legSwing = Math.sin(t * 12) * 4;
        g2.setColor(flashing ? new Color(180, 230, 180) : new Color(50, 130, 40));
        g2.fillRect(crx + 3, cry + 72 + (int) legSwing, 14, 16);
        g2.fillRect(crx + 23, cry + 72 - (int) legSwing, 14, 16);

        if (flashing) {
            g2.setColor(new Color(255, 255, 255, 160));
            g2.setStroke(new BasicStroke(3f));
            g2.drawRect(crx - 2, cry - 2, 44, 44);
            g2.drawRect(crx + 4, cry + 38, 32, 52);
            g2.setStroke(new BasicStroke(1f));
        }
    }

    private static void drawCrosshair(Graphics2D g2, int cx, int cy, int cw, int ch) {
        int midX = cx + cw / 2;
        int midY = cy + ch / 2;
        g2.setColor(new Color(255, 255, 255, 200));
        g2.fillRect(midX - 6, midY - 1, 13, 3);
        g2.fillRect(midX - 1, midY - 6, 3, 13);
    }

    private static void drawSword(Graphics2D g2, int cx, int cy, int cw, int ch) {
        int swX = cx + cw - 110;
        int swY = cy + ch - 120;

        Path2D.Double blade = new Path2D.Double();
        blade.moveTo(swX + 70, swY + 110);
        blade.lineTo(swX + 10, swY + 20);
        blade.lineTo(swX + 25, swY + 10);
        blade.lineTo(swX + 85, swY + 95);
        blade.closePath();
        g2.setColor(new Color(200, 215, 220));
        g2.fill(blade);
        g2.setColor(Color.BLACK);
        g2.draw(blade);

        g2.setColor(new Color(90, 60, 30));
        g2.fillRect(swX + 55, swY + 90, 35, 16);
    }

    private static void drawHud(Graphics2D g2, int cx, int cy, int cw, int ch) {
        int hudX = cx + cw / 2 - 90;
        int hudY = cy + ch - 24;

        g2.setColor(new Color(220, 20, 20));
        for (int i = 0; i < 10; i++) {
            g2.fillRect(hudX + (i * 8), hudY - 16, 6, 6);
        }
        g2.setColor(new Color(180, 110, 40));
        for (int i = 0; i < 10; i++) {
            g2.fillRect(hudX + 100 + (i * 8), hudY - 16, 6, 6);
        }

        g2.setColor(new Color(90, 215, 50));
        g2.fillRect(hudX, hudY - 6, 180, 3);
        g2.setColor(new Color(40, 40, 40));
        g2.drawRect(hudX - 1, hudY - 7, 182, 5);

        g2.setColor(new Color(140, 140, 140, 210));
        g2.fillRect(hudX, hudY, 180, 20);
        g2.setColor(Color.BLACK);
        g2.drawRect(hudX, hudY, 180, 20);
        for (int i = 0; i < 9; i++) {
            g2.drawRect(hudX + (i * 20), hudY, 20, 20);
        }

        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2f));
        g2.drawRect(hudX + 19, hudY - 1, 22, 22);
        g2.setStroke(new BasicStroke(1f));

        g2.setColor(new Color(160, 160, 160));
        g2.fillRect(hudX + 6, hudY + 4, 8, 12);
        g2.setColor(new Color(180, 220, 240));
        g2.fillRect(hudX + 26, hudY + 4, 8, 12);
        g2.setColor(new Color(160, 160, 160));
        g2.fillRect(hudX + 46, hudY + 4, 8, 12);
        g2.setColor(new Color(230, 210, 140));
        g2.fillRect(hudX + 126, hudY + 6, 10, 8);
        g2.setColor(new Color(255, 200, 50));
        g2.fillRect(hudX + 148, hudY + 4, 4, 12);
    }
}

// Draws Tung Tung Tung Tung Tung Tung Tung Tung Tung Sahur
final class SahurCharacter {
    private static final Color WOOD_BASE = new Color(165, 95, 45);
    private static final Color WOOD_LIGHT = new Color(210, 140, 75);
    private static final Color WOOD_SHADOW = new Color(105, 50, 20);
    private static final Color WOOD_DARK = new Color(60, 25, 10);
    private static final Color EYE_WHITE = new Color(235, 225, 205);
    private static final Color EYE_PUPIL = new Color(40, 20, 10);

    private SahurCharacter() {}

    static void draw(Graphics2D g2, int cx, int cy, double scale) {
        AffineTransform saved = g2.getTransform();
        g2.translate(cx, cy);

        double adjustedScale = scale * 1.4; 
        g2.scale(adjustedScale, adjustedScale);

        drawStick(g2);
        drawLegs(g2);
        drawViewerLeftArm(g2);
        drawBody(g2);
        drawFace(g2);
        drawViewerRightArm(g2);

        g2.setTransform(saved);
    }

    private static void drawStick(Graphics2D g2) {
        Path2D stick = new Path2D.Double();
        stick.moveTo(-25, 30);
        stick.lineTo(-15, 35);
        stick.lineTo(-60, 120);
        stick.lineTo(-75, 115);
        stick.closePath();

        g2.setColor(WOOD_SHADOW);
        g2.fill(stick);

        Path2D stickHighlight = new Path2D.Double();
        stickHighlight.moveTo(-22, 32);
        stickHighlight.lineTo(-18, 34);
        stickHighlight.lineTo(-63, 117);
        stickHighlight.lineTo(-68, 115);
        stickHighlight.closePath();

        g2.setColor(WOOD_BASE);
        g2.fill(stickHighlight);
    }

    private static void drawLegs(Graphics2D g2) {
        g2.setColor(WOOD_SHADOW);
        g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(-12, 60, -10, 130);

        g2.fillOval(-35, 125, 35, 16); 
        g2.fillOval(-42, 127, 14, 12); 
        g2.fillOval(-38, 133, 12, 10); 
        g2.fillOval(-32, 135, 10, 8);
        g2.fillOval(-25, 136, 10, 7);

        g2.setColor(WOOD_BASE);
        g2.drawLine(15, 60, 18, 135);

        g2.fillOval(5, 130, 35, 18);
        g2.fillOval(32, 132, 14, 14); 
        g2.fillOval(28, 139, 12, 11);
        g2.fillOval(20, 142, 10, 9);
        g2.fillOval(12, 143, 10, 8);
        g2.setStroke(new BasicStroke(1f));
    }

    private static void drawViewerLeftArm(Graphics2D g2) {
        g2.setStroke(new BasicStroke(4.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D arm = new Path2D.Double();
        arm.moveTo(-25, -10);
        arm.curveTo(-32, 15, -30, 30, -22, 45);

        g2.setColor(WOOD_SHADOW);
        g2.draw(arm);
        g2.setStroke(new BasicStroke(1f));

        g2.fillOval(-26, 40, 12, 12); 
    }

    private static void drawViewerRightArm(Graphics2D g2) {
        g2.setStroke(new BasicStroke(4.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D arm = new Path2D.Double();
        arm.moveTo(28, -5);
        arm.curveTo(35, 10, 33, 30, 25, 45); 

        g2.setColor(WOOD_BASE);
        g2.draw(arm);

        g2.setColor(WOOD_SHADOW);
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(arm);
        g2.setStroke(new BasicStroke(1f));

        g2.setColor(WOOD_BASE);
        g2.fillOval(20, 42, 11, 13);
    }

    private static void drawBody(Graphics2D g2) {
        Path2D body = new Path2D.Double();
        body.moveTo(-28, -105);
        body.curveTo(-32, -90, -32, 40, -25, 65);
        body.curveTo(-10, 75, 15, 75, 28, 65);
        body.curveTo(35, 40, 33, -90, 28, -105);
        body.curveTo(15, -120, -15, -120, -28, -105);
        body.closePath();

        g2.setColor(WOOD_BASE);
        g2.fill(body);

        Path2D shadow = new Path2D.Double();
        shadow.moveTo(-28, -105);
        shadow.curveTo(-32, -90, -32, 40, -25, 65);
        shadow.curveTo(-10, 75, 0, 75, -5, 65);
        shadow.curveTo(-5, 40, 0, -90, -5, -105);
        shadow.closePath();
        g2.setColor(WOOD_SHADOW);
        g2.fill(shadow);

        Path2D highlight = new Path2D.Double();
        highlight.moveTo(28, -105);
        highlight.curveTo(33, -90, 35, 40, 28, 65);
        highlight.curveTo(15, 75, 10, 75, 15, 65);
        highlight.curveTo(20, 40, 18, -90, 15, -105);
        highlight.closePath();
        g2.setColor(WOOD_LIGHT);
        g2.fill(highlight);
    }

    private static void drawFace(Graphics2D g2) {
        g2.setColor(WOOD_DARK);
        g2.fillOval(-28, -85, 24, 28);
        g2.setColor(WOOD_SHADOW);
        g2.fillOval(-30, -83, 22, 26);

        g2.setColor(EYE_WHITE);
        g2.fillOval(-26, -82, 18, 22);

        g2.setColor(EYE_PUPIL);
        g2.fillOval(-22, -77, 10, 12);
        g2.setColor(Color.WHITE);
        g2.fillOval(-19, -75, 4, 4);

        g2.setColor(WOOD_DARK);
        g2.fillOval(0, -90, 32, 34);
        g2.setColor(WOOD_BASE);
        g2.fillOval(-2, -88, 30, 32);

        g2.setColor(EYE_WHITE);
        g2.fillOval(2, -87, 25, 28);

        g2.setColor(EYE_PUPIL);
        g2.fillOval(8, -81, 14, 16);
        g2.setColor(Color.WHITE);
        g2.fillOval(12, -78, 5, 5);

        g2.setColor(WOOD_LIGHT);
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawArc(2, -95, 25, 15, 45, 100);
        g2.drawArc(-26, -88, 18, 12, 45, 100);
        g2.setStroke(new BasicStroke(1f));

        Path2D nose = new Path2D.Double();
        nose.moveTo(-5, -55);
        nose.curveTo(-18, -55, -22, -45, -15, -40);
        nose.lineTo(5, -45);
        nose.closePath();

        g2.setColor(WOOD_BASE);
        g2.fill(nose);
        g2.setColor(WOOD_LIGHT);
        g2.fillPolygon(new int[]{-5, -14, 0}, new int[]{-53, -42, -45}, 3);

        g2.setColor(WOOD_DARK);
        g2.setStroke(new BasicStroke(2f));
        g2.drawArc(-18, -48, 12, 10, 180, 120);
        g2.setStroke(new BasicStroke(1f));

        g2.setColor(WOOD_DARK);
        Path2D mouth = new Path2D.Double();
        mouth.moveTo(-15, -30);
        mouth.curveTo(-5, -20, 15, -20, 22, -35);
        mouth.curveTo(15, -25, -5, -25, -15, -30);
        mouth.closePath();
        g2.fill(mouth);

        g2.setColor(WOOD_SHADOW);
        g2.drawArc(15, -45, 12, 20, 270, 70);
    }
}

// Draws the speech bubble for Sahur
final class DialogBox {

    private DialogBox() {}

    static void draw(Graphics2D g2, int x, int y, int w, int h, String text) {
        drawShadow(g2, x, y, w, h);
        drawBubble(g2, x, y, w, h);
        drawText(g2, x, y, w, h, text);
    }

    private static void drawShadow(Graphics2D g2, int x, int y, int w, int h) {
        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRoundRect(x + 5, y + 5, w, h, 20, 20);

        Polygon tailShadow = new Polygon();
        tailShadow.addPoint(x + 5, y + h - 25);
        tailShadow.addPoint(x - 25, y + h - 5);
        tailShadow.addPoint(x + 25, y + h - 5);
        g2.fillPolygon(tailShadow);
    }

    private static void drawBubble(Graphics2D g2, int x, int y, int w, int h) {
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(x, y, w, h, 20, 20);

        Polygon tail = new Polygon();
        tail.addPoint(x, y + h - 30);
        tail.addPoint(x - 30, y + h - 10);
        tail.addPoint(x + 20, y + h - 2);
        g2.fillPolygon(tail);
    }

    private static void drawText(Graphics2D g2, int x, int y, int w, int h, String text) {
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Tahoma", Font.BOLD, 17));
        FontMetrics fm = g2.getFontMetrics();

        String[] lines = text.split("\n");
        int lineHeight = fm.getHeight();
        int totalTextHeight = lines.length * lineHeight;
        int startY = y + (h - totalTextHeight) / 2 + fm.getAscent();

        for (int i = 0; i < lines.length; i++) {
            int textWidth = fm.stringWidth(lines[i]);
            int startX = x + (w - textWidth) / 2;
            g2.drawString(lines[i], startX, startY + (i * lineHeight));
        }
    }
}

// The void scene
final class BlackScene {
    private static final int SAHUR_X = 140; 
    private static final int SAHUR_Y = 400; 
    private static final double SAHUR_SCALE = 1.8; 

    private static final String MONOLOGUE =
            "You brought me into the world\nagainst my will.\nAnd for doing my job,\nyou call me a monster?";

    private BlackScene() {}

    static void draw(Graphics2D g2, double t) {
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, Canvas.W, Canvas.H);

        SahurCharacter.draw(g2, SAHUR_X, SAHUR_Y, SAHUR_SCALE);

        if (t >= Timeline.DIALOG_BOX_APPEAR_AT) {
            DialogBox.draw(g2, 200, 205, 350, 130, MONOLOGUE);
        }
    }
}

// Draws Windows 11 scene
final class Win11Scene {

    private Win11Scene() {}

    static void draw(Graphics2D g2, double t) {
        drawDesktop(g2);
        Win11Taskbar.draw(g2);
    }

    // Draws the blue glowing background
    private static void drawDesktop(Graphics2D g2) {
        GradientPaint bg = new GradientPaint(
                0, 0, new Color(165, 195, 225), Canvas.W, Canvas.H, new Color(195, 215, 238));
        g2.setPaint(bg);
        g2.fillRect(0, 0, Canvas.W, Canvas.H);

        RadialGradientPaint centerGlow = new RadialGradientPaint(
                new Point2D.Float(Canvas.W * 0.5f, Canvas.H * 0.4f), 400f,
                new float[]{0.0f, 1.0f},
                new Color[]{new Color(230, 242, 255, 180), new Color(165, 195, 225, 0)});
        g2.setPaint(centerGlow);
        g2.fillRect(0, 0, Canvas.W, Canvas.H);

        drawBloomPetals(g2);
    }

    // Draws the overlapping blue shapes on the wallpaper
    private static void drawBloomPetals(Graphics2D g2) {
        int cx = Canvas.W / 2;
        int bottomY = Canvas.H - Canvas.TASKBAR_H;

        Path2D.Double p1 = new Path2D.Double();
        p1.moveTo(cx - 215, bottomY);
        p1.curveTo(cx - 320, 110, cx - 150, 50, cx, 80);
        p1.curveTo(cx + 220, 120, cx + 260, 280, cx + 170, bottomY);
        p1.closePath();
        g2.setPaint(new GradientPaint(cx - 120, 50, new Color(10, 45, 130), cx + 120, bottomY, new Color(0, 95, 210)));
        g2.fill(p1);

        Path2D.Double p2 = new Path2D.Double();
        p2.moveTo(cx - 170, bottomY);
        p2.curveTo(cx - 250, 150, cx - 70, 90, cx + 60, 130);
        p2.curveTo(cx + 200, 180, cx + 185, 330, cx + 85, bottomY);
        p2.closePath();
        g2.setPaint(new GradientPaint(cx - 100, 90, new Color(15, 115, 235), cx + 70, bottomY, new Color(0, 60, 175)));
        g2.fill(p2);

        Path2D.Double p3 = new Path2D.Double();
        p3.moveTo(cx - 120, bottomY);
        p3.curveTo(cx - 185, 190, cx - 15, 130, cx + 85, 170);
        p3.curveTo(cx + 145, 220, cx + 120, 350, cx - 15, bottomY);
        p3.closePath();
        g2.setPaint(new GradientPaint(cx - 65, 130, new Color(75, 175, 255), cx + 45, bottomY, new Color(20, 110, 220)));
        g2.fill(p3);

        Path2D.Double p4 = new Path2D.Double();
        p4.moveTo(cx - 70, bottomY);
        p4.curveTo(cx - 125, 250, cx + 20, 190, cx + 90, 230);
        p4.curveTo(cx + 125, 290, cx + 55, 370, cx - 10, bottomY);
        p4.closePath();
        g2.setPaint(new GradientPaint(cx - 25, 190, new Color(135, 210, 255), cx + 25, bottomY, new Color(40, 130, 240)));
        g2.fill(p4);
    }
}

// Draws the Windows 11 centered taskbar
final class Win11Taskbar {
    private static final int ICON_COUNT = 8;
    private static final int ICON_SPACING = 36;
    private static final int ICON_SIZE = 20;

    private Win11Taskbar() {}

    static void draw(Graphics2D g2) {
        int y = Canvas.H - Canvas.TASKBAR_H;

        g2.setColor(new Color(243, 243, 243, 235));
        g2.fillRect(0, y, Canvas.W, Canvas.TASKBAR_H);
        g2.setColor(new Color(225, 225, 225));
        g2.fillRect(0, y, Canvas.W, 1);

        int clusterW = ICON_COUNT * ICON_SPACING;
        int startX = (Canvas.W - clusterW) / 2;
        int iconY = y + (Canvas.TASKBAR_H - ICON_SIZE) / 2;

        drawStartIcon(g2, startX, iconY);
        drawSearchIcon(g2, startX + ICON_SPACING, iconY);
        drawTaskViewIcon(g2, startX + ICON_SPACING * 2, iconY);
        drawWidgetsIcon(g2, startX + ICON_SPACING * 3, iconY);
        drawTeamsIcon(g2, startX + ICON_SPACING * 4, iconY);
        drawFileExplorerIcon(g2, startX + ICON_SPACING * 5, iconY);
        drawEdgeIcon(g2, startX + ICON_SPACING * 6, iconY);
        drawStoreIcon(g2, startX + ICON_SPACING * 7, iconY);

        g2.setColor(new Color(0, 103, 192));
        g2.fill(new RoundRectangle2D.Double(startX + ICON_SPACING * 5 + 4, y + Canvas.TASKBAR_H - 3, 12, 2, 1, 1));

        drawSystemTray(g2, y);
    }

    private static void drawStartIcon(Graphics2D g2, int x, int y) {
        int s = 9;
        g2.setColor(new Color(0, 120, 215));
        g2.fill(new RoundRectangle2D.Double(x, y, s, s, 2, 2));
        g2.fill(new RoundRectangle2D.Double(x + s + 2, y, s, s, 2, 2));
        g2.fill(new RoundRectangle2D.Double(x, y + s + 2, s, s, 2, 2));
        g2.fill(new RoundRectangle2D.Double(x + s + 2, y + s + 2, s, s, 2, 2));
    }

    private static void drawSearchIcon(Graphics2D g2, int x, int y) {
        g2.setColor(new Color(80, 80, 80));
        g2.setStroke(new BasicStroke(1.8f));
        g2.drawOval(x + 1, y + 1, 12, 12);
        g2.drawLine(x + 10, y + 10, x + 16, y + 16);
        g2.setStroke(new BasicStroke(1f));
    }

    private static void drawTaskViewIcon(Graphics2D g2, int x, int y) {
        g2.setColor(new Color(80, 80, 80));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(x + 1, y + 3, 10, 14, 2, 2);
        g2.setColor(new Color(140, 140, 140));
        g2.drawRoundRect(x + 6, y + 1, 10, 14, 2, 2);
        g2.setStroke(new BasicStroke(1f));
    }

    private static void drawWidgetsIcon(Graphics2D g2, int x, int y) {
        g2.setColor(new Color(0, 120, 215));
        g2.fillRoundRect(x + 1, y + 2, 8, 16, 2, 2);
        g2.setColor(new Color(0, 164, 239));
        g2.fillRoundRect(x + 10, y + 2, 8, 16, 2, 2);
    }

    private static void drawTeamsIcon(Graphics2D g2, int x, int y) {
        g2.setColor(new Color(75, 70, 185));
        g2.fillOval(x + 1, y + 1, 18, 18);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Tahoma", Font.BOLD, 11));
        g2.drawString("T", x + 6, y + 14);
    }

    private static void drawFileExplorerIcon(Graphics2D g2, int x, int y) {
        g2.setColor(new Color(245, 180, 35));
        g2.fillRoundRect(x + 1, y + 4, 18, 13, 3, 3);
        g2.setColor(new Color(0, 120, 215));
        g2.fillRect(x + 4, y + 2, 7, 3);
    }

    private static void drawEdgeIcon(Graphics2D g2, int x, int y) {
        g2.setColor(new Color(15, 140, 205));
        g2.fillOval(x + 1, y + 1, 18, 18);
        g2.setColor(new Color(40, 200, 175));
        g2.fillOval(x + 5, y + 5, 10, 10);
    }

    private static void drawStoreIcon(Graphics2D g2, int x, int y) {
        g2.setColor(new Color(0, 120, 215));
        g2.fillRoundRect(x + 2, y + 5, 16, 13, 2, 2);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawArc(x + 5, y + 1, 10, 8, 0, 180);
        g2.setStroke(new BasicStroke(1f));
    }

    private static void drawSystemTray(Graphics2D g2, int taskbarY) {
        int trayX = Canvas.W - 135;
        g2.setColor(new Color(60, 60, 60));

        g2.setFont(new Font("Tahoma", Font.PLAIN, 10));
        g2.drawString("ENG", trayX, taskbarY + 21);

        int iconX = trayX + 28;
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawArc(iconX, taskbarY + 11, 11, 11, 45, 90);
        g2.drawArc(iconX + 2, taskbarY + 14, 7, 7, 45, 90);
        g2.setStroke(new BasicStroke(1f));

        String timeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("12:11"));
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("15/10/2021"));

        g2.setFont(new Font("Tahoma", Font.PLAIN, 10));
        g2.drawString(timeStr, trayX + 48, taskbarY + 15);
        g2.setFont(new Font("Tahoma", Font.PLAIN, 9));
        g2.drawString(dateStr, trayX + 45, taskbarY + 26);

        g2.setColor(new Color(0, 103, 192));
        g2.fillOval(Canvas.W - 18, taskbarY + 11, 12, 12);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Tahoma", Font.BOLD, 9));
        g2.drawString("3", Canvas.W - 14, taskbarY + 20);
    }
}