package scraper.plugins.debugger;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.exceptions.InvalidDataException;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshakeBuilder;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

public class DebuggerWebsocketServer extends WebSocketServer {

    protected Logger l = LoggerFactory.getLogger("DebuggerWebSocket");

    WebSocket debugger = null;
    final ReentrantLock lock = new ReentrantLock();
    final DebuggerNodeHookAddon actions;

    ObjectMapper m = new ObjectMapper();

    public DebuggerWebsocketServer(DebuggerNodeHookAddon actions, int port) {
        super(new InetSocketAddress(port));
        this.actions = actions;
        this.setReuseAddr(true);
        this.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                l.warn("Shutting down system");
                this.stop();
                l.warn("Graceful shutdown");
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }));
    }

    // returns a socket, if a client is connected
    public Optional<WebSocket> get() {
        return Optional.ofNullable(debugger);
    }

    // only a single debugger allowed to be connected at the same time
    @Override
    public ServerHandshakeBuilder onWebsocketHandshakeReceivedAsServer(WebSocket conn, Draft draft, ClientHandshake request) throws InvalidDataException {
        ServerHandshakeBuilder builder = super.onWebsocketHandshakeReceivedAsServer( conn, draft, request );
        try {
            lock.lock();
            if(debugger != null) throw new InvalidDataException(409, "A debugger has already connected");
        } finally { lock.unlock(); }

        return builder;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        try {
            lock.lock();
            debugger = conn;
        } finally { lock.unlock(); }
        l.info("Debugger connected");
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        try {
            lock.lock();
            debugger = null;
        } finally {
            lock.unlock();
        }
        l.warn("Debugger disconnected");
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        l.error("Web Socket error", ex);
        // TODO implement error handling
        //      idea: stop execution until debugger reconnects
    }


    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            //noinspection unchecked convention
            Map<String, Object> request = m.readValue(message, Map.class);
            String cmd = (String) request.get("command");
            //noinspection unchecked convention
            Map<String, Object> data = (Map<String, Object>) request.get("data");
            System.out.println(actions.getClass());

            Method cmdMethod = actions.getClass().getMethod(cmd, Map.class);
            cmdMethod.invoke(actions, data);

        } catch (Exception e) {
            l.error("Not a valid command: {}", message.substring(0,100));
        }



    }


    @Override
    public void onStart() {

    }

}
