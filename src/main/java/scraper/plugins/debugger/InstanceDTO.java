package scraper.plugins.debugger;

import scraper.api.node.Address;
import scraper.api.specification.ScrapeInstance;

import java.util.HashMap;
import java.util.Map;

public class InstanceDTO {

    private final String name;
    private final Map<String, Object> entryArguments;
    private final Map<Address, InstanceDTO> importedInstances = new HashMap<>();
    private final Map<Address, NodeDTO> nodes = new HashMap<>();

    public String getName() { return name; }
    public Map<String, Object> getEntryArguments() { return entryArguments; }
    public Map<Address, InstanceDTO> getImportedInstances() { return importedInstances; }
    public Map<Address, NodeDTO> getNodes() { return nodes; }

    public InstanceDTO(ScrapeInstance i) {
        this.name = i.getName();
        this.entryArguments = i.getEntryArguments();

        i.getRoutes().forEach((address, nodeContainer) -> nodes.put(address, new NodeDTO(nodeContainer)));
        i.getImportedInstances().forEach((adr, impl) -> importedInstances.put(adr, new InstanceDTO(impl)));
    }
}
