# Kestra Todoist Plugin

## What

- Provides plugin components under `io.kestra.plugin.todoist`.
- Includes classes such as `CreateTask`, `CompleteTask`, `DeleteTask`, `GetTask`.

## Why

- What user problem does this solve? Teams need to manage Todoist tasks through the Todoist API from orchestrated workflows instead of relying on manual console work, ad hoc scripts, or disconnected schedulers.
- Why would a team adopt this plugin in a workflow? It keeps Todoist steps in the same Kestra flow as upstream preparation, approvals, retries, notifications, and downstream systems.
- What operational/business outcome does it enable? It reduces manual handoffs and fragmented tooling while improving reliability, traceability, and delivery speed for processes that depend on Todoist.

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

## References

- https://kestra.io/docs/plugin-developer-guide
- https://kestra.io/docs/plugin-developer-guide/contribution-guidelines
