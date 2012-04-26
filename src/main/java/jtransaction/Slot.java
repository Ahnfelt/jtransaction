package jtransaction;

public interface Slot<T> {
    public T get();
    public void set(T value);
}
