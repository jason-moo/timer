package org.example.timer;

import io.netty.util.HashedWheelTimer;
import io.netty.util.internal.PlatformDependent;
import org.example.util.ListUtils;
import org.example.util.ObjectUtil;
import org.example.task.Task;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

public class AbstractTimer implements Timer {

    private static final AtomicIntegerFieldUpdater<AbstractTimer> WORKER_STATE_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(AbstractTimer.class, "workerState");

    volatile int workerState;

    public static final int WORKER_STATE_INIT = 0;

    public static final int WORKER_STATE_STARTED = 1;

    public static final int WORKER_STATE_SHUTDOWN = 2;

    protected volatile long startTime;

    protected int mask;

    private final Thread workerThread;

    private static final long MILLISECOND_NANOS = TimeUnit.MILLISECONDS.toNanos(1);

    protected final long tickDuration;

    private final CountDownLatch startTimeInitialized = new CountDownLatch(1);

    private final AbstractTimer.Worker worker = new AbstractTimer.Worker();

    private ThreadPoolExecutor threadPoolExecutor;

    public int getWorkerState() {
        return workerState;
    }

    public void setWorkerState(int workerState) {
        this.workerState = workerState;
    }

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
                    workerThread.run();
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

    protected void process(int index, long deadline) {
        List<Task> tasks = queryTimerTask(index, deadline);
        if (!CollectionUtils.isEmpty(tasks)) {
            List<List<Task>> divisionTask = ListUtils.division(tasks, 50);
            if (divisionTask != null) {
                int size = divisionTask.size();
                for (int i = 0; i < size; i++) {
                    List<Task> taskList = divisionTask.get(i);
                    threadPoolExecutor.execute(() -> {
                        handle(taskList);
                    });
                }
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

    @Override
    public String newTimeout(Task task, long delay, TimeUnit unit) {
        ObjectUtil.checkNotNull(task, "task");
        start();
        String taskId = addTask(task, delay, unit);
        return taskId;
    }

    @Override
    public List<Task> queryTimerTask(int index, long deadline) {
        return null;
    }

    @Override
    public String addTask(Task task, long delay, TimeUnit unit) {
        return null;
    }

    @Override
    public void handle(List<Task> tasks) {

    }

    private class Worker implements Runnable {

        private long tick;

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
                    process(idx, deadline);
                    tick++;
                }
            } while (WORKER_STATE_UPDATER.get(AbstractTimer.this) == WORKER_STATE_STARTED);

            // todo stop后的数据处理

        }

        private long waitForNextTick() {
            long deadline = tickDuration * (tick + 1);

            for (; ; ) {
                final long currentTime = System.nanoTime();
                long sleepTimeMs = (deadline - currentTime + startTime + 999999) / 10000;

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
                    System.out.println(LocalDateTime.now());
                } catch (InterruptedException ignored) {
                    //  todo
                }
            }
        }
    }


}
