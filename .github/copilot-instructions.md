# GitHub Copilot instructions for Akka development

- Use Akka Typed APIs (`akka.actor.typed.*`, `akka.persistence.typed.scaladsl`, `akka.projection.scaladsl`) instead of classic actors.
- Model protocols with sealed traits for commands and events, and keep behaviors strongly typed (e.g., `Behavior[Command]` with `Behaviors.setup/receiveMessage`).
- For persistence, prefer `EventSourcedBehavior` with `Effect.persist`/`Effect.none`, `RetentionCriteria`, and projections for read models.
- When using the ask pattern, rely on `context.ask`, `Timeout`, and the `Scheduler` from `context.system` rather than `Await.result`.
- For clustering, use typed `ClusterSharding` with `EntityTypeKey` and per-entity supervision/termination strategies.
- For Akka HTTP endpoints, use the routing DSL with JSON marshalling via `SprayJsonSupport`/`DefaultJsonProtocol`, avoiding manual string bodies.
