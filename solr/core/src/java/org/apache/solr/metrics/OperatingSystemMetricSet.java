/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.solr.metrics;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.PlatformManagedObject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import org.apache.solr.util.stats.MetricUtils;

/**
 * This is an extended replacement for {@link com.codahale.metrics.jvm.FileDescriptorRatioGauge}
 * - that class uses reflection and doesn't work under Java 9. This implementation tries to retrieve
 * bean properties from known implementations of {@link java.lang.management.OperatingSystemMXBean}.
 */
public class OperatingSystemMetricSet implements MetricSet {

  @Override
  public Map<String, Metric> getMetrics() {
    final Map<String, Metric> metrics = new HashMap<>();
    OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
    MetricUtils.addMXBeanMetrics(os, OperatingSystemMXBean.class, null, (k, v) -> {
      metrics.put(k, v);
    });
    // There are some additional implementation-specific properties we want to add
    // (not available on all JVMs):
    for (String clazz : Arrays.asList(
        "com.sun.management.OperatingSystemMXBean",
        "com.sun.management.UnixOperatingSystemMXBean",
        "com.ibm.lang.management.OperatingSystemMXBean"
        )) {
      try {
        final Class<? extends PlatformManagedObject> intf = Class.forName(clazz)
            .asSubclass(PlatformManagedObject.class);
        MetricUtils.addMXBeanMetrics(os, intf, null, (k, v) -> {
          // skip already existing properties
          if (!metrics.containsKey(k)) {
            metrics.put(k, v);
          }
        });
      } catch (ClassNotFoundException e) {
        // ignore
      }
    }
    return metrics;
  }
}
