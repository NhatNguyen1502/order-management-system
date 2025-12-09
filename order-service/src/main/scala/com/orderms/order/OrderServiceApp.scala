package com.orderms.order

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import com.orderms.order.grpc.OrderServiceHandler

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object OrderServiceApp {
  
  def main(args: Array[String]): Unit = {
    val system = ActorSystem[Nothing](Behaviors.empty, "OrderService")
    new OrderServiceApp(system).run()
  }
}

class OrderServiceApp(system: ActorSystem[_]) {
  
  def run(): Future[Http.ServerBinding] = {
    implicit val sys: ActorSystem[_] = system
    implicit val ec: ExecutionContext = system.executionContext
    
    val service: HttpRequest => Future[HttpResponse] =
      OrderServiceHandler(new OrderServiceImpl(system))
    
    val bound = Http()
      .newServerAt("0.0.0.0", 8081)
      .bind(service)
    
    bound.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info(
          "Order gRPC server bound to {}:{}",
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
