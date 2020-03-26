package org.example.timer;

import org.example.task.Task;

import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class QyyTimer extends AbstractTimer{

    public QyyTimer(long tickDuration, TimeUnit unit, int ticksPerWheel) {
        super(tickDuration, unit, ticksPerWheel);
    }

    public QyyTimer(ThreadFactory threadFactory, long tickDuration, TimeUnit unit, int ticksPerWheel, ThreadPoolExecutor threadPoolExecutor) {
        super(threadFactory, tickDuration, unit, ticksPerWheel, threadPoolExecutor);
    }

    @Override
    public String addTask(Task task) {
        return super.addTask(task);
    }

    @Override
    public List<Task> queryTimerTask(int index, long deadline) {


        return super.queryTimerTask(index, deadline);
    }

    @Override
    public void handle(List<Task> tasks) {

        super.handle(tasks);
    }
}
