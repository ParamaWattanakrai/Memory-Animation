import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import javax.swing.*;

/**
 * Demo: morphing one script into another (Khmer "ភាសា" -> Thai "ภาษา",
 * both meaning "language") using Java 2D.
 *
 * TRUE point-based morphing between two unrelated scripts has no universal
 * solution -- there is no natural "this stroke of the Khmer letter matches
 * that stroke of the Thai letter" correspondence. That's why the very first
 * version of this demo (blending vertex i of the whole Khmer string with
 * vertex i of the whole Thai string, treating each as one giant loop)
 * produced self-intersecting, torn shapes: it was mixing up unrelated parts
 * of unrelated letters.
 *
 * This version is a closer, still-imperfect approximation of a morph:
 *   1. Extract each glyph's CONTOURS separately. A contour is one closed
 *      loop -- a letter's outer boundary, or a hole/counter inside it.
 *   2. Sort each script's contours by area, largest first, and pair them
 *      up index-by-index. This is a heuristic (not a true structural
 *      match), but it reliably pairs big letter bodies with other big
 *      letter bodies, and small holes with other small holes.
 *   3. Where the contour counts don't match, unmatched contours shrink
 *      into (or grow out of) a single point instead of being forced onto
 *      an unrelated shape.
 *   4. Resample and interpolate EACH CONTOUR INDEPENDENTLY -- never the
 *      whole string as one path. A single closed loop morphing into
 *      another single closed loop stays well-behaved. It can look like
 *      letters "melting" into each other rather than a crisp per-letter
 *      swap, and a hole may visually jump between letters mid-transition
 *      (the pairing is by size, not by meaning) -- but it will not
 *      self-intersect or tear the way the whole-string version did.
 *
 * A straight accent line (Lab 2), a Bezier flourish (Lab 3), and a pulsing
 * midpoint circle (Lab 4) round out the required primitives.
 *
 * NOTE: this is a technique demo, not a finished submission. Adapt the
 * theme, add your "MY MEMORIES" content, and build it out with your
 * partner before submitting.
 */
public class TextMorphDemo extends JPanel implements Runnable {

    static final String TEXT_A = "ភាសា"; // Khmer: "language"
    static final String TEXT_B = "ภาษา"; // Thai: "language"
    static final float FONT_SIZE = 160f;
    static final int POINTS_PER_CONTOUR = 60;

    List<List<Point2D>> resampledA, resampledB; // resampledA.get(i) morphs into resampledB.get(i)

    double totalTime = 0;
    static final double TRANSITION = 1.5; // seconds to morph
    static final double HOLD = 1.5;       // seconds to hold on each word
    static final double CYCLE = 2 * TRANSITION + 2 * HOLD;

    public static void main(String[] args) {
        TextMorphDemo m = new TextMorphDemo();

        JFrame f = new JFrame();
        f.add(m);
        f.setTitle("Text Morph Demo");
        f.setSize(600, 600);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setVisible(true);

        (new Thread(m)).start();
    }

