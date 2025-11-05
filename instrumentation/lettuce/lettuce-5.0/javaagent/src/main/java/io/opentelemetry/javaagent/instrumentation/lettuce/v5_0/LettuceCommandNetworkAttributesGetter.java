/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.lettuce.v5_0;

import io.lettuce.core.protocol.RedisCommand;
import io.opentelemetry.instrumentation.api.semconv.network.ServerAttributesGetter;
import io.opentelemetry.instrumentation.api.util.VirtualField;
import java.net.InetSocketAddress;
import javax.annotation.Nullable;

final class LettuceCommandNetworkAttributesGetter
    implements ServerAttributesGetter<RedisCommand<?, ?, ?>> {

  private static final VirtualField<RedisCommand<?, ?, ?>, InetSocketAddress> serverAddressField =
      VirtualField.find(RedisCommand.class, InetSocketAddress.class);

  @Nullable
  @Override
  public String getServerAddress(RedisCommand<?, ?, ?> request) {
    InetSocketAddress address = serverAddressField.get(request);
    return address != null ? address.getHostString() : null;
  }

  @Nullable
  @Override
  public Integer getServerPort(RedisCommand<?, ?, ?> request) {
    InetSocketAddress address = serverAddressField.get(request);
    return address != null ? address.getPort() : null;
  }

  // Static method to set server address (to be called during instrumentation)
  static void setServerAddress(RedisCommand<?, ?, ?> command, InetSocketAddress address) {
    if (command != null && address != null) {
      serverAddressField.set(command, address);
    }
  }

  // Method to extract connection info - simplified approach without reflection
  static void extractAndSetConnectionInfo(Object connectionObject, RedisCommand<?, ?, ?> command) {
    // For now, we'll skip the complex reflection-based extraction
    // In a production implementation, you would need to:
    // 1. Cast connectionObject to the appropriate Lettuce connection type
    // 2. Extract the RedisURI or connection endpoint
    // 3. Convert to InetSocketAddress and set it
    // 
    // This is left as a placeholder to avoid performance-heavy reflection
    // The actual implementation would depend on Lettuce's specific API
  }
}
