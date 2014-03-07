package com.askcs.tokyodesktopclient.event;

import com.askcs.commons.entity.SensorState;

public class PcActivitySensorEvent {
    private SensorState sensorState;

    /**
     * @param state
     */
    public PcActivitySensorEvent(SensorState sensorState) {
        this.sensorState = sensorState;
    }

    /**
     * @return the state
     */
    public SensorState getSensorState() {
        return sensorState;
    }

    /**
     * @param state
     *            the state to set
     */
    public void setState(SensorState sensorState) {
        this.sensorState = sensorState;
    }
}
