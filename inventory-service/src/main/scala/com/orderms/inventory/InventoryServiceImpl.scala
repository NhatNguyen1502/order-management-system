package com.orderms.inventory

import akka.actor.typed.{ActorSystem}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.grpc.GrpcServiceException
import akka.util.Timeout
import com.orderms.inventory.grpc._
import io.grpc.Status

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

class InventoryServiceImpl(system: ActorSystem[_]) extends InventoryService {
  
  private implicit val timeout: Timeout = 5.seconds
  private implicit val ec: ExecutionContext = system.executionContext
  
  val TypeKey: EntityTypeKey[InventoryEntity.Command] = EntityTypeKey[InventoryEntity.Command]("Inventory")
  
  private val sharding = ClusterSharding(system)
  
  sharding.init(Entity(TypeKey) { entityContext =>
    InventoryEntity(entityContext.entityId)
  })
  
  override def checkInventory(request: CheckInventoryRequest): Future[CheckInventoryResponse] = {
    val entityRef = sharding.entityRefFor(TypeKey, request.productId)
    
    entityRef.ask[InventoryEntity.InventoryResponse](ref => 
      InventoryEntity.CheckInventory(request.quantity, ref)
    ).map {
      case InventoryEntity.InventoryAvailable(available, currentQty) =>
        CheckInventoryResponse(available, currentQty)
      case _ =>
        throw new GrpcServiceException(Status.INTERNAL.withDescription("Failed to check inventory"))
    }
  }
  
  override def reserveInventory(request: ReserveInventoryRequest): Future[ReserveInventoryResponse] = {
    val entityRef = sharding.entityRefFor(TypeKey, request.productId)
    
    entityRef.ask[InventoryEntity.InventoryResponse](ref => 
      InventoryEntity.ReserveInventory(request.quantity, request.orderId, ref)
    ).map {
      case InventoryEntity.ReservationCreated(success, resId) =>
        ReserveInventoryResponse(success, resId)
      case _ =>
        throw new GrpcServiceException(Status.INTERNAL.withDescription("Failed to reserve inventory"))
    }
  }
  
  override def releaseInventory(request: ReleaseInventoryRequest): Future[ReleaseInventoryResponse] = {
    // In a real implementation, we would need to track which product the reservation belongs to
    // For now, this is a simplified version
    Future.successful(ReleaseInventoryResponse(true))
  }
  
  override def updateInventory(request: UpdateInventoryRequest): Future[UpdateInventoryResponse] = {
    val entityRef = sharding.entityRefFor(TypeKey, request.productId)
    
    entityRef.ask[InventoryEntity.InventoryResponse](ref => 
      InventoryEntity.UpdateInventory(request.quantity, request.operation, ref)
    ).map {
      case InventoryEntity.InventoryUpdateResult(success, newQty) =>
        UpdateInventoryResponse(success, newQty)
      case _ =>
        throw new GrpcServiceException(Status.INTERNAL.withDescription("Failed to update inventory"))
    }
  }
  
  override def getInventory(request: GetInventoryRequest): Future[GetInventoryResponse] = {
    val entityRef = sharding.entityRefFor(TypeKey, request.productId)
    
    entityRef.ask[InventoryEntity.InventoryResponse](InventoryEntity.GetInventory).map {
      case InventoryEntity.InventoryDetails(state) =>
        GetInventoryResponse(
          state.productId,
          state.availableQuantity,
          state.reservedQuantity,
          state.updatedAt.toEpochMilli
        )
      case _ =>
        throw new GrpcServiceException(Status.INTERNAL.withDescription("Failed to get inventory"))
    }
  }
}
