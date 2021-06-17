package com.flowapp.NonNewtonian.Models;

public class ProblemResult {
    private final FlowResult nonNewtonianDirect;
    private final FlowResult nonNewtonianReverse;
    private final FlowResult newtonianDirect;
    private final FlowResult newtonianReverse;
    private final String steps;

    public ProblemResult(FlowResult nonNewtonianDirect, FlowResult nonNewtonianReverse, FlowResult newtonianDirect, FlowResult newtonianReverse, String steps) {
        this.nonNewtonianDirect = nonNewtonianDirect;
        this.nonNewtonianReverse = nonNewtonianReverse;
        this.newtonianDirect = newtonianDirect;
        this.newtonianReverse = newtonianReverse;
        this.steps = steps;
    }

    public FlowResult getNonNewtonianDirect() {
        return nonNewtonianDirect;
    }

    public FlowResult getNonNewtonianReverse() {
        return nonNewtonianReverse;
    }

    public FlowResult getNewtonianDirect() {
        return newtonianDirect;
    }

    public FlowResult getNewtonianReverse() {
        return newtonianReverse;
    }

    public String getSteps() {
        return steps;
    }
}
