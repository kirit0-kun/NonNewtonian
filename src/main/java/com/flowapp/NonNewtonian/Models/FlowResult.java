package com.flowapp.NonNewtonian.Models;

import java.util.List;

public class FlowResult {
    private final List<FlowLine> before;
    private final List<FlowLine> after;
    private final List<FlowLine> loops;

    public FlowResult(List<FlowLine> before, List<FlowLine> after, List<FlowLine> loops) {
        this.before = before;
        this.after = after;
        this.loops = loops;
    }

    public List<FlowLine> getBefore() {
        return before;
    }

    public List<FlowLine> getAfter() {
        return after;
    }

    public List<FlowLine> getLoops() {
        return loops;
    }
}
