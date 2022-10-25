package io.quarkus.awt.it;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.IOException;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import org.jboss.logging.Logger;

@Path("")
public class GraphicsResource {

    private static final Logger LOG = Logger.getLogger(GraphicsResource.class);

    @Path("/graphics")
    @GET
    public Response graphics(@QueryParam("entrypoint") String entrypoint) throws IOException {
        if ("IIORegistry".equals(entrypoint)) {
            IIORegistry.getDefaultInstance()
                    .getServiceProviders(ImageReaderSpi.class, true)
                    .forEachRemaining(reader -> LOG.infof("Available image reader: %s",
                            reader.getDescription(Locale.TAIWAN)));
        } else if ("GraphicsEnvironment".equals(entrypoint)) {
            final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            for (Font f : ge.getAllFonts()) {
                LOG.info(f.getFamily());
            }
        } else if ("Color".equals(entrypoint)) {
            final Color[] colors = new Color[] {
                    Color.WHITE, Color.RED, Color.GREEN, Color.BLUE, Color.BLACK,
                    new Color(190, 32, 40, 100),
                    new Color(Color.HSBtoRGB(20, 200, 30)) };
            for (Color c : colors) {
                LOG.infof("Color %s", c.toString());
            }
        } else if ("BufferedImage".equals(entrypoint)) {
            final Graphics2D g = new BufferedImage(10, 10, BufferedImage.TYPE_3BYTE_BGR).createGraphics();
            LOG.infof("Graphics2D: ", g.toString());
        } else if ("Transformations".equals(entrypoint)) {
            final AffineTransform af = AffineTransform.getRotateInstance(Math.toRadians(5), 1, 1);
            LOG.infof("Transform:  %s", af.toString());
        } else if ("ConvolveOp".equals(entrypoint)) {
            final ConvolveOp cop = new ConvolveOp(new Kernel(1, 1, new float[] { 0f }), ConvolveOp.EDGE_NO_OP, null);
            LOG.infof("ConvolveOp: %s", cop.toString());
        } else if ("Font".equals(entrypoint)) {
            final Font f = new Font("Arial", Font.PLAIN, 16);
            LOG.infof("Font: %s", f.getFamily());
        } else if ("Path2D".equals(entrypoint)) {
            final Path2D p = new Path2D.Double();
            LOG.infof("Path2D: %s", p.toString());
        } else if ("ImageReader".equals(entrypoint)) {
            final ImageReader p = ImageIO.getImageReadersByFormatName("JPEG").next();
            LOG.infof("ImageReader: %s", p.toString());
        } else if ("ImageWriter".equals(entrypoint)) {
            final ImageWriter p = ImageIO.getImageWritersByFormatName("GIF").next();
            LOG.infof("ImageWriter: %s", p.toString());
        }
        return Response.ok().build();
    }
}
