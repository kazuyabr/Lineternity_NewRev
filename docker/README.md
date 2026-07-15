# Lineternity Docker Infrastructure

Containerized deployment for Lineternity game server with independent services.

## Architecture

```
lineternity-network (shared)
├── lineternity-mariadb (MariaDB 10.11)
├── lineternity-init-db (creates l2jdb_login)
├── lineternity-loginserver (port 2106)
└── lineternity-gameserver-N (port 7776+N)
```

## Directory Structure

```
docker/
├── Dockerfile              # Multi-stage build (builder + runtime)
├── docker-compose.yml      # Base infrastructure (MariaDB + LoginServer + init-db)
├── stack.py                # Interactive menu manager
├── start.sh                # Wrapper for stack.py
├── entrypoint.sh           # Server startup script
├── init-db.sh              # Database initialization
├── sql/
│   ├── login.sql           # Login database schema
│   └── gameserver.sql      # Game database schema
├── templates/
│   ├── login/              # LoginServer property templates
│   │   ├── loginserver.properties
│   │   └── logging.properties
│   └── game/               # GameServer property templates (22 files)
│       ├── server.properties
│       ├── rates.properties
│       ├── players.properties
│       └── ...
├── gameservers/
│   └── template/
│       ├── docker-compose.yml  # GameServer compose template
│       └── .env.example        # Environment template
└── login/
    └── config/             # Generated LoginServer configs
```

## Quick Start

### Using stack.py (Recommended)

```bash
# Start interactive menu
python docker/stack.py

# Or use the wrapper
docker/start.sh
```

Menu options:
1. Reset completo + rebuild sem cache
2. Criar LoginServer
3. Criar GameServer
4. Remover servidor
5. Listar servidores ativos
6. Logs
7. Edicao em massa de environment
8. Gerenciar perfis de configuracao
9. Exit

### Manual Setup

```bash
# 1. Start base infrastructure
cd docker
docker compose -p lineternity up -d

# 2. Create GameServer directory
mkdir -p gameservers/gameserver-1/config

# 3. Copy compose template
cp gameservers/template/docker-compose.yml gameservers/gameserver-1/

# 4. Create .env file
cp gameservers/template/.env.example gameservers/gameserver-1/.env
# Edit gameservers/gameserver-1/.env with your settings

# 5. Start GameServer
cd gameservers/gameserver-1
docker compose -p lineternity-gs1 up -d
```

## Configuration Modes

### Basic Mode
Only mandatory fields:
- Database connection (Host, Port, User, Password)
- Database names (Login DB, Game DB)
- Network settings (Server ID, Hostname, Port)

### Advanced Mode
All configuration categories:

**GameServer Categories:**
- Database, Network, Identity
- Rates, Players, Protection
- AutoFarm, Limits, Chat
- Events, Clans, RaidBoss
- CancelManager, Auction

**LoginServer Categories:**
- Database, Network, Security

### Advanced Select Mode
Choose specific categories to configure.

## Configuration Profiles

Create, save, and load configuration profiles:

```bash
# Through stack.py menu
8. Gerenciar perfis de configuracao
```

Profiles are stored in `docker/profiles/`.

## Environment Variables

### GameServer

| Variable | Default | Description |
|----------|---------|-------------|
| SERVER_ID | 1 | Unique server ID |
| SERVER_HOSTNAME | gameserver-1 | Server hostname |
| PUBLIC_PORT | 7777 | Public port |
| LOGIN_HOSTNAME | loginserver | LoginServer hostname |
| DB_HOST | mariadb | Database host |
| DB_PORT | 3306 | Database port |
| DB_USER | root | Database user |
| DB_PASSWORD | root | Database password |
| LOGIN_DB | l2jdb_login | Login database |
| GAME_DB | l2jdb_gs1 | Game database |

### LoginServer

| Variable | Default | Description |
|----------|---------|-------------|
| HOSTNAME | localhost | LoginServer IP |
| LOGIN_PORT | 2106 | LoginServer port |
| DB_HOST | mariadb | Database host |
| DB_PORT | 3306 | Database port |
| DB_USER | root | Database user |
| DB_PASSWORD | root | Database password |
| LOGIN_DB | l2jdb_login | Login database |

## Commands

```bash
# Build image
docker compose -p lineternity build

# Start base infrastructure
docker compose -p lineternity up -d

# View logs
docker compose -p lineternity logs -f

# Stop everything
docker compose -p lineternity down -v

# Stop GameServer
cd gameservers/gameserver-1
docker compose -p lineternity-gs1 down
```

## Network

All services communicate through `lineternity-network`:
- `mariadb` - MariaDB hostname
- `loginserver` - LoginServer hostname
- `gameserver-N` - GameServer hostname (network alias)

## Database

- `l2jdb_login` - LoginServer database (created by init-db)
- `l2jdb_gs1` - GameServer 1 database (created on startup)
- `l2jdb_gsN` - GameServer N database (created on startup)

Each GameServer creates its own database automatically on first startup.

## Troubleshooting

### Container won't start
```bash
# Check logs
docker compose -p lineternity logs <service>

# Check status
docker compose -p lineternity ps
```

### Database connection failed
```bash
# Verify MariaDB is running
docker compose -p lineternity logs mariadb

# Test connection
docker exec -it lineternity-mariadb mysql -u root -proot
```

### GameServer not registering
```bash
# Check login database
docker exec -it lineternity-mariadb mysql -u root -proot l2jdb_login -e "SELECT * FROM gameservers;"

# Check hexid.txt
cat gameservers/gameserver-1/config/hexid.txt
```
