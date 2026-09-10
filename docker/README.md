# Lineternity Docker Infrastructure v2.0

Containerized deployment for Lineternity game server with fully independent services.

## Architecture v2.0

```
lineternity-network (shared)
├── lineternity-mariadb-login (MariaDB 10.11 - LoginServer DB)
├── lineternity-loginserver (port 2106)
├── lineternity-mariadb-gs1 (MariaDB 10.11 - GameServer 1 DB)
├── lineternity-gameserver-1 (port 7777)
├── lineternity-mariadb-gs2 (MariaDB 10.11 - GameServer 2 DB)
├── lineternity-gameserver-2 (port 7778)
└── lineternity-mariadb-gsN (MariaDB 10.11 - GameServer N DB)
```

**Key Features:**
- Each service (LoginServer, GameServer) is fully independent
- Each GameServer has its own local MariaDB
- LoginServer owns `l2jdb_login` (accounts, gameservers, hwid)
- GameServer-N owns `l2jdb_gsN` (characters, items, clans, etc.)
- GameServers register in LoginServer's MariaDB via `LOGIN_DB_HOST`

## Directory Structure

```
docker/
├── Dockerfile                  # Multi-stage build (builder + runtime)
├── docker-compose.yml          # MariaDB LoginServer + network
├── docker-compose.loginserver.yml  # LoginServer service
├── stack.py                    # Interactive menu manager (11 options)
├── entrypoint.sh               # Server startup script
├── sql/
│   ├── login.sql               # Login database schema
│   └── gameserver.sql          # Game database schema
├── templates/
│   ├── login/                  # LoginServer property templates
│   │   ├── loginserver.properties
│   │   └── logging.properties
│   └── game/                   # GameServer property templates (22 files)
│       ├── server.properties
│       ├── rates.properties
│       ├── players.properties
│       └── ...
├── gameservers/
│   └── template/
│       ├── docker-compose.yml  # GameServer compose template
│       └── .env.example        # Environment template
└── login/
    └── config/                 # Generated LoginServer configs
```

## Quick Start

### Using stack.py (Recommended)

```bash
# Start interactive menu
python docker/stack.py
```

Menu options:
1. Iniciar MariaDB LoginServer
2. Iniciar LoginServer
3. Criar GameServer
4. Iniciar GameServer
5. Parar GameServer
6. Parar Todos os Serviços
7. Listar servidores ativos
8. Logs
9. Edicao em massa de environment
10. Gerenciar perfis de configuracao
11. Sair

### Manual Setup

```bash
# 1. Start MariaDB LoginServer
cd docker
docker compose up -d

# 2. Start LoginServer
docker compose -f docker-compose.loginserver.yml --env-file login/.env up -d

# 3. Create GameServer directory
mkdir -p gameservers/gameserver-1/config

# 4. Copy compose template
cp gameservers/template/docker-compose.yml gameservers/gameserver-1/

# 5. Create .env file
cp gameservers/template/.env.example gameservers/gameserver-1/.env
# Edit gameservers/gameserver-1/.env with your settings

# 6. Start GameServer
cd gameservers/gameserver-1
docker compose up -d
```

## Configuration Modes

### Basic Mode
Only mandatory fields:
- Database connection (Host, Port, User, Password)
- LoginServer connection (LOGIN_DB_HOST, LOGIN_HOSTNAME)
- Database names (Login DB, Game DB)
- Network settings (Server ID, Hostname, Port)

### Advanced Mode
All configuration categories:

**GameServer Categories (20 categories):**
- database_local, database_remote, network, identity
- rates, players, protection, autofarm
- limits, chat, events, clans, npcs
- offlineshop, raidboss, safedisconnect, bosszerg
- siege, kamaloka, levelupmaker
- geoengine, translator, language, items, bossHeal

**LoginServer Categories:**
- database, network, security

### Advanced Select Mode
Choose specific categories to configure.

## Environment Variables

### GameServer

| Variable | Default | Description |
|----------|---------|-------------|
| SERVER_ID | 1 | Unique server ID |
| SERVER_HOSTNAME | gameserver-1 | Server hostname |
| PUBLIC_PORT | 7777 | Public port |
| LOGIN_HOSTNAME | loginserver | LoginServer hostname |
| LOGIN_PORT | 9014 | LoginServer port |
| DB_HOST | mariadb | Local MariaDB host |
| DB_PORT | 3306 | Local MariaDB port |
| DB_USER | root | Local MariaDB user |
| DB_PASSWORD | root | Local MariaDB password |
| LOGIN_DB_HOST | mariadb-login | LoginServer MariaDB host |
| LOGIN_DB_PORT | 3306 | LoginServer MariaDB port |
| LOGIN_DB_USER | root | LoginServer MariaDB user |
| LOGIN_DB_PASSWORD | root | LoginServer MariaDB password |
| LOGIN_DB | l2jdb_login | Login database |
| GAME_DB | l2jdb_gs1 | Game database |
| L2_EMAIL | contato@jogatinando.com.br | Server email |

### LoginServer

| Variable | Default | Description |
|----------|---------|-------------|
| HOSTNAME | localhost | LoginServer IP |
| LOGIN_PORT | 2106 | LoginServer port |
| DB_HOST | mariadb | MariaDB host |
| DB_PORT | 3306 | MariaDB port |
| DB_USER | root | MariaDB user |
| DB_PASSWORD | root | MariaDB password |
| LOGIN_DB | l2jdb_login | Login database |
| L2_EMAIL | contato@jogatinando.com.br | Server email |

## Commands

```bash
# Build image
docker compose build

# Start MariaDB LoginServer
docker compose up -d

# Start LoginServer
docker compose -f docker-compose.loginserver.yml --env-file login/.env up -d

# View logs
docker compose logs -f

# Stop everything
docker compose down

# Stop GameServer
cd gameservers/gameserver-1
docker compose down
```

## Network

All services communicate through `lineternity-network`:
- `mariadb-login` - LoginServer MariaDB hostname
- `loginserver` - LoginServer hostname
- `mariadb-gsN` - GameServer-N MariaDB hostname
- `gameserver-N` - GameServer-N hostname

## Database

- `l2jdb_login` - LoginServer database (accounts, gameservers, hwid)
- `l2jdb_gs1` - GameServer 1 database (characters, items, clans)
- `l2jdb_gsN` - GameServer N database

Each GameServer creates its own database automatically on first startup.

## Registration Flow

1. GameServer starts → creates local `l2jdb_gsN` database
2. Connects to LoginServer's MariaDB via `LOGIN_DB_HOST`
3. Inserts into `gameservers` table (server_id, hexid, host)
4. Connects to LoginServer via Java (port 9014)

## Troubleshooting

### Container won't start
```bash
# Check logs
docker compose logs <service>

# Check status
docker compose ps
```

### Database connection failed
```bash
# Verify MariaDB is running
docker compose logs mariadb

# Test connection
docker exec -it lineternity-mariadb-login mysql -u root -proot
```

### GameServer not registering
```bash
# Check login database
docker exec -it lineternity-mariadb-login mysql -u root -proot l2jdb_login -e "SELECT * FROM gameservers;"

# Check hexid.txt
cat gameservers/gameserver-1/config/hexid.txt
```
