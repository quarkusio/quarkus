package io.quarkus.awt.it;

import static java.awt.image.BufferedImage.TYPE_4BYTE_ABGR;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.awt.image.RescaleOp;
import java.io.IOException;
import java.util.Locale;
import java.util.Objects;

import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

import io.quarkus.runtime.Startup;

@Startup
@ApplicationScoped
public class Application {

    private static final Logger LOG = Logger.getLogger(Application.class);

    private BufferedImage abgrTestImage = null;

    @PostConstruct
    public void init() throws IOException, FontFormatException {
        // Touch image readers list
        IIORegistry.getDefaultInstance()
                .getServiceProviders(ImageReaderSpi.class, true)
                .forEachRemaining(reader -> LOG.infof("Available image reader: %s",
                        reader.getDescription(Locale.FRENCH)));

        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

        // Font source: https://ftp.gnu.org/gnu/freefont/
        // Note those fonts binaries were altered to bear different names, "My" prefix, "X" suffix.
        ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(
                Application.class.getResourceAsStream("/MyFreeMono.ttf"),
                "MyFreeMono.ttf not found.")));
        ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(
                Application.class.getResourceAsStream("/MyFreeSerif.ttf"),
                "MyFreeSerif.ttf not found.")));
        ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(
                Application.class.getResourceAsStream("/DejaVuSansMonoX.ttf"),
                "DejaVuSansMonoX.ttf not found.")));
    }

    public BufferedImage getABGRTestImage() throws IOException {
        // Why not in init()? Because it then could fail tests not related to e.g. blurring.
        if (abgrTestImage == null) {
            abgrTestImage = createABGRTestImage();
        }
        return abgrTestImage;
    }

    /**
     * Creates a test image a thumbnail strip of which is located in the doc directory
     * of this project, where java2d.png shows three interpretations next
     * to each other.
     *
     * **DO NOT** edit this method unless you also update ImageGeometryFontsTest.java
     * where particular pixels are sampled to smoke test the resulting imagery.
     * i.e. if you remove a rendered text or a shape from the image, the test
     * might fail unless you altered a part that is not sampled.
     *
     * See ImageGeometryFontsTest.java
     *
     * @return a test image
     */
    private static BufferedImage createABGRTestImage() {
        final int dx = 50; // times number of colours below, i.e. 350px
        final int h = 300;
        final Color[] colors = new Color[] {
                Color.WHITE, Color.RED, Color.GREEN, Color.BLUE, Color.BLACK,
                new Color(190, 32, 40, 100),
                new Color(Color.HSBtoRGB(20, 200, 30)) };
        final BufferedImage img = new BufferedImage(dx * colors.length, h, TYPE_4BYTE_ABGR);
        final Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Main rectangle
        g.setColor(Color.PINK);
        g.fillRect(0, 0, img.getWidth(), img.getHeight());

        // Small rectangles rotated
        final int wCenter = img.getWidth() / 2;
        final int hCenter = img.getHeight() / 2;
        final AffineTransform originalMatrix = g.getTransform();
        final AffineTransform af = AffineTransform.getRotateInstance(Math.toRadians(5), wCenter, hCenter);
        for (int i = 0; i < colors.length; i++) {
            g.setColor(colors[i]);
            g.fillRect(i * dx, 0, dx, h);
            g.transform(af);
        }
        g.setTransform(originalMatrix);

        // Transparent circle
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
        g.setColor(Color.MAGENTA);
        g.fillOval(0, 0, img.getWidth(), img.getHeight());
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

        // Some transformations and arbitrary render hints.
        // Note that these calls make little sense chained as such. We are merely touching relevant code paths.
        final float[] BLUR_ISH_KERNEL = {
                0.1f, 0.1f, 0.1f,
                0.1f, 0.5f, 0.1f,
                0.1f, 0.1f, 0.1f
        };
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
        final RenderingHints rhints = new RenderingHints(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        final ConvolveOp cop = new ConvolveOp(new Kernel(3, 3, BLUR_ISH_KERNEL), ConvolveOp.EDGE_NO_OP, rhints);
        g.drawImage(img, cop, 0, 0);
        final AffineTransform at = AffineTransform.getScaleInstance(1.9, 1.1);
        final AffineTransformOp aop = new AffineTransformOp(at, AffineTransformOp.TYPE_BICUBIC);
        g.drawImage(img, aop, 0, 0);
        final RescaleOp rop = new RescaleOp(0.2f, 1.0f, null);
        g.drawImage(img, rop, 0, 0);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

        // Text as glyph vector, draw bytes
        final FontRenderContext fontRenderContext = g.getFontRenderContext();
        g.setColor(Color.CYAN);
        final GlyphVector glyphVector = new Font("DejaVu Sans Mono X", Font.PLAIN, 16)
                .createGlyphVector(fontRenderContext, "Mandrel 1");
        g.drawGlyphVector(glyphVector, img.getWidth() / 2f, img.getHeight() / 2f);
        final byte[] bytesToDraw = new byte[] { 'M', 'A', 'N', 'D', 'R', 'E', 'L' };
        g.setFont(new Font("DejaVu Sans Mono X", Font.PLAIN, 16));
        g.drawBytes(bytesToDraw, 0, bytesToDraw.length, img.getWidth() - 75, img.getHeight() - 10);

        // Arcs
        g.setColor(Color.DARK_GRAY);
        g.drawArc(20, 20, 150, 150, 0, 90);
        g.setPaint(new GradientPaint(50, 50, Color.YELLOW, 150, 150, Color.WHITE));
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));
        g.fillArc(50, 50, 150, 150, 0, 90);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

        // Path2D, Arc2D, CubicCurve2D etc. plus intersection, plain draw string
        g.setColor(Color.YELLOW);
        final Path2D p = new Path2D.Double();
        p.moveTo(50, 50);
        p.quadTo(100, 0, 150, 100);
        g.draw(p);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));
        g.fill(new Arc2D.Double(20, 200, 40, 40, 90, 180, Arc2D.OPEN));
        g.draw(new CubicCurve2D.Double(30, 100, 150, 200, 100, 290, 280, 250));
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        final Ellipse2D.Double e = new Ellipse2D.Double(110, 230, 30, 20);
        g.draw(e);
        final RoundRectangle2D.Float outer = new RoundRectangle2D.Float(115, 215, 80, 80, 16, 16);
        final RoundRectangle2D.Float inner = new RoundRectangle2D.Float(120, 220, 70, 70, 10, 10);
        final Path2D pathOrRectangles = new Path2D.Float(Path2D.WIND_EVEN_ODD);
        pathOrRectangles.append(outer, false);
        pathOrRectangles.append(inner, false);
        g.fill(pathOrRectangles);
        g.setFont(new Font("DejaVu Sans Mono X", Font.BOLD, 15));
        g.drawString("Intersection: ", 120, 205);
        g.setFont(new Font("DejaVu Sans Mono X", Font.BOLD, 20));
        g.setColor(Color.RED);
        g.drawString((e.intersects(outer.getBounds2D()) || e.intersects(inner.getBounds2D())) ? "YES ❤" : "NO",
                img.getWidth() - 110, img.getHeight() - 88);

        // Polygons
        g.setColor(Color.GREEN);
        final Stroke sOrig = g.getStroke();
        g.setStroke(new BasicStroke(3f));
        g.drawLine(10, 280, 50, 280);
        g.setStroke(sOrig);
        g.setColor(Color.ORANGE);
        g.drawPolygon(new int[] { 288, 240, 284, 284, 300, 296, 328 }, new int[] { 13, 81, 57, 135, 135, 57, 81 }, 7);
        g.setColor(Color.CYAN);
        g.drawPolyline(new int[] { 290, 242, 286, 286, 302, 298, 330 }, new int[] { 15, 83, 59, 137, 137, 59, 82 }, 7);

        // Label, text, Font Metrics (font config init)
        g.setColor(Color.BLACK);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        // Name of the font is not just the name of the file. It is baked in it.
        g.setFont(new Font("MyFreeMono", Font.PLAIN, 15));
        g.drawString("Mandrel 3", 20, 20);
        g.setFont(new Font("MyFreeSerif", Font.PLAIN, 15));
        g.drawString("Mandrel 4", 20, 60);
        g.setFont(new Font("DejaVu Sans Mono X", Font.BOLD, 15));
        g.drawString("Mandrel 5", 20, 100);
        String text = "Quarkus Mandrel";
        g.setFont(new Font("MyFreeSerif", Font.PLAIN, 40));
        Rectangle2D strBound = g.getFontMetrics().getStringBounds(text, g);
        // Center of the text
        int strX = (int) (img.getMinX() + (img.getWidth() / 2) - (strBound.getWidth() / 2));
        int strY = (int) (img.getMinY() + (img.getHeight() / 2) + (strBound.getHeight() / 2));
        g.setColor(Color.DARK_GRAY); // shadow
        g.drawString(text, strX + 5, strY + 5);
        g.setColor(Color.ORANGE);
        g.drawString(text, strX, strY);

        g.dispose();
        return img;
    }
}
