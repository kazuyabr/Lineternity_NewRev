# Lineternity_NewRev Session Summary

## Status
✅ LoginServer (port 2106) - Ready
✅ GameServer-1 (port 7777) - Connected as [1] Bartz
✅ GameServer-2 (port 7778) - Connected (opcional)
✅ MariaDB Login (3306) - l2jdb_login ready
✅ MariaDB GS1 (3306) - l2jdb_gs1 ready (89 tables from tools/sql/)
✅ All containers running

## Arquitetura de Bancos de Dados
```
MariaDB
├── l2jdb_login          # LoginServer (gameservers, accounts, hwid)
├── l2jdb_gs1            # GameServer 1 (characters, items, clans, etc.)
├── l2jdb_gs2            # GameServer 2 (characters, items, clans, etc.)
└── l2jdb_gsN            # GameServer N...
```

## SCHEMA: tools/sql/ (fonte única)
- O schema real e completo está em `tools/sql/` (arquivos SQL individuais do aCis/RusaCis + novos)
- `character_status_points.sql` incluído consolidado (Status Points v4, sem reset_date)
- As antigas cópias fabricadas `sql/gameserver.sql`, `sql/login.sql` e `docker/sql/` foram REMOVIDAS
- O entrypoint importa de `/lineternity/tools/sql/` + aplica `tools/sql/migrations/`
- Se precisar recriar o banco, delete o database e reinicie o GameServer (ele reimporta automaticamente)

## Estrutura de Containers
```
lineternity-mariadb      # MariaDB central
lineternity-init-db      # Inicializa databases (roda uma vez)
lineternity-loginserver  # LoginServer único
lineternity-gameserver-1 # GameServer canal 1
lineternity-gameserver-2 # GameServer canal 2 (opcional)
```

## Fluxo de Inicialização
1. **init-db**: Cria databases (l2jdb_login, l2jdb_gs1, l2jdb_gs2) e importa tabelas
2. **loginserver**: Conecta em l2jdb_login, verifica gameservers registrados
3. **gameserver-N**: 
   - Aguarda MySQL estar pronto
   - Gera hexid aleatório (se não existir)
   - Registra em l2jdb_login.gameservers
   - Salva hexid.txt em game/config/
   - Conecta em l2jdb_gsN para operações de game

## Menu Stack (docker/stack.py v2.6)
```
1.  Compilar Projeto (Build)       ← Gradle build + distribution
2.  Criar Base (Setup Completo)    ← Wizard: MariaDB + LoginServer + GameServer #1
3.  Iniciar LoginServer            ← Detect MariaDB, build, start
4.  Iniciar GameServer             ← Select/create server, build, start
5.  Parar GameServer               ← Stop specific GameServer
6.  Parar Todos os Serviços        ← Stop all containers
7.  Listar servidores ativos       ← Show running containers
8.  Logs                           ← View container logs
9.  Editar Config por Servidor     ← Bulk edit properties per server
10. Gerenciar perfis de config     ← Save/load/delete config profiles
11. Setar GM / Access Level        ← Set GM access via DB
12. Atualizar Imagens              ← Pull/update Docker images
13. Atualizar Dados nos Containers ← docker cp config/xml to running containers
14. Aplicar Migrations SQL         ← Apply pending SQL migrations (log em logs/migrations-*.log)
15. Sincronizar Configs Docker → Source ← Para quem compila pela tools/ (sql.* protegidas)
16. Modo de Rede                   ← LOCAL / LAN / INTERNET (submenu, mostra estado atual)
17. Sair
```

### Modo de Rede (opção 16)
| Modo | BIND_PREFIX | Hostname anunciado | Acesso |
|------|-------------|--------------------|--------|
| LOCAL | `127.0.0.1:` | `127.0.0.1` | Só a máquina host |
| LAN | vazio (aberto) | IP LAN detectado | PCs da mesma rede |
| INTERNET | vazio (aberto) | IP público detectado ou DDNS | Externo (exige port-forward 2106+7777) |

Persistido como `NET_MODE` nos `.env`. O endereço anunciado vem do `GameServerAuth`
(GS envia sua chave `Hostname` → login resolve → `ServerList` entrega ao cliente).
**NUNCA anunciar nomes internos do Docker** (só resolvem na rede interna do Docker).

### Fluxo de Build (opção 1)
```
stack.py build_project()
  → java.exe GradleWrapperMain --no-daemon --rerun-tasks build distribution -x test
  → build.gradle.kts "distribution" task copia:
      game/data/, game/config/, libs/, login/, images/, sound/, Hwid/, tools/
      entrypoint.sh, init-db.sh, Dockerfile, docker/, gradle/, cache/
  → Gera: build/distribution/ (contexto para Docker)
```

