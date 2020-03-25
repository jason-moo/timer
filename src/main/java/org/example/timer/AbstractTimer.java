package org.example.timer;

import io.netty.util.HashedWheelTimer;
import io.netty.util.internal.PlatformDependent;
import org.example.TimerTask;
import org.example.util.ListUtils;
import org.example.util.ObjectUtil;
import org.example.util.Task;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

public abstract class AbstractTimer implements Timer {

    private static final AtomicIntegerFieldUpdater<AbstractTimer> WORKER_STATE_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(AbstractTimer.class, "workerState");

    private static volatile Integer workerState;

    public static final int WORKER_STATE_INIT = 0;

    public static final int WORKER_STATE_STARTED = 1;

    public static final int WORKER_STATE_SHUTDOWN = 2;

    private long tick;

    private long startTime;

    private int mask;

    private final Thread workerThread;

    private static final long MILLISECOND_NANOS = TimeUnit.MILLISECONDS.toNanos(1);

    private final long tickDuration;

    private final CountDownLatch startTimeInitialized = new CountDownLatch(1);

    private final AbstractTimer.Worker worker = new AbstractTimer.Worker();

    private ThreadPoolExecutor threadPoolExecutor;

    public AbstractTimer(long tickDuration, TimeUnit unit, int ticksPerWheel) {
        this(Executors.defaultThreadFactory(), tickDuration, unit, ticksPerWheel, (ThreadPoolExecutor) Executors.newCachedThreadPool());
    }

    public AbstractTimer(ThreadFactory threadFactory,
                         long tickDuration, TimeUnit unit, int ticksPerWheel, ThreadPoolExecutor threadPoolExecutor) {
        ObjectUtil.checkNotNull(threadFactory, "threadFactory");
        ObjectUtil.checkNotNull(unit, "unit");
        ObjectUtil.checkPositive(tickDuration, "tickDuration");
        ObjectUtil.checkPositive(ticksPerWheel, "ticksPerWheel");
        this.threadPoolExecutor = threadPoolExecutor;
        // Normalize ticksPerWheel to power of two and initialize the wheel.
        int length = normalizeTicksPerWheel(ticksPerWheel);
        mask = length - 1;

        // Convert tickDuration to nanos.
        long duration = unit.toNanos(tickDuration);

        // Prevent overflow.
        if (duration >= Long.MAX_VALUE / length) {
            throw new IllegalArgumentException(String.format(
                    "tickDuration: %d (expected: 0 < tickDuration in nanos < %d",
                    tickDuration, Long.MAX_VALUE / length));
        }
        Executors.newCachedThreadPool();

        if (duration < MILLISECOND_NANOS) {
            this.tickDuration = MILLISECOND_NANOS;
        } else {
            this.tickDuration = duration;
        }

        workerThread = threadFactory.newThread(worker);

    }

    public void start() {

        switch (WORKER_STATE_UPDATER.get(this)) {
            case WORKER_STATE_INIT:
                if (WORKER_STATE_UPDATER.compareAndSet(this, WORKER_STATE_INIT, WORKER_STATE_STARTED)) {
                    workerThread.start();
                }
                break;
            case WORKER_STATE_STARTED:
                break;
            case WORKER_STATE_SHUTDOWN:
                throw new IllegalStateException("cannot be started once stopped");
            default:
                throw new Error("Invalid WorkerState");
        }

        while (startTime == 0) {
            try {
                startTimeInitialized.await();
            } catch (InterruptedException ignore) {
            }
        }
    }

    private static int normalizeTicksPerWheel(int ticksPerWheel) {
        int normalizedTicksPerWheel = 1;
        while (normalizedTicksPerWheel < ticksPerWheel) {
            normalizedTicksPerWheel <<= 1;
        }
        return normalizedTicksPerWheel;
    }

    @Override
    public String newTimeout(TimerTask task) {
        ObjectUtil.checkNotNull(task, "task");
        start();
        String taskId = addTask(task);
        return taskId;
    }

    abstract String addTask(TimerTask task);

    abstract List<Task> queryTimerTask(int index,long deadline);

    abstract void handle(List<Task> tasks);

    protected void process(int index,long deadline){
        List<Task> tasks = queryTimerTask(index,deadline);
        List<List<Task>> divisionTask = ListUtils.division(tasks,50);
        if (divisionTask != null){
            int size = divisionTask.size();
            for (int i = 0; i < size; i++) {
                List<Task> taskList = divisionTask.get(i);
                threadPoolExecutor.execute(() ->{
                    handle(taskList);
                });
            }
        }
    }

    @Override
    public void stop() {
        if (Thread.currentThread() == workerThread) {
            throw new IllegalStateException(
                    HashedWheelTimer.class.getSimpleName() +
                            ".stop() cannot be called from " +
                            io.netty.util.TimerTask.class.getSimpleName());
        }

        if (!WORKER_STATE_UPDATER.compareAndSet(this, WORKER_STATE_STARTED, WORKER_STATE_SHUTDOWN)) {
            // workerState can be 0 or 2 at this moment - let it always be 2.
            if (WORKER_STATE_UPDATER.getAndSet(this, WORKER_STATE_SHUTDOWN) != WORKER_STATE_SHUTDOWN) {

            }
        }

        try {
            boolean interrupted = false;
            while (workerThread.isAlive()) {
                workerThread.interrupt();
                try {
                    workerThread.join(100);
                } catch (InterruptedException ignored) {
                    interrupted = true;
                }
            }
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        } finally {

        }
    }

    private class Worker implements Runnable {

        @Override
        public void run() {
            // Initialize the startTime.
            startTime = System.nanoTime();
            if (startTime == 0) {
                // We use 0 as an indicator for the uninitialized value here, so make sure it's not 0 when initialized.
                startTime = 1;
            }

            // Notify the other threads waiting for the initialization at start().
            startTimeInitialized.countDown();
            do {
                final long deadline = waitForNextTick();
                if (deadline > 0) {
                    int idx = (int) (tick & mask);
                    // todo 获取过期的任务然后处理
                    process(idx,deadline);
                    tick++;
                }
            } while (WORKER_STATE_UPDATER.get(AbstractTimer.this) == WORKER_STATE_STARTED);

            // todo stop后的数据处理

        }
    }

    private long waitForNextTick() {
        long deadline = TimeUnit.MILLISECONDS.toNanos(1) * (tick + 1);

        for (; ; ) {
            final long currentTime = System.nanoTime() - startTime;
            long sleepTimeMs = (deadline - currentTime + 999999) / 1000000;

            if (sleepTimeMs <= 0) {
                if (currentTime == Long.MIN_VALUE) {
                    return -Long.MAX_VALUE;
                } else {
                    return currentTime;
                }
            }
            // Check if we run on windows, as if thats the case we will need
            // to round the sleepTime as workaround for a bug that only affect
            // the JVM if it runs on windows.
            //
            // See https://github.com/netty/netty/issues/356
            if (PlatformDependent.isWindows()) {
                sleepTimeMs = sleepTimeMs / 10 * 10;
                if (sleepTimeMs == 0) {
                    sleepTimeMs = 1;
                }
            }

            try {
                Thread.sleep(sleepTimeMs);
            } catch (InterruptedException ignored) {
                //  todo
            }
        }
    }
}
