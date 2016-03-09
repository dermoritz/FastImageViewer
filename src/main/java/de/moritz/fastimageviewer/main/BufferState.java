package de.moritz.fastimageviewer.main;

public class BufferState {
    private double forward = 0;
    private double backward = 0;

    public BufferState(double forward, double backward){
        this.forward = forward;
        this.backward=backward;
    }

    public double getForward() {
        return forward;
    }

    public double getBackward() {
        return backward;
    }
}
