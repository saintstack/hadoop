/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.yarn.server.resourcemanager.scheduler;

import com.google.common.collect.Maps;
import org.apache.hadoop.metrics2.MetricsRecordBuilder;
import org.apache.hadoop.metrics2.MetricsSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.apache.hadoop.test.MetricsAsserts.assertCounter;
import static org.apache.hadoop.test.MetricsAsserts.assertGauge;
import static org.apache.hadoop.test.MetricsAsserts.getMetrics;
import static org.apache.hadoop.yarn.server.resourcemanager.scheduler
    .ResourceMetricsChecker.ResourceMetricsKey.AGGREGATE_CONTAINERS_ALLOCATED;
import static org.apache.hadoop.yarn.server.resourcemanager.scheduler
    .ResourceMetricsChecker.ResourceMetricsKey.AGGREGATE_CONTAINERS_RELEASED;
import static org.apache.hadoop.yarn.server.resourcemanager.scheduler
    .ResourceMetricsChecker.ResourceMetricsKey.ALLOCATED_CONTAINERS;
import static org.apache.hadoop.yarn.server.resourcemanager.scheduler
    .ResourceMetricsChecker.ResourceMetricsKey.ALLOCATED_MB;
import static org.apache.hadoop.yarn.server.resourcemanager.scheduler
    .ResourceMetricsChecker.ResourceMetricsKey.ALLOCATED_V_CORES;
import static org.apache.hadoop.yarn.server.resourcemanager.scheduler
    .ResourceMetricsChecker.ResourceMetricsKey.AVAILABLE_MB;
import static org.apache.hadoop.yarn.server.resourcemanager.scheduler
    .ResourceMetricsChecker.ResourceMetricsKey.AVAILABLE_V_CORES;
import static org.apache.hadoop.yarn.server.resourcemanager.scheduler
    .ResourceMetricsChecker.ResourceMetricsKey.PENDING_CONTAINERS;
import static org.apache.hadoop.yarn.server.resourcemanager.scheduler.ResourceMetricsChecker.ResourceMetricsKey.PENDING_MB;
import static org.apache.hadoop.yarn.server.resourcemanager.scheduler.ResourceMetricsChecker.ResourceMetricsKey.PENDING_V_CORES;
import static org.apache.hadoop.yarn.server.resourcemanager.scheduler
    .ResourceMetricsChecker.ResourceMetricsKey.RESERVED_CONTAINERS;
import static org.apache.hadoop.yarn.server.resourcemanager.scheduler
    .ResourceMetricsChecker.ResourceMetricsKey.RESERVED_MB;
import static org.apache.hadoop.yarn.server.resourcemanager.scheduler.ResourceMetricsChecker.ResourceMetricsKey.RESERVED_V_CORES;

final class ResourceMetricsChecker {
  private final static Logger LOG =
          LoggerFactory.getLogger(ResourceMetricsChecker.class);

  private static final ResourceMetricsChecker INITIAL_CHECKER =
      new ResourceMetricsChecker()
          .gaugeLong(ALLOCATED_MB, 0)
          .gaugeInt(ALLOCATED_V_CORES, 0)
          .gaugeInt(ALLOCATED_CONTAINERS, 0)
          .counter(AGGREGATE_CONTAINERS_ALLOCATED, 0)
          .counter(AGGREGATE_CONTAINERS_RELEASED, 0)
          .gaugeLong(AVAILABLE_MB, 0)
          .gaugeInt(AVAILABLE_V_CORES, 0)
          .gaugeLong(PENDING_MB, 0)
          .gaugeInt(PENDING_V_CORES, 0)
          .gaugeInt(PENDING_CONTAINERS, 0)
          .gaugeLong(RESERVED_MB, 0)
          .gaugeInt(RESERVED_V_CORES, 0)
          .gaugeInt(RESERVED_CONTAINERS, 0);

  enum ResourceMetricsKey {
    ALLOCATED_MB("AllocatedMB"),
    ALLOCATED_V_CORES("AllocatedVCores"),
    ALLOCATED_CONTAINERS("AllocatedContainers"),
    AGGREGATE_CONTAINERS_ALLOCATED("AggregateContainersAllocated"),
    AGGREGATE_CONTAINERS_RELEASED("AggregateContainersReleased"),
    AVAILABLE_MB("AvailableMB"),
    AVAILABLE_V_CORES("AvailableVCores"),
    PENDING_MB("PendingMB"),
    PENDING_V_CORES("PendingVCores"),
    PENDING_CONTAINERS("PendingContainers"),
    RESERVED_MB("ReservedMB"),
    RESERVED_V_CORES("ReservedVCores"),
    RESERVED_CONTAINERS("ReservedContainers");

    private String value;

    ResourceMetricsKey(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  private final Map<ResourceMetricsKey, Long> gaugesLong;
  private final Map<ResourceMetricsKey, Integer> gaugesInt;
  private final Map<ResourceMetricsKey, Long> counters;

  private ResourceMetricsChecker() {
    this.gaugesLong = Maps.newHashMap();
    this.gaugesInt = Maps.newHashMap();
    this.counters = Maps.newHashMap();
  }

  private ResourceMetricsChecker(ResourceMetricsChecker checker) {
    this.gaugesLong = Maps.newHashMap(checker.gaugesLong);
    this.gaugesInt = Maps.newHashMap(checker.gaugesInt);
    this.counters = Maps.newHashMap(checker.counters);
  }

  public static ResourceMetricsChecker createFromChecker(
          ResourceMetricsChecker checker) {
    return new ResourceMetricsChecker(checker);
  }

  public static ResourceMetricsChecker create() {
    return new ResourceMetricsChecker(INITIAL_CHECKER);
  }

  ResourceMetricsChecker gaugeLong(ResourceMetricsKey key, long value) {
    gaugesLong.put(key, value);
    return this;
  }

  ResourceMetricsChecker gaugeInt(ResourceMetricsKey key, int value) {
    gaugesInt.put(key, value);
    return this;
  }

  ResourceMetricsChecker counter(ResourceMetricsKey key, long value) {
    counters.put(key, value);
    return this;
  }

  ResourceMetricsChecker checkAgainst(MetricsSource source) {
    if (source == null) {
      throw new IllegalStateException("MetricsSource should not be null!");
    }
    MetricsRecordBuilder recordBuilder = getMetrics(source);
    logAssertingMessage(source);

    for (Map.Entry<ResourceMetricsKey, Long> gauge : gaugesLong.entrySet()) {
      assertGauge(gauge.getKey().value, gauge.getValue(), recordBuilder);
    }

    for (Map.Entry<ResourceMetricsKey, Integer> gauge : gaugesInt.entrySet()) {
      assertGauge(gauge.getKey().value, gauge.getValue(), recordBuilder);
    }

    for (Map.Entry<ResourceMetricsKey, Long> counter : counters.entrySet()) {
      assertCounter(counter.getKey().value, counter.getValue(), recordBuilder);
    }
    return this;
  }

  private void logAssertingMessage(MetricsSource source) {
    String queueName = ((QueueMetrics) source).queueName;
    Map<String, QueueMetrics> users = ((QueueMetrics) source).users;

    if (LOG.isDebugEnabled()) {
      LOG.debug("Asserting Resource metrics.. QueueName: " + queueName
          + ", users: " + (users != null && !users.isEmpty() ? users : ""));
    }
  }
}
