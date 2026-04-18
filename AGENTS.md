# Kestra Todoist Plugin

## What

- Provides plugin components under `io.kestra.plugin.todoist`.
- Includes classes such as `CreateTask`, `CompleteTask`, `DeleteTask`, `GetTask`.

## Why

- This plugin integrates Kestra with Todoist.
- It provides tasks that manage Todoist tasks through the Todoist API.

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
