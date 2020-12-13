package is.yarr.rdf.concurrent;

import java.util.concurrent.atomic.AtomicInteger;

public class Hold {
    private final Object holdObj = new Object();
    private final AtomicInteger waitingCount = new AtomicInteger();
    private boolean released;

    public void release() {
        waitingCount.set(0);
        synchronized (holdObj) {
            released = true;
        }

        synchronized (this) {
            notifyAll();
        }
    }

    /**
     * Clear the signal without notifying
     */
    public void reset() {
        waitingCount.set(0);
        synchronized (holdObj) {
            released = false;
        }
    }

    /**
     * Waits up to the timeout for the release
     *
     * @param timeout The timeout in milliseconds
     */
    public void waitForRelease(long timeout) throws InterruptedException {
        waitingCount.incrementAndGet();
        if (!isReleased()) {
            synchronized (this) {
                wait(timeout);
            }
        }
    }

    /**
     * Waits for the release
     */
    public void waitForRelease() throws InterruptedException {
        waitingCount.incrementAndGet();
        while (!isReleased()) {
            synchronized (this) {
                wait();
            }
        }
    }

    /**
     * Gets the amount of threads waiting for this hold.
     * har
     * @return The amount of threads waiting for this hold
     */
    public int getWaitingCount() {
        return waitingCount.get();
    }

    /**
     * Checks if the hold has been released.
     *
     * @return If it has been released
     */
    public boolean isReleased() {
        synchronized (holdObj) {
            return released;
        }
    }
}