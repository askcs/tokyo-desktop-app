package com.askcs.tokyodesktopclient.event;
import com.google.common.eventbus.EventBus;

/**
 * Maintains a singleton instance for obtaining the event bus over which
 * messages are passed.
 */
public final class BusProvider {

    private static final EventBus BUS = new EventBus();

    public static EventBus getBus() {
        return BUS;
    }

    // No need to instantiate this class.
    private BusProvider() {
    }
}
