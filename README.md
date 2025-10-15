# Kestra Todoist Plugin

Integrate Todoist task management with Kestra workflows. Create, list, and complete tasks programmatically as part of your automation pipelines.

## Available Tasks

### CreateTask

Creates a new task in Todoist.

**Parameters:**

- `apiToken` (required): Your Todoist API token
- `content` (required): Task title/content
- `taskDescription` (optional): Detailed description
- `priority` (optional): Priority level (1-4, where 4 is urgent)
- `projectId` (optional): ID of the project to add the task to
- `dueString` (optional): Human-readable due date (e.g., "tomorrow", "next Monday")

**Outputs:**

- `taskId`: ID of the created task
- `content`: Task content
- `url`: URL to view the task
- `response`: Full API response

### ListTasks

Retrieves a list of tasks from Todoist.

**Parameters:**

- `apiToken` (required): Your Todoist API token
- `projectId` (optional): Filter by project ID
- `filter` (optional): Custom filter (e.g., "today", "overdue", "priority 1")

**Outputs:**

- `tasks`: List of task objects
- `count`: Number of tasks retrieved

### GetTask

Retrieves details of a specific task.

**Parameters:**

- `apiToken` (required): Your Todoist API token
- `taskId` (required): ID of the task to retrieve

**Outputs:**

- `task`: Complete task object
- `taskId`: Task ID
- `content`: Task content

### CompleteTask

Marks a task as completed.

**Parameters:**

- `apiToken` (required): Your Todoist API token
- `taskId` (required): ID of the task to complete

**Outputs:**

- `taskId`: ID of the completed task
- `success`: Boolean indicating success

## Getting Started

### Prerequisites

- A Todoist account (free or premium)
- Todoist API token

### Setup Instructions

1. **Get your Todoist API token:**

   - Go to https://todoist.com/app/settings/integrations/developer
   - Scroll to the "API token" section
   - Copy your API token

2. **Configure the secret in Kestra:**

   - In Kestra UI, go to Namespaces
   - Select your namespace
   - Go to Secrets tab
   - Add a new secret with key `TODOIST_API_TOKEN` and your token as the value

   Or for local development, use environment variables (see Development section below)

## Example Workflows

### Create a Task

```yaml
id: create-task
namespace: demo.todoist

tasks:
  - id: create_task
    type: io.kestra.plugin.todoist.CreateTask
    apiToken: "{{ secret('TODOIST_API_TOKEN') }}"
    content: "Review deployment"
    taskDescription: "Check production deployment status"
    priority: 3
    dueString: "today"
```

### List Today's Tasks

```yaml
id: list-tasks
namespace: demo.todoist

tasks:
  - id: list_tasks
    type: io.kestra.plugin.todoist.ListTasks
    apiToken: "{{ secret('TODOIST_API_TOKEN') }}"
    filter: "today"
```

### Complete a Task

```yaml
id: complete-task
namespace: demo.todoist

tasks:
  - id: create_task
    type: io.kestra.plugin.todoist.CreateTask
    apiToken: "{{ secret('TODOIST_API_TOKEN') }}"
    content: "Automated task"

  - id: complete_task
    type: io.kestra.plugin.todoist.CompleteTask
    apiToken: "{{ secret('TODOIST_API_TOKEN') }}"
    taskId: "{{ outputs.create_task.taskId }}"
```

## Development

### Prerequisites

- Java 21
- Docker
- Todoist API token

### Running Tests

```bash
export TODOIST_API_TOKEN=your_token_here
./gradlew test
```

### Building the Plugin

```bash
./gradlew shadowJar
```

### Testing Locally with Kestra

1. **Setup secrets:**

   ```bash
   # Create .env.secrets with your API token
   echo "TODOIST_API_TOKEN=your_token_here" > .env.secrets

   # Encode it for Kestra
   while IFS='=' read -r key value; do echo "SECRET_$key=$(echo -n "$value" | base64)"; done < .env.secrets > .env_encoded
   ```

2. **Build and start:**

   ```bash
   ./gradlew shadowJar
   docker compose up -d
   ```

3. **Access Kestra:** http://localhost:8080

4. **Create a flow:** Go to Flows → Create and paste an example from above

5. **Making changes:** After modifying plugin code:
   ```bash
   ./gradlew shadowJar
   docker compose restart
   ```

### Stopping Kestra

```bash
docker compose down
```

## License

Apache 2.0 © [Kestra Technologies](https://kestra.io)
