package com.loganalyzer.reporter;

import com.loganalyzer.analyzer.AlertRule;
import com.loganalyzer.analyzer.StatisticsCollector;
import com.loganalyzer.parser.LogEntry;

import java.util.List;
import java.util.Map;

public interface ReportGenerator {

  String generate(StatisticsCollector stats, List<LogEntry> matched,
                  Map<String, Long> frequencies, List<AlertRule.Alert> alerts);
}
