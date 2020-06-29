package org.example.timer;

import com.alibaba.fastjson.JSON;
import org.example.dao.TimerTaskMapper;
import org.example.entity.TimerTask;
import org.example.redis.key.RedisKey;
import org.example.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class QyyTimer extends AbstractTimer {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private TimerTaskMapper timerTaskMapper;

    public QyyTimer(long tickDuration, TimeUnit unit, int ticksPerWheel) {
        super(tickDuration, unit, ticksPerWheel);
    }

    public QyyTimer(ThreadFactory threadFactory, long tickDuration, TimeUnit unit, int ticksPerWheel, ThreadPoolExecutor threadPoolExecutor) {
        super(threadFactory, tickDuration, unit, ticksPerWheel, threadPoolExecutor);
    }

    @Override
    public String addTask(Task task, long delay, TimeUnit unit) {
        super.start();
        long deadTime = System.nanoTime() + unit.toNanos(delay);
        long deadline = deadTime - this.startTime;
        if (delay > 0 && deadline < 0) {
            deadline = Long.MAX_VALUE;
        }
        long calculated = deadline / tickDuration;
        int index = (int) (calculated & mask);
        stringRedisTemplate.opsForZSet().add(RedisKey.getTaskKey(index), JSON.toJSONString(task), deadTime);
        TimerTask taskTemp = new TimerTask();
        taskTemp.setExecuteTime(deadline);
        taskTemp.setCustomData(task.getCustomData());
        timerTaskMapper.insert(taskTemp);
        return taskTemp.getId();
    }

    @Override
    public List<Task> queryTimerTask(int index, long deadline) {
        Set<String> strings = stringRedisTemplate.opsForZSet().range(RedisKey.getTaskKey(index),0,deadline);
        return strings.stream().map(e -> JSON.parseObject(e,Task.class)).collect(Collectors.toList());
    }

    @Override
    public void handle(List<Task> tasks) {
        tasks.forEach(e -> System.out.println(LocalDateTime.now() + JSON.toJSONString(e)));
    }

}
