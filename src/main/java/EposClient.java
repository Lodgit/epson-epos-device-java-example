import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class EposClient {
    private static final int PORT = 8009;
    private static final boolean BUFFER_ENABLED = true;
    private static final String TERMINATING_CHARACTER = "\0";

    private Socket connection = null;
    private BufferedReader reader = null;
    private BufferedWriter writer = null;
    private Document doc = null;

    private String ipAddress = "";
    private String deviceIDprinter = "";
    private String deviceIDscanner = "";
    private boolean printerOpened = false;

    private boolean connecting = false;
    private boolean disconnecting = false;
    private boolean reconnecting = false;
    private boolean canceledReconnection = false;
    private String clientID = null;
    private String dataID = null;

    public EposClient(String ipAddress, String deviceIDprinter) {
        this.ipAddress = ipAddress;
        this.deviceIDprinter = deviceIDprinter;
    }

    /**
     * Connect to ePOS-Device XML Server using Socket.
     */
    public void connect() {
        InetSocketAddress serverAddress = new InetSocketAddress(ipAddress, PORT);

        try {
            connection = new Socket();
            connection.connect(serverAddress, 5000);
        } catch (UnknownHostException ex) {
            appendConsole("Connecting to server failed due to unknown host.");
            ex.printStackTrace();

            connecting = false;
            connection = null;
//            enableButton(true);
            return;
        } catch (IOException ex) {
            appendConsole("Connecting to server failed due to io exception.");
            ex.printStackTrace();
            connecting = false;
            connection = null;
//            enableButton(true);
            return;
        }

        try {
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));

            connection.setSoTimeout(5000);

            // Receive reply message from server
            int chr;
            StringBuilder buffer = new StringBuilder();

            while ((chr = reader.read()) != 0) {
                buffer.append((char) chr);
            }

            // Parse received xml document(DOM)
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            String bufferStr = buffer.toString();
            doc = builder.parse(new ByteArrayInputStream(bufferStr.getBytes(StandardCharsets.UTF_8)));
            String firstNode = doc.getFirstChild().getNodeName();

            // Response of connect request
            if (firstNode.equals("connect")) {
                appendConsole("Connect to server success.");
                appendConsole("Connect XML Received: \n" + bufferStr);

                if (!reconnecting) {
                    // Disconnect old connection
                    if (clientID != null) {
                        disconnect();
                    }

                    new Thread(this::onReceive).start();

                    openPrinter();
                    // openScanner();
                }

                clientID = getChildren(doc, "client_id");

                // Change connect button to disconnect button ????
                connecting = false;
            } else {
                appendConsole("Connect to server failed.");
                disconnect();
            }
        } catch (SocketException ex) {
            appendConsole("Disconnected socket exception.");
            closeSocket();
            ex.printStackTrace();
        } catch (IOException ex) {
            appendConsole("Disconnected but io exception.");
            closeSocket();
            ex.printStackTrace();
        } catch (ParserConfigurationException ex) {
            appendConsole("XML parse error.");
            ex.printStackTrace();
        } catch (SAXException ex) {
            appendConsole("XML parse error sax exception.");
            ex.printStackTrace();
        }

        connecting = false;
    }

    /**
     * Send disconnect message to server.
     */
    public void disconnect() {
        String req = "<disconnect>" + "<data>"
                + "<client_id>" + clientID + "</client_id>"
                + "</data>" + "</disconnect>" + TERMINATING_CHARACTER;

        try {
            writer.write(req);
            writer.flush();
        } catch (IOException ex) {
            appendConsole("Disconnected from server!");
            closeSocket();
            ex.printStackTrace();
        }
    }

    /**
     * Reconnect to server.
     */
    public void reconnect() {
        reconnecting = true;
        String oldClinetID = clientID;
        String receivedDataID = dataID;

        appendConsole("Reconnecting to server...");

        // Cancel reconnection if connect button pushed ???
        closeSocket();

        do {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            connect();
        }
        while (connection == null && !canceledReconnection);

        canceledReconnection = false;
        reconnecting = false;

        if (connection == null) {
//            connectButton.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    if (connecting == false) {
//                        connecting = true;
//                        connectButton.setEnabled(false);
//                        new Thread(new Runnable() {
//                            @Override
//                            public void run() {
//                                connect();
//                            }
//                        }).start();
//                    }
//                }
//            });

//            setTexttoButton("connect");
//            enableButton(true);
            appendConsole("Canceled reconnection.");
        } else {
            String req = "<reconnect>" + "<data>"
                    + "<new_client_id>" + clientID + "</new_client_id>"
                    + "<old_client_id>" + oldClinetID + "</old_client_id>"
                    + "<received_id>" + receivedDataID + "</received_id>"
                    + "</data>" + "</reconnect>" + TERMINATING_CHARACTER;

            try {
                writer.write(req);
                writer.flush();
            } catch (IOException ex) {
                appendConsole("Disconnected");
                closeSocket();
                ex.printStackTrace();
            }
        }
    }

    /**
     * Close stream reader, writer and socket connection.
     */
    public void closeSocket() {
        disconnecting = true;

        // Wait for onReceive thread exits
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        try {
            if (reader != null) {
                reader.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        reader = null;

        try {
            if (writer != null) {
                writer.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        writer = null;

        try {
            if (connection != null) {
                connection.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        connection = null;

        printerOpened = false;

        appendConsole("Disconnected from server!");
        disconnecting = false;
    }

    /**
     * Receive XML message from the server and parse XML.
     */
    public void onReceive() {
        int chr;
        StringBuffer buffer;

        try {
            connection.setSoTimeout(500);
        } catch (SocketException ex) {
            ex.printStackTrace();
        }

        // Read input stream until disconnected
        while ((!disconnecting) && (connection != null)) {
            try {
                buffer = new StringBuffer();
                while ((chr = reader.read()) > 0) {
                    buffer.append((char) chr);
                }

                // Parse received xml document(DOM)
                DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                doc = builder.parse(new ByteArrayInputStream(buffer.toString().getBytes(StandardCharsets.UTF_8)));
                String firstNode = doc.getFirstChild().getNodeName();

                // Response of open_device request
                switch (firstNode) {
                    case "open_device":
                        String id = getChildren(doc, "device_id");
                        String code = getChildren(doc, "code");

                        appendConsole("open_device response: " + id + " : " + code);

                        if (id.equals(deviceIDprinter) && code.equals("OK")) {
                            printerOpened = true;
                        }

                        new Thread(new Runnable() {

                            @Override
                            public void run() {
                                print("This is an intentional message from the code. \nJose.");
                            }
                        }).start();
                        break;

                    // Server and client exchange the data using device_data message.
                    case "device_data":
//                    Input data from scanner
//                    if (getChildren(doc, "type").equals("ondata")) {
//                        appendOnData(getChildren(doc, "input"));
//                    }

                        // Response of print request from printer
                        if (getChildren(doc, "type").equals("onxmlresult")) {
                            Element el = (Element) doc.getElementsByTagName(
                                    "response").item(0);
                            if (el.getAttribute("success").equals("true")) {
                                appendConsole("device_data: Print success.");
                            } else {
                                appendConsole("device_data: Print failed.");
                            }
                        }
                        break;

                    // Response of reconnect request
                    case "reconnect":
                        if (getChildren(doc, "code").equals("OK")) {
                            printerOpened = true;
                            appendConsole("Reconnected.");
                        }
                        break;
                }

                // Save latest number of data_id
                String latestDataID = getChildren(doc, "data_id");
                if (latestDataID != null) {
                    dataID = latestDataID;
                }

            } catch (SocketTimeoutException ex) {
                ex.printStackTrace();
                appendConsole("Disconnected due socket timeout exception.");
                reconnect();
            } catch (IOException ex) {
                ex.printStackTrace();
                appendConsole("Disconnected.");
                reconnect();
            } catch (SAXException ex) {
                appendConsole("XML parse error due sax exception.");
                ex.printStackTrace();
            } catch (ParserConfigurationException ex) {
                appendConsole("XML parse error exception.");
                ex.printStackTrace();
            }
        }
    }

    /**
     * Send open_device message to use printer.
     */
    public void openPrinter() {
        appendConsole("open_device: Opening the printer device...");

        String req = "<open_device>" + "<device_id>" + deviceIDprinter
                + "</device_id>" + "<data>" + "<type>type_printer</type>"
                + "</data>" + "</open_device>" + TERMINATING_CHARACTER;

        appendConsole("open_device Request: \n" + req);

        try {
            writer.write(req);
            writer.flush();

        } catch (IOException ex) {
            appendConsole("Disconnected");
            closeSocket();
            ex.printStackTrace();
        }
    }

    /**
     * Send open_device message to use scanner.
     */
    public void openScanner() {
        String req = "<open_device>"
                + "<device_id>" + deviceIDscanner + "</device_id>"
                + "<data>" + "<type>type_scanner</type>"
                + "<buffer>" + BUFFER_ENABLED + "</buffer>"
                + "</data>" + "</open_device>" + TERMINATING_CHARACTER;

        try {
            writer.write(req);
            writer.flush();
        } catch (IOException ex) {
            appendConsole("Disconnected.");
            closeSocket();
            ex.printStackTrace();
        }
    }

    /**
     * Send print request.
     */
    public void print(String text) {
        if (!printerOpened) {
            appendConsole("Printer is not opened.");
            return;
        }

        appendConsole("Printing operation...");

        String req = "<device_data>"
                + "<sequence>100</sequence>"
                + "<device_id>"
                + deviceIDprinter
                + "</device_id>"
                + "<data>"
                + "<type>print</type>"
                + "<timeout>10000</timeout>"
                + "<printdata>"
                + "<epos-print xmlns=\"http://www.epson-pos.com/schemas/2011/03/epos-print\">"
                + "<text lang='ja' smooth='true'>Sample Print&#10;" + text
                + "</text>"
                + "<cut type=\"feed\" />"
                + "</epos-print>" + "</printdata>" + "</data>"
                + "</device_data>" + TERMINATING_CHARACTER;
        appendConsole("Printing operation request: \n" + req);

        try {
            writer.write(req);
            writer.flush();
        } catch (IOException ex) {
            appendConsole("Disconnected due io exception.");
            closeSocket();
            ex.printStackTrace();
        }
    }

    /**
     * Get value of child node from specified Document object and TagName.
     *
     * @param doc     specified Document object
     * @param tagName specified xml tagname
     * @return String of node value. If there is no such tagname, return null.
     */
    public String getChildren(Document doc, String tagName) {
        NodeList list = doc.getElementsByTagName(tagName);
        if (list.getLength() == 0) {
            return null;
        } else {
            try {
                Node node = list.item(0);
                return node.getFirstChild().getNodeValue();
            } catch (NullPointerException ex) {
                return null;
            }
        }
    }

    private void appendConsole(String str) {
        System.out.println(str);
    }
}
