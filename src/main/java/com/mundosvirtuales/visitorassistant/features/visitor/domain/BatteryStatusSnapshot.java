package com.mundosvirtuales.visitorassistant.features.visitor.domain;

public class BatteryStatusSnapshot {

    private final int batteryValue;
    private final int batteryStatus;

    public BatteryStatusSnapshot(int batteryValue, int batteryStatus) {
        this.batteryValue = batteryValue;
        this.batteryStatus = batteryStatus;
    }

    public int getBatteryValue() {
        return batteryValue;
    }

    public int getBatteryStatus() {
        return batteryStatus;
    }
}
