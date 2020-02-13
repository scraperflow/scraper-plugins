package scraper.plugins.debugger;

import scraper.api.node.NodeAddress;
import scraper.api.node.container.NodeContainer;
import scraper.api.node.type.Node;

import java.util.Map;

public class NodeDTO {
    private final Map<String, ?> nodeConfiguration;
    private final NodeAddress address;

    public Map<String, ?> getNodeConfiguration() { return nodeConfiguration; }
    public NodeAddress getAddress() { return address; }

    public NodeDTO(NodeContainer<? extends Node> nodeContainer) {
        this.nodeConfiguration = nodeContainer.getNodeConfiguration();
        this.address = nodeContainer.getAddress();
    }
}
