package com.flowapp.NonNewtonian.Models;

public class FlowLine {

    private final float startStatic;
    private final float startHt;

    private final float q;
    private final float v;
    private final float iDmm;

    private final float startLength;
    private final float length;

    private final float endStatic;
    private final float nPSH;

    private final float nre;
    private final float f;

    public FlowLine(float startStatic, float startHt, float q, float v, float iDmm, float startLength, float length, float endStatic, float nPSH, float nre, float f) {
        this.startStatic = startStatic;
        this.startHt = startHt;
        this.q = q;
        this.v = v;
        this.iDmm = iDmm;
        this.startLength = startLength;
        this.length = length;
        this.endStatic = endStatic;
        this.nPSH = nPSH;
        this.nre = nre;
        this.f = f;
    }

    public float getStartStatic() {
        return startStatic;
    }

    public float getStartHt() {
        return startHt;
    }

    public float getQ() {
        return q;
    }

    public float getV() {
        return v;
    }

    public float getIDmm() {
        return iDmm;
    }

    public float getStartLength() {
        return startLength;
    }

    public float getLength() {
        return length;
    }

    public float getEndStatic() {
        return endStatic;
    }

    public float getNPSH() {
        return nPSH;
    }

    public float getNre() {
        return nre;
    }

    public float getF() {
        return f;
    }
}
