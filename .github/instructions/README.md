# GitHub Copilot Instructions

This directory contains instruction files for GitHub Copilot to provide context-aware assistance when working on different parts of the Order Management System.

## Instruction Files

| File | Applies To | Description |
|------|-----------|-------------|
| `general.instructions.md` | `**/*` | General guidelines for the entire repository, including architecture, technology stack, and common commands |
| `akka.instructions.md` | `**/*.scala` | Akka-specific guidelines for building microservices with Scala, including event sourcing, CQRS, and actor patterns |
| `front-end.instructions.md` | `frontend/**` | Angular 20 frontend guidelines covering component structure, naming conventions, and best practices |
| `grpc.instructions.md` | `**/*.proto` | Protocol Buffer and gRPC guidelines for service definitions and API design |
| `docker.instructions.md` | `**/Dockerfile, **/docker-compose*.yml` | Docker and infrastructure guidelines for containerization and deployment |

## How It Works

GitHub Copilot automatically uses these instruction files based on the file patterns specified in the `applyTo` field of each file's frontmatter. When you're working on:

- **Scala files** (`.scala`): Both general and Akka instructions apply
- **Frontend files** (`frontend/**`): Both general and front-end instructions apply
- **Proto files** (`.proto`): Both general and gRPC instructions apply
- **Docker files**: Both general and Docker instructions apply
- **Any other file**: General instructions apply

## Customizing Instructions

To add or modify instructions:

1. Create a new `.instructions.md` file in this directory
2. Add frontmatter with `description` and `applyTo` pattern:
   ```yaml
   ---
   description: 'Your description here'
   applyTo: 'pattern/**/*.ext'
   ---
   ```
3. Write your guidelines in Markdown format
4. Commit the file to the repository

## File Structure Template

```markdown
---
description: 'Brief description of what these instructions cover'
applyTo: 'glob/pattern/**/*.ext'
---

# Title

## Section 1
Guidelines and best practices...

## Section 2
More guidelines...
```

## Pattern Syntax

The `applyTo` field uses glob patterns:
- `**/*` - All files in all directories
- `**/*.scala` - All Scala files anywhere
- `frontend/**` - All files in the frontend directory
- `**/Dockerfile` - All Dockerfile files anywhere
- Multiple patterns: `**/Dockerfile,**/docker-compose*.yml`

## Best Practices for Instructions

1. **Be Specific**: Provide concrete examples and code snippets
2. **Be Concise**: Focus on essential information and patterns
3. **Be Consistent**: Use consistent terminology across instruction files
4. **Stay Updated**: Keep instructions synchronized with codebase changes
5. **Test Patterns**: Verify `applyTo` patterns match the intended files

## Resources

- [GitHub Copilot Documentation](https://docs.github.com/en/copilot)
- [Best Practices for Copilot Coding Agent](https://gh.io/copilot-coding-agent-tips)
- [Order Management System Architecture](../../ARCHITECTURE.md)

## Contributing

When adding new services, features, or changing architecture:

1. Review existing instruction files
2. Update relevant instructions
3. Add new instruction files if needed
4. Test that patterns match correctly
5. Update this README

## Maintenance

These instruction files should be reviewed and updated:
- When adding new services or components
- When changing coding standards or patterns
- When upgrading major dependencies
- After significant architectural changes
- Periodically as part of technical debt reduction
