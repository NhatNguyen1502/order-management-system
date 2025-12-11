package com.orderms.gateway

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.grpc.GrpcClientSettings
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import ch.megard.akka.http.cors.scaladsl.CorsDirectives.cors
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import com.orderms.inventory.grpc.{GetInventoryRequest, InventoryServiceClient, UpdateInventoryRequest}
import com.orderms.order.grpc.{CreateOrderRequest, GetOrderRequest, OrderItem, OrderServiceClient}
import com.orderms.product.grpc.{CreateProductRequest, GetProductRequest, ListProductsRequest, ProductServiceClient}
import spray.json.{DefaultJsonProtocol, JsObject, RootJsonFormat, enrichAny}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Failure, Success}

trait JsonFormats extends SprayJsonSupport with DefaultJsonProtocol {
  // Order formats
  implicit val orderItemFormat: RootJsonFormat[OrderItemJson] = jsonFormat4(OrderItemJson)
  implicit val createOrderRequestFormat: RootJsonFormat[CreateOrderRequestJson] = jsonFormat2(CreateOrderRequestJson)
  implicit val orderFormat: RootJsonFormat[OrderJson] = jsonFormat7(OrderJson)
  
  // Product formats
  implicit val createProductRequestFormat: RootJsonFormat[CreateProductRequestJson] = jsonFormat4(CreateProductRequestJson)
  implicit val productFormat: RootJsonFormat[ProductJson] = jsonFormat7(ProductJson)
  
  // Inventory formats
  implicit val updateInventoryRequestFormat: RootJsonFormat[UpdateInventoryRequestJson] = jsonFormat3(UpdateInventoryRequestJson)
  implicit val inventoryFormat: RootJsonFormat[InventoryJson] = jsonFormat4(InventoryJson)
}

// JSON DTOs
case class OrderItemJson(productId: String, productName: String, quantity: Int, price: Double)
case class CreateOrderRequestJson(customerId: String, items: List[OrderItemJson])
case class OrderJson(orderId: String, customerId: String, items: List[OrderItemJson], status: String, totalAmount: Double, createdAt: Long, updatedAt: Long)

case class CreateProductRequestJson(name: String, description: String, price: Double, category: String)
case class ProductJson(productId: String, name: String, description: String, price: Double, category: String, createdAt: Long, updatedAt: Long)

case class UpdateInventoryRequestJson(productId: String, quantity: Int, operation: String)
case class InventoryJson(productId: String, availableQuantity: Int, reservedQuantity: Int, updatedAt: Long)

object ApiGateway extends JsonFormats {
  
  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem[_] = ActorSystem(Behaviors.empty, "ApiGateway")
    implicit val ec: ExecutionContext = system.executionContext
    
    // gRPC client settings
    val orderServiceSettings = GrpcClientSettings.connectToServiceAt("localhost", 8081).withTls(false)
    val inventoryServiceSettings = GrpcClientSettings.connectToServiceAt("localhost", 8082).withTls(false)
    val productServiceSettings = GrpcClientSettings.connectToServiceAt("localhost", 8083).withTls(false)
    
    // gRPC clients
    val orderServiceClient = OrderServiceClient(orderServiceSettings)
    val inventoryServiceClient = InventoryServiceClient(inventoryServiceSettings)
    val productServiceClient = ProductServiceClient(productServiceSettings)
    
    val routes = createRoutes(orderServiceClient, inventoryServiceClient, productServiceClient)
    
    val corsSettings = CorsSettings.defaultSettings
    val routesWithCors = cors(corsSettings) {
      routes
    }
    
    val bindingFuture = Http().newServerAt("127.0.0.1", 8080).bind(routesWithCors)
    
