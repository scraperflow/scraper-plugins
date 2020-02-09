package scraper.plugins.debugger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scraper.annotations.ArgsCommand;
import scraper.api.di.DIContainer;
import scraper.api.flow.FlowMap;
import scraper.api.node.NodeHook;
import scraper.api.node.container.NodeContainer;
import scraper.api.node.type.Node;
import scraper.api.plugin.Addon;
import scraper.utils.StringUtil;


@ArgsCommand(
        value = "debug",
        doc = "Starts a debugging websocket server and waits for a debugger to be present for processing the flow. If no port is specified with debug-port, then 8890 is used.",
        example = "scraper app.scrape debug"
)
@ArgsCommand(
        value = "debug-port",
        doc = "Port for debugging. Default is 8890",
        example = "scraper app.scrape debug debug-port:8890"
)
public class DebuggerNodeHookAddon implements NodeHook, Addon {

    /** Logger with the actual class name */
    protected Logger l = LoggerFactory.getLogger("Debugger");

    protected DebuggerWebsocketServer debugger;

    private final ObjectMapper m = new ObjectMapper();

    @Override
    public void accept(NodeContainer<? extends Node> n, FlowMap o) {
        if(debugger != null) {
            debugger.waitForReady();
            debugger.checkBreakpoint(n);

            try {
                // top hack
                o.put("$origin", n.getAddress().toString());
                debugger.debugger.get().send(m.writeValueAsString(o));
                o.remove("$origin");
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public void load(DIContainer loadedDependencies, String[] args) {
        if (StringUtil.getArgument(args, "debug") != null) {
            l.warn("Debugging activated");
            String debugPort = StringUtil.getArgument(args, "debug-port");
            if(debugPort == null) {
                l.warn("Using default port 8890 for debugging");
                debugger = new DebuggerWebsocketServer( 8890);
            } else {
                int port = Integer.parseInt(debugPort);
                l.warn("Using port {} for debugging", port);
                debugger = new DebuggerWebsocketServer(port);
            }
        }
    }
}

