/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.spymemcached;

import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.spy.memcached.MemcachedNode;
import net.spy.memcached.ops.Operation;

public class MemcachedConnectionInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named("net.spy.memcached.MemcachedConnection");
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isMethod().and(named("addOperation")).and(takesArguments(2)),
        this.getClass().getName() + "$AddOperationAdvice");

    transformer.applyAdviceToMethod(
        isMethod().and(named("insertOperation")).and(takesArguments(2)),
        this.getClass().getName() + "$InsertOperationAdvice");
  }

  @SuppressWarnings("unused")
  public static class AddOperationAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void onEnter(
        @Advice.Argument(0) MemcachedNode node, @Advice.Argument(1) Operation operation) {

      VirtualFieldStore.storeNode(operation, node);
    }
  }

  @SuppressWarnings("unused")
  public static class InsertOperationAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void onEnter(
        @Advice.Argument(0) MemcachedNode node, @Advice.Argument(1) Operation operation) {

      VirtualFieldStore.storeNode(operation, node);
    }
  }
}
