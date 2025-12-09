package com.orderms.product

import akka.actor.typed.ActorSystem
import akka.grpc.GrpcServiceException
import com.orderms.product.grpc._
import io.grpc.Status

import scala.collection.concurrent.TrieMap
import scala.concurrent.{ExecutionContext, Future}
import java.time.Instant

class ProductServiceImpl(system: ActorSystem[_]) extends ProductService {
  
  private implicit val ec: ExecutionContext = system.executionContext
  
  // In-memory storage for simplicity (would be a database in production)
  private val products = TrieMap.empty[String, ProductData]
  
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
    val now = Instant.now()
    
    val product = ProductData(
      productId,
      request.name,
      request.description,
      request.price,
      request.category,
      now,
      now
    )
    
    products.put(productId, product)
    Future.successful(CreateProductResponse(productId))
  }
  
  override def getProduct(request: GetProductRequest): Future[GetProductResponse] = {
    products.get(request.productId) match {
      case Some(product) =>
        val grpcProduct = Product(
          product.productId,
          product.name,
          product.description,
          product.price,
          product.category,
          product.createdAt.toEpochMilli,
          product.updatedAt.toEpochMilli
        )
        Future.successful(GetProductResponse(Some(grpcProduct)))
      case None =>
        Future.failed(
          new GrpcServiceException(
            Status.NOT_FOUND.withDescription(s"Product ${request.productId} not found")
          )
        )
    }
  }
  
  override def updateProduct(request: UpdateProductRequest): Future[UpdateProductResponse] = {
    products.get(request.productId) match {
      case Some(product) =>
        val updated = product.copy(
          name = request.name,
          description = request.description,
          price = request.price,
          category = request.category,
          updatedAt = Instant.now()
        )
        products.put(request.productId, updated)
        Future.successful(UpdateProductResponse(true))
      case None =>
        Future.successful(UpdateProductResponse(false))
    }
  }
  
  override def deleteProduct(request: DeleteProductRequest): Future[DeleteProductResponse] = {
    val removed = products.remove(request.productId).isDefined
    Future.successful(DeleteProductResponse(removed))
  }
  
  override def listProducts(request: ListProductsRequest): Future[ListProductsResponse] = {
    val filtered = if (request.category.nonEmpty) {
      products.values.filter(_.category == request.category)
    } else {
      products.values
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