    bindingFuture.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info(
          "API Gateway online at http://{}:{}/",
          address.getHostString,
          address.getPort
        )
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
    Await.result(system.whenTerminated, Duration.Inf)
  }
  
  private def createRoutes(
    orderService: OrderServiceClient,
    inventoryService: InventoryServiceClient,
    productService: ProductServiceClient
  ): Route = {
    
    pathPrefix("api") {
      concat(
        // Order endpoints
        pathPrefix("orders") {
          concat(
            pathEnd {
              post {
                entity(as[CreateOrderRequestJson]) { request =>
                  val items = request.items.map(item =>
                    OrderItem(item.productId, item.productName, item.quantity, item.price)
                  )
                  val grpcRequest = CreateOrderRequest(request.customerId, items)
                  
                  onSuccess(orderService.createOrder(grpcRequest)) { response =>
                    complete(StatusCodes.Created, Map("orderId" -> response.orderId, "status" -> response.status))
                  }
                }
              }
            },
            path(Segment) { orderId =>
              get {
                onSuccess(orderService.getOrder(GetOrderRequest(orderId))) { response =>
                  response.order match {
                    case Some(order) =>
                      val orderJson = OrderJson(
                        order.orderId,
                        order.customerId,
                        order.items.map(i => OrderItemJson(i.productId, i.productName, i.quantity, i.price)).toList,
                        order.status,
                        order.totalAmount,
                        order.createdAt,
                        order.updatedAt
                      )
                      complete(orderJson)
                    case None =>
                      complete(StatusCodes.NotFound, "Order not found")
                  }
                }
              }
            }
          )
        },
        // Product endpoints
        pathPrefix("products") {
          concat(
            pathEnd {
              concat(
                post {
                  entity(as[CreateProductRequestJson]) { request =>
                    val grpcRequest = CreateProductRequest(
                      request.name,
                      request.description,
                      request.price,
                      request.category
                    )
                    onSuccess(productService.createProduct(grpcRequest)) { response =>
                      complete(StatusCodes.Created, Map("productId" -> response.productId))
                    }
                  }
                },
                get {
                  parameters("category".optional, "page".as[Int].optional, "pageSize".as[Int].optional) {
                    (category, page, pageSize) =>
                      val request = ListProductsRequest(
                        category.getOrElse(""),
                        page.getOrElse(1),
                        pageSize.getOrElse(10)
                      )
                      onSuccess(productService.listProducts(request)) { response =>
                        val products = response.products.map { p =>
                          ProductJson(p.productId, p.name, p.description, p.price, p.category, p.createdAt, p.updatedAt)
                        }
                        complete(JsObject(
                          "products" -> products.toJson,
                          "total" -> response.total.toJson
                        ))
                      }
                  }
                }
              )
            },
            path(Segment) { productId =>
              get {
                onSuccess(productService.getProduct(GetProductRequest(productId))) { response =>
                  response.product match {
                    case Some(product) =>
                      val productJson = ProductJson(
                        product.productId,
                        product.name,
                        product.description,
                        product.price,
                        product.category,
                        product.createdAt,
                        product.updatedAt
                      )
                      complete(productJson)
                    case None =>
                      complete(StatusCodes.NotFound, "Product not found")
                  }
                }
              }
            }
          )
        },
        // Inventory endpoints
        pathPrefix("inventory") {
          concat(
            path(Segment) { productId =>
              get {
                onSuccess(inventoryService.getInventory(GetInventoryRequest(productId))) { response =>
                  val inventoryJson = InventoryJson(
                    response.productId,
                    response.availableQuantity,
                    response.reservedQuantity,
                    response.updatedAt
                  )
                  complete(inventoryJson)
                }
              }
            },
            path("update") {
              post {
                entity(as[UpdateInventoryRequestJson]) { request =>
                  val grpcRequest = UpdateInventoryRequest(
                    request.productId,
                    request.quantity,
                    request.operation
                  )
                  onSuccess(inventoryService.updateInventory(grpcRequest)) { response =>
                    complete(JsObject(
                      "success" -> response.success.toJson,
                      "newQuantity" -> response.newQuantity.toJson
                    ))
                  }
                }
              }
            }
          )
        }
      )
    }
  }
}
