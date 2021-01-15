package com.example.circularboard;

public class CircleKeys {

    private float centerX, centerY, radius;
    private int code;

    public CircleKeys(float centerX, float centerY, float radius) {
        setCenterXY(centerX, centerY);
        this.radius = radius;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public void setCenterXY(float centerX, float centerY) {
        this.centerX = centerX;
        this.centerY = centerY;
    }

    public boolean contains(float x, float y) {
        float dx = x - centerX;
        float dy = y - centerY;
        return (dx * dx + dy * dy) <= (radius * radius);
    }

}
