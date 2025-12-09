package com.orderms.inventory

import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect}
import akka.actor.typed.scaladsl.Behaviors

import java.time.Instant

object InventoryEntity {

  // Commands
  sealed trait Command
  final case class CheckInventory(quantity: Int, replyTo: ActorRef[InventoryResponse]) extends Command
  final case class ReserveInventory(quantity: Int, orderId: String, replyTo: ActorRef[InventoryResponse]) extends Command
  final case class ReleaseInventory(reservationId: String, replyTo: ActorRef[InventoryResponse]) extends Command
  final case class UpdateInventory(quantity: Int, operation: String, replyTo: ActorRef[InventoryResponse]) extends Command
  final case class GetInventory(replyTo: ActorRef[InventoryResponse]) extends Command

  // Events
  sealed trait Event
  final case class InventoryReserved(productId: String, quantity: Int, orderId: String, reservationId: String, timestamp: Instant) extends Event
  final case class InventoryReleased(reservationId: String, quantity: Int, timestamp: Instant) extends Event
  final case class InventoryUpdated(productId: String, quantity: Int, operation: String, timestamp: Instant) extends Event

  // State
  final case class InventoryState(
    productId: String,
    availableQuantity: Int,
    reservedQuantity: Int,
    reservations: Map[String, Reservation],
    updatedAt: Instant
  ) {
    def applyEvent(event: Event): InventoryState = event match {
      case InventoryReserved(_, qty, orderId, resId, timestamp) =>
        copy(
          availableQuantity = availableQuantity - qty,
          reservedQuantity = reservedQuantity + qty,
          reservations = reservations + (resId -> Reservation(resId, orderId, qty)),
          updatedAt = timestamp
        )
      case InventoryReleased(resId, qty, timestamp) =>
        copy(
          availableQuantity = availableQuantity + qty,
          reservedQuantity = reservedQuantity - qty,
          reservations = reservations - resId,
          updatedAt = timestamp
        )
      case InventoryUpdated(_, qty, op, timestamp) =>
        op match {
          case "add" => copy(availableQuantity = availableQuantity + qty, updatedAt = timestamp)
          case "set" => copy(availableQuantity = qty, updatedAt = timestamp)
          case _ => this
        }
    }
  }

  object InventoryState {
    def empty(productId: String): InventoryState = 
      InventoryState(productId, 0, 0, Map.empty, Instant.EPOCH)
  }

  final case class Reservation(reservationId: String, orderId: String, quantity: Int)

  // Responses
  sealed trait InventoryResponse
  final case class InventoryAvailable(available: Boolean, currentQuantity: Int) extends InventoryResponse
  final case class ReservationCreated(success: Boolean, reservationId: String) extends InventoryResponse
  final case class ReservationReleased(success: Boolean) extends InventoryResponse
  final case class InventoryUpdateResult(success: Boolean, newQuantity: Int) extends InventoryResponse
  final case class InventoryDetails(state: InventoryState) extends InventoryResponse
  final case class InventoryNotFound(productId: String) extends InventoryResponse

  def apply(productId: String): Behavior[Command] = {
    Behaviors.setup { context =>
      EventSourcedBehavior[Command, Event, InventoryState](
        persistenceId = PersistenceId.ofUniqueId(s"inventory-$productId"),
        emptyState = InventoryState.empty(productId),
        commandHandler = commandHandler,
        eventHandler = (state, event) => state.applyEvent(event)
      )
    }
  }

  private def commandHandler(state: InventoryState, command: Command): ReplyEffect[Event, InventoryState] = {
    command match {
      case CheckInventory(quantity, replyTo) =>
        val available = state.availableQuantity >= quantity
        Effect.reply(replyTo)(InventoryAvailable(available, state.availableQuantity))

      case ReserveInventory(quantity, orderId, replyTo) =>
        if (state.availableQuantity >= quantity) {
          val reservationId = java.util.UUID.randomUUID().toString
          val event = InventoryReserved(state.productId, quantity, orderId, reservationId, Instant.now())
          Effect.persist(event).thenReply(replyTo) { _ =>
            ReservationCreated(true, reservationId)
          }
        } else {
          Effect.reply(replyTo)(ReservationCreated(false, ""))
        }

      case ReleaseInventory(reservationId, replyTo) =>
        state.reservations.get(reservationId) match {
          case Some(reservation) =>
            val event = InventoryReleased(reservationId, reservation.quantity, Instant.now())
            Effect.persist(event).thenReply(replyTo) { _ =>
              ReservationReleased(true)
            }
          case None =>
            Effect.reply(replyTo)(ReservationReleased(false))
        }

      case UpdateInventory(quantity, operation, replyTo) =>
        val event = InventoryUpdated(state.productId, quantity, operation, Instant.now())
        Effect.persist(event).thenReply(replyTo) { newState =>
          InventoryUpdateResult(true, newState.availableQuantity)
        }

      case GetInventory(replyTo) =>
        Effect.reply(replyTo)(InventoryDetails(state))
    }
  }
}
