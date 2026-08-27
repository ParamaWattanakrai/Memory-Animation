import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javax.swing.*;

/**
 * Animates a "Windows XP -> Windows 7 -> Windows 11 -> Black Screen" sequence.
 */
public class Assignment1_67050314 extends JPanel implements Runnable {

    // ---- Canvas ----
    static final int W = 600, H = 600;
    static final int TASKBAR_H = 34;
    static final int WIN11_TASKBAR_H = 48;
    private static final int TARGET_FPS = 60;

    // ---- Animation timeline (seconds since start) ----
    private static final double XP_END = 5.0; // Extended for slower XP scene
    private static final double TRANSITION_DURATION = 2.0;
    private static final double WIN7_START = XP_END + TRANSITION_DURATION; // 7.0s
    private static final double WIN7_TO_WIN11_START = WIN7_START + 4.0;    // 11.0s
    private static final double WIN11_START = WIN7_TO_WIN11_START + TRANSITION_DURATION; // 13.0s
    private static final double WIN11_TO_BLACK_START = WIN11_START + 1.5; // Shortened Win 11 duration to 1.5s
    private static final double BLACK_START = WIN11_TO_BLACK_START + TRANSITION_DURATION;

    private static final double MINECRAFT_WINDOW_APPEAR_AT = WIN7_START;

    // ---- Shared window chrome ----
    private static final int TITLE_BAR_H = 30;

    // ---- Fixed window placements ----
    private static final Rectangle MEMORIES_WINDOW_1 = new Rectangle(40, 45, 290, 190);
    private static final Rectangle MEMORIES_WINDOW_2 = new Rectangle(80, 110, 290, 190);
    private static final Rectangle MINESWEEPER_WINDOW = new Rectangle(295, 210, 280, 340);
    private static final Rectangle MINECRAFT_WINDOW = new Rectangle(70, 40, 460, 380);

    private static final int TRANSITION_2_X = 298;
    private static final int TRANSITION_2_Y = 205;

    double totalTime = 0;
    int mouseX = 0, mouseY = 0;

