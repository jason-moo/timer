package org.example.redis.key;

public class RedisKey {

    private static final String TASK_KEY = "task:";

    public static String getTaskKey(int index) {
        return TASK_KEY + index;
    }

}
