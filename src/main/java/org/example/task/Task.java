package org.example.task;

import lombok.Data;

import java.util.concurrent.TimeUnit;

@Data
public class Task {

    private String id;

    private String customData;

    private long delay;

    private TimeUnit unit;

}
