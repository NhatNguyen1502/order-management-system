package com.orderms.order

import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect}
import akka.actor.typed.scaladsl.Behaviors

import java.time.Instant

object OrderEntity {

  // Commands
  sealed trait Command
  final case class CreateOrder(customerId: String, items: List[OrderItem], replyTo: ActorRef[OrderResponse]) extends Command
  final case class UpdateStatus(newStatus: OrderStatus, replyTo: ActorRef[OrderResponse]) extends Command
  final case class GetOrder(replyTo: ActorRef[OrderResponse]) extends Command

  // Events
  sealed trait Event
  final case class OrderCreated(orderId: String, customerId: String, items: List[OrderItem], timestamp: Instant) extends Event
  final case class OrderStatusUpdated(orderId: String, status: OrderStatus, timestamp: Instant) extends Event

  // State
  final case class OrderState(
    orderId: String,
    customerId: String,
    items: List[OrderItem],
    status: OrderStatus,
    totalAmount: Double,
    createdAt: Instant,
    updatedAt: Instant
  ) {
    def applyEvent(event: Event): OrderState = event match {
      case OrderCreated(id, custId, orderItems, timestamp) =>
        val total = orderItems.map(item => item.price * item.quantity).sum
        OrderState(id, custId, orderItems, OrderStatus.Pending, total, timestamp, timestamp)
      case OrderStatusUpdated(_, newStatus, timestamp) =>
        copy(status = newStatus, updatedAt = timestamp)
    }
  }

  object OrderState {
    val empty: OrderState = OrderState("", "", List.empty, OrderStatus.Pending, 0.0, Instant.EPOCH, Instant.EPOCH)
  }

  // Domain Models
  final case class OrderItem(productId: String, productName: String, quantity: Int, price: Double)

  sealed trait OrderStatus
  object OrderStatus {
    case object Pending extends OrderStatus
    case object Confirmed extends OrderStatus
    case object Shipped extends OrderStatus
    case object Delivered extends OrderStatus
    case object Cancelled extends OrderStatus

    def fromString(s: String): OrderStatus = s.toLowerCase match {
      case "pending" => Pending
      case "confirmed" => Confirmed
      case "shipped" => Shipped
      case "delivered" => Delivered
      case "cancelled" => Cancelled
      case _ => Pending
    }

    def toString(status: OrderStatus): String = status match {
      case Pending => "pending"
      case Confirmed => "confirmed"
      case Shipped => "shipped"
      case Delivered => "delivered"
      case Cancelled => "cancelled"
    }
  }

  // Responses
  sealed trait OrderResponse
  final case class OrderCreatedResponse(orderId: String, status: String) extends OrderResponse
  final case class OrderUpdatedResponse(success: Boolean) extends OrderResponse
  final case class OrderDetails(state: OrderState) extends OrderResponse
  final case class OrderNotFound(orderId: String) extends OrderResponse

  def apply(orderId: String): Behavior[Command] = {
    Behaviors.setup { context =>
      EventSourcedBehavior[Command, Event, OrderState](
        persistenceId = PersistenceId.ofUniqueId(orderId),
        emptyState = OrderState.empty,
        commandHandler = commandHandler,
        eventHandler = (state, event) => state.applyEvent(event)
      )
    }
  }

  private def commandHandler(state: OrderState, command: Command): ReplyEffect[Event, OrderState] = {
    command match {
      case CreateOrder(customerId, items, replyTo) =>
        if (state.orderId.isEmpty) {
          val event = OrderCreated(state.orderId, customerId, items, Instant.now())
          Effect.persist(event).thenReply(replyTo) { newState =>
            OrderCreatedResponse(newState.orderId, OrderStatus.toString(newState.status))
          }
        } else {
          Effect.reply(replyTo)(OrderCreatedResponse(state.orderId, "already_exists"))
        }

      case UpdateStatus(newStatus, replyTo) =>
        if (state.orderId.nonEmpty) {
          val event = OrderStatusUpdated(state.orderId, newStatus, Instant.now())
          Effect.persist(event).thenReply(replyTo) { _ =>
            OrderUpdatedResponse(true)
          }
        } else {
          Effect.reply(replyTo)(OrderNotFound(state.orderId))
        }

      case GetOrder(replyTo) =>
        if (state.orderId.nonEmpty) {
          Effect.reply(replyTo)(OrderDetails(state))
        } else {
          Effect.reply(replyTo)(OrderNotFound(state.orderId))
        }
    }
  }
}
