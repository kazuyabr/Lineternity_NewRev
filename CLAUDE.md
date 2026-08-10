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

## SCHEMA CRÍTICO: tools/sql/ vs sql/gameserver.sql
**O `sql/gameserver.sql` é um schema FABRICADO/INVENTADO que não corresponde ao código Java!**
- O schema real está em `tools/sql/` (86 arquivos SQL individuais do aCis/RusaCis original)
- O entrypoint agora importa de `tools/sql/` em vez de `sql/gameserver.sql`
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

## Menu Stack (docker/stack.py)
```
1. Criar Base (Setup Completo)     ← Setup para máquina nova
2. Iniciar MariaDB LoginServer
3. Iniciar LoginServer
4. Criar GameServer
5. Iniciar GameServer
6. Parar GameServer
7. Parar Todos os Serviços
8. Listar servidores ativos
9. Logs (ver logs de containers)
10. Edicao em massa de environment
11. Gerenciar perfis de configuracao
12. Sair
```

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
- Testar login de cliente no GameServer
- Adicionar mais GameServers descomentando no docker-compose.yml
- Configurar volumes para persistir dados dos gameservers
- Limpar logs de debug (ERRO while loading chat filter words, custom event data, etc.)

## Comandos Úteis
```bash
# Build da imagem
docker-compose build --no-cache

# Subir todos os containers
docker-compose -p lineternity up -d --force-recreate

# Ver logs
docker-compose -p lineternity logs -f

# Verificar databases
docker exec -it lineternity-mariadb mysql -u root -proot -e "SHOW DATABASES;"

# Verificar gameservers registrados
docker exec -it lineternity-mariadb mysql -u root -proot l2jdb_login -e "SELECT * FROM gameservers;"

# Adicionar gameserver-2
# 1. Descomentar gameserver-2 no docker-compose.yml
# 2. docker-compose -p lineternity up -d --force-recreate gameserver-2
```
