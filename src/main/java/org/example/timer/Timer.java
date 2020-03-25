package org.example.timer;


import org.example.TimerTask;

public interface Timer {

    /**
     * Schedules the specified {@link} for one-time execution after
     * the specified delay.
     *
     * @return a handle which is associated with the specified task
     *
     * @throws IllegalStateException       if this timer has been {@linkplain #stop() stopped} already
     *
     */
    String newTimeout(TimerTask task);

    /**
     * Releases all resources acquired by this {@link io.netty.util.Timer} and cancels all
     * tasks which were scheduled but not executed yet.
     *
     * @return the handles associated with the tasks which were canceled by
     *         this method
     */
    void stop();
}
