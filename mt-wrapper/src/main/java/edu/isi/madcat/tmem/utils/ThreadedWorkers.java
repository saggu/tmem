package edu.isi.madcat.tmem.utils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.poi.ss.formula.functions.T;

import edu.isi.madcat.tmem.logging.ExceptionHandler;

public class ThreadedWorkers {

  class Worker extends Thread {
    private Deque<Runnable> q;
    private Lock lock;

    public Worker(Deque<Runnable> q, Lock lock) {
      super();
      this.q = q;
      this.lock = lock;
    }

    @Override
    public void run() {
      while (true) {
        Runnable nextItem = null;
        lock.lock();
        if (q.size() > 0) {
          nextItem = q.getLast();
          q.removeLast();
        }
        lock.unlock();
        if (nextItem == null) {
          break;
        }
        nextItem.run();
      }
    }
  }

  private int numThreads;

  public ThreadedWorkers(int numThreads) {
    this.numThreads = numThreads;
  }

  public <U extends Runnable> void run(List<U> tasks) {
    Deque<Runnable> q = new ArrayDeque<Runnable>(tasks);
    List<Worker> workers = new ArrayList<Worker>();
    Lock lock = new ReentrantLock();
    for (int i = 0; i < numThreads; i++) {
      Worker worker = new Worker(q, lock);
      workers.add(worker);
      worker.start();
    }
    for (Worker worker : workers) {
      try {
        worker.join();
      } catch (InterruptedException e) {
      }
    }
  }
}
