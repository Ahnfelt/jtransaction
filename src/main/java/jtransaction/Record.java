package jtransaction;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Record implements InvocationHandler {

    private static final AtomicLong nextId = new AtomicLong();

    private final Class type;
    final long id = nextId.getAndIncrement();
    final Lock lock = new ReentrantLock();
    Map<String, Object> fieldValues = Collections.emptyMap();


    @SuppressWarnings("unchecked")
    public static <T> T create(Class<T> type) {
        return (T) Proxy.newProxyInstance(
                Record.class.getClassLoader(),
                new Class<?>[] { type },
                new Record(type));
    }

    @SuppressWarnings("unchecked")
    public static <T> T shallowCopy(T record) {
        Record internal = (Record) Proxy.getInvocationHandler(record);
        internal.addToTransaction();
        Record copy = new Record(internal.type);
        copy.fieldValues = internal.fieldValues;
        return (T) Proxy.newProxyInstance(
                Record.class.getClassLoader(),
                new Class<?>[] { internal.type },
                copy);
    }


    public Record(Class type) {
        this.type = type;
    }


    public Object invoke(Object object, Method method, Object[] objects) throws Throwable {
        if(!method.getDeclaringClass().equals(Object.class)) {

            if(method.getName().startsWith("get") &&
                    (objects == null || objects.length == 0)) {

                return get(method.getName().substring(3));

            } else if(
                    method.getName().startsWith("set") &&
                    objects.length == 1 &&
                    method.getReturnType().equals(Void.TYPE)) {

                set(method.getName().substring(3), objects[0]);
                return null;

            }
        }
        return method.invoke(object, objects);
    }


    private void set(String fieldName, Object value) {
        Transaction transaction = addToTransaction();
        if(transaction == null) throw new Transaction.NotInTransactionException();
        if(!transaction.written.containsKey(this)) {
            transaction.written.put(this, new HashMap<String, Object>());
        }
        transaction.written.get(this).put(fieldName, value);
    }


    private Object get(String fieldName) {
        Transaction transaction = addToTransaction();
        if(transaction == null) return fieldValues.get(fieldName);
        Map<String, Object> written = transaction.written.get(this);
        if(written != null && written.containsKey(fieldName)) {
            return written.get(fieldName);
        } else {
            return transaction.accessed.get(this).get(fieldName);
        }
    }


    private Transaction addToTransaction() {
        Transaction transaction = Transaction.transaction.get();
        if(transaction != null && !transaction.accessed.containsKey(this)) {
            transaction.accessed.put(this, fieldValues);
        }
        return transaction;
    }
}
