package scraper.plugins.debugger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scraper.annotations.ArgsCommand;
import scraper.api.di.DIContainer;
import scraper.api.flow.FlowMap;
import scraper.api.node.container.NodeContainer;
import scraper.api.node.type.Node;
import scraper.api.plugin.Addon;
import scraper.api.plugin.Hook;
import scraper.api.plugin.NodeHook;
import scraper.api.specification.ScrapeInstance;
import scraper.api.specification.ScrapeSpecification;
import scraper.utils.StringUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;


@ArgsCommand(
        value = "debug",
        doc = "Starts a debugging websocket server and waits for a debugger to be present for processing the flow. If no port is specified with debug-port, then 8890 is used.",
        example = "scraper app.scrape debug"
)
@ArgsCommand(
        value = "debug-ip",
        doc = "Binding ip for debugging. Default is 0.0.0.0",
        example = "scraper app.scrape debug debug-ip:0.0.0.0"
)
@ArgsCommand(
        value = "debug-port",
        doc = "Port for debugging. Default is 8890",
        example = "scraper app.scrape debug debug-port:8890"
)
public class DebuggerNodeHookAddon implements NodeHook, Hook, Addon {
    /** Logger with the actual class name */
    protected Logger l = LoggerFactory.getLogger("Debugger");
    protected DebuggerWebsocketServer debugger;
    private final ObjectMapper m = new ObjectMapper();
    private Set<ScrapeSpecification> specs = new HashSet<>();

    private final DebuggerState state = new DebuggerState();

    @Override
    public void accept(NodeContainer<? extends Node> n, FlowMap o) {
        if(debugger != null) {
            state.waitUntilReady();

            debugger.get().ifPresent(client -> {
                client.send(wrap("nodePre", Map.of("nodeId", n.getAddress().getRepresentation(), "flowMap", o)));

                state.waitIfBreakpoint(n,
                        () -> client.send(wrap("breakpoint", Map.of("flowId", o.getId()))),
                        () -> client.send(wrap("breakpointContinue", Map.of("flowId", o.getId())))
                );
            });
        }
    }

    @Override
    public void acceptAfter(NodeContainer<? extends Node> n, FlowMap o) {
        if(debugger != null) {
            state.waitUntilReady();
            wrap("nodePost", Map.of("nodeId", n.getAddress().getRepresentation(), "flowMap", o));
        }
    }

    @Override
    public void load(DIContainer loadedDependencies, String[] args) {
        if (StringUtil.getArgument(args, "debug") != null) {
            l.warn("Debugging activated");
            String debugPort = StringUtil.getArgument(args, "debug-port");
            String debugIp = StringUtil.getArgument(args, "debug-ip");
            String bindingIp = "0.0.0.0";
            int port = 8890;
            if(debugPort != null) port = Integer.parseInt(debugPort);
            if(debugIp != null) bindingIp = debugIp;

            debugger = new DebuggerWebsocketServer( this, port, bindingIp);
        }
    }

    @Override
    public void execute(DIContainer dependencies, String[] args, Map<ScrapeSpecification, ScrapeInstance> scraper) {
        scraper.forEach((s,i) -> specs.add(s));
    }

    private String wrap(String type, Object data) {
        try {
            return m.writeValueAsString(Map.of("type", type, "data", data));
        } catch (JsonProcessingException e) {
            return "Error: " + e.getMessage();
        }
    }


    //======================
    // CLIENT API
    //======================

    @SuppressWarnings("unused") // reflection
    public void requestSpecifications(Map<String, Object> data) {
        l.info("Requesting specifications");
        for (ScrapeSpecification specc : specs) {
            debugger.get().ifPresent(client -> client.send(wrap("specification", specc)));
        }
    }

    @SuppressWarnings("unused") // reflection
    public void setReady(Map<String, Object> data) {
        l.info("Debugger is ready, waking up all flows");
        state.setReady(true);

    }

    @SuppressWarnings({"unused", "unchecked"}) // reflection
    public void setBreakpoints(Map<String, Object> data) {
        List<String> breakpoints = (List<String>) data.get("breakpoints");

        breakpoints.forEach(b -> {
            Pattern p = Pattern.compile("<?(\\w*\\.\\w*\\.\\w*)>?");
            p.matcher(b).results().map(m -> m.group(1)).forEach(state::addBreakpoint);
        });
    }

}

