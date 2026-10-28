package data_structures.tests;

import java.util.Vector;
import java.util.concurrent.CyclicBarrier;

// TODO:
// - add tests, e.g. see ThreadedTestInteger.java for an example
// - specifically need to add tests for removeLowest() which are currently missing,
//   as only remove(val) is tested.
class ThreadedTestsString {
    public static TestCase<?>[] testCases(String ds) {
        return new ThreadedTestsString().allTestCases(ds);
    }

    private TestCase<?>[] allTestCases(String ds) {
        TestCase<?>[] testCases = { 
	    // TODO
	};
        return testCases;
    }

    abstract class ThreadedTestCase<T extends Comparable<T>>
            extends TestCase<T> {
        static final int REPEAT = 10000;

        public ThreadedTestCase(String ds) {
            super(ds);
        };

        abstract void runThreaded() throws Exception;

        @Override
        public void run() {
            try {
                for (int i = 0; i < REPEAT; i++) {
                    buildSorted(this.ds);
                    this.runThreaded();
                }
            } catch (TestFailedException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    abstract class TestRunnable implements Runnable {
        abstract void runThreaded() throws Exception;

        public void run() {
            try {
                this.runThreaded();
            } catch (TestFailedException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    // TODO: add tests, e.g. see ThreadedTestInteger.java for a start
}
