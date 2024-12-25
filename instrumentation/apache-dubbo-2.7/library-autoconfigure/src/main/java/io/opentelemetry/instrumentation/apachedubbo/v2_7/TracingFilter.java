/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.apachedubbo.v2_7;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import java.util.concurrent.CompletableFuture;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcInvocation;

final class TracingFilter implements Filter {

  private final Instrumenter<DubboRequest, Result> clientInstrumenter;
  private final Instrumenter<DubboRequest, Result> serverInstrumenter;

  TracingFilter(
      Instrumenter<DubboRequest, Result> clientInstrumenter,
      Instrumenter<DubboRequest, Result> serverInstrumenter) {
    this.clientInstrumenter = clientInstrumenter;
    this.serverInstrumenter = serverInstrumenter;
  }

  @Override
  @SuppressWarnings("deprecation") // deprecation for RpcContext.getContext()
  public Result invoke(Invoker<?> invoker, Invocation invocation) {
    if (!(invocation instanceof RpcInvocation)) {
      return invoker.invoke(invocation);
    }

    RpcContext rpcContext = RpcContext.getContext();
    if (rpcContext.getUrl() == null || "injvm".equals(rpcContext.getUrl().getProtocol())) {
      return invoker.invoke(invocation);
    }

    Instrumenter<DubboRequest, Result> instrumenter;
    boolean isServer = rpcContext.isProviderSide();
    if (this.clientInstrumenter != null && this.serverInstrumenter != null) {
      instrumenter = isServer ? serverInstrumenter : clientInstrumenter;
    } else {
      instrumenter =
          this.clientInstrumenter != null ? this.clientInstrumenter : this.serverInstrumenter;
    }

    Context parentContext = Context.current();
    DubboRequest request = DubboRequest.create((RpcInvocation) invocation, rpcContext);

    if (!instrumenter.shouldStart(parentContext, request)) {
      return invoker.invoke(invocation);
    }
    Context context = instrumenter.start(parentContext, request);

    Result result;
    boolean isSynchronous = true;
    try (Scope ignored = context.makeCurrent()) {
      result = invoker.invoke(invocation);
      if (instrumenter == this.clientInstrumenter) {
        CompletableFuture<Object> future = rpcContext.getCompletableFuture();
        if (future != null) {
          isSynchronous = false;
          future.whenComplete(
              (o, throwable) -> instrumenter.end(context, request, result, throwable));
        }
      }
    } catch (Throwable e) {
      instrumenter.end(context, request, null, e);
      throw e;
    }
    if (isSynchronous) {
      instrumenter.end(context, request, result, result.getException());
    }
    return result;
  }
}
