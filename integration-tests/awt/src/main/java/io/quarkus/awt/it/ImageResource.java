package io.quarkus.awt.it;

import static io.quarkus.awt.it.enums.ColorSpaceEnum.CS_DEFAULT;
import static java.awt.image.BufferedImage.TYPE_3BYTE_BGR;
import static java.awt.image.BufferedImage.TYPE_BYTE_BINARY;
import static javax.imageio.ImageWriteParam.MODE_DEFAULT;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.function.Supplier;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.plugins.tiff.BaselineTIFFTagSet;
import javax.imageio.plugins.tiff.TIFFDirectory;
import javax.imageio.plugins.tiff.TIFFField;
import javax.imageio.plugins.tiff.TIFFTag;
import javax.imageio.stream.ImageOutputStream;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import io.quarkus.awt.it.enums.ColorSpaceEnum;
import io.quarkus.awt.it.enums.ImageType;

@Path("")
public class ImageResource {

    @Inject
    Application application;

    /**
     * DECODERS
     *
     * Converts any JDK supported image types to PNG,
     * reading metadata along the way. The point is to
     * exercise code that needs reflection and JNI access
     * in packages such as javax.imageio.plugins.
     *
     * @param data images in JDK supported formats
     * @param filename name of the file, used to get the extension and mark log
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Path("/topng/{filename}")
    public Response image(MultipartFormDataInput data, @PathParam("filename") String filename) throws IOException {
        try (final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                final InputStream bin = data.getFormDataMap().get("image").get(0).getBody(InputStream.class, null)) {
            String colorSpaceName = null;
            String compressionName = null;
            // There are image readers only for jpg, tiff, bmp, gif, wbmp and png.
            final String extension = filename.split("\\.")[1];
            if ("jp2".equals(extension)) {
                final BufferedImage image = ImageIO.read(bin);
                colorSpaceName = image.getColorModel().getColorSpace().toString();
                ImageIO.write(image, "PNG", bos);
            } else {
                final ImageReader imageReader = ImageIO.getImageReadersByFormatName(extension).next();
                imageReader.setInput(
                        ImageIO.createImageInputStream(bin),
                        true);
                // Reads both image data and metadata. It exposes code paths in e.g. TIFF plugin.
                final IIOImage iioimg = imageReader.readAll(0, imageReader.getDefaultReadParam());
                colorSpaceName = iioimg.getRenderedImage().getColorModel().getColorSpace().toString();
                final IIOMetadataNode root = (IIOMetadataNode) iioimg.getMetadata()
                        .getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName);
                // To read some attributes:
                final IIOMetadataNode compressionNode = (IIOMetadataNode) root.getElementsByTagName("CompressionTypeName")
                        .item(0);
                if (compressionNode != null) {
                    compressionName = compressionNode.getAttribute("value");
                }
                ImageIO.write(iioimg.getRenderedImage(), "PNG", bos);
            }

            return Response
                    .accepted()
                    .type(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                    .entity(bos.toByteArray())
                    .header("color-space", colorSpaceName)
                    .header("compression", compressionName)
                    .build();
        }
    }

    /**
     * ENCODERS
     *
     * Endpoint creates a simple image of a square within a square
     * and encodes this image with various encoders, compressors and
     * colour models according to the request.
     *
     * The aim is to exhaust common code paths in native-image initialization.
     *
     * @param type image type, e.g. TYPE_INT_ARGB_PRE
     * @param format e.g. GIF
     * @param cs e.g. CS_sRGB
     * @param compress e.g. LZW
     * @return encoded image
     */
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Path("/generate/{type}/{format}/{cs}/{compress}")
    public Response image(
            @PathParam("type") ImageType type,
            @PathParam("format") String format,
            @PathParam("cs") ColorSpaceEnum cs,
            @PathParam("compress") String compress) throws IOException {

        final BufferedImage img = simpleRectangular(type);
        // Used only in TIFF, but we calculate the ColorModel for others too to touch the code path.
        final BufferedImage result = imgDifferentColorModel(cs, img, format);
        final ImageWriter writer = ImageIO.getImageWritersByFormatName(format).next();

        try (final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                final ImageOutputStream output = ImageIO.createImageOutputStream(bos)) {
            writer.setOutput(output);

            final ImageWriteParam params = writer.getDefaultWriteParam();
            if (!"Default".equalsIgnoreCase(compress)) {
                // The compression is set, not left to the default.
                params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                // Fed directly from the api, no check, it's the test code's responsibility to pick valid values.
                params.setCompressionType(compress);
                // This value is interpreted wildly differently among various compressions.
                // It could be a fraction between 0 and 1, it could also be rounded to the closest
                // integer to pick from a range of setting e.g. between 1 and 6.
                // 0.6 does "something" for all used compressions and that is good enough for the exercise here.
                params.setCompressionQuality(0.6f);
            }

            // We manually write some additional metadata
            // and touch ImageTypeSpecifier class.
            final IIOMetadata iioMetadata = writer.getDefaultImageMetadata(new ImageTypeSpecifier(img), params);
            final String[] desc = new String[] { "Some totally legit description.", "Here it comes." };

            if ("TIFF".equalsIgnoreCase(format)) {
                writeTIFF(writer, iioMetadata, params, desc, img, result);
            } else if ("PNG".equalsIgnoreCase(format)) {
                writePNG(writer, iioMetadata, params, desc, img);
            } else if ("JPEG".equalsIgnoreCase(format)) {
                writeJPEG(writer, iioMetadata, params, desc, img);
            } else if ("GIF".equalsIgnoreCase(format)) {
                writeGIF(writer, iioMetadata, params, desc, img);
            } else {
                //BMP and WBMP metadata are immutable, and we don't replace them with anything, so use the default:
                writer.write(null, new IIOImage(img, null, iioMetadata), params);
            }

            output.flush();
            writer.dispose();

            return Response
                    .accepted()
                    .type(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                    .header("Content-Disposition", "attachment; filename=\"picture." + format.toLowerCase() + "\"")
                    .entity(bos.toByteArray())
                    .build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Path("/graphics/{format}")
    public Response image(@PathParam("format") String format) throws IOException {
        final BufferedImage img = application.getABGRTestImage();
        try (final ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            switch (format) {
                // Handles transparency
                case "TIFF":
                case "GIF":
                case "PNG":
                    ImageIO.write(img, format, bos);
                    break;
                case "JPG":
                case "BMP":
                    // Doesn't handle transparency.
                    final BufferedImage imgBGR = new BufferedImage(img.getWidth(), img.getHeight(), TYPE_3BYTE_BGR);
                    imgBGR.getGraphics().drawImage(img, 0, 0, null);
                    ImageIO.write(imgBGR, format, bos);
                    break;
                case "WBMP":
                    // Handles neither transparency nor colours, it's monochrome.
                    final BufferedImage imgBINARY = new BufferedImage(img.getWidth(), img.getHeight(), TYPE_BYTE_BINARY);
                    imgBINARY.getGraphics().drawImage(img, 0, 0, null);
                    ImageIO.write(imgBINARY, format, bos);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown image format " + format);
            }

            return Response
                    .accepted()
                    .type(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                    .header("Content-Disposition", "attachment; filename=\"picture." + format.toLowerCase() + "\"")
                    .entity(bos.toByteArray())
                    .build();
        }
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/fonts")
    public Response fonts() {
        return Response.ok().entity(Arrays.toString(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames())).build();
    }

    /**
     * Prepares a TIFF with two images and image description metadata.
     *
     * @param writer Image writer
     * @param iioMetadata metadata to be manipulated
     * @param params settings for the image encoding, e.g. compression, tiling etc.
     * @param desc some text we bake in the metadata
     * @param img image one
     * @param result image two
     */
    public static void writeTIFF(ImageWriter writer, IIOMetadata iioMetadata, ImageWriteParam params, String[] desc,
            BufferedImage img, BufferedImage result) throws IOException {
        // https://docs.oracle.com/javase/10/docs/api/javax/imageio/metadata/doc-files/tiff_metadata.html
        params.setTilingMode(MODE_DEFAULT);
        final TIFFDirectory descDir = TIFFDirectory.createFromMetadata(iioMetadata);
        descDir.addTIFFField(new TIFFField(new TIFFTag("ImageDescription",
                BaselineTIFFTagSet.TAG_IMAGE_DESCRIPTION, 1 << TIFFTag.TIFF_ASCII), TIFFTag.TIFF_ASCII, desc.length,
                desc));
        // Write the data part
        // 2 images in the TIFF container:
        writer.prepareWriteSequence(null);
        writer.writeToSequence(new IIOImage(img, null, descDir.getAsMetadata()), params);
        writer.writeToSequence(new IIOImage(result, null, descDir.getAsMetadata()), params);
        writer.endWriteSequence();
    }

    /**
     * Prepares a PNG with description metadata.
     *
     * @param writer Image writer
     * @param iioMetadata metadata to be manipulated
     * @param params settings for the image encoding, e.g. compression
     * @param desc some text we bake in the metadata
     * @param img image
     */
    public static void writePNG(ImageWriter writer, IIOMetadata iioMetadata, ImageWriteParam params, String[] desc,
            BufferedImage img) throws IOException {
        // https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/javax/imageio/metadata/doc-files/png_metadata.html
        // Metadata is mutable, we can just add to the tree:
        final IIOMetadataNode textEntry = new IIOMetadataNode("tEXtEntry");
        textEntry.setAttribute("keyword", "ImageDescription");
        textEntry.setAttribute("value", String.join("\n", desc));
        final IIOMetadataNode text = new IIOMetadataNode("tEXt");
        text.appendChild(textEntry);
        final IIOMetadataNode root = new IIOMetadataNode("javax_imageio_png_1.0");
        root.appendChild(text);
        iioMetadata.mergeTree("javax_imageio_png_1.0", root);
        writer.write(iioMetadata, new IIOImage(img, null, iioMetadata), params);
    }

    /**
     * Prepares a JPEG with description metadata.
     *
     * @param writer Image writer
     * @param iioMetadata metadata to be manipulated
     * @param params settings for the image encoding, e.g. compression
     * @param desc some text we bake in the metadata
     * @param img image
     */
    public static void writeJPEG(ImageWriter writer, IIOMetadata iioMetadata, ImageWriteParam params, String[] desc,
            BufferedImage img) throws IOException {
        // JPEG metadata is a bit of a mayhem. It's just a container and the metadata are TIFF like nodes.
        // An EXIF blob is not used for brevity, so we used the com node instead.
        // See com node: https://docs.oracle.com/javase/7/docs/api/javax/imageio/metadata/doc-files/jpeg_metadata.html
        final IIOMetadataNode root = (IIOMetadataNode) iioMetadata.getAsTree("javax_imageio_jpeg_image_1.0");
        final IIOMetadataNode markerSequence = (IIOMetadataNode) root.getElementsByTagName("markerSequence").item(0);
        final IIOMetadataNode comment = new IIOMetadataNode("com");
        comment.setAttribute("comment", String.join("\n", desc));
        markerSequence.appendChild(comment);
        iioMetadata.mergeTree("javax_imageio_jpeg_image_1.0", root);
        writer.write(iioMetadata, new IIOImage(img, null, iioMetadata), params);
    }

    /**
     * Prepares a GIF with description metadata.
     *
     * @param writer Image writer
     * @param iioMetadata metadata to be manipulated
     * @param params settings for the image encoding, e.g. compression
     * @param desc some text we bake in the metadata
     * @param img image
     */
    public static void writeGIF(ImageWriter writer, IIOMetadata iioMetadata, ImageWriteParam params, String[] desc,
            BufferedImage img) throws IOException {
        // https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/javax/imageio/metadata/doc-files/gif_metadata.html
        final IIOMetadataNode root = (IIOMetadataNode) iioMetadata.getAsTree("javax_imageio_gif_image_1.0");
        final IIOMetadataNode commentsNode = new IIOMetadataNode("CommentExtensions");
        root.appendChild(commentsNode);
        commentsNode.setAttribute("CommentExtension", String.join("\n", desc));
        iioMetadata.mergeTree("javax_imageio_gif_image_1.0", root);
        writer.write(iioMetadata, new IIOImage(img, null, iioMetadata), params);
    }

    /**
     * Small, simple, rectangular image.
     *
     * **DO NOT** edit what this method does without altering
     * encoders_test_config.txt too. There are recorded pixel values
     * at positions x:25 y:25 and x:2 y:2 respectively to sanity check
     * the output.
     *
     * @param type type sets the main layout, e.g. RGBA or grayscale.
     * @return image
     */
    public static BufferedImage simpleRectangular(ImageType type) {
        final BufferedImage img = new BufferedImage(50, 50, type.code);
        final Graphics2D g = img.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        g.setColor(Color.RED);
        g.fillRect(10, 10, img.getWidth() - 20, img.getHeight() - 20);
        g.dispose();
        return img;
    }

    /**
     * There is not much sense in this conversion except
     * for touching relevant code paths to make sure native-image
     * is not missing any initialization.
     *
     * @param cs Color space
     * @param img input image
     * @param format image file format
     * @return image with a converted color model, if possible
     */
    public static BufferedImage imgDifferentColorModel(ColorSpaceEnum cs, BufferedImage img, String format) {
        final ColorModel colorModel = ((Supplier<ColorModel>) (() -> {
            if (cs != CS_DEFAULT) {
                if ("JPEG".equalsIgnoreCase(format) || "BMP".equalsIgnoreCase(format) || "WBMP".equalsIgnoreCase(format)) {
                    return new ComponentColorModel(
                            ColorSpace.getInstance(cs.code), false, false, Transparency.BITMASK,
                            img.getRaster().getDataBuffer().getDataType());
                } else {
                    return new ComponentColorModel(
                            ColorSpace.getInstance(cs.code), true, true, Transparency.OPAQUE,
                            img.getRaster().getDataBuffer().getDataType());
                }
            }
            return img.getColorModel();
        })).get();
        final WritableRaster writableRaster = colorModel.createCompatibleWritableRaster(img.getRaster().getWidth(),
                img.getRaster().getHeight());
        final BufferedImage result = new BufferedImage(colorModel, writableRaster, true, null);
        final Graphics2D gx = (Graphics2D) result.getGraphics();
        gx.setComposite(AlphaComposite.SrcOver);
        gx.drawRenderedImage(img, null);
        gx.dispose();
        return result;
    }
}
