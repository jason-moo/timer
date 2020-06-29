package org.example.task;

import lombok.Data;

import java.util.concurrent.TimeUnit;

@Data
public class Task {

    private String id;

    private String customData;

    private long delay;

    private TimeUnit unit;

    public Task(String customData, long delay, TimeUnit unit) {
        this.customData = customData;
        this.delay = delay;
        this.unit = unit;
    }

    public Task(String customData) {
        this.customData = customData;
    }
}
