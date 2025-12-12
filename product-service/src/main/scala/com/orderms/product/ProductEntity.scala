package com.orderms.product

import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect}
import akka.actor.typed.scaladsl.Behaviors

import java.time.Instant

object ProductEntity {

  // Commands
  sealed trait Command
  final case class CreateProduct(name: String, description: String, price: Double, category: String, replyTo: ActorRef[ProductResponse]) extends Command
  final case class UpdateProduct(name: String, description: String, price: Double, category: String, replyTo: ActorRef[ProductResponse]) extends Command
  final case class DeleteProduct(replyTo: ActorRef[ProductResponse]) extends Command
  final case class GetProduct(replyTo: ActorRef[ProductResponse]) extends Command

  // Events
  sealed trait Event
  final case class ProductCreated(productId: String, name: String, description: String, price: Double, category: String, timestamp: Instant) extends Event
  final case class ProductUpdated(productId: String, name: String, description: String, price: Double, category: String, timestamp: Instant) extends Event
  final case class ProductDeleted(productId: String, timestamp: Instant) extends Event

  // State
  final case class ProductState(
    productId: String,
    name: String,
    description: String,
    price: Double,
    category: String,
    deleted: Boolean,
    createdAt: Instant,
    updatedAt: Instant
  ) {
    def applyEvent(event: Event): ProductState = event match {
      case ProductCreated(id, n, d, p, c, timestamp) =>
        ProductState(id, n, d, p, c, deleted = false, timestamp, timestamp)
      case ProductUpdated(_, n, d, p, c, timestamp) =>
        copy(name = n, description = d, price = p, category = c, updatedAt = timestamp)
      case ProductDeleted(_, timestamp) =>
        copy(deleted = true, updatedAt = timestamp)
    }
  }

  object ProductState {
    val empty: ProductState = ProductState("", "", "", 0.0, "", deleted = false, Instant.EPOCH, Instant.EPOCH)
  }

  // Responses
  sealed trait ProductResponse
  final case class ProductCreatedResponse(productId: String) extends ProductResponse
  final case class ProductUpdatedResponse(success: Boolean) extends ProductResponse
  final case class ProductDeletedResponse(success: Boolean) extends ProductResponse
  final case class ProductDetails(state: ProductState) extends ProductResponse
  final case class ProductNotFound(productId: String) extends ProductResponse

  def apply(productId: String): Behavior[Command] = {
    Behaviors.setup { context =>
      EventSourcedBehavior[Command, Event, ProductState](
        persistenceId = PersistenceId.ofUniqueId(s"product-$productId"),
        emptyState = ProductState.empty,
        commandHandler = commandHandler(productId),
        eventHandler = (state, event) => state.applyEvent(event)
      )
    }
  }

  private def commandHandler(productId: String)(state: ProductState, command: Command): ReplyEffect[Event, ProductState] = {
    command match {
      case CreateProduct(name, description, price, category, replyTo) =>
        if (state.productId.isEmpty) {
          val event = ProductCreated(productId, name, description, price, category, Instant.now())
          Effect.persist(event).thenReply(replyTo) { newState =>
            ProductCreatedResponse(newState.productId)
          }
        } else {
          Effect.reply(replyTo)(ProductCreatedResponse(state.productId))
        }

      case UpdateProduct(name, description, price, category, replyTo) =>
        if (state.productId.nonEmpty && !state.deleted) {
          val event = ProductUpdated(state.productId, name, description, price, category, Instant.now())
          Effect.persist(event).thenReply(replyTo) { _ =>
            ProductUpdatedResponse(true)
          }
        } else {
          Effect.reply(replyTo)(ProductNotFound(state.productId))
        }

      case DeleteProduct(replyTo) =>
        if (state.productId.nonEmpty && !state.deleted) {
          val event = ProductDeleted(state.productId, Instant.now())
          Effect.persist(event).thenReply(replyTo) { _ =>
            ProductDeletedResponse(true)
          }
        } else {
          Effect.reply(replyTo)(ProductNotFound(state.productId))
        }

      case GetProduct(replyTo) =>
        if (state.productId.nonEmpty && !state.deleted) {
          Effect.reply(replyTo)(ProductDetails(state))
        } else {
          Effect.reply(replyTo)(ProductNotFound(state.productId))
        }
    }
  }
}
