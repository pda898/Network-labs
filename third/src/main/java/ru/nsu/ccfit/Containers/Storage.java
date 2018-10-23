package ru.nsu.ccfit.Containers;

import ru.nsu.ccfit.Containers.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Storage<E> {
    private List<E> container = new ArrayList<>();
    private boolean wakeupState = false;

    private final Lock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private final Condition update = lock.newCondition();

    public void put(E object) {
        lock.lock();
        container.add(object);
        notEmpty.signal();
        lock.unlock();
    }

    public E get() {
        lock.lock();
        try {
            while ((!wakeupState)&&(container.size() == 0))
                notEmpty.await();
            if (wakeupState) {
                return null;
            }
            E tmp = container.remove(0);
            update.signal();
            return tmp;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        return null;
    }

   public void wakeup() {
       lock.lock();
       try {
           wakeupState = true;
           update.signalAll();
           notEmpty.signalAll();
       } finally {
           lock.unlock();
       }
   }
}
