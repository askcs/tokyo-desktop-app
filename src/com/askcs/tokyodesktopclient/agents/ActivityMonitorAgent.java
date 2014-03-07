package com.askcs.tokyodesktopclient.agents;

import java.util.logging.Logger;

import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.askcs.commons.agent.SensorAgent;
import com.askcs.commons.entity.SensorState;
import com.askcs.tokyodesktopclient.event.BusProvider;
import com.askcs.tokyodesktopclient.event.PcActivitySensorEvent;
import com.askcs.tokyodesktopclient.event.UnsupportedPlatformEvent;
import com.askcs.tokyodesktopclient.jna.IdleTime;
import com.askcs.tokyodesktopclient.jna.LinuxIdleTime;
import com.askcs.tokyodesktopclient.jna.MacIdleTime;
import com.askcs.tokyodesktopclient.jna.WindowsIdleTime;

@Access(AccessType.PUBLIC)
public class ActivityMonitorAgent extends SensorAgent {
    private static final Logger log = Logger.getLogger(ActivityMonitorAgent.class.toString());

    /**
     * Retrieve the username from the agent. Not accessible via JSON-RPC
     */
    @Access(AccessType.UNAVAILABLE)
    public String getUsername() {
        return super.getUsername();
    }

    /**
     * Retrieve the password from the agent. Not accessible via JSON-RPC
     */
    @Access(AccessType.UNAVAILABLE)
    public String getPassword() {
        return super.getPassword();
    }


    /**
     * Retrieves the state from the sensor,processes it and updates the gui if
     * changed.
     */
    @Override
    public void monitor() {
        SensorState currentState = getCurrentSensorState();
        boolean changed = processState(currentState);
        if (changed) {
            BusProvider.getBus().post(new PcActivitySensorEvent(getStoredSensorState()));
        }
    }

    /**
     * Measures the Idle time and returns state
     * 
     * @return true unless idleTime is greater then 5 minutes
     */
    public SensorState getCurrentSensorState() {
        long idleTime = getIdleTime();
        if (idleTime == -1l) {
            return SensorState.UNKNOWN;
        }
        if (idleTime > 5 * 60000l) {
            return SensorState.UNAVAILABLE;
        } else {
            return SensorState.AVAILABLE;
        }
    }

    /**
     * Send the state to the SensorStateAgent
     * 
     * @param state
     */
    @Override
    public void sendSensorState(SensorState sensorState) {
        try {
            getSensorStateAgent().setPcActivityState(sensorState);
            setLastSendTime(System.currentTimeMillis());
        } catch (Exception e) {
            log.warning("failed to get SensorStateAgent");
        }
    }
    
    /**
     * Get the idleTime from the OS, tries Linux native implementation if the
     * detected OS is not Windows or Mac os X. Triggers UnsupportedPlatformEvent
     * and returns -1l if there is no working implementation method available
     * 
     * @return idleTimeInMiliseconds
     */
    private long getIdleTime() {
        String osName = System.getProperty("os.name").toLowerCase();
        IdleTime idleTime = null;
        try {

                if (osName.contains("win")) {
                    idleTime = new WindowsIdleTime();
                } else
                    if (osName.contains("mac")) {
                        idleTime = new MacIdleTime();
                } else {
                    // assume/try linux
                    idleTime = new LinuxIdleTime();
                    }
            return idleTime.getIdleTimeMillis();
        } catch (UnsatisfiedLinkError e) {
            BusProvider.getBus().post(new UnsupportedPlatformEvent());
            return -1l;
        } catch (NoClassDefFoundError e) {
            BusProvider.getBus().post(new UnsupportedPlatformEvent());
            return -1l;
        }
    }
}