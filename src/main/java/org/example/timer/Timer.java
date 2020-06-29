package org.example.timer;


import org.example.task.Task;

import java.util.List;
import java.util.concurrent.TimeUnit;

public interface Timer {

    /**
     * Schedules the specified {@link} for one-time execution after
     * the specified delay.
     *
     * @return a handle which is associated with the specified task
     * @throws IllegalStateException if this timer has been {@linkplain #stop() stopped} already
     */
    String newTimeout(Task task, long delay, TimeUnit unit);

    /**
     * Releases all resources acquired by this {@link io.netty.util.Timer} and cancels all
     * tasks which were scheduled but not executed yet.
     *
     * @return the handles associated with the tasks which were canceled by
     * this method
     */
    void stop();

    abstract String addTask(Task task, long delay, TimeUnit unit);

    abstract List<Task> queryTimerTask(int index, long deadline);

    abstract void handle(List<Task> tasks);

}
