/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.spymemcached;

import javax.annotation.Nullable;
import net.spy.memcached.MemcachedNode;

/**
 * Thread-local context for storing the actual handling node during span processing.
 * This allows the network attributes getter to access the real server that handled the request.
 */
public final class SpymemcachedRequestContext {
  private static final ThreadLocal<MemcachedNode> CURRENT_HANDLING_NODE = new ThreadLocal<>();

  private SpymemcachedRequestContext() {}

  /**
   * Sets the current handling node in thread-local storage.
   * This is called by completion listeners when they can extract the handling node from the operation.
   */
  public static void setCurrentHandlingNode(MemcachedNode handlingNode) {
    CURRENT_HANDLING_NODE.set(handlingNode);
  }

  /**
   * Gets the current handling node from thread-local storage.
   * This is used by the network attributes getter to get the actual server address.
   */
  @Nullable
  public static MemcachedNode getCurrentHandlingNode() {
    return CURRENT_HANDLING_NODE.get();
  }

  /**
   * Clears the current handling node from thread-local storage.
   * This should always be called in a finally block to prevent memory leaks.
   */
  public static void clearCurrentHandlingNode() {
    CURRENT_HANDLING_NODE.remove();
  }
}