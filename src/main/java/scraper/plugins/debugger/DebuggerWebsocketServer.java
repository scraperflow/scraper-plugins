package scraper.plugins.debugger;

import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.exceptions.InvalidDataException;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshakeBuilder;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scraper.api.node.Address;
import scraper.api.node.container.NodeContainer;
import scraper.api.node.type.Node;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

public class DebuggerWebsocketServer extends WebSocketServer {

    protected Logger l = LoggerFactory.getLogger("DebuggerWebSocket");

    final AtomicReference<WebSocket> debugger = new AtomicReference<>();

    private final AtomicBoolean ready = new AtomicBoolean(false);

    private final Object waiting = new Object();

    Set<String> breakpoints = ConcurrentHashMap.newKeySet();

    public DebuggerWebsocketServer(int port) {
        super(new InetSocketAddress(port));
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

    void checkBreakpoint(NodeContainer<? extends Node> n) {
        for (String breakpoint : breakpoints) {
            Address addr = n.getJobInstance().addressOf(breakpoint);
            if(n.getAddress().equals(addr)) {
                l.warn("BREAKPOINT TRIGGERED: {} <-> {}", breakpoint, n.getAddress().getRepresentation());
                synchronized (waiting) {
                    try {
                        l.warn("Waiting for 'CONTINUE' message from client");
                        waiting.wait();
                    } catch (InterruptedException e) {
                        l.error("Continuing because interrupt");
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    void waitForReady() {
        synchronized (ready) {
            if(!ready.get()) {
                try {
                    l.info("Waiting for debugger to connect and say READY");
                    ready.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public ServerHandshakeBuilder onWebsocketHandshakeReceivedAsServer(WebSocket conn, Draft draft, ClientHandshake request) throws InvalidDataException {
        ServerHandshakeBuilder builder = super.onWebsocketHandshakeReceivedAsServer( conn, draft, request );
        //To your checks and throw an InvalidDataException to indicate that you reject this handshake.
        synchronized (debugger) {
            if(debugger.get() != null) throw new InvalidDataException(409, "A debugger has already connected");
        }

        return builder;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        synchronized (debugger) {
            debugger.set(conn);
        }

        l.info("Debugger connected");
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        synchronized (ready) {
            ready.set(false);
            debugger.set(null);
        }

        l.warn("Debugger disconnected, pausing flow");
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        if(message.equalsIgnoreCase("CONTINUE")) {
            synchronized (waiting) {
                waiting.notifyAll();
            }
        } else if(message.equalsIgnoreCase("READY")) {
            synchronized (ready) {
                ready.set(true);
                ready.notifyAll();
            }
        } else {
            Pattern p = Pattern.compile("<(\\w*\\.\\w*\\.\\w*)>");
            p.matcher(message).results().map(m -> m.group(1)).forEach(astr -> breakpoints.add(astr));
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        l.error("Web Socket error", ex);
    }

    @Override
    public void onStart() {

    }
}
