package edu.isi.madcat.tmem.utils;

import java.util.Map;
import java.util.TreeMap;

public class Stopwatch {
  private class Item {
    private String name;

    private long totalTime;

    private boolean isRunning;

    private long startTime;

    public Item(String name) {
      super();
      this.name = name;
      this.totalTime = 0;
      this.isRunning = false;
      this.startTime = -1;
    }

    public void clear() {
      this.totalTime = 0;
      this.isRunning = false;
      this.startTime = -1;
    }

    public String getName() {
      return name;
    }

    public long getStartTime() {
      return startTime;
    }

    public long getTotalTime() {
      return totalTime;
    }

    public boolean isRunning() {
      return isRunning;
    }

    public void setName(String name) {
      this.name = name;
    }

    public void setRunning(boolean isRunning) {
      this.isRunning = isRunning;
    }

    public void setStartTime(long startTime) {
      this.startTime = startTime;
    }

    public void setTotalTime(long totalTime) {
      this.totalTime = totalTime;
    }

    public void start() {
      if (!isRunning) {
        isRunning = true;
        startTime = System.currentTimeMillis();
      }
    }

    public void stop() {
      if (isRunning) {
        isRunning = false;
        startTime = -1;
        long currentTime = System.currentTimeMillis();
        totalTime += currentTime - startTime;
      }
    }
  }

  public static Stopwatch GLOBAL;

  static {
    GLOBAL = new Stopwatch();
  }

  private Map<String, Item> items;

  public Stopwatch() {
    items = new TreeMap<String, Item>();
  }

  public double getSeconds(String name) {
    Item item = items.get(name);
    if (item == null) {
      throw new RuntimeException("Stopwatch item is not started: " + name);
    }
    item.stop();
    double seconds = (double) item.getTotalTime() / (double) 1000.0;
    item.start();
    return seconds;
  }

  public void restart(String name) {
    Item item = items.get(name);
    if (item == null) {
      item = new Item(name);
      items.put(name, item);
    }
    item.clear();
    item.start();
  }

  public void start(String name) {
    Item item = items.get(name);
    if (item == null) {
      item = new Item(name);
      items.put(name, item);
    }
    item.start();
  }

  public void stop(String name) {
    Item item = items.get(name);
    if (item == null) {
      throw new RuntimeException("Stopwatch item is not started: " + name);
    }
    item.stop();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, Item> e : items.entrySet()) {
      String name = e.getKey();
      sb.append(String.format("Stopwatch \"%s\" %.2f seconds\n", name, getSeconds(name)));
    }
    return sb.toString();
  }
}
