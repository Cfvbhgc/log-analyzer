package com.loganalyzer.util;

import com.loganalyzer.parser.LogEntry;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.Deque;

public class TimeWindow {

  private final int minutes;
  private final Deque<LogEntry> buffer;

  private TimeWindow(int minutes) {
    this.minutes = minutes;
    this.buffer = new ArrayDeque<>();
  }

  public static TimeWindow of(int minutes) {
    return new TimeWindow(minutes);
  }

  public void add(LogEntry entry) {
    buffer.addLast(entry);
  }

  public void evict(LocalDateTime now) {
    var cutoff = now.minus(minutes, ChronoUnit.MINUTES);
    while (!buffer.isEmpty() && buffer.peekFirst().getTimestamp().isBefore(cutoff)) {
      buffer.pollFirst();
    }
  }

  public int size() {
    return buffer.size();
  }

  public void clear() {
    buffer.clear();
  }

  public Deque<LogEntry> entries() {
    return buffer;
  }
}
