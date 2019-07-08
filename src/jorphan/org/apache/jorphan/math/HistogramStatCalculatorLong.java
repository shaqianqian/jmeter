/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.jorphan.math;

import java.util.HashMap;
import java.util.Map;

import org.HdrHistogram.Histogram;
import org.LatencyUtils.LatencyStats;
import org.LatencyUtils.SimplePauseDetector;

public class HistogramStatCalculatorLong implements IStatCalculator<Long> {
    private SimplePauseDetector defaultPauseDetector = new SimplePauseDetector();
    private LatencyStats latencyStats = new LatencyStats(1, 3600000000000L, 2, 1024, 10000000000L, defaultPauseDetector);
    private Histogram histogram = new Histogram(latencyStats.getIntervalHistogram());
    private long bytes = 0;
    private long sentBytes = 0;
    private long sum = 0;
    private long min = Long.MAX_VALUE;
    private long max = Long.MIN_VALUE;

    public HistogramStatCalculatorLong() {
    }

    @Override
    public void clear() {
        bytes = 0;
        sentBytes = 0;
        sum = 0;
        min = Long.MAX_VALUE;
        max = Long.MIN_VALUE;
        latencyStats.stop();
        histogram.reset();
    }

    @Override
    public void addBytes(long newValue) {
        bytes += newValue;
    }

    @Override
    public void addSentBytes(long newValue) {
        sentBytes += newValue;
    }

    @Override
    public void addAll(IStatCalculator<Long> calc) {
        if (calc instanceof HistogramStatCalculatorLong) {
            HistogramStatCalculatorLong histoCalc = (HistogramStatCalculatorLong) calc;
            sum += histoCalc.sum;
            bytes += histoCalc.bytes;
            sentBytes += histoCalc.sentBytes;
            histogram.add(histoCalc.histogram);
            max = Math.max(histoCalc.max, max);
            min = Math.min(histoCalc.min, min);
        } else {
            throw new IllegalArgumentException("Only instances of HistogramStatCalculator allowed.");
        }
    }

    @Override
    public Long getMedian() {
        return histogram.getValueAtPercentile(50);
    }

    @Override
    public long getTotalBytes() {
        return bytes;
    }

    @Override
    public long getTotalSentBytes() {
        return sentBytes;
    }

    @Override
    public Long getPercentPoint(float percent) {
        return getPercentPoint((double) percent);
    }

    @Override
    public Long getPercentPoint(double percent) {
        return histogram.getValueAtPercentile(100.0 * percent);
    }

    @Override
    public Map<Number, Number[]> getDistribution() {
        Map<Number, Number[]> result = new HashMap<>();
        histogram.percentiles(5).forEach(p -> {
            result.put(p.getValueIteratedTo(),
                    new Number[] { p.getValueIteratedTo(), p.getCountAddedInThisIterationStep() });
        });
        return result;
    }

    @Override
    public double getMean() {
        return histogram.getMean();
    }

    @Override
    public double getStandardDeviation() {
        return histogram.getStdDeviation();
    }

    @Override
    public Long getMin() {
        return min;
    }

    @Override
    public Long getMax() {
        return max;
    }

    @Override
    public long getCount() {
        return histogram.getTotalCount();
    }

    @Override
    public double getSum() {
        return sum;
    }

    @Override
    public void addValue(Long val, long sampleCount) {
        sum += val * sampleCount;
        for(int i=0;i<sampleCount;i++) {            
            latencyStats.recordLatency(val); 
        }
        histogram.add(latencyStats.getIntervalHistogram());
        max = Math.max(val, max);
        min = Math.min(val, min);
    }

    @Override
    public void addValue(Long val) {
        sum += val;        
        latencyStats.recordLatency(val); 
        histogram.add(latencyStats.getIntervalHistogram());
        max = Math.max(val, max);
        min = Math.min(val, min);
    }
}