### Fluxo de Container (opções 3/4)
```
stack.py start_loginserver() / start_game_server()
  → Verifica build/distribution/ existe
  → Cria rede lineternity-network
  → docker-compose build --no-cache (usa docker/Dockerfile)
  → docker-compose up -d
  → Dockerfile: COPY . . → /lineternity/
  → ENTRYPOINT: /lineternity/docker/entrypoint.sh
```

### Docker Compose Files (docker/)
| Arquivo | Uso |
|---------|-----|
| `docker-compose.loginserver.yml` | LoginServer + MariaDB embedded |
| `docker-compose.loginserver-external.yml` | LoginServer + MariaDB externo |
| `gameservers/template/docker-compose.yml` | Template para GameServers |
| `gameservers/gameserver-N/docker-compose.yml` | GameServer específico |

### ⚠️ IMPORTANTE: Usar stack.py, NÃO docker-compose
- **NUNCA** rodar `docker compose` diretamente — sempre usar stack.py
- Stack.py já gerencia build, rede, .env, e sincronização entre serviços
- Exceção: `docker exec` para queries diretas no MariaDB

## Fases Implementadas
| Fase | Descrição | Status |
|------|-----------|--------|
| 1 | Email fix: brprojeto→contato@jogatinando.com.br | ✅ |
| 2 | Compose separation: MariaDB + LoginServer + GameServer | ✅ |
| 3 | Entrypoint rewrite: login/gameserver modes | ✅ |
| 4 | GAME_CATEGORIES Part 1: database, network, events, npcs, offlineshop, raidboss | ✅ |
| 5 | GAME_CATEGORIES Part 2: safedisconnect, bosszerg, siege, kamaloka | ✅ |
| 6 | GAME_CATEGORIES Part 3: levelupmaker, geoengine, translator, language, items, bossHeal | ✅ |
| 7 | Menu update: 9→12 options with new categories | ✅ |
| 8 | Placeholders added to 14 non-templated property files | ✅ |
| 9 | README.md updated, tagged v2.0.0-docker | ✅ |
| 10 | MariaDB auto-detection for LoginServer | ✅ |
| 10b | Credential fixes: user/password prompts | ✅ |
| 10c | Docker exec fix: Windows compatibility | ✅ |
| 11 | Create Base wizard: full setup for new machines | ✅ |
| 12 | SQL Schema fix: import from tools/sql/ instead of broken sql/gameserver.sql | ✅ |
| 12b | JvmOptimizer stack trace fix for crash debugging | ✅ |
| 12c | entrypoint.sh SQL path fix (dirname → /l2Brproject/sql/) | ✅ |
| 13 | Unify Pix.properties into donation.properties (remove legacy) | ✅ |

## ⚠️ MERGE CONFLICT: Pix.properties (REMOVIDO)

**O que aconteceu**: O upstream (aCis/RusaCis) ainda possui `game/config/Pix.properties`. Este arquivo foi **removido** na fase 13 porque suas configurações foram consolidadas em `donation.properties`.

**Por que foi removido**: `Pix.properties` e `donation.properties` definiam as mesmas chaves (MercadoPagoApiToken, PayTime, CheckTime, etc.). O código Java usava APENAS UM token global (`Config.DONATION_MP_TOKEN`), então a duplicação era desnecessária.

**O que fazer se houver conflito com `Pix.properties` em merge**:
1. **NÃO aceitar o arquivo do upstream** — ele é obsoleto para este projeto
2. Remover `Pix.properties` do branch de merge (`git rm game/config/Pix.properties`)
3. Verificar se o upstream adicionou **novas chaves** em `Pix.properties`
4. Se sim, migrar essas novas chaves para `donation.properties` na seção "Master Switches"
5. Verificar se `Config.java:loadDonation()` referencia `Pix.properties` — se sim, remover

**Chaves que estavam em `Pix.properties` e foram migradas para `donation.properties`**:
| Chave original | Nova chave em donation.properties |
|----------------|-----------------------------------|
| `EnablePixMod` | `EnablePixMod` (Master Switches) |
| `AnnounceDonatorItemGlobal` | `AnnounceDonatorItemGlobal` (Master Switches) |
| `MercadoPagoApiToken` | Já existia (redundante) |
| `AllowedEmailAddresses` | Já existia (redundante) |
| `PayTime` / `CheckTime` | Já existiam (redundantes) |
| `PurchasableItems` (plural) | `PurchasableItem` (singular) |
| `DeleteExpiredPurchases` | `DeleteInactivePurchases` |
| `HideCompletedPurchases` | `HideEndedPurchases` |

## Estratégia de Merge com CORE

