import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import javax.smartcardio.*;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Scanner;

/**
 * NFC Bridge — rulează local pe laptopul cu cititorul NFC.
 * Citeste UID-ul cardului via PC/SC si il trimite browserului prin WebSocket.
 *
 * Pornire: java -jar nfc-bridge.jar
 * Oprire:  Ctrl+C
 */
public class NfcBridge extends WebSocketServer {

    private static final byte[] GET_UID = {(byte) 0xFF, (byte) 0xCA, 0x00, 0x00, 0x00};
    private static final int PORT = 7000;
    private static final int POLL_MS = 300;

    public NfcBridge() {
        super(new InetSocketAddress("localhost", PORT));
        setReuseAddr(true);
        setConnectionLostTimeout(0);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("[Bridge] Browser conectat: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("[Bridge] Browser deconectat.");
    }

    @Override
    public void onMessage(WebSocket conn, String message) {}

    @Override
    public void onError(WebSocket conn, Exception ex) {
        if (conn != null) System.err.println("[Bridge] Eroare WebSocket: " + ex.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("[Bridge] ✓ WebSocket pornit pe ws://localhost:" + PORT);
        System.out.println("[Bridge] Asteapta card NFC...");
        System.out.println("[Bridge] Apasa Ctrl+C pentru a opri.");
        System.out.println("─".repeat(50));
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02X", b));
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        boolean testMode = args.length > 0 && args[0].equals("--test");

        System.out.println("╔══════════════════════════════════╗");
        System.out.println("║     Awana Betania — NFC Bridge   ║");
        if (testMode)
        System.out.println("║          MOD TEST (fara hardware) ║");
        System.out.println("╚══════════════════════════════════╝");

        NfcBridge bridge = new NfcBridge();
        bridge.start();

        if (testMode) {
            System.out.println("[TEST] Scrie un UID si apasa Enter pentru a simula o scanare:");
            System.out.println("[TEST] Exemplu: A1B2C3D4");
            System.out.println("─".repeat(50));
            Scanner scanner = new Scanner(System.in);
            while (scanner.hasNextLine()) {
                String uid = scanner.nextLine().trim().toUpperCase();
                if (!uid.isEmpty()) {
                    System.out.println("[TEST] Simulez card: " + uid);
                    bridge.broadcast("{\"uid\":\"" + uid + "\"}");
                }
            }
            return;
        }

        TerminalFactory factory = TerminalFactory.getDefault();
        String lastUid = "";
        boolean readerWarningShown = false;

        while (true) {
            try {
                List<CardTerminal> terminals = factory.terminals().list();

                if (terminals.isEmpty()) {
                    if (!readerWarningShown) {
                        System.out.println("[Bridge] ⚠ Niciun cititor NFC detectat. Conecteaza cititorul USB.");
                        readerWarningShown = true;
                    }
                    lastUid = "";
                    Thread.sleep(2000);
                    continue;
                }

                readerWarningShown = false;
                CardTerminal terminal = terminals.get(0);

                if (terminal.isCardPresent()) {
                    try {
                        Card card = terminal.connect("*");
                        CardChannel channel = card.getBasicChannel();
                        ResponseAPDU resp = channel.transmit(new CommandAPDU(GET_UID));
                        card.disconnect(false);

                        if (resp.getSW() == 0x9000 && resp.getData().length > 0) {
                            String uid = bytesToHex(resp.getData());
                            if (!uid.equals(lastUid)) {
                                lastUid = uid;
                                System.out.println("[Bridge] ✓ Card detectat: " + uid);
                                bridge.broadcast("{\"uid\":\"" + uid + "\"}");
                            }
                        }
                    } catch (CardException e) {
                        lastUid = "";
                    }
                } else {
                    if (!lastUid.isEmpty()) {
                        lastUid = "";
                    }
                }

            } catch (Exception e) {
                lastUid = "";
                readerWarningShown = false;
            }

            Thread.sleep(POLL_MS);
        }
    }
}
