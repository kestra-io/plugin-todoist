# Kestra Todoist Plugin

## What

description = 'Todoist plugin for Kestra Exposes 6 plugin components (tasks, triggers, and/or conditions).

## Why

Enables Kestra workflows to interact with Todoist, allowing orchestration of Todoist-based operations as part of data pipelines and automation workflows.

## How

### Architecture

Single-module plugin. Source packages under `io.kestra.plugin`:

- `todoist`

Infrastructure dependencies (Docker Compose services):

- `app`

### Key Plugin Classes

- `io.kestra.plugin.todoist.CompleteTask`
- `io.kestra.plugin.todoist.CreateTask`
- `io.kestra.plugin.todoist.DeleteTask`
- `io.kestra.plugin.todoist.GetTask`
- `io.kestra.plugin.todoist.ListTasks`
- `io.kestra.plugin.todoist.UpdateTask`

### Project Structure

```
plugin-todoist/
├── src/main/java/io/kestra/plugin/todoist/
├── src/test/java/io/kestra/plugin/todoist/
├── build.gradle
└── README.md
```

### Important Commands

```bash
# Build the plugin
./gradlew shadowJar

# Run tests
./gradlew test

# Build without tests
./gradlew shadowJar -x test
```

### Configuration

All tasks and triggers accept standard Kestra plugin properties. Credentials should use
`{{ secret('SECRET_NAME') }}` — never hardcode real values.

## Agents

**IMPORTANT:** This is a Kestra plugin repository (prefixed by `plugin-`, `storage-`, or `secret-`). You **MUST** delegate all coding tasks to the `kestra-plugin-developer` agent. Do NOT implement code changes directly — always use this agent.
