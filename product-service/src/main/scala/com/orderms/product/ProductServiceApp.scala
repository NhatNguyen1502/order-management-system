package com.orderms.product

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import com.orderms.product.grpc.ProductServiceHandler

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

object ProductServiceApp {
  
  def main(args: Array[String]): Unit = {
    val system = ActorSystem[Nothing](Behaviors.empty, "ProductService")
    new ProductServiceApp(system).run()
    // Keep the server running until terminated (workaround for demo purposes)
    Await.result(system.whenTerminated, Duration.Inf)
  }
}

class ProductServiceApp(system: ActorSystem[_]) {
  
  private def run(): Future[Http.ServerBinding] = {
    implicit val sys: ActorSystem[_] = system
    implicit val ec: ExecutionContext = system.executionContext
    
    val service: HttpRequest => Future[HttpResponse] =
      ProductServiceHandler(new ProductServiceImpl(system))
    
    val bound = Http()(system)
      .newServerAt(interface = "127.0.0.1", port = 8083)
      .bind(service)
    
    bound.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info(
          "Product gRPC server bound to {}:{}",
          address.getHostString,
          address.getPort
        )
      case Failure(ex) =>
        system.log.error("Failed to bind gRPC endpoint, terminating system", ex)
        system.terminate()
    }
    
    bound
  }
}
