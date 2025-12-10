name := "order-management-system"
version := "0.1.0"
scalaVersion := "2.13.12"

lazy val akkaVersion = "2.10.12"
lazy val akkaHttpVersion = "10.7.3"
lazy val akkaGrpcVersion = "2.5.8"
lazy val akkaPersistenceVersion = "2.8.5"
lazy val akkaProjectionVersion = "1.6.16"
lazy val SlickVersion = "3.5.1"

lazy val commonSettings = Seq(
  scalaVersion := "2.13.17",
  scalacOptions ++= Seq(
    "-deprecation",
    "-feature",
    "-unchecked",
    "-Xlint"
  ),
  resolvers += "Akka library repository".at(s"https://repo.akka.io/fOnF6aq4lmGHCfvMkKEDUvyyaRnfhkJFBqIcPN4r9iux7LK-/secure")
)

lazy val commonDependencies = Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-sharding-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-discovery" % akkaVersion,
  "ch.qos.logback" % "logback-classic" % "1.5.21",
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
  "org.scalatest" %% "scalatest" % "3.2.19" % Test
)

lazy val root = (project in file("."))
  .aggregate(apiGateway, orderService, inventoryService, productService)
  .settings(commonSettings)

lazy val apiGateway = (project in file("api-gateway"))
  .settings(commonSettings)
  .settings(
    name := "api-gateway",
    libraryDependencies ++= commonDependencies ++ Seq(
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "com.lightbend.akka.grpc" %% "akka-grpc-runtime" % akkaGrpcVersion,
      "ch.megard" %% "akka-http-cors" % "1.2.0"
    )
  )
  .enablePlugins(AkkaGrpcPlugin)
  .dependsOn(orderService % "protobuf->compile;compile->compile",
             inventoryService % "protobuf->compile;compile->compile",
             productService % "protobuf->compile;compile->compile")

lazy val orderService = (project in file("order-service"))
  .settings(commonSettings)
  .settings(
    name := "order-service",
    libraryDependencies ++= commonDependencies ++ Seq(
      "com.typesafe.akka" %% "akka-persistence-typed" % akkaPersistenceVersion,
      "com.typesafe.akka" %% "akka-persistence-query" % akkaPersistenceVersion,
      "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion,
      "com.lightbend.akka" %% "akka-projection-eventsourced" % akkaProjectionVersion,
      "com.lightbend.akka" %% "akka-projection-jdbc" % akkaProjectionVersion,
      "com.lightbend.akka" %% "akka-projection-slick" % akkaProjectionVersion,
      "com.lightbend.akka" %% "akka-persistence-jdbc" % "5.5.4",
      "com.typesafe.slick" %% "slick" % SlickVersion,
      "com.typesafe.slick" %% "slick-hikaricp" % SlickVersion,
      "org.postgresql" % "postgresql" % "42.6.0",
      "com.zaxxer" % "HikariCP" % "5.1.0"
    )
  )
  .enablePlugins(AkkaGrpcPlugin)

lazy val inventoryService = (project in file("inventory-service"))
  .settings(commonSettings)
  .settings(
    name := "inventory-service",
    libraryDependencies ++= commonDependencies ++ Seq(
      "com.typesafe.akka" %% "akka-persistence-typed" % akkaPersistenceVersion,
      "com.typesafe.akka" %% "akka-persistence-query" % akkaPersistenceVersion,
      "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion,
      "com.lightbend.akka" %% "akka-persistence-jdbc" % "5.5.4",
      "com.typesafe.slick" %% "slick" % SlickVersion,
      "com.typesafe.slick" %% "slick-hikaricp" % SlickVersion,
      "org.postgresql" % "postgresql" % "42.6.0",
      "com.zaxxer" % "HikariCP" % "7.0.2"
    )
  )
  .enablePlugins(AkkaGrpcPlugin)

lazy val productService = (project in file("product-service"))
  .settings(commonSettings)
  .settings(
    name := "product-service",
    libraryDependencies ++= commonDependencies
  )
  .enablePlugins(AkkaGrpcPlugin)
