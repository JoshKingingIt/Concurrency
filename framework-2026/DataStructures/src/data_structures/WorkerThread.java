package data_structures;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * A <code>WorkerThread</code> adds items to a data structure, and then removes
 * some other items.
 *
 * @param <T>
 *            the type of the items.
 */
public class WorkerThread<T extends Comparable<T>> extends Thread {

    /** Thread number. */
    private final int id;
    /** The data structure to be used. */
    private final Sorted<T> sorted;
    /** The items to be added by this thread. */
    private final T[] itemsToAdd;
    /** The items to be removed by this thread. */
    private final T[] itemsToRemove;
    /**
     * If > 0, CPU time to be spent in between add or remove operations, in
     * microseconds.
     */
    private final int workTime;
    private final boolean doSameOrder;
    /**
     * When set, have thread 0 print the data structure when addition is done.
     */
    private final boolean doDebug;
    /** Barrier to be used after addition. */
    private final CyclicBarrier barrier;

    ThreadMXBean bean;

    /** some example code to study cache effects.
     * NOTE: static, which can cause false sharing, depending on the access pattern.
     */
    private final static int[] workArray = new int[8192];
    private final int maxThreads = 32;

    /**
     * Creates a worker thread with the specified parameters.
     *
     * @param id
     *            the thread number.
     * @param list
     *            the data structure.
     * @param itemsToAdd
     *            the items to add to the data structure.
     * @param itemsToRemove
     *            the items to remove from the data structure.
     * @param workTime
     *            if {@literal >} 0, specifies the amount of CPU time to be
     *            spent in between add or remove operations, in microseconds.
     * @param barrier
     *            used to synchronize the threads after adding items.
     * @param same_order
     *            when set, the items are removed in the same order as inserted
     * @param debug
     *            when set, the data structure is printed when all items have
     *            been added.
     */
    public WorkerThread(int id, Sorted<T> list, T[] itemsToAdd,
            T[] itemsToRemove, int workTime, CyclicBarrier barrier,
            boolean same_order, boolean debug) {
            // boolean do_add, boolean do_remove_and_work) {
        this.sorted = list;
        this.id = id;
        this.itemsToAdd = itemsToAdd;
        this.itemsToRemove = itemsToRemove;
        this.workTime = workTime;
        this.barrier = barrier;
        this.doSameOrder = same_order;
        this.doDebug = debug;
        if (workTime > 0) {
            bean = ManagementFactory.getThreadMXBean();
        }
    }

    @Override
    public void run() {
        if (itemsToAdd != null) {
            // add my work items, note: do no work while adding
            for (T t : itemsToAdd) {
                // No work while adding!!
                sorted.add(t);
            }
    
            // Barrier, and possibly print result.
            try {
                barrier.await();
                if (this.doDebug) {
                    if (this.id == 0) {
                        ArrayList<T> result = sorted.toArrayList();
                        for (int i = 0; i < result.size() - 1; i++) {
                            if (result.get(i).compareTo(result.get(i + 1)) > 0) {
                                System.err.println(
                                        "The result is not correctly sorted");
                                break;
                            }
                        }
                        System.out.printf(
                                "Output after adding, before removing:\n%s\n",
                                sorted.toArrayList().toString());
                    }
                    barrier.await();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (BrokenBarrierException e) {
                e.printStackTrace();
            }
        }

        if (itemsToRemove != null) {
            // remove work items, and perform the work itself

            if (this.doSameOrder) {
                // Remove my own items in same order as inserted.
                for (T t : itemsToRemove) {
                    sorted.remove(t);
                    doWork(t);
                }
            } else {
                // Remove items in sorted order
                T item;
    
                while ((item = sorted.removeLowest()) != null) {
                    doWork(item);
                }
            }
    
            // Barrier
            try {
                barrier.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (BrokenBarrierException e) {
                e.printStackTrace();
            }
        } // doRemoveAndWork
    }

    /**
     * Spends some CPU time, but only if {@link #workTime} is {@literal >} 0, in
     * which case it tries to spend the specified (in microseconds) amount of
     * CPU time.
     *
     * If {@link #workTime} is {@literal <} 0, this triggers execution of
     * some more realistic workload that is not purely CPU based, but also
     * involve memory/cache, which thus may be used for further performance analysis,
     * The standard code below modifies portion of a shared array; and the
     * performance will depend on factors like cache line and cache sizes.
     * TODO: change this code to do perform alternative work and analyze this.
     */
    private void doWork(T item) {
        if (bean != null) {
            // Do some work in between operations. workTime indicates the cpu
            // time to be consumed, in microseconds.
            long start = bean.getCurrentThreadCpuTime();

            // getThreadCpuTime() returns cpu time in nano seconds.
            long end = start + workTime * 1000;
            while (bean.getCurrentThreadCpuTime() < end)
                ; // busy until we used enough cpu time.
        } else {
	    if (workTime < 0) {
		int alternativeWork = -workTime;
	       	// tune to actual cache line size in ints:
		// int cacheLineSkip = 32;
		int cacheLineSkip = 16;
		int threadIndex = (this.id % this.maxThreads);
		int workIndex = threadIndex * cacheLineSkip;
		
		for (int i = 0; i < alternativeWork; i++) {
		     // update array entry
		     this.workArray[workIndex] = i;

		     // pick next index
		     workIndex += maxThreads * cacheLineSkip;
		     if (workIndex >= this.workArray.length) {
			 // overflow, reset
			 workIndex = threadIndex * cacheLineSkip;
		     }
		}
	    }
	}
    }
}
