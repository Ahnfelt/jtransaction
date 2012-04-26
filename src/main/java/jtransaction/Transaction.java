package jtransaction;

import java.util.*;

public class Transaction {

    static ThreadLocal<Transaction> transaction = new ThreadLocal<Transaction>();
    Map<Record, Map<String, Object>> accessed = new HashMap<Record, Map<String, Object>>();
    Map<Record, Map<String, Object>> written = new HashMap<Record, Map<String, Object>>();


    public static class RollbackException extends RuntimeException {}


    public static void rollback() {
        throw new RollbackException();
    }


    public static void run(Runnable runnable) {
        boolean alreadyInTransaction = transaction.get() != null;
        if(!alreadyInTransaction) transaction.set(new Transaction());
        try {
           do {
               runnable.run();
           } while(!transaction.get().commit());
        } finally {
           if(!alreadyInTransaction) transaction.set(null);
        }
    }


    private boolean commit() {

        List<Record> lockOrder = new ArrayList<Record>(accessed.keySet());
        Collections.sort(lockOrder, new Comparator<Record>() {
            public int compare(Record a, Record b) {
                return new Long(a.id).compareTo(b.id);
            }
        });

        for(Record record: lockOrder) {
            record.lock.lock();
        }

        try {

            for(Map.Entry<Record, Map<String, Object>> entry: accessed.entrySet()) {
                if(entry.getKey().fieldValues != entry.getValue()) {
                    return false;
                }
            }

            for(Map.Entry<Record, Map<String, Object>> entry: written.entrySet()) {
                Map<String, Object> newValues = new HashMap<String, Object>(accessed.get(entry.getKey()));
                newValues.putAll(entry.getValue());
                entry.getKey().fieldValues = newValues;
            }
            return true;

        } finally {

            Collections.reverse(lockOrder);
            for(Record record: lockOrder) {
                record.lock.unlock();
            }

        }

    }
}
