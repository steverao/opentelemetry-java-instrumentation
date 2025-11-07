/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.spymemcached;

import io.opentelemetry.instrumentation.api.util.VirtualField;
import javax.annotation.Nullable;
import net.spy.memcached.MemcachedNode;
import net.spy.memcached.internal.GetFuture;
import net.spy.memcached.internal.OperationFuture;
import net.spy.memcached.ops.Operation;

/**
 * Central store for all VirtualField instances used in spymemcached instrumentation.
 * This approach is required for indy mode compatibility.
 */
public final class VirtualFieldStore {
  
  // VirtualField to store MemcachedNode in Operation
  private static final VirtualField<Operation, MemcachedNode> OPERATION_NODE_FIELD =
      VirtualField.find(Operation.class, MemcachedNode.class);
  
  // VirtualField to store Operation in OperationFuture
  private static final VirtualField<OperationFuture<?>, Operation> OPERATION_FUTURE_OPERATION_FIELD =
      VirtualField.find(OperationFuture.class, Operation.class);
  
  // VirtualField to store Operation in GetFuture
  private static final VirtualField<GetFuture<?>, Operation> GET_FUTURE_OPERATION_FIELD =
      VirtualField.find(GetFuture.class, Operation.class);

  private VirtualFieldStore() {}

  /**
   * Store MemcachedNode in Operation.
   * Called by MemcachedConnectionInstrumentation when addOperation/insertOperation is invoked.
   */
  public static void storeNode(Operation operation, MemcachedNode node) {
    OPERATION_NODE_FIELD.set(operation, node);
  }

  /**
   * Retrieve MemcachedNode from Operation.
   * Called by CompletionListener to get the actual handling node.
   */
  @Nullable
  public static MemcachedNode getNode(Operation operation) {
    return OPERATION_NODE_FIELD.get(operation);
  }

  /**
   * Store Operation in OperationFuture.
   * Called by SetOperationInstrumentation when setOperation is invoked.
   */
  public static void storeOperation(OperationFuture<?> future, Operation operation) {
    OPERATION_FUTURE_OPERATION_FIELD.set(future, operation);
  }

  /**
   * Store Operation in GetFuture.
   * Called by SetOperationInstrumentation when setOperation is invoked.
   */
  public static void storeOperation(GetFuture<?> future, Operation operation) {
    GET_FUTURE_OPERATION_FIELD.set(future, operation);
  }

  /**
   * Retrieve Operation from OperationFuture.
   * Called by CompletionListener to get the Operation.
   */
  @Nullable
  public static Operation getOperation(OperationFuture<?> future) {
    return OPERATION_FUTURE_OPERATION_FIELD.get(future);
  }

  /**
   * Retrieve Operation from GetFuture.
   * Called by CompletionListener to get the Operation.
   */
  @Nullable
  public static Operation getOperation(GetFuture<?> future) {
    return GET_FUTURE_OPERATION_FIELD.get(future);
  }
}
