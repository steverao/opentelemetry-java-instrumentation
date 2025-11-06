/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.redisson;

import io.opentelemetry.instrumentation.api.semconv.network.NetworkAttributesGetter;
import io.opentelemetry.instrumentation.api.semconv.network.ServerAttributesGetter;
import java.net.InetSocketAddress;
import javax.annotation.Nullable;

final class RedissonNetAttributesGetter 
    implements ServerAttributesGetter<RedissonRequest>, NetworkAttributesGetter<RedissonRequest, Void> {

  @Nullable
  @Override
  public String getServerAddress(RedissonRequest request) {
    InetSocketAddress address = request.getAddress();
    return address != null ? address.getHostString() : null;
  }

  @Nullable
  @Override
  public Integer getServerPort(RedissonRequest request) {
    InetSocketAddress address = request.getAddress();
    return address != null ? address.getPort() : null;
  }

  @Override
  public InetSocketAddress getNetworkPeerInetSocketAddress(
      RedissonRequest request, @Nullable Void unused) {
    return request.getAddress();
  }
}
