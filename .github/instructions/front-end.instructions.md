---
description: 'Guidelines for Angular 20 Frontend'
applyTo: 'frontend/**'
---

# Front End Guidelines

- These instructions are applied to projects: `frontend/`

## File & Folder Organization

### General Principle

- Colocation: Components, services, and utilities that are used only in a single feature must live inside that feature’s folder.
- Feature-first structure: Organize by feature domain, not by technical type (avoid components/, services/ at root).
- Keep related files together: Component + styles + template + test + models should live in the same directory.
- Shallow hierarchy: Avoid more than 3–4 nested folder levels.
- Shared functionality moves to:
  - shared/components/
  - shared/pipes/
  - shared/directives/
  - shared/services/
  - shared/utils/
  - only when reused across multiple features
- Use path aliases defined in tsconfig.json:
  - @app/*, @shared/*, @core/*, @env/*

### Recommended Structure

```
project-root/
|-- src/
    │
    ├── app/
    │   ├── core/                     # Singleton services, guards, interceptors
    │   │     ├── interceptors/
    │   │     ├── guards/
    │   │     ├── services/
    │   │     └── core.module.ts
    │   │
    │   ├── shared/                   # Reusable UI elements/services across features
    │   │     ├── components/
    │   │     ├── directives/
    │   │     ├── pipes/
    │   │     ├── utils/
    │   │     └── models/
    │   │
    │   ├── features/                 # Feature modules, grouped by domain
    │   │     ├── auth/
    │   │     │     ├── login/
    │   │     │     │    ├── login.component.ts
    │   │     │     │    ├── login.component.html
    │   │     │     │    ├── login.component.scss
    │   │     │     │    └── login.model.ts
    │   │     │     └── auth.routes.ts
    │   │     │
    │   │     ├── dashboard/
    │   │     │     ├── dashboard.component.ts
    │   │     │     ├── dashboard.routes.ts
    │   │     │     └── ...
    │   │     │
    │   │     └── <feature>/
    │   │
    │   ├── layouts/
    │   │     ├── admin-layout/
    │   │     ├── public-layout/
    │   │     └── ...
    │   │
    │   ├── app.routes.ts            # Global routing definitions
    │   └── app.config.ts            # Providers, config, DI setup
    │
    ├── assets/
    │
    ├── environments/                # environment.ts, environment.prod.ts
    │
    └── styles/                      # Global styles, mixins, variables
```

### Naming Conventions

- Folders: kebab-case (e.g., `user` or `user-modal`).
- Components: PascalCase (e.g., `UserCardComponent` or `OrderListComponent`)
- Services: PascalCase + Service suffix (e.g., `AuthService` or `UserService`).
- Function/ Variable name: camelCase (e.g. `getAccessToken` or `customerId`).
- Signals: camelCase with Signal suffix (e.g., `userSignal`, `isLoadingSignal`).
- Models/Interfaces: PascalCase (e.g., `User`, `OrderResponse`).
- Functions/variables: camelCase (e.g., `fetchOrders`, `isLoggedIn`).
- Pipes: PascalCase + Pipe suffix (e.g., `DateFormatPipe`).

## Implementation Guidelines

### Components

- Always use standalone components (Angular 20 default).
- Keep components small and single-responsibility.
- Use OnPush change detection by default (changeDetection: ChangeDetectionStrategy.OnPush).
- Use Signals instead of heavy RxJS state where possible.
- Avoid complex logic in components → move to:
  - services
  - signals stores
  - utils

### Component Props (Inputs/Outputs)

- Always type inputs strictly.
- Use @Input({ required: true }) when needed.
- Never mutate input objects; always copy before modifying.

### API routes

- Resource Naming: use plural nouns for collections (`/api/users`, `/api/chat`); avoid use verbs in URL paths and use kebab-case for multi-word resources (`/api/user-profile`)
- HTTP Methods: GET for retrieval, POST for creation, PUT for full updates, PATCH for partial updates, DELETE for removal.
- Define backend endpoints in: src/app/core/constants/api-endpoints.ts

### Authentication

- Centralize auth logic inside AuthService
- Use route guards for: canActivate, canLoad, canMatch
- Never store tokens in local/session storage if possible → use HttpOnly cookies.`signOut`.
- Avoid storing sensitive info in memory without encryption.

### Error handling and logging

- Centralize logging in: shared/utils/logger.ts
- Never use console.log in production code.
- Map server errors via global HTTP interceptor.

### Styling

- Consistent Approach: use Angular Material and SCSS.
- Scoped Styles: Ensure styles are scoped to avoid global conflicts.

### Performance

- Always lazy-load feature routes.`index` as a key if the list can change.
- Prefer standalone components instead of modules.
- Use pure pipes where possible.
- Avoid recreating Observables inside templates.