package jtransaction;

import java.util.*;

public class Transaction {

    static ThreadLocal<Transaction> transaction = new ThreadLocal<Transaction>();
    Map<Record, Map<String, Object>> accessed = new HashMap<Record, Map<String, Object>>();
    Map<Record, Map<String, Object>> written = new HashMap<Record, Map<String, Object>>();


    private static class RetryException extends RuntimeException {}


    public static void retry() {
        throw new RetryException();
    }


    public static class RollbackException extends RuntimeException {}


    public static void rollback() {
        throw new RollbackException();
    }


    public static void run(Runnable runnable) {

        if(transaction.get() != null) {

            runnable.run();

        } else {

            transaction.set(new Transaction());
            try {

                boolean retry;
                do {

                    retry = false;
                    try {

                        runnable.run();

                    } catch(RetryException e) {

                        retry = true;
                        transaction.get().waitForRetry();
                        transaction.set(new Transaction());

                    }

                } while(retry || !transaction.get().commit());

            } finally {

                transaction.set(null);

            }
        }
    }


    private void waitForRetry() {

        final Object latch = new Object();

        for(Record record: accessed.keySet()) {
            record.latches.put(latch, latch);
        }

        try {

            synchronized(latch) {
                while(!readyToRetry()) {
                    try {
                        latch.wait();
                    } catch(InterruptedException e) {
                        throw new RollbackException();
                    }
                }
            }

        } finally {

            for(Record record: accessed.keySet()) {
                record.latches.remove(latch);
            }

        }
    }


    private boolean readyToRetry() {

        for(Map.Entry<Record, Map<String, Object>> entry: accessed.entrySet()) {
            if(entry.getKey().fieldValues != entry.getValue()) {
                return true;
            }
        }

        return false;
    }


    private boolean commit() {

        final boolean[] result = new boolean[1];

        lockAccessedRecords(new Runnable() {
            public void run() {

                for (Map.Entry<Record, Map<String, Object>> entry : accessed.entrySet()) {
                    if (entry.getKey().fieldValues != entry.getValue()) {
                        result[0] = false;
                        return;
                    }
                }

                for (Map.Entry<Record, Map<String, Object>> entry : written.entrySet()) {
                    Map<String, Object> newValues = new HashMap<String, Object>(accessed.get(entry.getKey()));
                    newValues.putAll(entry.getValue());
                    entry.getKey().fieldValues = newValues;
                }
                result[0] = true;

            }
        });

        return result[0];
    }


    private void lockAccessedRecords(Runnable runnable) {

        List<Record> lockOrder = new ArrayList<Record>(accessed.keySet());

        Collections.sort(lockOrder, byId);

        for(Record record: lockOrder) {
            record.lock.lock();
        }

        try {

            runnable.run();

        } finally {

            Collections.reverse(lockOrder);
            for(Record record: lockOrder) {
                record.lock.unlock();
            }

        }
    }


    private static final Comparator<Record> byId = new Comparator<Record>() {
        public int compare(Record a, Record b) {
            return new Long(a.id).compareTo(b.id);
        }
    };
}
