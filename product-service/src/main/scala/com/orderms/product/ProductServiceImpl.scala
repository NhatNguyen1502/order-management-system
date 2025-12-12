package com.orderms.product

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.grpc.GrpcServiceException
import akka.util.Timeout
import com.orderms.product.grpc._
import io.grpc.Status

import scala.collection.concurrent.TrieMap
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import java.time.Instant

class ProductServiceImpl(system: ActorSystem[_]) extends ProductService {

  private implicit val sys: ActorSystem[_] = system
  private implicit val timeout: Timeout = Timeout(5.seconds)
  
  private val sharding = ClusterSharding(system)
  
  // Entity type key for product entities
  private val ProductEntityKey: EntityTypeKey[ProductEntity.Command] =
    EntityTypeKey[ProductEntity.Command]("ProductEntity")
  
  // Initialize cluster sharding for products
  sharding.init(Entity(ProductEntityKey) { entityContext =>
    ProductEntity(entityContext.entityId)
  })
  
  // In-memory cache for list operations (simple caching strategy)
  private val productsCache = TrieMap.empty[String, ProductData]
  
  case class ProductData(
    productId: String,
    name: String,
    description: String,
    price: Double,
    category: String,
    createdAt: Instant,
    updatedAt: Instant
  )
  
  override def createProduct(request: CreateProductRequest): Future[CreateProductResponse] = {
    val productId = java.util.UUID.randomUUID().toString
    val entityRef = sharding.entityRefFor(ProductEntityKey, productId)
    
    import akka.actor.typed.scaladsl.AskPattern._
    implicit val ec: ExecutionContext = system.executionContext
    
    entityRef.ask[ProductEntity.ProductResponse](replyTo => 
      ProductEntity.CreateProduct(request.name, request.description, request.price, request.category, replyTo)
    ).flatMap {
      case ProductEntity.ProductCreatedResponse(id) =>
        // Fetch the product to get the actual timestamp and update cache
        entityRef.ask[ProductEntity.ProductResponse](replyTo => 
          ProductEntity.GetProduct(replyTo)
        ).map {
          case ProductEntity.ProductDetails(state) =>
            productsCache.put(id, ProductData(
              state.productId,
              state.name,
              state.description,
              state.price,
              state.category,
              state.createdAt,
              state.updatedAt
            ))
            CreateProductResponse(id)
          case _ =>
            // Even if we can't cache, the product was created successfully
            CreateProductResponse(id)
        }
      case _ =>
        Future.failed(new GrpcServiceException(Status.INTERNAL.withDescription("Failed to create product")))
    }
  }
  
  override def getProduct(request: GetProductRequest): Future[GetProductResponse] = {
    val entityRef = sharding.entityRefFor(ProductEntityKey, request.productId)
    
    import akka.actor.typed.scaladsl.AskPattern._
    
    entityRef.ask[ProductEntity.ProductResponse](replyTo => 
      ProductEntity.GetProduct(replyTo)
    ).map {
      case ProductEntity.ProductDetails(state) =>
        // Update cache with actual persisted state
        productsCache.put(state.productId, ProductData(
          state.productId,
          state.name,
          state.description,
          state.price,
          state.category,
          state.createdAt,
          state.updatedAt
        ))
        val grpcProduct = Product(
          state.productId,
          state.name,
          state.description,
          state.price,
          state.category,
          state.createdAt.toEpochMilli,
          state.updatedAt.toEpochMilli
        )
        GetProductResponse(Some(grpcProduct))
      case ProductEntity.ProductNotFound(_) =>
        throw new GrpcServiceException(
          Status.NOT_FOUND.withDescription(s"Product ${request.productId} not found")
        )
      case _ =>
        throw new GrpcServiceException(Status.INTERNAL.withDescription("Failed to get product"))
    }
  }
  
  override def updateProduct(request: UpdateProductRequest): Future[UpdateProductResponse] = {
    val entityRef = sharding.entityRefFor(ProductEntityKey, request.productId)
    
    import akka.actor.typed.scaladsl.AskPattern._
    implicit val ec: ExecutionContext = system.executionContext
    
    entityRef.ask[ProductEntity.ProductResponse](replyTo => 
      ProductEntity.UpdateProduct(request.name, request.description, request.price, request.category, replyTo)
    ).flatMap {
      case ProductEntity.ProductUpdatedResponse(success) =>
        if (success) {
          // Fetch the updated product to sync cache with actual timestamp
          entityRef.ask[ProductEntity.ProductResponse](replyTo => 
            ProductEntity.GetProduct(replyTo)
          ).map {
            case ProductEntity.ProductDetails(state) =>
              productsCache.put(request.productId, ProductData(
                state.productId,
                state.name,
                state.description,
                state.price,
                state.category,
                state.createdAt,
                state.updatedAt
              ))
              UpdateProductResponse(true)
            case _ =>
              // Even if we can't cache, the product was updated successfully
              UpdateProductResponse(true)
          }
        } else {
          Future.successful(UpdateProductResponse(false))
        }
      case ProductEntity.ProductNotFound(_) =>
        Future.successful(UpdateProductResponse(false))
      case _ =>
        Future.failed(new GrpcServiceException(Status.INTERNAL.withDescription("Failed to update product")))
    }
  }
  
  override def deleteProduct(request: DeleteProductRequest): Future[DeleteProductResponse] = {
    val entityRef = sharding.entityRefFor(ProductEntityKey, request.productId)
    
    import akka.actor.typed.scaladsl.AskPattern._
    
    entityRef.ask[ProductEntity.ProductResponse](replyTo => 
      ProductEntity.DeleteProduct(replyTo)
    ).map {
      case ProductEntity.ProductDeletedResponse(success) =>
        if (success) {
          productsCache.remove(request.productId)
        }
        DeleteProductResponse(success)
      case ProductEntity.ProductNotFound(_) =>
        DeleteProductResponse(false)
      case _ =>
        throw new GrpcServiceException(Status.INTERNAL.withDescription("Failed to delete product"))
    }
  }
  
  override def listProducts(request: ListProductsRequest): Future[ListProductsResponse] = {
    val filtered = if (request.category.nonEmpty) {
      productsCache.values.filter(_.category == request.category)
    } else {
      productsCache.values
    }
    
    val page = if (request.page <= 0) 1 else request.page
    val pageSize = if (request.pageSize <= 0) 10 else request.pageSize
    val skip = (page - 1) * pageSize
    
    val paged = filtered.slice(skip, skip + pageSize).map { product =>
      Product(
        product.productId,
        product.name,
        product.description,
        product.price,
        product.category,
        product.createdAt.toEpochMilli,
        product.updatedAt.toEpochMilli
      )
    }.toSeq
    
    Future.successful(ListProductsResponse(paged, filtered.size))
  }
}