### Branches
- `origin/main` = upstream (aCis/RusaCis)
- `main` = espelho do upstream
- `MERGE-VERSION-CORE-v{versão}` = branch documentada do merge
- `develop` = nosso trabalho
- `feature/*` = features

### Arquivos que NUNCA aceitar do upstream
| Arquivo | Ação |
|---------|------|
| `game/config/Pix.properties` | REMOVER (consolidado em donation.properties) |
| `game/config/brproject.properties` | REMOVER (usamos brand.properties) |
| `BrProjectMeta.java` | Manter LINETERNITY branding |
| `Team.java` | Manter L2Lineternity header |
| `JvmOptimizer.java` | Manter paths lineternity |
| `libs/server.jar` | Remover e rebuildar |
| `StartBrproject.bat` | Remover |
| `cache/brproject-java.inc.bat` | Remover |

### Arquivos que precisam de merge cuidadoso
| Arquivo | O que verificar |
|---------|-----------------|
| `Config.java` | Novas configs → migrar para nossos arquivos |
| `players.properties` | Migrar augmentation → augmented.properties |

### `augmented.properties` — NÃO substituir por merge
Este arquivo é específico do Lineternity. Se o upstream criar `augmented.properties`:
1. Manter nossa versão
2. Verificar se há novas configs do upstream → adicionar
3. Documentar mudança em CLAUDE.md

### `brand.properties` — NÃO substituir por merge
Mesmo caso de augmented.properties.

## Augmentation Settings (`augmented.properties`)
| Config | Descrição | Padrão |
|--------|-----------|--------|
| `AllowAugmentedTrade` | Permite trade de itens augmented | False |
| `AugmentationNGGlowChance` | Chance de glow (No-Grade) | 0 |
| `AugmentationMidGlowChance` | Chance de glow (Mid-Grade) | 40 |
| `AugmentationHighGlowChance` | Chance de glow (High-Grade) | 70 |
| `AugmentationTopGlowChance` | Chance de glow (Top-Grade) | 100 |
| `AugmentationNGSkillChance` | Chance de skill (No-Grade) | 15 |
| `AugmentationMidSkillChance` | Chance de skill (Mid-Grade) | 30 |
| `AugmentationHighSkillChance` | Chance de skill (High-Grade) | 45 |
| `AugmentationTopSkillChance` | Chance de skill (Top-Grade) | 60 |
| `AugmentationBaseStatChance` | Chance de stat base | 1 |

## Variáveis de Ambiente
| Variável | Descrição | Padrão |
|----------|-----------|--------|
| DB_HOST | Host do MariaDB | mariadb |
| DB_PORT | Porta do MariaDB | 3306 |
| DB_USER | Usuário do MariaDB | root |
| DB_PASSWORD | Senha do MariaDB | root |
| LOGIN_DB | Database do LoginServer | l2jdb_login |
| GAME_DB | Database do GameServer | l2jdb_gs1 |
| SERVER_ID | ID do GameServer | 1 |
| SERVER_HOSTNAME | Hostname do GameServer | gameserver-1 |

## Próximos Passos
- Testar login de cliente (cliente original + Fermata) com AllowGuardSystem=True (fix _hasHWID em GameClient/SendProtocolVersion)
- Adicionar mais GameServers descomentando no docker-compose.yml
- Configurar volumes para persistir dados dos gameservers
- Limpar logs de debug (ERRO while loading chat filter words, custom event data, etc.)

## Debug Mode (stack.py opção 17) — Networking
O debug roda o GameServer Java FORA do Docker (host Windows) usando os containers Docker existentes.
Como o Windows host NÃO alcança a rede bridge do Docker (172.x.x.x), as compose files EXPÕEM portas
no host e o `debug_game_server()` pluga `127.0.0.1:PORTA` em `server.properties`:

| Serviço | Porta host | Container | Uso no debug |
|---------|-----------|----------|--------------|
| mariadb-gsN | `13306:3306` | lineternity-mariadb-gsN | sql.url = jdbc:mariadb://127.0.0.1:13306/l2jdb_gsN |
| loginserver (proto Java) | `19014:9014` | lineternity-loginserver | LoginHost=127.0.0.1 + LoginPort=19014 |
| mariadb-login | `13308:3306` | lineternity-mariadb-login | (não usado pelo debug; GS usa host.docker.internal:3308) |

**IMPORTANTE**: Se os containers foram criados ANTES de adicionar as portas, recreate com
`docker compose ... up -d --force-recreate` (docker compose não detecta mudança de porta sozinho).

