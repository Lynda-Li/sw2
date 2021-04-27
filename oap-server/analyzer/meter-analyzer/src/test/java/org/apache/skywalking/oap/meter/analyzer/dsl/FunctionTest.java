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
 *
 */

package org.apache.skywalking.oap.meter.analyzer.dsl;

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Collection;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

@Slf4j
@RunWith(Parameterized.class)
public class FunctionTest {

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public ImmutableMap<String, SampleFamily> input;

    @Parameterized.Parameter(2)
    public String expression;

    @Parameterized.Parameter(3)
    public Result want;

    @Parameterized.Parameter(4)
    public boolean isThrow;

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            {
                "tag-override",
                of("instance_cpu_percentage", SampleFamily.build(Sample.builder().labels(of("region", "us")).build())),
                "instance_cpu_percentage.tag({ ['svc':'product', 'instance':'10.0.0.1'] })",
                Result.success(SampleFamily.build(Sample.builder().labels(of("svc", "product", "instance", "10.0.0.1")).build())),
                false,
            },
            {
                "tag-add",
                of("instance_cpu_percentage", SampleFamily.build(Sample.builder().labels(of("region", "us")).build())),
                "instance_cpu_percentage.tag({tags -> tags.az = 'az1' })",
                Result.success(SampleFamily.build(Sample.builder().labels(of("region", "us", "az", "az1")).build())),
                false,
            },
            {
                "tag-remove",
                of("instance_cpu_percentage", SampleFamily.build(Sample.builder().labels(of("region", "us")).build())),
                "instance_cpu_percentage.tag({tags -> tags.remove('region') })",
                Result.success(SampleFamily.build(Sample.builder().labels(ImmutableMap.of()).build())),
                false,
            },
            {
                "tag-update",
                of("instance_cpu_percentage", SampleFamily.build(Sample.builder().labels(of("region", "us")).build())),
                "instance_cpu_percentage.tag({tags -> if (tags['region'] == 'us') {tags.region = 'zh'} })",
                Result.success(SampleFamily.build(Sample.builder().labels(of("region", "zh")).build())),
                false,
            },
            {
                "tag-append",
                of("instance_cpu_percentage", SampleFamily.build(Sample.builder().labels(of("region", "us")).build())),
                "instance_cpu_percentage.tag({tags -> tags.region = 'prefix::' + tags.region})",
                Result.success(SampleFamily.build(Sample.builder().labels(of("region", "prefix::us")).build())),
                false,
                },
            {
                "histogram",
                of("instance_cpu_percentage", SampleFamily.build(
                    Sample.builder().labels(of("le", "0.025")).value(100).build(),
                    Sample.builder().labels(of("le", "1.25")).value(300).build(),
                    Sample.builder().labels(of("le", "0.75")).value(122).build(),
                    Sample.builder().labels(of("le", String.valueOf(Integer.MAX_VALUE))).value(410).build())
                ),
                "instance_cpu_percentage.histogram()",
                Result.success(SampleFamily.build(
                    Sample.builder().labels(of("le", "0")).value(100).build(),
                    Sample.builder().labels(of("le", "25")).value(22).build(),
                    Sample.builder().labels(of("le", "750")).value(178).build(),
                    Sample.builder().labels(of("le", "1250")).value(110).build())
                ),
                false,
            },
            {
                "histogram_percentile",
                of("instance_cpu_percentage", SampleFamily.build(
                    Sample.builder().labels(of("le", "0.025")).value(100).build(),
                    Sample.builder().labels(of("le", "1.25")).value(300).build(),
                    Sample.builder().labels(of("le", "0.75")).value(122).build(),
                    Sample.builder().labels(of("le", String.valueOf(Integer.MAX_VALUE))).value(410).build())
                ),
                "instance_cpu_percentage.histogram().histogram_percentile([75,99])",
                Result.success(SampleFamily.build(
                    Sample.builder().labels(of("le", "0")).value(100).build(),
                    Sample.builder().labels(of("le", "25")).value(22).build(),
                    Sample.builder().labels(of("le", "750")).value(178).build(),
                    Sample.builder().labels(of("le", "1250")).value(110).build())
                ),
                false,
            },
        });
    }

    @Test
    public void test() {
        Expression e = DSL.parse(expression);
        Result r = null;
        try {
            r = e.run(input);
        } catch (Throwable t) {
            if (isThrow) {
                return;
            }
            log.error("Test failed", t);
            fail("Should not throw anything");
        }
        if (isThrow) {
            fail("Should throw something");
        }
        assertThat(r, is(want));
    }
}