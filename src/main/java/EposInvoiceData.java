import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class EposInvoiceData {
    private EposInvoiceOptions opts;
    private List<String> data;
    private static Integer MAX_UNIT_DOTS = 65535;

    public EposInvoiceData(EposInvoiceOptions options) {
        this.opts = options;
        this.data = new ArrayList<String>();
    }

    void addLogo(String key1, String key2) {
        this.data.add("<logo " +
                "key1=\"" + key1 + "\" " +
                "key2=\"" + key2 + "\" " +
                "align=\"center\" />");
    }

    /**
     * Note: This functions is not working yet
     */
    void addImage(String imagePath, Integer width, Integer height, String align) {
        BufferedImage img;

        try {
            img = ImageIO.read(new File(imagePath));

            String base64 = imgToBase64String(img);

            this.data.add("<image " +
                    "width=\"" + width.toString() + "\" " +
                    "height=\"" + height.toString() + "\" " +
                    "color=\"color_1\"" +
                    "align=\"" + align + "\" " +
                    ">" + base64 + "</image>");
        } catch (IOException e) {
            System.out.println("Image is not was not possible to load.");
            System.exit(1);
            e.printStackTrace();
        }
    }

    void addText(String str, String align) {
        this.data.add("<text " +
                "lang=\"" + opts.textLang + "\" " +
                "smooth=\"" + opts.textSmooth + "\" " +
                "align=\"" + align + "\" " +
                "em=\"false\"" +
                ">" + str + "&#10;</text>");
    }

    void addBoldText(String str, String align) {
        this.data.add("<text " +
                "lang=\"" + opts.textLang + "\" " +
                "smooth=\"" + opts.textSmooth + "\" " +
                "align=\"" + align + "\" " +
                "em=\"true\"" +
                ">" + str + "&#10;</text>");
    }

    void addQRCode(String str, String align) {
        this.data.add("<symbol " +
                "type=\"qrcode_model_1\" " +
                "level=\"default\" " +
                "width=\"3\" " +
                "height=\"0\" " +
                "align=\"" + align + "\" " +
                "size=\"0\">" + str + "</symbol>");
    }

    void addEmptyLines(Integer lines) {
        this.data.add("<feed line=\"" + lines.toString() + "\" />");
    }

    void startPage() {
        this.data.add("<page>");
    }

    void endPage() {
        this.data.add("</page>");
    }

    void addDirection() {
        this.data.add("<direction dir=\"left_to_\" />");
    }

    void addPosition(Integer x, Integer y) {
        this.data.add("<position x=\"" + x.toString() + "\" y=\"" + y.toString() + "\" />");
    }

    void addArea(Integer x, Integer y, Integer width, Integer height) {
        this.data.add("<area " +
                "x=\"" + x.toString() + "\" y=\"" + y.toString() + "\" " +
                "width=\"" + width.toString() + "\" height=\"" + height.toString() + "\" />");
    }

    /**
     * Convert all data into xml string
     */
    public String toXML() {
        return String.join("", this.data);
    }

    private static String imgToBase64String(BufferedImage img) {
        WritableRaster raster = img.getRaster();
        DataBufferByte data = (DataBufferByte) raster.getDataBuffer();
        return Base64.getEncoder().encodeToString(data.getData());
    }
}
