/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.xxljob.v1_9_2;

import com.xxl.job.core.glue.GlueTypeEnum;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.api.incubator.semconv.code.CodeAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.javaagent.bootstrap.internal.InstrumentationConfig;
import io.opentelemetry.javaagent.instrumentation.xxljob.common.XxlJobCodeAttributesGetter;
import io.opentelemetry.javaagent.instrumentation.xxljob.common.XxlJobExperimentalAttributeExtractor;
import io.opentelemetry.javaagent.instrumentation.xxljob.common.XxlJobProcessRequest;
import io.opentelemetry.javaagent.instrumentation.xxljob.common.XxlJobSpanNameExtractor;

public final class XxlJobSingletons {
  private static final String INSTRUMENTATION_NAME = "io.opentelemetry.xxl-job-1.9.2";

  private static final boolean CAPTURE_EXPERIMENTAL_SPAN_ATTRIBUTES =
      InstrumentationConfig.get()
          .getBoolean("otel.instrumentation.xxl-job.experimental-span-attributes", false);

  private static final Instrumenter<XxlJobProcessRequest, Void> INSTRUMENTER;

  static {
    XxlJobSpanNameExtractor spanNameExtractor = new XxlJobSpanNameExtractor();
    InstrumenterBuilder<XxlJobProcessRequest, Void> builder =
        Instrumenter.<XxlJobProcessRequest, Void>builder(
                GlobalOpenTelemetry.get(), INSTRUMENTATION_NAME, spanNameExtractor)
            .addAttributesExtractor(
                CodeAttributesExtractor.create(new XxlJobCodeAttributesGetter()))
            .setSpanStatusExtractor(
                (spanStatusBuilder, xxlJobProcessRequest, response, error) -> {
                  if (error != null
                      || Boolean.FALSE.equals(xxlJobProcessRequest.getSchedulingSuccess())) {
                    spanStatusBuilder.setStatus(StatusCode.ERROR);
                  }
                });
    if (CAPTURE_EXPERIMENTAL_SPAN_ATTRIBUTES) {
      builder.addAttributesExtractor(
          AttributesExtractor.constant(AttributeKey.stringKey("job.system"), "xxl-job"));
      builder.addAttributesExtractor(new XxlJobExperimentalAttributeExtractor());
    }
    INSTRUMENTER = builder.buildInstrumenter();
  }

  public static Instrumenter<XxlJobProcessRequest, Void> instrumenter() {
    return INSTRUMENTER;
  }

  @SuppressWarnings({"Unused", "ReturnValueIgnored"})
  private static void limitSupportedVersions() {
    // GLUE_POWERSHELL was added in 1.9.2. Using this constant here ensures that muzzle will disable
    // this instrumentation on earlier versions where this constant does not exist.
    GlueTypeEnum.GLUE_POWERSHELL.name();
  }

  private XxlJobSingletons() {}
}
