package org.example;

import org.example.util.Task;

import java.util.concurrent.TimeUnit;

public class TimerTask {

    private Task task;

    private long delay;

    private TimeUnit unit;

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public long getDelay() {
        return delay;
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }

    public TimeUnit getUnit() {
        return unit;
    }

    public void setUnit(TimeUnit unit) {
        this.unit = unit;
    }
}
