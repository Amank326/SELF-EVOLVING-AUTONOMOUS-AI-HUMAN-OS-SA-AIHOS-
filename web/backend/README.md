# SA-AIHOS Backend Setup

## Overview
This is the backend API server for SA-AIHOS (Self-Evolving Autonomous AI Human OS). It handles:
- AI Agent decision-making and autonomous reasoning
- Memory management (episodic, semantic, procedural)
- Database persistence (SQLite)
- Real-time WebSocket updates
- RESTful API endpoints

## Architecture

### Core Components

**AIAgent.js** - Autonomous decision-making system
- Implements the THINK → ACT → REFLECT → EVOLVE loop
- Manages autonomy levels and decision scheduling
- Tracks cognitive cycles and behavioral evolution

**MemoryManager.js** - Multi-temporal memory storage
- Episodic Memory: Event logs with outcomes
- Semantic Memory: Learned facts and knowledge
- Procedural Memory: Behavioral rules and heuristics

**ReasoningEngine.js** - Decision generation and analysis
- Generates decision options from context
- Scores options based on multiple factors
- Provides explainable reasoning chains

**Database.js** - Persistent storage layer
- SQLite database with complete schema
- Tables for episodes, facts, rules, tasks, decisions
- Analytics and reporting capabilities

### Database Schema

```
episodes - Episodic memory (events with outcomes)
semantic_facts - Learned knowledge
procedural_rules - Behavioral rules with weights
tasks - Task management
decisions - Decision history
reflections - Post-decision analysis
evolution_events - Rule modification history
sessions - User sessions
settings - System configuration
```

## Installation

### Prerequisites
- Node.js 14+ (https://nodejs.org/)
- npm (comes with Node.js)

### Setup Steps

1. **Navigate to backend directory**
   ```bash
   cd web/backend
   ```

2. **Install dependencies**
   ```bash
   npm install
   ```

3. **Start the server**
   ```bash
   npm start
   ```

   Or for development with auto-reload:
   ```bash
   npm run dev
   ```

The server will start on `http://localhost:3000`

## API Endpoints

### Authentication
- `POST /api/auth/login` - Login user
- `POST /api/auth/logout` - Logout user

### Dashboard
- `GET /api/dashboard/overview` - System status and metrics
- `GET /api/dashboard/metrics` - Performance metrics

### AI Agent Control
- `GET /api/agent/status` - Get agent status
- `POST /api/agent/toggle-autonomy` - Enable/disable autonomy
- `POST /api/agent/set-autonomy-level` - Set autonomy percentage
- `POST /api/agent/trigger-reflection` - Trigger deep reflection

### Memory Management
- `GET /api/memory/overview` - Memory statistics
- `GET /api/memory/episodes` - Episodic memory
- `GET /api/memory/semantic` - Semantic facts
- `GET /api/memory/procedural` - Procedural rules
- `POST /api/memory/store-episode` - Store new episode

### Tasks
- `GET /api/tasks` - Get tasks (filter by status)
- `POST /api/tasks/create` - Create new task
- `POST /api/tasks/:id/complete` - Mark task complete

### Reasoning
- `GET /api/reasoning/latest` - Get latest reasoning
- `POST /api/reasoning/analyze` - Analyze options

### Evolution
- `GET /api/evolution/history` - Evolution history
- `GET /api/evolution/rules` - Current evolution rules
- `POST /api/evolution/update-rule` - Update rule

### Analytics
- `GET /api/analytics/decision-timeline` - Timeline data
- `GET /api/analytics/performance` - Performance metrics

### Settings
- `GET /api/settings` - Get all settings
- `POST /api/settings/update` - Update setting

## WebSocket Connection

Connect to: `ws://localhost:3000`

Events:
- `agent_decision` - New AI decision made
- `memory_update` - Memory changed
- `task_complete` - Task completed
- `evolution_event` - Rule evolution occurred

## AI Agent Cognition Loop

The AI Agent runs a continuous decision-making loop:

### Phase 1: SENSE
- Gather system context (time, tasks, memory state)
- Detect triggers for decision-making

### Phase 2: THINK
- Generate decision options from context
- Score options based on rules and history
- Select best option

### Phase 3: ACT
- Check autonomy gate (won't act if autonomy % says no)
- Execute selected action
- Log decision and result

### Phase 4: REFLECT
- Analyze outcome (success/failure)
- Identify causality factors
- Generate learning signals

### Phase 5: EVOLVE
- Update behavioral rule weights
- Adjust autonomy level based on performance
- Store evolution events

## Configuration

Default settings are stored in the database. Key parameters:

- `autonomy_level` (0-100): Likelihood to act autonomously
- `decision_interval` (ms): How often to make decisions
- `memory_threshold` (%): When to consolidate memory

## Debugging

### Enable Verbose Logging
The application logs all operations to the console.

### Database Inspection
SQLite database is stored at: `web/backend/data/aihos.db`

Use any SQLite viewer to inspect:
- Episode history
- Learned rules and their weights
- Decision accuracy over time

### Reset Database
```bash
npm run database:reset
```

## Performance Considerations

- Decision cycle: ~100ms
- Memory consolidation: Automatic when >70% full
- WebSocket connection: Real-time updates
- Database queries: Optimized with indexes

## Future Enhancements

- [ ] Local LLM integration for advanced reasoning
- [ ] Multi-agent collaboration
- [ ] Advanced pattern recognition
- [ ] Predictive decision analysis
- [ ] Distributed memory management
- [ ] Advanced analytics dashboard

## Troubleshooting

### Port 3000 already in use
```bash
# Find and kill process on port 3000
# Windows:
netstat -ano | findstr :3000
taskkill /PID <PID> /F

# Mac/Linux:
lsof -i :3000
kill -9 <PID>
```

### Dependencies installation fails
```bash
# Clear npm cache
npm cache clean --force

# Reinstall
npm install
```

### Database corruption
```bash
# Reset database
npm run database:reset

# Restart server
npm start
```

## License
MIT License - See LICENSE file

## Support
For issues and questions, please refer to the main project documentation.