    public Assignment1_67050314() {
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
            }
        });
    }

    public static void main(String[] args) {
        Assignment1_67050314 m = new Assignment1_67050314();
        m.setPreferredSize(new Dimension(W, H));

        JFrame f = new JFrame();
        f.add(m);
        f.setTitle("Windows OS Transition Sequence");
        f.setResizable(false);
        f.pack();
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setLocationRelativeTo(null);
        f.setVisible(true);
        (new Thread(m)).start();
    }

    @Override
    public void run() {
        long lastTimeMs = System.currentTimeMillis();
        while (!Thread.currentThread().isInterrupted()) {
            long nowMs = System.currentTimeMillis();
            totalTime += (nowMs - lastTimeMs) / 1000.0;
            lastTimeMs = nowMs;

            repaint();

            try {
                Thread.sleep(1000 / TARGET_FPS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        BufferedImage buffer = new BufferedImage(W + 1, H + 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = buffer.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (totalTime < XP_END) {
            drawXPScene(g2);
        } else if (totalTime < WIN7_START) {
            Point origin = minesweeperExplosionOrigin();
            drawTransition(g2, XP_END, origin.x, origin.y, () -> drawXPScene(g2), () -> drawWin7Scene(g2));
        } else if (totalTime < WIN7_TO_WIN11_START) {
            drawWin7Scene(g2);
        } else if (totalTime < WIN11_START) {
            drawTransition(g2, WIN7_TO_WIN11_START, TRANSITION_2_X, TRANSITION_2_Y,
                    () -> drawWin7Scene(g2), () -> drawWin11Scene(g2));
        } else if (totalTime < WIN11_TO_BLACK_START) {
            drawWin11Scene(g2);
        } else if (totalTime < BLACK_START) {
            drawTransitionNoExplosion(g2, WIN11_TO_BLACK_START, W / 2, H / 2, 
                    () -> drawWin11Scene(g2), () -> drawBlackScene(g2));
        } else {
            drawBlackScene(g2);
        }

        drawDebugInfo(g2);
        g.drawImage(buffer, 0, 0, null);
    }

    private void drawTransition(Graphics2D g2, double phaseStart, int originX, int originY,
                                 Runnable drawOldScene, Runnable drawNewScene) {
        double progress = (totalTime - phaseStart) / TRANSITION_DURATION;
        double maxDist = Math.hypot(W, H);
        double radius = Math.pow(progress, 2.5) * maxDist;

        drawOldScene.run();

        Shape oldClip = g2.getClip();
        g2.setClip(new Ellipse2D.Double(originX - radius, originY - radius, radius * 2, radius * 2));
        drawNewScene.run();
        g2.setClip(oldClip);

        drawRetroExplosion(g2, originX, originY, progress);
    }

    private void drawTransitionNoExplosion(Graphics2D g2, double phaseStart, int originX, int originY,
                                 Runnable drawOldScene, Runnable drawNewScene) {
        double progress = (totalTime - phaseStart) / TRANSITION_DURATION;
        double maxDist = Math.hypot(W, H);
        double radius = Math.pow(progress, 2.5) * maxDist;

        drawOldScene.run();

        Shape oldClip = g2.getClip();
        g2.setClip(new Ellipse2D.Double(originX - radius, originY - radius, radius * 2, radius * 2));
        drawNewScene.run();
        g2.setClip(oldClip);
    }

    private void drawBlackScene(Graphics2D g2) {
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, W, H);
        
        drawSahur(g2, W / 2, H / 2 - 20);
    }

    private void drawSahur(Graphics2D g2, int cx, int cy) {
        Color cBase = new Color(175, 107, 50);
        Color cShadow = new Color(110, 50, 10);
        Color cHighlight = new Color(215, 155, 95);
        Color cDark = new Color(50, 20, 5);
        Color cEye = new Color(230, 215, 185);
        
        drawPolyRel(g2, cShadow, cx, cy, new int[]{-32, -22, -100, -125}, new int[]{65, 60, 210, 220});
        drawPolyRel(g2, cHighlight, cx, cy, new int[]{-29, -25, -105, -115}, new int[]{65, 63, 210, 215}); 
        drawPolyRel(g2, cShadow, cx, cy, new int[]{-135, -105, -115, -145}, new int[]{200, 205, 235, 225}); 
        
        drawPolyRel(g2, cShadow, cx, cy, new int[]{-15, -5, -10, -20}, new int[]{90, 90, 175, 175}); 
        drawPolyRel(g2, cBase, cx, cy, new int[]{-23, -7, -9, -25}, new int[]{170, 172, 185, 183}); 
        drawPolyRel(g2, cShadow, cx, cy, new int[]{-22, -12, -18, -30}, new int[]{180, 180, 255, 255}); 
        drawPolyRel(g2, cBase, cx, cy, new int[]{-55, -10, -15, -60}, new int[]{250, 250, 265, 265}); 
        
        drawPolyRel(g2, cShadow, cx, cy, new int[]{25, 35, 38, 28}, new int[]{90, 90, 170, 170}); 
        drawPolyRel(g2, cBase, cx, cy, new int[]{24, 40, 42, 26}, new int[]{167, 169, 182, 180}); 
        drawPolyRel(g2, cShadow, cx, cy, new int[]{28, 38, 42, 32}, new int[]{178, 178, 255, 255}); 
        drawPolyRel(g2, cBase, cx, cy, new int[]{15, 50, 55, 10}, new int[]{250, 250, 265, 265}); 

        drawPolyRel(g2, cBase, cx, cy, 
            new int[]{-15, 10, 35, 45, 50, 48, 30, -10, -25, -35, -45, -55, -50, -30}, 
            new int[]{-205, -210, -200, -150, -50, 80, 100, 105, 90, 20, -80, -120, -160, -190});
            
        drawPolyRel(g2, cShadow, cx, cy, 
            new int[]{-15, 0, -10, -20, -25, -35, -45, -55, -50, -30},
            new int[]{-205, -190, -50, 80, 90, 20, -80, -120, -160, -190});
            
        drawPolyRel(g2, cHighlight, cx, cy, 
            new int[]{35, 45, 50, 48, 30, 20, 25},
            new int[]{-200, -150, -50, 80, 100, 30, -100});

        drawPolyRel(g2, cDark, cx, cy, new int[]{-50, -30, -15, -25, -45, -55}, new int[]{-170, -180, -155, -130, -135, -150});
        drawPolyRel(g2, cEye, cx, cy, new int[]{-45, -32, -20, -28, -42, -50}, new int[]{-165, -172, -155, -138, -140, -150});
        drawPolyRel(g2, cDark, cx, cy, new int[]{-35, -25, -22, -28, -38}, new int[]{-160, -165, -150, -142, -152});
        drawPolyRel(g2, Color.WHITE, cx, cy, new int[]{-30, -25, -26, -31}, new int[]{-155, -158, -152, -150}); 
        
        drawPolyRel(g2, cDark, cx, cy, new int[]{5, 25, 40, 35, 15, 0}, new int[]{-185, -190, -165, -140, -135, -160});
        drawPolyRel(g2, cEye, cx, cy, new int[]{10, 25, 35, 30, 15, 5}, new int[]{-180, -185, -165, -145, -140, -160});
        drawPolyRel(g2, cDark, cx, cy, new int[]{20, 30, 33, 25, 17}, new int[]{-172, -175, -162, -158, -162});
        drawPolyRel(g2, Color.WHITE, cx, cy, new int[]{23, 27, 26, 22}, new int[]{-168, -170, -165, -163}); 
        
        drawPolyRel(g2, cHighlight, cx, cy, new int[]{-10, -25, 0}, new int[]{-140, -115, -120}); 
        drawPolyRel(g2, cShadow, cx, cy, new int[]{0, -25, 10}, new int[]{-120, -115, -110}); 
        
        drawPolyRel(g2, cDark, cx, cy, new int[]{-30, -10, 15, 25, 20, 0, -25}, new int[]{-105, -95, -100, -115, -120, -102, -110});

        drawPolyRel(g2, cBase, cx, cy, new int[]{-30, -20, -35, -45}, new int[]{-30, -30, 25, 25}); 
        drawPolyRel(g2, cShadow, cx, cy, new int[]{-45, -35, -25, -35}, new int[]{25, 25, 60, 60}); 
        drawPolyRel(g2, cDark, cx, cy, new int[]{-40, -20, -25, -45}, new int[]{55, 55, 70, 70}); 
        
        drawPolyRel(g2, cShadow, cx, cy, new int[]{45, 55, 62, 52}, new int[]{-30, -30, 25, 25}); 
        drawPolyRel(g2, cShadow, cx, cy, new int[]{52, 62, 55, 45}, new int[]{25, 25, 70, 70}); 
        drawPolyRel(g2, cBase, cx, cy, new int[]{42, 58, 55, 40}, new int[]{65, 65, 80, 80}); 
    }

    private void drawPolyRel(Graphics2D g2, Color c, int cx, int cy, int[] x, int[] y) {
        int[] nx = new int[x.length];
        int[] ny = new int[y.length];
        for(int i = 0; i < x.length; i++) {
            nx[i] = cx + x[i];
            ny[i] = cy + y[i];
        }
        g2.setColor(c);
        g2.fillPolygon(nx, ny, x.length);
    }

    private Point minesweeperExplosionOrigin() {
        int panelX = MINESWEEPER_WINDOW.x + 12;
        int panelY = MINESWEEPER_WINDOW.y + 42;
        int panelW = MINESWEEPER_WINDOW.width - 24;
        int panelH = 38;
        int gridX = panelX;
        int gridY = panelY + panelH + 8;
        int gridH = MINESWEEPER_WINDOW.height - (gridY - MINESWEEPER_WINDOW.y) - 12;
        int cellW = (panelW - 6) / 9;
        int cellH = (gridH - 6) / 9;

        return new Point(gridX + 4 + 8 * cellW + cellW / 2, gridY + 4 + 4 * cellH + cellH / 2);
    }

    private void drawRetroExplosion(Graphics2D g2, int cx, int cy, double progress) {
        double easeOut = 1.0 - Math.pow(1.0 - progress, 3);
        int globalAlpha = Math.max(0, Math.min(255, (int)((1.0 - progress) * 255)));
        if (globalAlpha <= 0) return;

        int ringRadius = (int)(easeOut * 800);
        g2.setStroke(new BasicStroke((float)((1.0 - progress) * 25f)));
        g2.setColor(new Color(255, 255, 255, (int)(globalAlpha * 0.4)));
        g2.drawOval(cx - ringRadius, cy - ringRadius, ringRadius * 2, ringRadius * 2);
        
        int coreRadius = (int)(easeOut * 600);
        if (coreRadius > 0) {
            RadialGradientPaint blastGrad = new RadialGradientPaint(
                new Point2D.Float(cx, cy),
                coreRadius + 1f, 
                new float[]{0.0f, 0.2f, 0.6f, 1.0f},
                new Color[]{
                    new Color(255, 255, 255, globalAlpha),        
                    new Color(255, 200, 50, globalAlpha),         
                    new Color(255, 50, 0, (int)(globalAlpha*0.7)),
                    new Color(50, 0, 0, 0)                        
                }
            );
            g2.setPaint(blastGrad);
            g2.fillOval(cx - coreRadius, cy - coreRadius, coreRadius * 2, coreRadius * 2);
        }

        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 24; i++) {
            double angle = (i * 15) + (i % 2 == 0 ? 0 : 7); 
            double rad = Math.toRadians(angle);
            
            double speedMultiplier = 0.5 + ((i * 7) % 10) / 10.0; 
            double dist = easeOut * 700 * speedMultiplier;
            double trailLength = 20 + (1.0 - progress) * 80 * speedMultiplier;
            
            int x2 = (int)(cx + Math.cos(rad) * dist);
            int y2 = (int)(cy + Math.sin(rad) * dist);
            int x1 = (int)(cx + Math.cos(rad) * Math.max(0, dist - trailLength));
            int y1 = (int)(cy + Math.sin(rad) * Math.max(0, dist - trailLength));

            g2.setColor(new Color(255, (int)(200 * (1.0 - progress)), 0, globalAlpha));
            g2.drawLine(x1, y1, x2, y2);
        }
        g2.setStroke(new BasicStroke(1f));
    }

    private void drawDebugInfo(Graphics2D g2) {
        g2.setColor(Color.BLACK);
        g2.fillRect(5, 5, 130, 45);
        g2.setColor(Color.GREEN);
        g2.drawRect(5, 5, 130, 45);
        g2.setFont(new Font("Monospaced", Font.BOLD, 14));
        g2.drawString("X: " + mouseX + " Y: " + mouseY, 15, 23);
        
        g2.drawString(String.format("Time: %.2fs", totalTime), 15, 40);
        
        g2.setColor(Color.RED);
        g2.drawLine(mouseX - 10, mouseY, mouseX + 10, mouseY);
        g2.drawLine(mouseX, mouseY - 10, mouseX, mouseY + 10);
    }

    private void drawPoly(Graphics2D g2, Color c, int[] x, int[] y) {
        g2.setColor(c);
        g2.fillPolygon(x, y, x.length);
    }

    private void drawMidpointCircle(Graphics2D g2, int cx, int cy, int radius, Color color) {
        g2.setColor(color);
        int x = 0;
        int y = radius;
        int p = 1 - radius;

        fillCircleSymmetric(g2, cx, cy, x, y);

        while (x < y) {
            x++;
            if (p < 0) {
                p += 2 * x + 1;
            } else {
                y--;
                p += 2 * (x - y) + 1;
            }
            fillCircleSymmetric(g2, cx, cy, x, y);
        }
    }

    private void fillCircleSymmetric(Graphics2D g2, int cx, int cy, int x, int y) {
        g2.drawLine(cx - x, cy + y, cx + x, cy + y);
        g2.drawLine(cx - x, cy - y, cx + x, cy - y);
        g2.drawLine(cx - y, cy + x, cx + y, cy + x);
        g2.drawLine(cx - y, cy - x, cx + y, cy - x);
    }

    private void drawXPScene(Graphics2D g2) {
        drawXPDesktop(g2);
        
        // 1. First text window appears after breathing room
        if (totalTime >= 0.5) {
            drawWindow(g2, MEMORIES_WINDOW_1.x, MEMORIES_WINDOW_1.y, 
                       MEMORIES_WINDOW_1.width, MEMORIES_WINDOW_1.height, 
                       "Notes.txt", "'Don't play for too long' –Mother");
        }

        // 2. Second text window appears with additional spacing
        if (totalTime >= 1.5) {
            drawWindow(g2, MEMORIES_WINDOW_2.x, MEMORIES_WINDOW_2.y, 
                       MEMORIES_WINDOW_2.width, MEMORIES_WINDOW_2.height, 
                       "ntoe.txt", "Helloooooooooooo");
        }

        // 3. Minesweeper window appears last at bottom right
        if (totalTime >= 2.5) {
            drawMinesweeperWindow(g2, MINESWEEPER_WINDOW.x, MINESWEEPER_WINDOW.y,
                    MINESWEEPER_WINDOW.width, MINESWEEPER_WINDOW.height);
        }

        drawXPTaskbar(g2);
    }

    private void drawXPDesktop(Graphics2D g2) {
        Color cSkyDark = new Color(58, 121, 223);
        Color cSkyMid = new Color(135, 179, 241);
        Color cCloudShadow = new Color(175, 203, 241);
        Color cCloud = new Color(233, 239, 246);
        
        Color cGrassLight = new Color(135, 186, 46);
        Color cGrassMid = new Color(102, 152, 36);
        Color cGrassDark = new Color(71, 107, 26);
        Color cGrassDeep = new Color(52, 80, 20);

        g2.setColor(cSkyMid);
        g2.fillRect(0, 0, W, H);

        drawPoly(g2, cSkyDark, 
            new int[]{0, 110, 160, 200, 150, 120, 80, 30, 0}, 
            new int[]{0, 0,   30,  80,  120, 140, 130, 90, 70});
        drawPoly(g2, cSkyDark, 
            new int[]{220, 600, 600, 480, 450, 400, 320, 280, 250}, 
            new int[]{0,   0,   250, 280, 210, 220, 160, 100, 50});

        drawMidpointCircle(g2, 510, 65, 38, new Color(233, 239, 246));

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

    private void drawXPWindowChrome(Graphics2D g2, int x, int y, int w, int h, Color bodyColor, String title) {
        g2.setColor(new Color(0, 0, 0, 60));
        g2.fill(new RoundRectangle2D.Double(x + 6, y + 6, w, h, 12, 12));

        g2.setColor(bodyColor);
        g2.fill(new RoundRectangle2D.Double(x, y, w, h, 12, 12));

        GradientPaint titleGrad = new GradientPaint(x, y, new Color(0, 88, 225), x, y + TITLE_BAR_H, new Color(30, 110, 255));
        g2.setPaint(titleGrad);
        Path2D.Double titleBar = new Path2D.Double();
        titleBar.moveTo(x, y + TITLE_BAR_H);
        titleBar.lineTo(x, y + 10);
        titleBar.quadTo(x, y, x + 10, y);
        titleBar.lineTo(x + w - 10, y);
        titleBar.quadTo(x + w, y, x + w, y + 10);
        titleBar.lineTo(x + w, y + TITLE_BAR_H);
        titleBar.closePath();
        g2.fill(titleBar);
        g2.setPaint(null);

        g2.setColor(new Color(255, 255, 255, 100));
        g2.draw(new RoundRectangle2D.Double(x + 1, y + 1, w - 2, h - 2, 10, 10));

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Tahoma", Font.BOLD, 13));
        g2.drawString(title, x + 12, y + 20);

        drawWindowControlButtons(g2, x, y, w);

        g2.setColor(new Color(0, 70, 200));
        g2.drawLine(x, y + 10, x, y + h - 10);
        g2.drawLine(x + w, y + 10, x + w, y + h - 10);
        g2.drawLine(x + 10, y, x + w - 10, y);
        g2.drawLine(x + 10, y + h, x + w - 10, y + h);
    }

    private void drawWindowControlButtons(Graphics2D g2, int x, int y, int w) {
        int bw = 22, bh = 22, gap = 2;
        int bx = x + w - (bw * 3 + gap * 2) - 6, by = y + 4;

        g2.setPaint(new GradientPaint(bx, by, new Color(80, 160, 255), bx, by + bh, new Color(30, 100, 220)));
        g2.fill(new RoundRectangle2D.Double(bx, by, bw, bh, 4, 4));
        g2.setColor(Color.WHITE);
        g2.fillRect(bx + 6, by + bh - 7, bw - 12, 3);

        int mx = bx + bw + gap;
        g2.setPaint(new GradientPaint(mx, by, new Color(80, 160, 255), mx, by + bh, new Color(30, 100, 220)));
        g2.fill(new RoundRectangle2D.Double(mx, by, bw, bh, 4, 4));
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2f));
        g2.drawRect(mx + 6, by + 6, bw - 12, bh - 12);
        g2.fillRect(mx + 6, by + 6, bw - 12, 3);
        g2.setStroke(new BasicStroke(1f));

        int cx = mx + bw + gap, cy = by;
        g2.setPaint(new GradientPaint(cx, cy, new Color(240, 100, 80), cx, cy + bh, new Color(210, 40, 30)));
        g2.fill(new RoundRectangle2D.Double(cx, cy, bw, bh, 4, 4));
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2f));
        g2.drawLine(cx + 7, cy + 7, cx + bw - 7, cy + bh - 7);
        g2.drawLine(cx + bw - 7, cy + 7, cx + 7, cy + bh - 7);
        g2.setStroke(new BasicStroke(1f));
    }

    private void drawWindow(Graphics2D g2, int x, int y, int w, int h, String title, String content) {
        drawXPWindowChrome(g2, x, y, w, h, new Color(240, 240, 235), title);

        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Tahoma", Font.PLAIN, 12));
        g2.drawString(content, x + 16, y + TITLE_BAR_H + 30);
    }

    private void drawMinesweeperWindow(Graphics2D g2, int x, int y, int w, int h) {
        drawXPWindowChrome(g2, x, y, w, h, new Color(192, 192, 192), "Minesweeper");

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

        // Adjust Minesweeper step timing to match the extended XP timeline
        int gameStep;
        if (totalTime < 3.1) {
            gameStep = 0;
        } else if (totalTime < 3.7) {
            gameStep = 1;
        } else if (totalTime < 4.3) {
            gameStep = 2;
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

    private void drawWin7Scene(Graphics2D g2) {
        drawWin7Desktop(g2);

        if (totalTime >= MINECRAFT_WINDOW_APPEAR_AT) {
            drawWin7MinecraftWindow(g2, MINECRAFT_WINDOW.x, MINECRAFT_WINDOW.y,
                    MINECRAFT_WINDOW.width, MINECRAFT_WINDOW.height);
        }

        drawWin7Taskbar(g2);
    }

    private void drawWin7Desktop(Graphics2D g2) {
        GradientPaint sky = new GradientPaint(0, 0, new Color(15, 95, 185), 0, H, new Color(5, 45, 105));
        g2.setPaint(sky);
        g2.fillRect(0, 0, W, H);

        RadialGradientPaint glow = new RadialGradientPaint(
                new Point2D.Float(W / 2f, H / 2f - 30),
                350f,
                new float[]{0.0f, 1.0f},
                new Color[]{new Color(55, 175, 245, 160), new Color(0, 0, 0, 0)}
        );
        g2.setPaint(glow);
        g2.fillRect(0, 0, W, H);

        g2.setStroke(new BasicStroke(40f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D.Double ribbon1 = new Path2D.Double();
        ribbon1.moveTo(-50, H - 40);
        ribbon1.curveTo(W * 0.3, H * 0.35, W * 0.7, H * 0.85, W + 50, H * 0.55);
        g2.setColor(new Color(255, 255, 255, 22));
        g2.draw(ribbon1);

        g2.setStroke(new BasicStroke(16f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D.Double ribbon2 = new Path2D.Double();
        ribbon2.moveTo(-20, H - 20);
        ribbon2.curveTo(W * 0.4, H * 0.3, W * 0.6, H * 0.8, W + 20, H * 0.5);
        g2.setColor(new Color(255, 255, 255, 45));
        g2.draw(ribbon2);

        g2.setStroke(new BasicStroke(3f));
        Path2D.Double ribbon3 = new Path2D.Double();
        ribbon3.moveTo(0, H - 100);
        ribbon3.curveTo(W * 0.35, H * 0.35, W * 0.65, H * 0.75, W, H * 0.55);
        g2.setColor(new Color(255, 255, 255, 130));
        g2.draw(ribbon3);

        drawWin7CenterLogo(g2, W / 2, H / 2 - 30);
    }

    private void drawWin7CenterLogo(Graphics2D g2, int cx, int cy) {
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

    private void drawWin7MinecraftWindow(Graphics2D g2, int x, int y, int w, int h) {
        int titleH = 30;

        g2.setColor(new Color(0, 0, 0, 80));
        g2.fill(new RoundRectangle2D.Double(x + 6, y + 6, w, h, 14, 14));

        g2.setColor(new Color(130, 185, 225, 170));
        g2.fill(new RoundRectangle2D.Double(x, y, w, h, 12, 12));

        GradientPaint glassGlow = new GradientPaint(x, y, new Color(255, 255, 255, 140), x, y + titleH, new Color(255, 255, 255, 30));
        g2.setPaint(glassGlow);
        g2.fill(new RoundRectangle2D.Double(x, y, w, titleH, 12, 12));

        int border = 7;
        int clientX = x + border;
        int clientY = y + titleH;
        int clientW = w - border * 2;
        int clientH = h - titleH - border;

        g2.setColor(new Color(15, 25, 40));
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g2.drawString("Minecraft 1.2.5 - Singleplayer", x + 12, y + 20);

        int bw = 26, bh = 18;
        int cx = x + w - bw - 6, cy = y + 2;

        g2.setPaint(new GradientPaint(cx, cy, new Color(230, 90, 80, 230), cx, cy + bh, new Color(180, 40, 30, 240)));
        g2.fill(new RoundRectangle2D.Double(cx, cy, bw, bh, 4, 4));
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(cx + 9, cy + 5, cx + bw - 9, cy + bh - 5);
        g2.drawLine(cx + bw - 9, cy + 5, cx + 9, cy + bh - 5);

        int mx = cx - bw - 2;
        g2.setPaint(new GradientPaint(mx, cy, new Color(225, 240, 250, 160), mx, cy + bh, new Color(175, 200, 220, 190)));
        g2.fill(new RoundRectangle2D.Double(mx, cy, bw, bh, 4, 4));
        g2.setColor(new Color(40, 50, 65));
        g2.drawRect(mx + 8, cy + 4, 9, 8);

        int nx = mx - bw - 2;
        g2.setPaint(new GradientPaint(nx, cy, new Color(225, 240, 250, 160), nx, cy + bh, new Color(175, 200, 220, 190)));
        g2.fill(new RoundRectangle2D.Double(nx, cy, bw, bh, 4, 4));
        g2.setColor(new Color(40, 50, 65));
        g2.drawLine(nx + 8, cy + 11, nx + 16, cy + 11);
        g2.setStroke(new BasicStroke(1f));

        Shape oldClip = g2.getClip();
        g2.clip(new Rectangle2D.Double(clientX, clientY, clientW, clientH));
        drawMinecraftScene(g2, clientX, clientY, clientW, clientH);
        g2.setClip(oldClip); 

        g2.setColor(new Color(0, 0, 0, 150));
        g2.drawRect(clientX - 1, clientY - 1, clientW + 1, clientH + 1);
    }

    private void drawMinecraftScene(Graphics2D g2, int cx, int cy, int cw, int ch) {
        g2.setColor(new Color(10, 15, 30));
        g2.fillRect(cx, cy, cw, ch);

        g2.setColor(Color.WHITE);
        g2.fillRect(cx + 30, cy + 20, 2, 2);
        g2.fillRect(cx + 120, cy + 40, 2, 2);
        g2.fillRect(cx + 250, cy + 15, 2, 2);
        g2.fillRect(cx + 380, cy + 50, 2, 2);
        g2.fillRect(cx + 90, cy + 70, 2, 2);

        g2.setColor(new Color(240, 240, 220));
        g2.fillRect(cx + cw - 60, cy + 20, 24, 24);

        g2.setColor(new Color(35, 25, 20));
        g2.fillPolygon(
            new int[]{cx, cx + 130, cx + 180, cx},
            new int[]{cy + 130, cy + 40, cy + 150, cy + 150}, 4
        );

        int hx = cx + cw - 120, hy = cy + 40;
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

        int fx = cx + cw / 2 + 25, fy = cy + 105;
        g2.setColor(new Color(30, 90, 210, 210));
        g2.fillRect(fx, fy, 42, 38);
        g2.setColor(new Color(80, 140, 240, 180));
        g2.fillRect(fx + 6, fy + 4, 30, 30);

        g2.setColor(new Color(210, 195, 140));
        g2.fillRect(cx, cy + 140, cw, ch - 140);

        g2.setColor(new Color(190, 175, 120));
        for (int i = 0; i < cw; i += 24) {
            g2.drawLine(cx + i, cy + 140, cx + i - 40, cy + ch);
        }
        for (int j = 140; j < ch; j += 20) {
            g2.drawLine(cx, cy + j, cx + cw, cy + j);
        }

        g2.setColor(new Color(140, 140, 140));
        g2.fillPolygon(
            new int[]{cx, cx + 150, cx + 110, cx},
            new int[]{cy + 170, cy + 190, cy + ch, cy + ch}, 4
        );
        g2.setColor(new Color(110, 110, 110));
        g2.drawPolygon(
            new int[]{cx, cx + 150, cx + 110, cx},
            new int[]{cy + 170, cy + 190, cy + ch, cy + ch}, 4
        );

        boolean isFlashing = (totalTime >= WIN7_START + 1.0) && ((int)(totalTime * 8) % 2 == 0); 
        int crx = cx + cw / 2 - 22;
        int cry = cy + 95;

        g2.setColor(new Color(0, 0, 0, 70));
        g2.fillOval(crx - 6, cry + 80, 52, 16);

        g2.setColor(isFlashing ? new Color(220, 255, 220) : new Color(75, 170, 60));
        g2.fillRect(crx, cry, 40, 40);

        g2.setColor(isFlashing ? new Color(150, 180, 150) : Color.BLACK);
        g2.fillRect(crx + 6, cry + 10, 9, 9);
        g2.fillRect(crx + 25, cry + 10, 9, 9);
        g2.fillRect(crx + 15, cry + 19, 10, 14); 
        g2.fillRect(crx + 11, cry + 24, 18, 12); 
        g2.fillRect(crx + 11, cry + 32, 5, 5);  
        g2.fillRect(crx + 24, cry + 32, 5, 5);  

        g2.setColor(isFlashing ? new Color(200, 245, 200) : new Color(65, 155, 50));
        g2.fillRect(crx + 6, cry + 40, 28, 36);

        double legAnim = Math.sin(totalTime * 12) * 4;
        g2.setColor(isFlashing ? new Color(180, 230, 180) : new Color(50, 130, 40));
        g2.fillRect(crx + 3, cry + 72 + (int)legAnim, 14, 16);  
        g2.fillRect(crx + 23, cry + 72 - (int)legAnim, 14, 16); 

        if (isFlashing) {
            g2.setColor(new Color(255, 255, 255, 160));
            g2.setStroke(new BasicStroke(3f));
            g2.drawRect(crx - 2, cry - 2, 44, 44);
            g2.drawRect(crx + 4, cry + 38, 32, 52);
            g2.setStroke(new BasicStroke(1f));
        }

        int midX = cx + cw / 2;
        int midY = cy + ch / 2;
        g2.setColor(new Color(255, 255, 255, 200));
        g2.fillRect(midX - 6, midY - 1, 13, 3);
        g2.fillRect(midX - 1, midY - 6, 3, 13);

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
        g2.fillRect(swX + 65, swY + 95, 25, 8);

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
        g2.drawRect(hudX + 20 - 1, hudY - 1, 22, 22);
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

    private void drawWin11Scene(Graphics2D g2) {
        drawWin11Desktop(g2);
        drawWin11Taskbar(g2);
    }

    private void drawWin11Desktop(Graphics2D g2) {
        GradientPaint bg = new GradientPaint(
                0, 0, new Color(165, 195, 225),
                W, H, new Color(195, 215, 238)
        );
        g2.setPaint(bg);
        g2.fillRect(0, 0, W, H);

        RadialGradientPaint centerGlow = new RadialGradientPaint(
                new Point2D.Float(W * 0.5f, H * 0.4f), 400f,
                new float[]{0.0f, 1.0f},
                new Color[]{new Color(230, 242, 255, 180), new Color(165, 195, 225, 0)}
        );
        g2.setPaint(centerGlow);
        g2.fillRect(0, 0, W, H);

        drawBloomPetals(g2);
    }

    private void drawBloomPetals(Graphics2D g2) {
        int cx = W / 2;
        int bottomY = H - WIN11_TASKBAR_H;

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

    private void drawWin11Taskbar(Graphics2D g2) {
        int y = H - WIN11_TASKBAR_H;

        g2.setColor(new Color(243, 243, 243, 235));
        g2.fillRect(0, y, W, WIN11_TASKBAR_H);

        g2.setColor(new Color(225, 225, 225));
        g2.fillRect(0, y, W, 1);

        int totalIcons = 8;
        int spacing = 38;
        int clusterW = totalIcons * spacing;
        int startX = (W - clusterW) / 2;
        int iconY = y + (WIN11_TASKBAR_H - 24) / 2;

        drawWin11StartIcon(g2, startX, iconY);
        drawSearchIcon(g2, startX + spacing, iconY);
        drawTaskViewIcon(g2, startX + spacing * 2, iconY);
        drawWidgetsIcon(g2, startX + spacing * 3, iconY);
        drawTeamsIcon(g2, startX + spacing * 4, iconY);
        drawFileExplorerIcon(g2, startX + spacing * 5, iconY);
        drawEdgeIcon(g2, startX + spacing * 6, iconY);
        drawStoreIcon(g2, startX + spacing * 7, iconY);

        g2.setColor(new Color(0, 103, 192));
        g2.fill(new RoundRectangle2D.Double(startX + spacing * 5 + 4, y + WIN11_TASKBAR_H - 4, 16, 3, 2, 2));

        drawWin11SystemTray(g2, y);
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
        g2.setStroke(new BasicStroke(1f));
    }

    private void drawTaskViewIcon(Graphics2D g2, int x, int y) {
        g2.setColor(new Color(80, 80, 80));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(x + 2, y + 4, 12, 16, 3, 3);
        g2.setColor(new Color(140, 140, 140));
        g2.drawRoundRect(x + 8, y + 2, 12, 16, 3, 3);
        g2.setStroke(new BasicStroke(1f));
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
        g2.setStroke(new BasicStroke(1f));
    }

    private void drawWin11SystemTray(Graphics2D g2, int taskbarY) {
        int trayX = W - 145;
        g2.setColor(new Color(60, 60, 60));
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));

        g2.drawString("ENG", trayX, taskbarY + 20);
        g2.drawString("TH", trayX + 2, taskbarY + 32);

        int iconX = trayX + 30;
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawArc(iconX, taskbarY + 16, 12, 12, 45, 90);
        g2.drawArc(iconX + 2, taskbarY + 19, 8, 8, 45, 90);
        g2.setStroke(new BasicStroke(1f));

        String timeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("12:11"));
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("15/10/2021"));

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2.drawString(timeStr, trayX + 55, taskbarY + 20);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        g2.drawString(dateStr, trayX + 52, taskbarY + 34);

        g2.setColor(new Color(0, 103, 192));
        g2.fillOval(W - 22, taskbarY + 18, 12, 12);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
        g2.drawString("3", W - 18, taskbarY + 27);
    }
}