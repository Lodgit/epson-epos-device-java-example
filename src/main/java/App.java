import java.util.Arrays;

public class App {
    static EposClient client;

    public static void main(String[] args) {
        String ipAddress;
        String deviceIDprinter;

        // Only two arguments are supported
        // E.g: --address=10.10.10.10 --deviceid=printer_id
        if (args.length == 2) {
            String[] flag;

            flag = args[0].split("=");
            ipAddress = flag[1];

            flag = args[1].split("=");
            deviceIDprinter = flag[1];

            System.out.println("Arguments used: " + Arrays.toString(args));

            // --- Printing data initialization

            EposInvoiceOptions opts = new EposInvoiceOptions();
            opts.textLang = "de";
            opts.textSmooth = "true";

            EposInvoiceData invoice = new EposInvoiceData(opts);

            // 1. Header
            //  -- Logo (centered)
            invoice.addLogo("32", "33");
//            invoice.addImage("./files/lodgit.bmp", 8, 8, "center");
            //  -- Empty line
            invoice.addEmptyLines(1);
            //  -- Description
            invoice.addText("Lodgit Hotelsoftware GmbH", "center");
            invoice.addText("Industriestr. 95 (Aufgang D)", "center");
            invoice.addText("04229 Leipzig", "center");
            invoice.addText("Tel. +493414206944", "center");
            // End Header
            invoice.addEmptyLines(1);

            // Page 1 (Items header)
            invoice.startPage();
            invoice.addArea(20, 0, 540, 60);

            invoice.addPosition(0, 40);
            invoice.addBoldText("Anz.", "right");

            invoice.addPosition(80, 40);
            invoice.addBoldText("Artikelname", "left");

            invoice.addPosition(380, 40);
            invoice.addBoldText("Preis €", "right");

            // Empty line
            invoice.addEmptyLines(1);
            invoice.endPage();

            // -- Page 2 (Items body)

            invoice.startPage();
            invoice.addArea(20, 0, 540, 100);

            // Item 1 (y=40)
            invoice.addPosition(0, 40);
            invoice.addText("1", "right");

            invoice.addPosition(80, 40);
            invoice.addText("KLC. Müllbeutel 60L", "left");

            invoice.addPosition(380, 40);
            invoice.addText("1,35", "right");

            // Item 2 (y=80)
            invoice.addPosition(0, 70);
            invoice.addText("2", "right");

            invoice.addPosition(80, 70);
            invoice.addText("K. Gouda jung Stük", "left");

            invoice.addPosition(380, 70);
            invoice.addText("10,48", "right");

            // Empty line
            invoice.addEmptyLines(1);
            invoice.endPage();

            // 2. Products details

            // Footer
            // invoice.addQRCode("https://www.lodgit-hotelsoftware.de/", "center");

//            System.out.println(invoice.toXML());

            printEposData(ipAddress, deviceIDprinter, invoice.toXML());
        } else {
            System.err.println("There are arguments missing.");
            System.exit(1);
        }
    }

    private static void printEposData(String ipAddress, String deviceIDprinter, String dataToPrint) {
        client = new EposClient(ipAddress, deviceIDprinter, dataToPrint);
        client.connect();
    }
}
