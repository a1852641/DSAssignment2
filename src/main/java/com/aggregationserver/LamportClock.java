package com.aggregationserver;

/**
 * Represents a Lamport Clock for maintaining logical time in distributed systems.
 * Provides methods for ticking, updating based on received timestamps, and retrieving the current time.
 */
public class LamportClock {

    // Logical clock value
    private int clock;

    /**
     * Initializes the Lamport clock to zero.
     */
    public LamportClock() {
        this.clock = 0;
    }

    /**
     * Increments the clock by 1, representing the passage of a local event.
     * This method should be called before each event in a process.
     */
    public synchronized void tick() {
        clock++;
    }

    /**
     * Updates the clock based on a received clock value from another process.
     * The clock is set to the maximum of the local clock and the received clock, plus one.
     * 
     * @param receivedClock The clock value from the received message or event.
     */
    public synchronized void update(int receivedClock) {
        clock = Math.max(clock, receivedClock) + 1;
    }

    /**
     * Retrieves the current clock time.
     * 
     * @return The current logical clock time.
     */
    public synchronized int getTime() {
        return clock;
    }

    /**
     * Sets the clock time manually to a specified value.
     * 
     * @param time The new logical clock time to set.
     */
    public synchronized void setTime(int time) {
        this.clock = time;
    }
}
