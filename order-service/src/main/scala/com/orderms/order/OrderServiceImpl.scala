package com.orderms.order

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.grpc.GrpcServiceException
import akka.util.Timeout
import com.orderms.order.grpc._
import io.grpc.Status

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class OrderServiceImpl(system: ActorSystem[_]) extends OrderService {
  
  private implicit val timeout: Timeout = 5.seconds
  private implicit val ec: ExecutionContext = system.executionContext
  
  val TypeKey: EntityTypeKey[OrderEntity.Command] = EntityTypeKey[OrderEntity.Command]("Order")
  
  private val sharding = ClusterSharding(system)
  
  // Initialize sharding
  sharding.init(Entity(TypeKey) { entityContext =>
    OrderEntity(entityContext.entityId)
  })
  
  override def createOrder(request: CreateOrderRequest): Future[CreateOrderResponse] = {
    val orderId = java.util.UUID.randomUUID().toString
    val items = request.items.map(item => 
      OrderEntity.OrderItem(item.productId, item.productName, item.quantity, item.price)
    ).toList
    
    val entityRef = sharding.entityRefFor(TypeKey, orderId)
    
    entityRef.ask[OrderEntity.OrderResponse](ref => 
      OrderEntity.CreateOrder(request.customerId, items, ref)
    ).map {
      case OrderEntity.OrderCreatedResponse(id, status) =>
        CreateOrderResponse(orderId = id, status = status)
      case _ =>
        throw new GrpcServiceException(Status.INTERNAL.withDescription("Failed to create order"))
    }
  }
  
  override def getOrder(request: GetOrderRequest): Future[GetOrderResponse] = {
    val entityRef = sharding.entityRefFor(TypeKey, request.orderId)
    
    entityRef.ask[OrderEntity.OrderResponse](OrderEntity.GetOrder).map {
      case OrderEntity.OrderDetails(state) =>
        val order = Order(
          orderId = state.orderId,
          customerId = state.customerId,
          items = state.items.map(item => 
            OrderItem(item.productId, item.productName, item.quantity, item.price)
          ),
          status = OrderEntity.OrderStatus.toString(state.status),
          totalAmount = state.totalAmount,
          createdAt = state.createdAt.toEpochMilli,
          updatedAt = state.updatedAt.toEpochMilli
        )
        GetOrderResponse(Some(order))
      case OrderEntity.OrderNotFound(id) =>
        throw new GrpcServiceException(Status.NOT_FOUND.withDescription(s"Order $id not found"))
      case _ =>
        throw new GrpcServiceException(Status.INTERNAL.withDescription("Failed to get order"))
    }
  }
  
  override def updateOrderStatus(request: UpdateOrderStatusRequest): Future[UpdateOrderStatusResponse] = {
    val entityRef = sharding.entityRefFor(TypeKey, request.orderId)
    val status = OrderEntity.OrderStatus.fromString(request.status)
    
    entityRef.ask[OrderEntity.OrderResponse](ref => 
      OrderEntity.UpdateStatus(status, ref)
    ).map {
      case OrderEntity.OrderUpdatedResponse(success) =>
        UpdateOrderStatusResponse(success)
      case OrderEntity.OrderNotFound(id) =>
        throw new GrpcServiceException(Status.NOT_FOUND.withDescription(s"Order $id not found"))
      case _ =>
        throw new GrpcServiceException(Status.INTERNAL.withDescription("Failed to update order"))
    }
  }
  
  override def listOrders(request: ListOrdersRequest): Future[ListOrdersResponse] = {
    // This is a simplified implementation
    // In production, this would query the read model from PostgreSQL
    Future.successful(ListOrdersResponse(Seq.empty, 0))
  }
}
