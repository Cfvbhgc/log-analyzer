package com.loganalyzer.analyzer;

import com.loganalyzer.parser.LogEntry;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PatternAnalyzerTest {

  private final PatternAnalyzer analyzer = new PatternAnalyzer();
  private final LocalDateTime base = LocalDateTime.of(2024, 1, 10, 14, 0, 0);

  @Test
  void scanFindsMatchingByPattern() {
    var entries = List.of(
      LogEntry.of(base, "ERROR", "svc.A", "NullPointerException in handler"),
      LogEntry.of(base.plusSeconds(1), "INFO", "svc.B", "Request completed"),
      LogEntry.of(base.plusSeconds(2), "ERROR", "svc.C", "NullPointerException in processor")
    );

    var result = analyzer.scan(entries, "NullPointer");
    assertEquals(2, result.size());
  }

  @Test
  void scanFindsMatchingByLevel() {
    var entries = List.of(
      LogEntry.of(base, "ERROR", "svc.A", "fail"),
      LogEntry.of(base.plusSeconds(1), "WARN", "svc.B", "slow"),
      LogEntry.of(base.plusSeconds(2), "ERROR", "svc.C", "crash")
    );

    var result = analyzer.scan(entries, "ERROR");
    assertEquals(2, result.size());
  }

  @Test
  void freqCountsNormalizedPatterns() {
    var entries = List.of(
      LogEntry.of(base, "ERROR", "svc", "Timeout after 100ms"),
      LogEntry.of(base.plusSeconds(1), "ERROR", "svc", "Timeout after 200ms"),
      LogEntry.of(base.plusSeconds(2), "ERROR", "svc", "Timeout after 300ms"),
      LogEntry.of(base.plusSeconds(3), "ERROR", "svc", "Connection refused")
    );

    var freq = analyzer.freq(entries);
    assertFalse(freq.isEmpty());

    var first = freq.entrySet().iterator().next();
    assertEquals(3, first.getValue());
  }

  @Test
  void clusterGroupsNearbyEntries() {
    var entries = List.of(
      LogEntry.of(base, "ERROR", "svc", "err1"),
      LogEntry.of(base.plusMinutes(1), "ERROR", "svc", "err2"),
      LogEntry.of(base.plusMinutes(2), "ERROR", "svc", "err3"),
      LogEntry.of(base.plusHours(2), "ERROR", "svc", "err4"),
      LogEntry.of(base.plusHours(2).plusMinutes(1), "ERROR", "svc", "err5")
    );

    var clusters = analyzer.cluster(entries, 5);
    assertEquals(2, clusters.size());
  }

  @Test
  void topErrorsReturnsMostFrequent() {
    var entries = List.of(
      LogEntry.of(base, "ERROR", "svc", "OOM"),
      LogEntry.of(base.plusSeconds(1), "ERROR", "svc", "OOM"),
      LogEntry.of(base.plusSeconds(2), "ERROR", "svc", "OOM"),
      LogEntry.of(base.plusSeconds(3), "ERROR", "svc", "Disk full"),
      LogEntry.of(base.plusSeconds(4), "INFO", "svc", "OK")
    );

    var top = analyzer.topErrors(entries, 2);
    assertEquals(2, top.size());
  }

  @Test
  void scanWithRegex() {
    var entries = List.of(
      LogEntry.of(base, "ERROR", "svc", "GET /api/users/123 -> 500"),
      LogEntry.of(base.plusSeconds(1), "INFO", "svc", "GET /api/users/456 -> 200"),
      LogEntry.of(base.plusSeconds(2), "ERROR", "svc", "POST /api/orders -> 500")
    );

    var result = analyzer.scan(entries, "-> 500");
    assertEquals(2, result.size());
  }
}
