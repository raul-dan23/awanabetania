import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import javax.smartcardio.*;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Scanner;

/**
 * NFC Bridge — runs locally on the laptop with the USB NFC reader.
 *
 * Reads the hardware UID of any card placed on the reader via the PC/SC API
 * ({@code javax.smartcardio}) and broadcasts it to all connected browsers as a
 * JSON WebSocket message: {@code {"uid":"A1B2C3D4"}}.
 *
 * <p>Start: {@code java -jar nfc-bridge.jar}
 * <br>Stop:  {@code Ctrl+C}
 * <br>Test mode (no hardware): {@code java -jar nfc-bridge.jar --test}
 */
public class NfcBridge extends WebSocketServer {

    /** APDU command to retrieve the card UID (ISO/IEC 7816 GET DATA). */
    private static final byte[] GET_UID = {(byte) 0xFF, (byte) 0xCA, 0x00, 0x00, 0x00};
    private static final int PORT = 7000;
    private static final int POLL_MS = 300;

    /** Creates the WebSocket server bound to localhost:{@value #PORT}. */
    public NfcBridge() {
        super(new InetSocketAddress("localhost", PORT));
        setReuseAddr(true);
        setConnectionLostTimeout(0);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("[Bridge] Browser connected: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("[Bridge] Browser disconnected.");
    }

    /** Incoming messages from browsers are not used by this bridge. */
    @Override
    public void onMessage(WebSocket conn, String message) {}

    @Override
    public void onError(WebSocket conn, Exception ex) {
        if (conn != null) System.err.println("[Bridge] WebSocket error: " + ex.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("[Bridge] ✓ WebSocket started on ws://localhost:" + PORT);
        System.out.println("[Bridge] Waiting for NFC card...");
        System.out.println("[Bridge] Press Ctrl+C to stop.");
        System.out.println("─".repeat(50));
    }

    /** Converts a raw byte array to an uppercase hex string (e.g. {@code A1B2C3D4}). */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02X", b));
        return sb.toString();
    }

    /**
     * Entry point.
     *
     * <p>In normal mode: polls the first detected PC/SC terminal every {@value #POLL_MS} ms.
     * When a card is placed, sends its UID to all connected browsers. The UID is only
     * broadcast once per card placement (deduplicated via {@code lastUid}).
     *
     * <p>In test mode ({@code --test}): reads UIDs from stdin and broadcasts them directly,
     * allowing end-to-end testing without physical hardware.
     *
     * @param args pass {@code --test} to run without a physical NFC reader
     */
    public static void main(String[] args) throws Exception {
        boolean testMode = args.length > 0 && args[0].equals("--test");

        System.out.println("╔══════════════════════════════════╗");
        System.out.println("║     Awana Betania — NFC Bridge   ║");
        if (testMode)
        System.out.println("║       TEST MODE (no hardware)    ║");
        System.out.println("╚══════════════════════════════════╝");

        NfcBridge bridge = new NfcBridge();
        bridge.start();

        if (testMode) {
            System.out.println("[TEST] Type a UID and press Enter to simulate a card scan:");
            System.out.println("[TEST] Example: A1B2C3D4");
            System.out.println("─".repeat(50));
            Scanner scanner = new Scanner(System.in);
            while (scanner.hasNextLine()) {
                String uid = scanner.nextLine().trim().toUpperCase();
                if (!uid.isEmpty()) {
                    System.out.println("[TEST] Simulating card: " + uid);
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
                        System.out.println("[Bridge] ⚠ No NFC reader detected. Connect the USB reader.");
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
                                System.out.println("[Bridge] ✓ Card detected: " + uid);
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
