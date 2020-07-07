package scraper.plugins.debugger;

import scraper.api.flow.FlowMap;

import java.util.HashMap;
import java.util.Map;

public class FlowMapDTO {
    private final Map<String, Object> content = new HashMap<>();

    private final String flowId;
    private final int sequence;

    private final String parentId;
    private final Integer parentSequence;

    //
    public Map<String, ?> getContent() { return content; }
    public String getParentId() { return parentId; }
    public Integer getSequence() { return sequence; }
    public String getFlowId() { return flowId; }
    public Integer getParentSequence() { return parentSequence; }

    public FlowMapDTO(FlowMap o) {
        o.forEach((l,v) -> content.put(l.getLocation().getRaw().toString(), v));
        if(o.getParentId().isPresent()) {
            parentId = o.getParentId().get().toString();
            this.parentSequence = o.getParentSequence().get();
        } else {
            parentId = null;
            parentSequence = null;
        }

        this.flowId = o.getId().toString();
        this.sequence = o.getSequence();
    }
}
