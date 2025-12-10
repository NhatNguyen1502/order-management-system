package com.orderms.product

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.{ConnectionContext, Http}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import com.orderms.product.grpc.ProductServiceHandler
import com.typesafe.config.ConfigFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object ProductServiceApp {
  
  def main(args: Array[String]): Unit = {
    val system = ActorSystem[Nothing](Behaviors.empty, "ProductService")
    new ProductServiceApp(system).run()
    import scala.concurrent.Await
    import scala.concurrent.duration.Duration
    Await.result(system.whenTerminated, Duration.Inf)
  }
}

class ProductServiceApp(system: ActorSystem[_]) {
  
  def run(): Future[Http.ServerBinding] = {
    implicit val sys: ActorSystem[_] = system
    implicit val ec: ExecutionContext = system.executionContext
    
    val service: HttpRequest => Future[HttpResponse] =
      ProductServiceHandler(new ProductServiceImpl(system))
    
    val bound = Http()(system)
      .newServerAt(interface = "127.0.0.1", port = 8080)
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
