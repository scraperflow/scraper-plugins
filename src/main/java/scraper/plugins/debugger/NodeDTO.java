package scraper.plugins.debugger;

import scraper.api.node.container.NodeContainer;
import scraper.api.node.type.Node;

import java.util.Map;

@SuppressWarnings("unused") // DTO
public class NodeDTO {
    private final Map<String, ?> nodeConfiguration;
    private final String address;

    public Map<String, ?> getNodeConfiguration() { return nodeConfiguration; }
    public String getAddress() { return address; }

    public NodeDTO(NodeContainer<? extends Node> nodeContainer) {
        this.nodeConfiguration = nodeContainer.getNodeConfiguration();
        this.address = nodeContainer.getAddress().toString();
    }
}
