package com.aggregationserver;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class LamportClockTest {

    @Test
    public void testInitialTime() {
        LamportClock clock = new LamportClock();
        assertEquals(0, clock.getTime());
    }

    @Test
    public void testTick() {
        LamportClock clock = new LamportClock();
        clock.tick();
        assertEquals(1, clock.getTime());
    }

    @Test
    public void testMaxTime() {
        LamportClock clock = new LamportClock();
        clock.updateTime(10);
        clock.tick();
        assertEquals(11, clock.getTime());
    }

    @Test
    public void testUpdateTime() {
        LamportClock clock = new LamportClock();
        clock.updateTime(100);
        assertEquals(100, clock.getTime());
    }
}
