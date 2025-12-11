package com.orderms.inventory

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import com.orderms.inventory.grpc.InventoryServiceHandler

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

object InventoryServiceApp {
  
  def main(args: Array[String]): Unit = {
    val system = ActorSystem[Nothing](Behaviors.empty, "InventoryService")
    new InventoryServiceApp(system).run()
    Await.result(system.whenTerminated, Duration.Inf)
  }
}

class InventoryServiceApp(system: ActorSystem[_]) {
  
  def run(): Future[Http.ServerBinding] = {
    implicit val sys: ActorSystem[_] = system
    implicit val ec: ExecutionContext = system.executionContext
    
    val service: HttpRequest => Future[HttpResponse] =
      InventoryServiceHandler(new InventoryServiceImpl(system))
    
    val bound = Http()
      .newServerAt("127.0.0.1", 8082)
      .bind(service)
    
    bound.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info(
          "Inventory gRPC server bound to {}:{}",
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
