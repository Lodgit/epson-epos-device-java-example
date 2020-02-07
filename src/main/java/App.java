public class App {
    public static void main(String[] args) {

        // TODO: Read stdin arguments
        EposClient client = new EposClient("", "");

        client.connect();
    }
}
