package br.ufsc.egc.rudger.expertprofiling.concurrent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ServiceThreadExecutor extends ThreadPoolExecutor {

    private static final int PROCESSORS = Runtime.getRuntime().availableProcessors();
    private final int maxWaitCommand;

    private ServiceThreadExecutor(final int corePoolSize, final int maximumPoolSize, final long keepAliveTime, final TimeUnit unit,
            final BlockingQueue<Runnable> workQueue, final int maxWaitCommand) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
        this.maxWaitCommand = maxWaitCommand;
    }

    public static ServiceThreadExecutor newCachedThreadPool() {
        return new ServiceThreadExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), 0);
    }

    public static ServiceThreadExecutor newCachedThreadPool(final int maxWaitCommand) {
        return new ServiceThreadExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), maxWaitCommand);
    }

    public static ServiceThreadExecutor newFixedThreadPool(final int nThreads, final int maxWaitCommand) {
        return new ServiceThreadExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), maxWaitCommand);
    }

    public static ServiceThreadExecutor newFixedThreadPool(final int nThreads) {
        return new ServiceThreadExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), 0);
    }

    public static ServiceThreadExecutor newProcessorsThreadPool(final int maxWaitCommand) {
        return new ServiceThreadExecutor(PROCESSORS, PROCESSORS, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), maxWaitCommand);
    }

    public static ServiceThreadExecutor newProcessorsThreadPool() {
        return new ServiceThreadExecutor(PROCESSORS, PROCESSORS, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), 0);
    }

    @Override
    public void execute(final Runnable command) {
        if (this.maxWaitCommand > 0) {
            while (this.getTaskCount() - this.getCompletedTaskCount() >= this.maxWaitCommand) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    // NOOP
                }
            }
        }

        super.execute(command);
    }

}
