package com.loganalyzer.parser;

import java.util.regex.Pattern;

public enum LogFormat {

  APACHE(
    Pattern.compile(
      "^(?<ip>\\S+) \\S+ \\S+ \\[(?<time>[^]]+)] \"(?<method>\\S+) (?<path>\\S+) \\S+\" (?<status>\\d{3}) (?<size>\\S+)"
    )
  ),

  NGINX(
    Pattern.compile(
      "^(?<ip>\\S+) - (?<user>\\S+) \\[(?<time>[^]]+)] \"(?<method>\\S+) (?<path>\\S+) \\S+\" (?<status>\\d{3}) (?<size>\\d+) \"(?<referer>[^\"]*)\" \"(?<agent>[^\"]*)\""
    )
  ),

  JSON(
    Pattern.compile("^\\s*\\{.*}\\s*$")
  ),

  CUSTOM(
    Pattern.compile(
      "^(?<time>\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}[^\\s]*) +\\[?(?<level>TRACE|DEBUG|INFO|WARN|ERROR|FATAL)]?\\s+(?<source>\\S+)\\s*[-:]\\s*(?<message>.+)$"
    )
  );

  private final Pattern pattern;

  LogFormat(Pattern pattern) {
    this.pattern = pattern;
  }

  public Pattern pattern() {
    return pattern;
  }
}
