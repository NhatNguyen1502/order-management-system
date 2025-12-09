name := "order-management-system"
version := "0.1.0"
scalaVersion := "2.13.12"

lazy val akkaVersion = "2.8.5"
lazy val akkaHttpVersion = "10.5.3"
lazy val akkaGrpcVersion = "2.4.0"
lazy val akkaPersistenceVersion = "2.8.5"
lazy val akkaProjectionVersion = "1.5.0"

lazy val commonSettings = Seq(
  scalaVersion := "2.13.12",
  scalacOptions ++= Seq(
    "-deprecation",
    "-feature",
    "-unchecked",
    "-Xlint"
  ),
  resolvers ++= Seq(
    "Akka library repository".at("https://repo.akka.io/maven"),
    "Akka Projection repository".at("https://repo.akka.io/maven")
  )
)

lazy val commonDependencies = Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-sharding-typed" % akkaVersion,
  "ch.qos.logback" % "logback-classic" % "1.4.11",
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
  "org.scalatest" %% "scalatest" % "3.2.17" % Test
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
      "com.typesafe.akka" %% "akka-http2-support" % akkaHttpVersion,
      "ch.megard" %% "akka-http-cors" % "1.2.0"
    )
  )
  .enablePlugins(AkkaGrpcPlugin)
  .dependsOn(orderService % "protobuf", inventoryService % "protobuf", productService % "protobuf")

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
      "com.typesafe.akka" %% "akka-persistence-jdbc" % "5.3.0",
      "com.typesafe.slick" %% "slick" % "3.4.1",
      "com.typesafe.slick" %% "slick-hikaricp" % "3.4.1",
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
      "com.typesafe.akka" %% "akka-persistence-jdbc" % "5.3.0",
      "com.typesafe.slick" %% "slick" % "3.4.1",
      "com.typesafe.slick" %% "slick-hikaricp" % "3.4.1",
      "org.postgresql" % "postgresql" % "42.6.0",
      "com.zaxxer" % "HikariCP" % "5.1.0"
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
