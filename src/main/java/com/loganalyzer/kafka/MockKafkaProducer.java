package com.loganalyzer.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.loganalyzer.analyzer.AlertRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class MockKafkaProducer {

  private static final Logger log = LoggerFactory.getLogger(MockKafkaProducer.class);
  private static final ObjectMapper mapper = new ObjectMapper()
    .registerModule(new JavaTimeModule());

  private final String topic;
  private final String bootstrapServers;
  private final List<String> sentMessages;

  private MockKafkaProducer(String topic, String bootstrapServers) {
    this.topic = topic;
    this.bootstrapServers = bootstrapServers;
    this.sentMessages = new ArrayList<>();
  }

  public static MockKafkaProducer of(String topic) {
    return new MockKafkaProducer(topic, "localhost:9092");
  }

  public static MockKafkaProducer of(String topic, String bootstrapServers) {
    return new MockKafkaProducer(topic, bootstrapServers);
  }

  public void emit(AlertRule.Alert alert) {
    var payload = new LinkedHashMap<String, Object>();
    payload.put("type", "ALERT");
    payload.put("rule", alert.getRuleName());
    payload.put("triggeredAt", alert.getTriggeredAt().toString());
    payload.put("count", alert.getCount());
    payload.put("threshold", alert.getThreshold());
    payload.put("windowMinutes", alert.getWindowMinutes());
    payload.put("sample", alert.getSample());

    try {
      var json = mapper.writeValueAsString(payload);
      sentMessages.add(json);
      log.info("[MockKafka] -> topic={} | servers={} | payload={}", topic, bootstrapServers, json);
    } catch (Exception e) {
      log.error("[MockKafka] serialization failed: {}", e.getMessage());
    }
  }

  public void emit(List<AlertRule.Alert> alerts) {
    alerts.forEach(this::emit);
  }

  public List<String> drain() {
    var copy = List.copyOf(sentMessages);
    sentMessages.clear();
    return copy;
  }

  public int pending() {
    return sentMessages.size();
  }

  public String getTopic() {
    return topic;
  }

  public String getBootstrapServers() {
    return bootstrapServers;
  }
}
