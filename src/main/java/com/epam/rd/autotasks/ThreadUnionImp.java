package com.epam.rd.autotasks;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ThreadUnionImp implements ThreadUnion {
    private static final Logger logger = Logger.getLogger(ThreadUnionImp.class.getName());

    private String name;
    private List<Thread> threads;
    private List<FinishedThreadResult> finished;
    private int threadCounter;
    private boolean isShutdown;

    public ThreadUnionImp(String name) {
        this.name = name;
        this.threads = new ArrayList<>();
        this.finished = new ArrayList<>();
        this.threadCounter = 0;
        this.isShutdown = false;
    }

    @Override
    public int totalSize() {
        return threadCounter;
    }

    @Override
    public int activeSize() {
        int count = 0;
        for (Thread thread : threads) {
            logger.info(thread + " is alive: " + thread.isAlive());
            if (thread.isAlive()) {
                count++;
            }
        }
        return count;
    }

    //    Interrupts all created threads and prevents creating of more Threads.
    @Override
    public void shutdown() {
        isShutdown = true;
        for (Thread thread : threads) {
            thread.interrupt();
        }
    }

    //    Returns true if shutdown was called.
    @Override
    public boolean isShutdown() {
        return isShutdown;
    }

    @Override
    public void awaitTermination() {
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        logger.info("awaitTermination()");
    }

    @Override
    public boolean isFinished() {
        if (!isShutdown()) {
            logger.info("isFinished() - " + false);
            return false;
        }

        for (Thread thread : threads) {
            if (thread.isAlive()) {
                logger.info("isFinished() - " + false);
                return false;
            }
        }

        logger.info("isFinished() - " + true);
        return true;
    }

    @Override
    public List<FinishedThreadResult> results() {
        return finished;
    }


    /*
    Creates and registers a new Thread.
    Name of the thread should be "<thread-union-name>-worker-n", where n is a number of a thread.
    A ThreadUnion must monitor execution of a created thread - refer to results() method.
     */
    @Override
    public synchronized Thread newThread(Runnable r) {
//        if shutDown is invoked before it will return true and no more thread will be created
        if (isShutdown()) {
            throw new IllegalStateException("Cannot create new thread after shutdown");
        }

//        name of the thread
        String threadName = String.format("%s-worker-%d", name, threadCounter++);

        Runnable wrappedRunnable = () -> {
            boolean isExceptionThrown = false;
            try {
                logger.info(threadName + " STARTED!");
                r.run();
            } catch (Throwable throwable) {
                logger.info("Exception was thrown!");
                synchronized (finished) {
                    finished.add(new FinishedThreadResult(threadName, throwable));
                }
                isExceptionThrown = true;
            } finally {
                if (!isExceptionThrown) {
                    logger.info("Exception was not thrown, but thread is Finished!");
                    synchronized (finished) {
                        finished.add(new FinishedThreadResult(threadName));
                    }
                }
                logger.info("Thread is Finished!");
            }
        };

//        Create and add thread to the list
        Thread thread = new Thread(wrappedRunnable, threadName);

//        Adding(Registering) thread to the threads list
        logger.info("Added new Thread: " + thread.getName());
        threads.add(thread);


//        thread.setUncaughtExceptionHandler((t, e) -> {
//            synchronized (finishedThreadsWithExceptionResults) {
//                finishedThreadsWithExceptionResults.add(new FinishedThreadResult(t.getName(), e));
//            }
//        });


        logger.info("List of Threads: " + threads);
        return thread;
    }


}
