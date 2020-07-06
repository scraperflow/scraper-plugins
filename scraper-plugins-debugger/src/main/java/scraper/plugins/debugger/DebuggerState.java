package scraper.plugins.debugger;

import scraper.api.node.Address;
import scraper.api.node.container.NodeContainer;
import scraper.api.node.type.Node;

import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;

public class DebuggerState {
    protected System.Logger l = System.getLogger("DebuggerState");

    // flows will wait on this object
    private final AtomicBoolean ready = new AtomicBoolean(false);
    private final AtomicBoolean breaking = new AtomicBoolean(false);

    private final Set<String> breakpoints = new HashMap<String, Object>().keySet();

    public void waitUntilReady() {
        try {
            synchronized (ready) {
                if(!ready.get()) {
                    l.log(INFO,"Waiting for debugger to connect");
                    ready.wait();
                }
            }
        } catch (Exception ignored) {}
    }

    public void setReady(boolean b) {
        synchronized (ready) {
            ready.set(true);

            // if ready, notify every flow to wake up
            if(b) ready.notifyAll();
        }
    }

    public void waitIfBreakpoint(NodeContainer<? extends Node> n, Runnable onWait, Runnable onContinue) {
        for (String breakpoint : breakpoints) {
            Address addr = n.addressOf(breakpoint);
            if(n.getAddress().equals(addr)) {
                l.log(INFO,"BREAKPOINT TRIGGERED: {} <-> {}", breakpoint, n.getAddress().getRepresentation());
                synchronized (breaking) {
                    try {
                        onWait.run();
                        breaking.wait();
                        break;
                    } catch (InterruptedException e) {
                        l.log(ERROR,"Continuing because interrupt");
                        e.printStackTrace();
                    } finally {
                        onContinue.run();
                    }
                }
            }
        }

    }

    public void addBreakpoint(String br) {
        breakpoints.add(br);
    }
}