    public TextMorphDemo() {
        Font fontA = findFontFor(TEXT_A, FONT_SIZE);
        Font fontB = findFontFor(TEXT_B, FONT_SIZE);

        List<List<Point2D>> contoursA = getContours(TEXT_A, fontA);
        List<List<Point2D>> contoursB = getContours(TEXT_B, fontB);

        // Largest first, so big letter bodies pair with big letter bodies
        // and small holes pair with small holes -- as well as a simple
        // area-based heuristic can manage.
        contoursA.sort((c1, c2) -> Double.compare(area(c2), area(c1)));
        contoursB.sort((c1, c2) -> Double.compare(area(c2), area(c1)));

        int n = Math.max(contoursA.size(), contoursB.size());
        resampledA = new ArrayList<>();
        resampledB = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            List<Point2D> ca = i < contoursA.size() ? contoursA.get(i) : null;
            List<Point2D> cb = i < contoursB.size() ? contoursB.get(i) : null;

            if (ca != null && cb != null) {
                resampledA.add(resampleContour(ca, POINTS_PER_CONTOUR));
                resampledB.add(resampleContour(cb, POINTS_PER_CONTOUR));
            } else if (ca != null) {
                // B has no counterpart here: this contour shrinks to a point.
                resampledA.add(resampleContour(ca, POINTS_PER_CONTOUR));
                resampledB.add(collapse(centroid(ca), POINTS_PER_CONTOUR));
            } else {
                // A has no counterpart here: this contour grows from a point.
                resampledB.add(resampleContour(cb, POINTS_PER_CONTOUR));
                resampledA.add(collapse(centroid(cb), POINTS_PER_CONTOUR));
            }
        }
    }

    // ---------------------------------------------------------------
    // Find an installed font that can render every character in `text`.
    // ---------------------------------------------------------------
    private Font findFontFor(String text, float size) {
        String[] candidates = {
                "Leelawadee UI", "Leelawadee", "Tahoma",
                "Khmer UI", "Khmer OS", "Khmer OS System",
                "Noto Sans Thai", "Noto Sans Khmer",
                "Segoe UI", "Arial Unicode MS"
        };
        for (String name : candidates) {
            Font f = new Font(name, Font.PLAIN, (int) size);
            if (!f.getFamily().equals(name)) continue; // not actually installed
            if (f.canDisplayUpTo(text) == -1) return f;
        }
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        for (String name : ge.getAvailableFontFamilyNames()) {
            Font f = new Font(name, Font.PLAIN, (int) size);
            if (f.canDisplayUpTo(text) == -1) return f;
        }
        System.err.println("WARNING: no installed font could fully display \"" + text
                + "\" -- install one with the right script coverage (e.g. Noto Sans "
                + "Khmer / Noto Sans Thai), then rerun.");
        return new Font("SansSerif", Font.PLAIN, (int) size);
    }

    // ---------------------------------------------------------------
    // Extract every contour of a string's glyph outline as its own point
    // list, centred on (0,0) so differently-sized words still line up.
    // ---------------------------------------------------------------
    private List<List<Point2D>> getContours(String text, Font font) {
        FontRenderContext frc = new FontRenderContext(null, true, true);
        TextLayout layout = new TextLayout(text, font, frc);
        Rectangle2D bounds = layout.getBounds();
        double cx = bounds.getX() + bounds.getWidth() / 2.0;
        double cy = bounds.getY() + bounds.getHeight() / 2.0;
        Shape outline = layout.getOutline(AffineTransform.getTranslateInstance(-cx, -cy));

        List<List<Point2D>> contours = new ArrayList<>();
        List<Point2D> current = null;
        PathIterator pi = outline.getPathIterator(null, 1.0); // flatness = 1px
        double[] coords = new double[6];
        while (!pi.isDone()) {
            int type = pi.currentSegment(coords);
            if (type == PathIterator.SEG_MOVETO) {
                current = new ArrayList<>();
                current.add(new Point2D.Double(coords[0], coords[1]));
                contours.add(current);
            } else if (type == PathIterator.SEG_LINETO) {
                if (current != null) current.add(new Point2D.Double(coords[0], coords[1]));
            } else if (type == PathIterator.SEG_CLOSE) {
                if (current != null && !current.isEmpty()) current.add(current.get(0));
            }
            pi.next();
        }
        return contours;
    }

    private double area(List<Point2D> c) {
        double a = 0;
        for (int i = 0; i < c.size() - 1; i++) {
            Point2D p1 = c.get(i), p2 = c.get(i + 1);
            a += p1.getX() * p2.getY() - p2.getX() * p1.getY();
        }
        return Math.abs(a) / 2.0;
    }

    private Point2D centroid(List<Point2D> c) {
        double sx = 0, sy = 0;
        for (Point2D p : c) { sx += p.getX(); sy += p.getY(); }
        return new Point2D.Double(sx / c.size(), sy / c.size());
    }

    private List<Point2D> collapse(Point2D p, int k) {
        List<Point2D> r = new ArrayList<>(k);
        for (int i = 0; i < k; i++) r.add(p);
        return r;
    }

    // Resample ONE closed contour to exactly k points, evenly spaced by arc
    // length. Since this only ever runs on a single loop (never the whole
    // string at once), there's no cross-contour "bridge" problem here.
    private List<Point2D> resampleContour(List<Point2D> path, int k) {
        if (path.size() < 2) {
            return collapse(path.isEmpty() ? new Point2D.Double(0, 0) : path.get(0), k);
        }

        double[] cumLen = new double[path.size()];
        for (int i = 1; i < path.size(); i++) {
            cumLen[i] = cumLen[i - 1] + path.get(i - 1).distance(path.get(i));
        }
        double total = cumLen[path.size() - 1];
        if (total == 0) total = 1;

        List<Point2D> result = new ArrayList<>(k);
        int seg = 1;
        for (int i = 0; i < k; i++) {
            double target = total * i / (double) (k - 1);
            while (seg < path.size() - 1 && cumLen[seg] < target) seg++;
            double segStart = cumLen[seg - 1], segEnd = cumLen[seg];
            double t = (segEnd - segStart == 0) ? 0 : (target - segStart) / (segEnd - segStart);
            Point2D a = path.get(seg - 1), b = path.get(seg);
            result.add(new Point2D.Double(
                    a.getX() + (b.getX() - a.getX()) * t,
                    a.getY() + (b.getY() - a.getY()) * t));
        }
        return result;
    }

    // ---------------------------------------------------------------
    // game loop (same pattern as Lab 5)
    // ---------------------------------------------------------------
    @Override
    public void run() {
        double lastTime = System.currentTimeMillis();
        while (true) {
            double now = System.currentTimeMillis();
            totalTime += (now - lastTime) / 1000.0;
            lastTime = now;
            repaint();
            try {
                Thread.sleep(1000 / 60);
            } catch (InterruptedException e) {
                System.err.println(e);
            }
        }
    }

    // morph progress in [0,1]: 0 = fully A, 1 = fully B. Loops A -> B -> A
    // with smoothstep easing.
    private double morphT() {
        double tt = totalTime % CYCLE;
        double t;
        if (tt < HOLD) {
            t = 0;
        } else if (tt < HOLD + TRANSITION) {
            double p = (tt - HOLD) / TRANSITION;
            t = p * p * (3 - 2 * p);
        } else if (tt < 2 * HOLD + TRANSITION) {
            t = 1;
        } else {
            double p = (tt - 2 * HOLD - TRANSITION) / TRANSITION;
            t = 1 - p * p * (3 - 2 * p);
        }
        return t;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        BufferedImage buffer = new BufferedImage(601, 601, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = buffer.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(173, 216, 230)); // light blue background
        g2.fillRect(0, 0, 600, 600);

        // --- decorative pulsing circle, from Lab 4 ---
        g2.setColor(new Color(255, 255, 255, 90));
        int pulse = (int) (140 + 20 * Math.sin(totalTime * 2));
        midpointCircle(g2, 300, 300, pulse);

        // --- the actual morph: blend each contour pair independently, then
        //     fill the union with the even-odd rule so holes stay open ---
        double t = morphT();
        Path2D.Double path = new Path2D.Double(Path2D.WIND_EVEN_ODD);
        for (int ci = 0; ci < resampledA.size(); ci++) {
            List<Point2D> a = resampledA.get(ci), b = resampledB.get(ci);
            for (int j = 0; j < a.size(); j++) {
                double x = a.get(j).getX() + (b.get(j).getX() - a.get(j).getX()) * t + 300;
                double y = a.get(j).getY() + (b.get(j).getY() - a.get(j).getY()) * t + 300;
                if (j == 0) path.moveTo(x, y); else path.lineTo(x, y);
            }
            path.closePath();
        }
        g2.setColor(Color.WHITE);
        g2.fill(path);

        // --- a straight accent line, from Lab 2 ---
        bresenhamLine(g2, 60, 460, 540, 460);

        // --- a Bezier flourish, from Lab 3 ---
        bezierCurve(g2, 80, 420, 200, 470, 400, 370, 520, 420);

        g.drawImage(buffer, 0, 0, null);
    }

    // ---------------------------------------------------------------
    // primitives reused from earlier labs
    // ---------------------------------------------------------------
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

    private void bezierCurve(Graphics g, int x1, int y1, int x2, int y2, int x3, int y3, int x4, int y4) {
        for (int i = 0; i <= 1000; i++) {
            double t = i / 1000.0;
            double x = Math.pow(1 - t, 3) * x1 + 3 * t * Math.pow(1 - t, 2) * x2
                    + 3 * Math.pow(t, 2) * (1 - t) * x3 + Math.pow(t, 3) * x4;
            double y = Math.pow(1 - t, 3) * y1 + 3 * t * Math.pow(1 - t, 2) * y2
                    + 3 * Math.pow(t, 2) * (1 - t) * y3 + Math.pow(t, 3) * y4;
            plot(g, (int) Math.round(x), (int) Math.round(y));
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
}