## LoginServer: External vs Embedded MariaDB
- `docker/login/.env` tem `EXTERNAL_MARIADB=true` + `DB_HOST=localhost` + `DB_PORT=3308`.
- Isso significa o LoginServer usa o MariaDB do HOST (container `mariadb` em `0.0.0.0:3308`, que tem `l2jdb_login`).
- Por isso o compose correto é `docker-compose.loginserver-external.yml` (DB_HOST=host.docker.internal, DB_PORT=3308).
- NUNCA use `docker-compose.loginserver.yml` (embedded) com esse .env — o LoginServer tentaria `localhost:3308` DENTRO do container (sem MariaDB) e falharia.
- O GameServer-1 `.env` tem `LOGIN_DB_HOST=host.docker.internal` + `LOGIN_DB_PORT=3308` (mesmo host MariaDB). Consistente.

## HWID Dual-Client Fix (AllowGuardSystem=True)
`AllowGuardSystem` controla cryptography key transform (enableCrypt) E HWID validation. Para aceitar
cliente original (com HWID) E Fermata (sem HWID) simultaneamente:
- `GameClient._hasHWID` flag: setado em `SendProtocolVersion` pela presença de dados HWID.
- `enableCrypt()` transforma a chave Blowfish SÓ se `_hasHWID` (senão usa chave raw).
- `AuthLogin`/`RequestGameStart`/`EnterWorld` guardam HWID checks com `hwid.isProtectionOn() && getClient().hasHWID()`.
- Log esperado p/ Fermata: "Client ... connected without HWID module".

## Status Points System v3

### Duas Pools Separadas
- **Attribute Points**: STR/CON/DEX/INT/WIT/MEN → multiplicadores exponenciais
- **Direct Status Points**: P.Def/M.Def/HP/MP/CP/P.Atk/M.Atk/Accuracy/Evasion/Crit → bônus flat

### Velocidade NÃO é distribuível
- Velocidade (P.Atk Speed, M.Atk Speed, Move Speed) NÃO entra no pool de pontos
- Velocidade é limitada por DEX/WIT com cap configurável:
  - `MaxAttackSpeedPoints=1300` (P.Atk Speed sem montaria)
  - `MaxMagicAttackSpeedPoints=2200` (M.Atk Speed sem montaria)
  - `MaxMovementSpeedPoints=220` (Move Speed sem montaria)
- Com montaria: sem cap (bônus do mount)
- Hard limits em project.properties: `MaxPAtkSpeed=1500`, `MaxMAtkSpeed=1999`

### MaxDirect* Limits (statuspoints.properties)
| Config | Limite por stat |
|--------|-----------------|
| `MaxDirectPDef` | 200 |
| `MaxDirectMDef` | 100 |
| `MaxDirectHp` | 500 |
| `MaxDirectMp` | 200 |
| `MaxDirectCp` | 300 |
| `MaxDirectPAtk` | 50 |
| `MaxDirectMAtk` | 50 |
| `MaxDirectAccuracy` | 20 |
| `MaxDirectEvasion` | 20 |
| `MaxDirectCrit` | 20 |

### Buttons Logic
- **[+] attribute**: hidden when `attrAvailable=0` OR `isMaxedAttr()` OR `isOldChar`
- **[+] direct**: hidden when `statusAvailable=0` OR `isMaxedDirect()` OR `isOldChar`
- **[-] attribute/direct**: hidden when `current <= confirmed` OR `!dirty` OR `isOldChar`
- **[CONFIRM]**: visible when `!isOldChar && dirty`
- **[RESET]**: visible when `isOldChar || hasAnyDistributed()`

### isOldChar (runtime only)
- Computed: `version < 3 && createTime < StatusPointActivationTimestamp`
- NOT stored in DB (column dropped by migration 004)
- Old chars see Reset button to trigger migration

## Comandos Úteis
```bash
# Build + Start (via stack.py)
python docker/stack.py              # Menu interativo

# Build da imagem (via stack.py opção 1)
python docker/stack.py              # → Opção 1: Compilar Projeto

# Verificar databases
docker exec -it lineternity-mariadb mysql -u root -proot -e "SHOW DATABASES;"

# Verificar gameservers registrados
docker exec -it lineternity-mariadb mysql -u root -proot l2jdb_login -e "SELECT * FROM gameservers;"

# Verificar Status Points de um jogador
docker exec -it lineternity-mariadb mysql -u root -proot l2jdb_gs1 -e "SELECT * FROM character_status_points;"
```

<!-- CORTEX:START -->
## Project Memory (auto-managed by Cortex)

### Last Session
_No session recorded yet._

### Recent Decisions
_No decisions recorded yet._

### Current Context
- Project: Lineternity_NewRev
- Status: Ready

### Open Problems
_No open problems._

_Last updated: 2026-07-01T17:35:44.073Z | Tokens: 64/800_
<!-- CORTEX:END -->
