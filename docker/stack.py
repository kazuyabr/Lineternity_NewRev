#!/usr/bin/env python3
"""
Lineternity Stack Manager
Interactive menu for managing Lineternity game server infrastructure.
Adapted from acacia-2d stack.py pattern.
"""

import os
import re
import sys
import subprocess
import shutil
from pathlib import Path
from dataclasses import dataclass, field
from typing import Optional

# ============================================================
# Constants
# ============================================================

DOCKER_DIR = Path(__file__).parent
PROJECT_ROOT = DOCKER_DIR.parent
GAMESERVERS_DIR = DOCKER_DIR / "gameservers"
TEMPLATES_DIR = DOCKER_DIR / "templates"
LOGIN_TEMPLATES = TEMPLATES_DIR / "login"
GAME_TEMPLATES = TEMPLATES_DIR / "game"

# ============================================================
# Data Classes
# ============================================================

@dataclass
class PropertyConfig:
    key: str
    label: str
    default: str
    description: str = ""
    required: bool = False
    category: str = "general"

@dataclass
class CategoryConfig:
    name: str
    label: str
    properties: list[PropertyConfig] = field(default_factory=list)

@dataclass
class ServerInfo:
    server_id: int
    hostname: str
    public_port: int
    game_db: str
    compose_path: Path
    env_path: Path
    config_dir: Path

# ============================================================
# LoginServer Configuration
# ============================================================

LOGIN_CATEGORIES = {
    "database": CategoryConfig(
        name="database",
        label="Database",
        properties=[
            PropertyConfig("DB_HOST", "Database Host", "mariadb", "MariaDB hostname", True, "database"),
            PropertyConfig("DB_PORT", "Database Port", "3306", "MariaDB port", True, "database"),
            PropertyConfig("DB_USER", "Database User", "root", "MariaDB username", True, "database"),
            PropertyConfig("DB_PASSWORD", "Database Password", "root", "MariaDB password", True, "database"),
            PropertyConfig("LOGIN_DB", "Login Database", "l2jdb_login", "Login database name", True, "database"),
        ]
    ),
    "network": CategoryConfig(
        name="network",
        label="Network",
        properties=[
            PropertyConfig("HOSTNAME", "Hostname", "localhost", "IP for clients", True, "network"),
            PropertyConfig("LOGIN_PORT", "Login Port", "2106", "LoginServer port", True, "network"),
        ]
    ),
    "security": CategoryConfig(
        name="security",
        label="Security",
        properties=[
            PropertyConfig("L2_EMAIL", "License Email", "contato@jogatinando.com.br", "License email", False, "security"),
        ]
    ),
}

# ============================================================
# GameServer Configuration
# ============================================================

GAME_CATEGORIES = {
    "database_local": CategoryConfig(
        name="database_local",
        label="Database Local (GameServer)",
        properties=[
            PropertyConfig("DB_HOST", "DB Host Local", "mariadb-gs1", "MariaDB hostname local", True, "database_local"),
            PropertyConfig("DB_PORT", "DB Port Local", "3306", "MariaDB port local", True, "database_local"),
            PropertyConfig("DB_USER", "DB User Local", "root", "MariaDB username local", True, "database_local"),
            PropertyConfig("DB_PASSWORD", "DB Password Local", "root", "MariaDB password local", True, "database_local"),
            PropertyConfig("GAME_DB", "Game Database", "l2jdb_gs1", "Game database name", True, "database_local"),
        ]
    ),
    "database_remote": CategoryConfig(
        name="database_remote",
        label="Database Remoto (LoginServer)",
        properties=[
            PropertyConfig("LOGIN_DB_HOST", "DB Host Login", "mariadb-login", "MariaDB hostname do Login (remoto)", True, "database_remote"),
            PropertyConfig("LOGIN_DB_PORT", "DB Port Login", "3306", "MariaDB port do Login", True, "database_remote"),
            PropertyConfig("LOGIN_DB_USER", "DB User Login", "root", "MariaDB username do Login", True, "database_remote"),
            PropertyConfig("LOGIN_DB_PASSWORD", "DB Password Login", "root", "MariaDB password do Login", True, "database_remote"),
            PropertyConfig("LOGIN_DB", "Login Database", "l2jdb_login", "Login database name", True, "database_remote"),
        ]
    ),
    "network": CategoryConfig(
        name="network",
        label="Network",
        properties=[
            PropertyConfig("SERVER_ID", "Server ID", "1", "Unique server ID", True, "network"),
            PropertyConfig("SERVER_HOSTNAME", "Server Hostname", "gameserver-1", "Hostname for clients", True, "network"),
            PropertyConfig("PUBLIC_PORT", "Public Port", "7777", "Public port for clients", True, "network"),
            PropertyConfig("LOGIN_HOSTNAME", "Login Hostname", "loginserver", "LoginServer hostname", True, "network"),
            PropertyConfig("LOGIN_PORT", "Login Port", "9014", "LoginServer Java port", True, "network"),
        ]
    ),
    "identity": CategoryConfig(
        name="identity",
        label="Identity",
        properties=[
            PropertyConfig("MAX_ONLINE", "Max Online Users", "3000", "Max simultaneous players", False, "identity"),
            PropertyConfig("L2_EMAIL", "License Email", "contato@jogatinando.com.br", "License email", False, "identity"),
        ]
    ),
    "rates": CategoryConfig(
        name="rates",
        label="Rates",
        properties=[
            PropertyConfig("RATE_XP", "XP Rate", "15.", "XP multiplier", False, "rates"),
            PropertyConfig("RATE_SP", "SP Rate", "15.", "SP multiplier", False, "rates"),
            PropertyConfig("RATE_DROP", "Drop Rate", "1.5", "Adena drop multiplier", False, "rates"),
            PropertyConfig("RATE_DROP_ITEMS", "Item Drop Rate", "1.5", "Item drop multiplier", False, "rates"),
        ]
    ),
    "players": CategoryConfig(
        name="players",
        label="Players",
        properties=[
            PropertyConfig("MAX_BUFFS", "Max Buffs", "25", "Maximum buff slots", False, "players"),
        ]
    ),
    "protection": CategoryConfig(
        name="protection",
        label="Protection",
        properties=[
            PropertyConfig("MAX_DUALBOX", "Max Dualbox", "2", "Max clients per IP", False, "protection"),
        ]
    ),
    "chat": CategoryConfig(
        name="chat",
        label="Chat",
        properties=[
            PropertyConfig("GLOBAL_CHAT", "Global Chat", "ON", "Global chat mode", False, "chat"),
            PropertyConfig("TRADE_CHAT", "Trade Chat", "ON", "Trade chat mode", False, "chat"),
        ]
    ),
    "autofarm": CategoryConfig(
        name="autofarm",
        label="AutoFarm",
        properties=[
            PropertyConfig("AUTOFARM_ENABLED", "AutoFarm Enabled", "True", "Enable autofarm system", False, "autofarm"),
        ]
    ),
    "limits": CategoryConfig(
        name="limits",
        label="Limits",
        properties=[
            PropertyConfig("MAX_RUN_SPEED", "Max Run Speed", "250", "Maximum run speed", False, "limits"),
            PropertyConfig("MAX_PATK", "Max P.Atk", "32000", "Maximum physical attack", False, "limits"),
            PropertyConfig("MAX_MATK", "Max M.Atk", "32000", "Maximum magic attack", False, "limits"),
        ]
    ),
    "events": CategoryConfig(
        name="events",
        label="Events",
        properties=[
            PropertyConfig("OlympiadEnabled", "Olympiad Enabled", "True", "Enable Olympiad", False, "events"),
            PropertyConfig("OlyStartTime", "Oly Start Time", "18", "Olympiad start hour", False, "events"),
            PropertyConfig("OlyStartPoints", "Oly Start Points", "18", "Olympiad start points", False, "events"),
            PropertyConfig("OlyCPeriod", "Oly Competition Period", "21600000", "Olympiad competition period (ms)", False, "events"),
            PropertyConfig("OlyBattle", "Oly Battle Time", "360000", "Olympiad battle time (ms)", False, "events"),
            PropertyConfig("CTFEventEnabled", "CTF Enabled", "False", "Enable CTF event", False, "events"),
            PropertyConfig("DMEventEnabled", "DM Enabled", "False", "Enable Deathmatch event", False, "events"),
            PropertyConfig("TvTEventEnabled", "TvT Enabled", "False", "Enable Team vs Team event", False, "events"),
            PropertyConfig("LMEventEnabled", "LM Enabled", "False", "Enable Last Man event", False, "events"),
        ]
    ),
    "clans": CategoryConfig(
        name="clans",
        label="Clans",
        properties=[
            PropertyConfig("DaysBeforeJoinAClan", "Days Before Join Clan", "1", "Days before joining another clan", False, "clans"),
            PropertyConfig("DaysBeforeCreateAClan", "Days Before Create Clan", "10", "Days before creating a new clan", False, "clans"),
            PropertyConfig("DaysToPassToDissolveAClan", "Days to Dissolve Clan", "7", "Days to dissolve a clan", False, "clans"),
            PropertyConfig("MaxNumOfClansInAlly", "Max Clans in Ally", "3", "Maximum clans in ally", False, "clans"),
            PropertyConfig("ClanMembersForWar", "Clan Members for War", "15", "Members needed for clan war", False, "clans"),
        ]
    ),
    "npcs": CategoryConfig(
        name="npcs",
        label="NPCs & Bosses",
        properties=[
            PropertyConfig("SpawnMultiplier", "Spawn Multiplier", "1.5", "Monster spawn rate multiplier", False, "npcs"),
            PropertyConfig("ChampionFrequency", "Champion Frequency", "0", "Champion mob frequency (0=disabled)", False, "npcs"),
            PropertyConfig("ChampionHp", "Champion HP Multiplier", "2", "Champion HP multiplier", False, "npcs"),
            PropertyConfig("MonsterHP", "Monster HP Multiplier", "1.0", "Monster HP multiplier", False, "npcs"),
            PropertyConfig("MonsterPAtk", "Monster P.Atk Multiplier", "1.0", "Monster physical attack multiplier", False, "npcs"),
            PropertyConfig("RaidbossHP", "RaidBoss HP Multiplier", "1.0", "RaidBoss HP multiplier", False, "npcs"),
            PropertyConfig("GrandbossHP", "GrandBoss HP Multiplier", "1.0", "GrandBoss HP multiplier", False, "npcs"),
            PropertyConfig("NobleItemId", "Noble Item ID", "4037", "Item ID for Noblesse", False, "npcs"),
            PropertyConfig("NobleItemCount", "Noble Item Count", "50", "Count of Noble item", False, "npcs"),
            PropertyConfig("WeddingPrice", "Wedding Price", "1000000", "Wedding system price", False, "npcs"),
            PropertyConfig("FreeTeleport", "Free Teleport", "False", "Enable free teleport", False, "npcs"),
            PropertyConfig("ShowNpcLevel", "Show NPC Level", "True", "Show NPC level in game", False, "npcs"),
            PropertyConfig("MobAggroInPeaceZone", "Mob Aggro Peace Zone", "False", "Monsters aggro in peace zones", False, "npcs"),
        ]
    ),
    "offlineshop": CategoryConfig(
        name="offlineshop",
        label="Offline Shop",
        properties=[
            PropertyConfig("OfflineTradeEnable", "Offline Trade", "True", "Enable offline trade shops", False, "offlineshop"),
            PropertyConfig("OfflineCraftEnable", "Offline Craft", "True", "Enable offline craft shops", False, "offlineshop"),
            PropertyConfig("OfflineModeInPeaceZone", "Peace Zone Only", "True", "Offline shop in peace zone only", False, "offlineshop"),
            PropertyConfig("OfflineModeNoDamage", "No Damage", "False", "Offline shop immune to damage", False, "offlineshop"),
            PropertyConfig("OfflineMaxDays", "Max Offline Days", "7", "Max days to stay offline", False, "offlineshop"),
            PropertyConfig("OfflineDisconnectFinished", "Disconnect Finished", "True", "Auto-disconnect when shop done", False, "offlineshop"),
        ]
    ),
    "raidboss": CategoryConfig(
        name="raidboss",
        label="RaidBoss",
        properties=[
            PropertyConfig("RBSleepTime", "RB Sleep Time", "60", "RaidBoss sleep time (min)", False, "raidboss"),
            PropertyConfig("RBAgroTime", "RB Aggro Time", "60", "RaidBoss aggro time (min)", False, "raidboss"),
            PropertyConfig("DisableRaidCurse", "Disable Raid Curse", "True", "Disable RaidBoss curse drops", False, "raidboss"),
        ]
    ),
    "safedisconnect": CategoryConfig(
        name="safedisconnect",
        label="Safe Disconnect",
        properties=[
            PropertyConfig("SafeDisconnectEnabled", "Safe Disconnect Enabled", "True", "Enable safe disconnect system", False, "safedisconnect"),
            PropertyConfig("SafeDisconnectTimeoutMs", "Timeout (ms)", "300000", "Safe disconnect timeout in ms", False, "safedisconnect"),
            PropertyConfig("SafeDisconnectTitle", "Title", "Disconnect...", "Title shown during safe disconnect", False, "safedisconnect"),
            PropertyConfig("SafeDisconnectImmobilize", "Immobilize", "True", "Immobilize player during disconnect", False, "safedisconnect"),
            PropertyConfig("SafeDisconnectInvulnerable", "Invulnerable", "True", "Make player invulnerable during disconnect", False, "safedisconnect"),
        ]
    ),
    "bosszerg": CategoryConfig(
        name="bosszerg",
        label="Boss Zerg (Anti-Zerg)",
        properties=[
            PropertyConfig("BossZergEnabled", "Boss Zerg Enabled", "True", "Enable anti-zerg system", False, "bosszerg"),
            PropertyConfig("BossZergRange", "Zerg Range", "1200", "Range to detect zerg", False, "bosszerg"),
            PropertyConfig("BossZergMinPartySize", "Min Party Size", "3", "Min party size to trigger", False, "bosszerg"),
            PropertyConfig("BossZergMaxAllyMembers", "Max Ally Members", "18", "Max ally members allowed", False, "bosszerg"),
            PropertyConfig("BossZergHealPenaltyMultiplier", "Heal Penalty", "0.75", "Heal penalty multiplier during zerg", False, "bosszerg"),
        ]
    ),
    "siege": CategoryConfig(
        name="siege",
        label="Siege",
        properties=[
            PropertyConfig("SiegeLength", "Siege Length", "120", "Siege duration in minutes", False, "siege"),
            PropertyConfig("SiegeClanMinLevel", "Clan Min Level", "4", "Minimum clan level for siege", False, "siege"),
            PropertyConfig("AttackerMaxClans", "Max Attacker Clans", "10", "Max attacking clans", False, "siege"),
            PropertyConfig("DefenderMaxClans", "Max Defender Clans", "10", "Max defending clans", False, "siege"),
        ]
    ),
    "kamaloka": CategoryConfig(
        name="kamaloka",
        label="Kamaloka",
        properties=[
            PropertyConfig("MaxDailyEntries", "Max Daily Entries", "4", "Max daily Kamaloka entries", False, "kamaloka"),
            PropertyConfig("RewardItemId", "Reward Item ID", "4037", "Kamaloka reward item", False, "kamaloka"),
            PropertyConfig("RewardItemCount", "Reward Count", "15", "Kamaloka reward count", False, "kamaloka"),
        ]
    ),
    "levelupmaker": CategoryConfig(
        name="levelupmaker",
        label="Level Up Maker",
        properties=[
            PropertyConfig("LevelUpMakerEnabled", "LevelUp Maker Enabled", "True", "Enable level-up teleport system", False, "levelupmaker"),
            PropertyConfig("LevelUpMakerLevelOffset", "Level Offset", "5", "Level offset for teleport areas", False, "levelupmaker"),
            PropertyConfig("LevelUpMakerRefreshIntervalSec", "Refresh Interval", "500", "Refresh interval in seconds", False, "levelupmaker"),
        ]
    ),
    "geoengine": CategoryConfig(
        name="geoengine",
        label="GeoEngine & Pathfinding",
        properties=[
            PropertyConfig("EnableRealisticMovement", "Realistic Movement", "False", "Enable realistic movement (anti-slide)", False, "geoengine"),
            PropertyConfig("EnablePathfinderCache", "Pathfinder Cache", "False", "Enable pathfinder caching", False, "geoengine"),
            PropertyConfig("AttackUsePathfinder", "Attack Use Pathfinder", "True", "Use pathfinding for attacks", False, "geoengine"),
        ]
    ),
    "translator": CategoryConfig(
        name="translator",
        label="Auto Translator (DeepL)",
        properties=[
            PropertyConfig("DeeplAuthKey", "DeepL Auth Key", "", "DeepL API key for auto-translation", False, "translator"),
        ]
    ),
    "language": CategoryConfig(
        name="language",
        label="Language & Locale",
        properties=[
            PropertyConfig("defaultLocale", "Default Locale", "en-US", "Default server locale", False, "language"),
            PropertyConfig("locales", "Available Locales", "en-US,ru-RU", "Available locales (comma-separated)", False, "language"),
        ]
    ),
    "items": CategoryConfig(
        name="items",
        label="Items",
        properties=[
            PropertyConfig("ItemsGcCleanupEnabled", "Item GC Enabled", "True", "Enable item garbage collection", False, "items"),
            PropertyConfig("ItemsGcCleanupTime", "Item GC Time", "120", "Item GC cleanup interval (min)", False, "items"),
        ]
    ),
    "bossHeal": CategoryConfig(
        name="bossHeal",
        label="Boss Heal Block",
        properties=[
            PropertyConfig("BlockHealOnRaidBoss", "Block Heal Raid Boss", "True", "Block healing on RaidBosses", False, "bossHeal"),
            PropertyConfig("BlockHealOnGrandBoss", "Block Heal Grand Boss", "True", "Block healing on GrandBosses", False, "bossHeal"),
        ]
    ),
}

# Mandatory configs for basic mode
MANDATORY_CONFIGS = {
    "login": ["DB_HOST", "DB_PORT", "DB_USER", "DB_PASSWORD", "LOGIN_DB", "HOSTNAME", "LOGIN_PORT"],
    "game": [
        "DB_HOST", "DB_PORT", "DB_USER", "DB_PASSWORD", "GAME_DB",
        "LOGIN_DB_HOST", "LOGIN_DB_PORT", "LOGIN_DB_USER", "LOGIN_DB_PASSWORD", "LOGIN_DB",
        "SERVER_ID", "SERVER_HOSTNAME", "PUBLIC_PORT",
        "LOGIN_HOSTNAME", "LOGIN_PORT",
    ],
}

# ============================================================
# Menu Functions
# ============================================================

def clear_screen():
    os.system('cls' if os.name == 'nt' else 'clear')

def print_header(title: str):
    clear_screen()
    print("=" * 60)
    print(f"  {title}")
    print("=" * 60)
    print()

def choose_from_menu(title: str, options: list[str]) -> int:
    print_header(title)
    for i, option in enumerate(options, 1):
        print(f"  [{i}] {option}")
    print()
    
    while True:
        try:
            choice = input("  Selecione uma opcao: ").strip()
            if choice.isdigit():
                idx = int(choice) - 1
                if 0 <= idx < len(options):
                    return idx
            print("  Opcao invalida. Tente novamente.")
        except (EOFError, KeyboardInterrupt):
            print("\n  Operacao cancelada.")
            return -1

def confirm(message: str) -> bool:
    while True:
        choice = input(f"  {message} (s/n): ").strip().lower()
        if choice in ('s', 'sim', 'y', 'yes'):
            return True
        if choice in ('n', 'nao', 'no'):
            return False
        print("  Resposta invalida. Digite 's' ou 'n'.")

def prompt_property(config: PropertyConfig) -> str:
    if config.required:
        prompt = f"  {config.label} [{config.default}]: "
    else:
        prompt = f"  {config.label} [{config.default}] (enter para padrao): "
    
    value = input(prompt).strip()
    
    if not value and config.default:
        return config.default
    
    if config.required and not value:
        print(f"  ERRO: {config.label} e obrigatorio!")
        return prompt_property(config)
    
    return value

# ============================================================
# Configuration Collection
# ============================================================

def collect_category_config(category: CategoryConfig) -> dict[str, str]:
    config = {}
    print(f"\n  --- {category.label} ---")
    for prop in category.properties:
        config[prop.key] = prompt_property(prop)
    return config

def collect_mandatory_config(server_type: str) -> dict[str, str]:
    config = {}
    mandatory = MANDATORY_CONFIGS[server_type]
    categories = LOGIN_CATEGORIES if server_type == "login" else GAME_CATEGORIES
    
    for cat_name, cat_config in categories.items():
        for prop in cat_config.properties:
            if prop.key in mandatory:
                config[prop.key] = prompt_property(prop)
    
    return config

def collect_category_by_name(server_type: str, category_name: str) -> dict[str, str]:
    categories = LOGIN_CATEGORIES if server_type == "login" else GAME_CATEGORIES
    if category_name in categories:
        return collect_category_config(categories[category_name])
    return {}

def collect_advanced_config_with_selection(server_type: str) -> dict[str, str]:
    categories = LOGIN_CATEGORIES if server_type == "login" else GAME_CATEGORIES
    
    print(f"\n  Configuracao Avancada - {server_type.upper()}")
    print("  Selecione as categorias para configurar.\n")
    
    cat_names = list(categories.keys())
    options = [categories[name].label for name in cat_names]
    options.append("Todas as categorias")
    options.append("Voltar")
    
    while True:
        idx = choose_from_menu("Selecione a categoria", options)
        
        if idx == len(options) - 1:  # Voltar
            break
        elif idx == len(options) - 2:  # Todas
            config = {}
            for name in cat_names:
                config.update(collect_category_config(categories[name]))
            return config
        elif 0 <= idx < len(cat_names):
            config = collect_category_config(categories[cat_names[idx]])
            return config
    
    return {}

def collect_full_config(server_type: str) -> dict[str, str]:
    config = {}
    categories = LOGIN_CATEGORIES if server_type == "login" else GAME_CATEGORIES
    
    print(f"\n  Configuracao Avancada - {server_type.upper()}")
    print("  Pressione Enter para usar o valor padrao em cada campo.\n")
    
    for cat_name, cat_config in categories.items():
        config.update(collect_category_config(cat_config))
    
    return config

def select_config_mode() -> str:
    options = [
        "Modo Basico (campos obrigatorios apenas)",
        "Modo Avancado (todas as configuracoes)",
        "Modo Avancado com Selecao (escolher categorias)",
    ]
    idx = choose_from_menu("Modo de Configuracao", options)
    if idx == 0:
        return "basic"
    elif idx == 1:
        return "advanced"
    elif idx == 2:
        return "advanced_select"
    return "basic"

# ============================================================
# Property File Generation
# ============================================================

def generate_properties(template_path: Path, output_path: Path, config: dict[str, str]):
    content = template_path.read_text(encoding='utf-8')
    
    for key, value in config.items():
        placeholder = "{{" + key + "}}"
        content = content.replace(placeholder, value)
    
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(content, encoding='utf-8')

def create_login_properties(config_dir: Path, config: dict[str, str]):
    for template_file in LOGIN_TEMPLATES.glob("*.properties"):
        output_file = config_dir / template_file.name
        generate_properties(template_file, output_file, config)
    print(f"  Properties de login criados em: {config_dir}")

def create_game_properties(config_dir: Path, config: dict[str, str]):
    for template_file in GAME_TEMPLATES.glob("*.properties"):
        output_file = config_dir / template_file.name
        generate_properties(template_file, output_file, config)
    print(f"  Properties de game criados em: {config_dir}")

# ============================================================
# Docker Compose Operations
# ============================================================

def run_compose(compose_path: Path, *args, env_file: Optional[Path] = None):
    cmd = ["docker", "compose", "-f", str(compose_path)]
    if env_file:
        cmd.extend(["--env-file", str(env_file)])
    cmd.extend(args)
    
    print(f"  Executando: {' '.join(cmd)}")
    result = subprocess.run(cmd, cwd=str(DOCKER_DIR))
    return result.returncode == 0

def run_compose_with_project(compose_path: Path, project_name: str, *args, env_file: Optional[Path] = None):
    cmd = ["docker", "compose", "-f", str(compose_path), "-p", project_name]
    if env_file:
        cmd.extend(["--env-file", str(env_file)])
    cmd.extend(args)
    
    print(f"  Executando: {' '.join(cmd)}")
    result = subprocess.run(cmd, cwd=str(DOCKER_DIR))
    return result.returncode == 0

# ============================================================
# Server Management
# ============================================================

def create_login_server():
    print_header("Criar LoginServer")
    
    # Detectar MariaDB existente
    print("  Verificando MariaDB disponivel...")
    detection = detect_or_configure_mariadb()
    
    if detection["found"]:
        print(f"\n  MariaDB detectado: {detection.get('container_name', 'manual')} ({detection['host']}:{detection['port']})")
        print(f"  Usuario: {detection.get('user', 'root')}")
        
        if detection["database_exists"]:
            print(f"  Database l2jdb_login: EXISTE")
        else:
            print(f"  Database l2jdb_login: NAO EXISTE (sera criado pelo entrypoint)")
        
        if confirm("\n  Usar este MariaDB para o LoginServer?"):
            # Usar MariaDB externo
            update_login_env(
                detection["host"], 
                detection["port"],
                detection.get("user", "root"),
                detection.get("password", "root"),
                external=True
            )
            use_external = True
        else:
            use_external = False
    else:
        use_external = False
    
    config_mode = select_config_mode()
    
    if config_mode == "basic":
        config = collect_mandatory_config("login")
    elif config_mode == "advanced_select":
        config = collect_advanced_config_with_selection("login")
    else:
        config = collect_full_config("login")
    
    # Se usando MariaDB externo, atualizar config com credenciais detectadas
    if use_external:
        config["DB_HOST"] = detection["host"]
        config["DB_PORT"] = detection["port"]
        config["DB_USER"] = detection.get("user", "root")
        config["DB_PASSWORD"] = detection.get("password", "root")
    
    print("\n  Configuracao coletada:")
    for key, value in config.items():
        # Mascarar senha
        if "PASSWORD" in key:
            print(f"    {key}: ***")
        else:
            print(f"    {key}: {value}")
    
    if not confirm("\n  Confirmar criacao do LoginServer?"):
        print("  Operacao cancelada.")
        return
    
    # Create config directory
    config_dir = DOCKER_DIR / "login" / "config"
    config_dir.mkdir(parents=True, exist_ok=True)
    
    # Generate properties
    create_login_properties(config_dir, config)
    
    print("\n  LoginServer criado com sucesso!")
    print(f"  Config dir: {config_dir}")
    
    if confirm("\n  Iniciar MariaDB e LoginServer agora?"):
        start_mariadb_login()
        start_loginserver()

def create_game_server():
    print_header("Criar GameServer")
    
    # Get server ID
    while True:
        server_id_str = input("  Server ID (1-255): ").strip()
        if server_id_str.isdigit():
            server_id = int(server_id_str)
            if 1 <= server_id <= 255:
                break
        print("  ID invalido. Use um numero entre 1 e 255.")
    
    server_dir = GAMESERVERS_DIR / f"gameserver-{server_id}"
    
    if server_dir.exists():
        print(f"\n  AVISO: GameServer {server_id} ja existe em {server_dir}")
        if not confirm("  Sobrescrever?"):
            print("  Operacao cancelada.")
            return
    
    config_mode = select_config_mode()
    
    # Set default values based on server ID
    defaults = {
        "SERVER_ID": str(server_id),
        "SERVER_HOSTNAME": f"gameserver-{server_id}",
        "PUBLIC_PORT": str(7776 + server_id),
        "GAME_DB": f"l2jdb_gs{server_id}",
    }
    
    if config_mode == "basic":
        config = collect_mandatory_config("game")
    elif config_mode == "advanced_select":
        config = collect_advanced_config_with_selection("game")
    else:
        config = collect_full_config("game")
    
    # Apply defaults
    for key, value in defaults.items():
        if key not in config or not config[key]:
            config[key] = value
    
    print("\n  Configuracao coletada:")
    for key, value in config.items():
        print(f"    {key}: {value}")
    
    if not confirm(f"\n  Confirmar criacao do GameServer #{server_id}?"):
        print("  Operacao cancelada.")
        return
    
    # Create server directory structure
    server_dir.mkdir(parents=True, exist_ok=True)
    config_dir = server_dir / "config"
    config_dir.mkdir(parents=True, exist_ok=True)
    
    # Copy docker-compose template
    compose_template = GAMESERVERS_DIR / "template" / "docker-compose.yml"
    compose_file = server_dir / "docker-compose.yml"
    shutil.copy2(compose_template, compose_file)
    
    # Create .env file
    env_file = server_dir / ".env"
    env_content = "\n".join(f"{key}={value}" for key, value in config.items())
    env_file.write_text(env_content, encoding='utf-8')
    
    # Generate properties
    create_game_properties(config_dir, config)
    
    print(f"\n  GameServer #{server_id} criado com sucesso!")
    print(f"  Server dir: {server_dir}")
    print(f"  Config dir: {config_dir}")
    print(f"  Env file: {env_file}")

def remove_game_server():
    print_header("Remover GameServer")
    
    servers = list_existing_game_servers()
    if not servers:
        print("  Nenhum GameServer encontrado.")
        input("\n  Pressione Enter para continuar...")
        return
    
    options = [f"GameServer #{s.server_id} ({s.hostname})" for s in servers]
    options.append("Cancelar")
    
    idx = choose_from_menu("Selecione o GameServer para remover", options)
    if idx == len(servers):
        print("  Operacao cancelada.")
        return
    
    server = servers[idx]
    
    if not confirm(f"  Remover GameServer #{server.server_id}? Esta acao e irreversivel!"):
        print("  Operacao cancelada.")
        return
    
    # Stop container if running
    compose_file = server.compose_path
    if compose_file.exists():
        print("  Parando container...")
        run_compose(compose_file, "down", env_file=server.env_path)
    
    # Remove directory
    server_dir = GAMESERVERS_DIR / f"gameserver-{server.server_id}"
    if server_dir.exists():
        shutil.rmtree(server_dir)
        print(f"  Diretorio removido: {server_dir}")
    
    print(f"\n  GameServer #{server.server_id} removido com sucesso!")

def list_existing_game_servers() -> list[ServerInfo]:
    servers = []
    
    if not GAMESERVERS_DIR.exists():
        return servers
    
    for server_dir in sorted(GAMESERVERS_DIR.iterdir()):
        if server_dir.is_dir() and server_dir.name.startswith("gameserver-"):
            try:
                server_id = int(server_dir.name.split("-")[1])
            except (ValueError, IndexError):
                continue
            
            env_file = server_dir / ".env"
            config = {}
            if env_file.exists():
                for line in env_file.read_text().splitlines():
                    if "=" in line and not line.startswith("#"):
                        key, value = line.split("=", 1)
                        config[key.strip()] = value.strip()
            
            servers.append(ServerInfo(
                server_id=server_id,
                hostname=config.get("SERVER_HOSTNAME", f"gameserver-{server_id}"),
                public_port=int(config.get("PUBLIC_PORT", 7776 + server_id)),
                game_db=config.get("GAME_DB", f"l2jdb_gs{server_id}"),
                compose_path=server_dir / "docker-compose.yml",
                env_path=env_file,
                config_dir=server_dir / "config",
            ))
    
    return servers

def list_servers():
    print_header("Servidores Ativos")
    
    # Check LoginServer MariaDB
    print("  --- LoginServer Infrastructure ---")
    result = subprocess.run(
        ["docker", "ps", "-a", "--filter", "name=lineternity-mariadb-login", "--format", "{{.Names}}\t{{.Status}}"],
        capture_output=True, text=True
    )
    if result.stdout.strip():
        for line in result.stdout.strip().splitlines():
            parts = line.split("\t")
            if len(parts) == 2:
                print(f"    {parts[0]}: {parts[1]}")
    else:
        print("    MariaDB LoginServer: nao iniciado")
    
    # Check LoginServer
    result = subprocess.run(
        ["docker", "ps", "-a", "--filter", "name=lineternity-loginserver", "--format", "{{.Names}}\t{{.Status}}"],
        capture_output=True, text=True
    )
    if result.stdout.strip():
        for line in result.stdout.strip().splitlines():
            parts = line.split("\t")
            if len(parts) == 2:
                print(f"    {parts[0]}: {parts[1]}")
    else:
        print("    LoginServer: nao iniciado")
    
    # Check gameservers
    print("\n  --- GameServers ---")
    servers = list_existing_game_servers()
    if servers:
        for server in servers:
            # Check MariaDB for this server
            mariadb_name = f"lineternity-mariadb-gs{server.server_id}"
            result = subprocess.run(
                ["docker", "ps", "-a", "--filter", f"name={mariadb_name}", "--format", "{{.Names}}\t{{.Status}}"],
                capture_output=True, text=True
            )
            mariadb_status = "nao iniciado"
            if result.stdout.strip():
                parts = result.stdout.strip().split("\t")
                if len(parts) == 2:
                    mariadb_status = parts[1]
            
            # Check GameServer
            gs_name = f"lineternity-gameserver-{server.server_id}"
            result = subprocess.run(
                ["docker", "ps", "-a", "--filter", f"name={gs_name}", "--format", "{{.Names}}\t{{.Status}}"],
                capture_output=True, text=True
            )
            gs_status = "nao iniciado"
            if result.stdout.strip():
                parts = result.stdout.strip().split("\t")
                if len(parts) == 2:
                    gs_status = parts[1]
            
            print(f"    GameServer #{server.server_id}: {server.hostname} (port {server.public_port})")
            print(f"      MariaDB: {mariadb_status}")
            print(f"      GameServer: {gs_status}")
    else:
        print("    Nenhum GameServer configurado.")
    
    input("\n  Pressione Enter para continuar...")

def show_logs_menu():
    print_header("Logs")
    
    options = [
        "Logs do MariaDB LoginServer",
        "Logs do LoginServer",
        "Logs do GameServer (selecionar)",
        "Logs de todos os containers",
        "Voltar",
    ]
    
    idx = choose_from_menu("Selecione o tipo de log", options)
    
    if idx == 0:
        print("\n  Logs do MariaDB LoginServer (Ctrl+C para sair):")
        subprocess.run(["docker", "logs", "-f", "lineternity-mariadb-login"])
    elif idx == 1:
        print("\n  Logs do LoginServer (Ctrl+C para sair):")
        subprocess.run(["docker", "logs", "-f", "lineternity-loginserver"])
    elif idx == 2:
        servers = list_existing_game_servers()
        if not servers:
            print("  Nenhum GameServer encontrado.")
        else:
            options = [f"GameServer #{s.server_id}" for s in servers]
            idx = choose_from_menu("Selecione o GameServer", options)
            if 0 <= idx < len(servers):
                server = servers[idx]
                print(f"\n  Logs do GameServer #{server.server_id} (Ctrl+C para sair):")
                subprocess.run(["docker", "logs", "-f", f"lineternity-gameserver-{server.server_id}"])
    elif idx == 3:
        print("\n  Logs de todos os containers (Ctrl+C para sair):")
        subprocess.run(["docker", "logs", "-f", "lineternity-mariadb-login", "lineternity-loginserver"])
    elif idx == 4:
        return

# ============================================================
# MariaDB Detection
# ============================================================

def detect_mariadb_containers():
    """Lista containers MariaDB ativos via docker ps (exclui containers Lineternity)"""
    result = subprocess.run(
        ["docker", "ps", "--format", "{{.Names}}\t{{.Ports}}\t{{.Status}}"],
        capture_output=True, text=True
    )
    
    containers = []
    if not result.stdout.strip():
        return containers
    
    for line in result.stdout.strip().splitlines():
        parts = line.split("\t")
        if len(parts) < 3:
            continue
        
        name = parts[0]
        ports = parts[1]
        status = parts[2]
        
        # Excluir containers Lineternity
        if name.startswith("lineternity-"):
            continue
        
        # Verificar se é MariaDB (verificar porta 3306 mapeada)
        if "3306" in ports or "mariadb" in name.lower() or "mysql" in name.lower():
            # Extrair porta externa do formato "0.0.0.0:3308->3306/tcp"
            external_port = "3306"
            if "->" in ports:
                port_part = ports.split("->")[0]
                if ":" in port_part:
                    external_port = port_part.split(":")[-1]
            
            containers.append({
                "name": name,
                "ports": ports,
                "external_port": external_port,
                "status": status,
            })
    
    return containers

def test_mariadb_connection(host, port, user, password):
    """Testa conexão MySQL/MariaDB usando docker run (não depende de mysql no host)"""
    try:
        result = subprocess.run(
            ["docker", "run", "--rm", "--network", "host",
             "mariadb:10.11", "mysql",
             "-h", host, "-P", port, "-u", user, f"-p{password}",
             "--skip-ssl", "-e", "SELECT 1"],
            capture_output=True, text=True, timeout=30
        )
        return result.returncode == 0
    except (subprocess.TimeoutExpired, FileNotFoundError):
        return False

def check_database_exists(host, port, user, password, db_name):
    """Verifica se database existe usando docker run"""
    try:
        result = subprocess.run(
            ["docker", "run", "--rm", "--network", "host",
             "mariadb:10.11", "mysql",
             "-h", host, "-P", port, "-u", user, f"-p{password}",
             "--skip-ssl", "-N", "-e",
             f"SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME='{db_name}'"],
            capture_output=True, text=True, timeout=30
        )
        return result.stdout.strip() == db_name
    except (subprocess.TimeoutExpired, FileNotFoundError):
        return False

def detect_or_configure_mariadb():
    """
    Detecta MariaDB existente ou pede configuração manual.
    Retorna: {found: bool, host: str, port: str, user: str, password: str, database_exists: bool}
    """
    print("\n  Procurando MariaDB existente...\n")
    
    # 1. Auto-detectar via Docker
    containers = detect_mariadb_containers()
    
    if containers:
        print("  Containers MariaDB encontrados:")
        for i, c in enumerate(containers, 1):
            print(f"    [{i}] {c['name']} ({c['external_port']}) - {c['status']}")
        print()
        
        # Para cada container, testar conexão primeiro com root/root
        for c in containers:
            host = "localhost"
            port = c["external_port"]
            
            print(f"  Testando conexao com {c['name']} ({host}:{port})...")
            if test_mariadb_connection(host, port, "root", "root"):
                print(f"  Conexao OK com {c['name']} (root/root)!")
                
                db_exists = check_database_exists(host, port, "root", "root", "l2jdb_login")
                
                return {
                    "found": True,
                    "host": host,
                    "port": port,
                    "user": "root",
                    "password": "root",
                    "container_name": c["name"],
                    "database_exists": db_exists,
                }
            else:
                print(f"  Falha com root/root. Pedindo credenciais...")
                # Pedir credenciais para este container
                user = input(f"  Usuario para {c['name']} [root]: ").strip() or "root"
                password = input(f"  Senha para {c['name']} [root]: ").strip() or "root"
                
                if test_mariadb_connection(host, port, user, password):
                    print(f"  Conexao OK com {c['name']} ({user})!")
                    
                    db_exists = check_database_exists(host, port, user, password, "l2jdb_login")
                    
                    return {
                        "found": True,
                        "host": host,
                        "port": port,
                        "user": user,
                        "password": password,
                        "container_name": c["name"],
                        "database_exists": db_exists,
                    }
                else:
                    print(f"  Falha na conexao com {c['name']}.")
        
        print()
    
    # 2. Nenhum MariaDB encontrado via Docker - pedir configuração manual
    print("  Nenhum MariaDB acessivel encontrado via Docker.")
    print()
    
    if not confirm("  Deseja informar host:port de um MariaDB existente?"):
        return {"found": False, "host": "", "port": "", "user": "", "password": "", "database_exists": False}
    
    # Pedir host, porta, usuario e senha
    while True:
        host = input("  Host do MariaDB [localhost]: ").strip() or "localhost"
        port = input("  Porta do MariaDB [3306]: ").strip() or "3306"
        user = input("  Usuario do MariaDB [root]: ").strip() or "root"
        password = input("  Senha do MariaDB [root]: ").strip() or "root"
        
        print(f"\n  Testando conexao com {user}@{host}:{port}...")
        if test_mariadb_connection(host, port, user, password):
            print("  Conexao OK!")
            
            db_exists = check_database_exists(host, port, user, password, "l2jdb_login")
            
            return {
                "found": True,
                "host": host,
                "port": port,
                "user": user,
                "password": password,
                "container_name": "",
                "database_exists": db_exists,
            }
        else:
            print("\n  Falha na conexao.")
            print()
            print("  Opcoes:")
            print("    [1] Tentar outras credenciais")
            print("    [2] Criar novo container MariaDB com credenciais customizadas")
            print("    [3] Cancelar")
            
            choice = input("\n  Selecione: ").strip()
            
            if choice == "2":
                # Criar container com credenciais customizadas
                print("\n  Credenciais para o novo container MariaDB:")
                new_user = input("  Usuario [root]: ").strip() or "root"
                new_password = input("  Senha [root]: ").strip() or "root"
                
                return {
                    "found": False,
                    "host": "mariadb-login",
                    "port": "3306",
                    "user": new_user,
                    "password": new_password,
                    "container_name": "",
                    "database_exists": False,
                    "create_container": True,
                }
            elif choice == "3":
                return {"found": False, "host": "", "port": "", "user": "", "password": "", "database_exists": False}
            # Senão, loop continua para tentar outras credenciais

def update_login_env(host, port, user="root", password="root", external=True):
    """Atualiza login/.env com configuração do MariaDB"""
    env_file = DOCKER_DIR / "login" / ".env"
    
    if not env_file.exists():
        # Criar .env com valores padrão
        env_content = f"""# ============================================================
# Lineternity LoginServer Environment Configuration
# ============================================================

# MariaDB Login (local ou remoto)
DB_HOST={host}
DB_PORT={port}
DB_USER={user}
DB_PASSWORD={password}
LOGIN_DB=l2jdb_login

# Hostname público do LoginServer (IP para clientes)
HOSTNAME=localhost

# Porta do LoginServer
LOGIN_PORT=2106

# Licença
L2_EMAIL=contato@jogatinando.com.br

# MariaDB externo (se true, não cria container mariadb-login)
EXTERNAL_MARIADB={'true' if external else 'false'}
"""
    else:
        # Atualizar .env existente
        content = env_file.read_text(encoding='utf-8')
        
        # Atualizar DB_HOST
        if "DB_HOST=" in content:
            content = re.sub(r"DB_HOST=.*", f"DB_HOST={host}", content)
        else:
            content += f"\nDB_HOST={host}"
        
        # Atualizar DB_PORT
        if "DB_PORT=" in content:
            content = re.sub(r"DB_PORT=.*", f"DB_PORT={port}", content)
        else:
            content += f"\nDB_PORT={port}"
        
        # Atualizar DB_USER
        if "DB_USER=" in content:
            content = re.sub(r"DB_USER=.*", f"DB_USER={user}", content)
        else:
            content += f"\nDB_USER={user}"
        
        # Atualizar DB_PASSWORD
        if "DB_PASSWORD=" in content:
            content = re.sub(r"DB_PASSWORD=.*", f"DB_PASSWORD={password}", content)
        else:
            content += f"\nDB_PASSWORD={password}"
        
        # Atualizar ou adicionar EXTERNAL_MARIADB
        if "EXTERNAL_MARIADB=" in content:
            content = re.sub(r"EXTERNAL_MARIADB=.*", f"EXTERNAL_MARIADB={'true' if external else 'false'}", content)
        else:
            content += f"\nEXTERNAL_MARIADB={'true' if external else 'false'}"
        
        env_content = content
    
    env_file.write_text(env_content, encoding='utf-8')
    print(f"  login/.env atualizado: DB_HOST={host}, DB_PORT={port}, DB_USER={user}")

# ============================================================
# Service Management
# ============================================================

def start_mariadb_login():
    print_header("Iniciar MariaDB LoginServer")
    
    # Detectar MariaDB existente
    detection = detect_or_configure_mariadb()
    
    if detection["found"]:
        # MariaDB externo encontrado
        print(f"\n  MariaDB detectado: {detection.get('container_name', 'manual')} ({detection['host']}:{detection['port']})")
        print(f"  Usuario: {detection.get('user', 'root')}")
        
        if detection["database_exists"]:
            print(f"  Database l2jdb_login: EXISTE")
        else:
            print(f"  Database l2jdb_login: NAO EXISTE (sera criado pelo entrypoint)")
        
        if confirm("\n  Usar este MariaDB para o LoginServer?"):
            # Usar MariaDB externo
            update_login_env(
                detection["host"], 
                detection["port"],
                detection.get("user", "root"),
                detection.get("password", "root"),
                external=True
            )
            print("\n  Configuracao atualizada para usar MariaDB externo.")
            print("  LoginServer ira conectar em:", detection["host"] + ":" + detection["port"])
            return True
        else:
            print("\n  Criando container mariadb-login...")
    
    # Se detect_or_configure_mariadb retornou create_container=True,
    # usar credenciais customizadas
    if detection.get("create_container"):
        custom_user = detection.get("user", "root")
        custom_password = detection.get("password", "root")
    else:
        custom_user = "root"
        custom_password = "root"
    
    # Criar container mariadb-login
    compose_file = DOCKER_DIR / "docker-compose.yml"
    if not compose_file.exists():
        print(f"  ERRO: docker-compose.yml nao encontrado em {compose_file}")
        return False
    
    # Atualizar .env para usar container local com credenciais
    update_login_env("mariadb-login", "3306", custom_user, custom_password, external=False)
    
    # Se credenciais customizadas, passar via environment
    if custom_user != "root" or custom_password != "root":
        os.environ["MYSQL_ROOT_PASSWORD"] = custom_password
        print(f"  Criando MariaDB com senha customizada...")
    
    print("  Iniciando MariaDB LoginServer...")
    if not run_compose(compose_file, "up", "-d"):
        print("  ERRO ao iniciar MariaDB LoginServer!")
        # Limpar environment
        if "MYSQL_ROOT_PASSWORD" in os.environ:
            del os.environ["MYSQL_ROOT_PASSWORD"]
        return False
    
    # Limpar environment
    if "MYSQL_ROOT_PASSWORD" in os.environ:
        del os.environ["MYSQL_ROOT_PASSWORD"]
    
    print("\n  MariaDB LoginServer iniciado com sucesso!")
    print(f"  Credenciais: {custom_user}/***@mariadb-login:3306")
    return True

def start_loginserver():
    print_header("Iniciar LoginServer")
    
    env_file = DOCKER_DIR / "login" / ".env"
    if not env_file.exists():
        print(f"  ERRO: .env nao encontrado em {env_file}")
        print("  Execute 'Criar LoginServer' primeiro.")
        return False
    
    # Verificar se usa MariaDB externo
    use_external = False
    if env_file.exists():
        content = env_file.read_text(encoding='utf-8')
        for line in content.splitlines():
            if line.startswith("EXTERNAL_MARIADB=") and line.split("=", 1)[1].strip().lower() == "true":
                use_external = True
                break
    
    # Escolher compose file correto
    if use_external:
        compose_file = DOCKER_DIR / "docker-compose.loginserver-external.yml"
        print("  Usando MariaDB externo (sem depends_on mariadb-login)")
    else:
        compose_file = DOCKER_DIR / "docker-compose.loginserver.yml"
    
    if not compose_file.exists():
        print(f"  ERRO: {compose_file.name} nao encontrado em {compose_file}")
        return False
    
    print("  Iniciando LoginServer...")
    if not run_compose(compose_file, "up", "-d", env_file=env_file):
        print("  ERRO ao iniciar LoginServer!")
        return False
    
    print("\n  LoginServer iniciado com sucesso!")
    return True

def stop_all_services():
    print_header("Parar Todos os Serviços")
    
    if not confirm("  Parar todos os containers Lineternity?"):
        print("  Operacao cancelada.")
        return
    
    # Stop all game servers
    print("\n  Parando GameServers...")
    for server_dir in GAMESERVERS_DIR.iterdir():
        if server_dir.is_dir():
            compose = server_dir / "docker-compose.yml"
            env = server_dir / ".env"
            if compose.exists():
                run_compose(compose, "down", env_file=env if env.exists() else None)
    
    # Stop login server
    print("\n  Parando LoginServer...")
    compose_file = DOCKER_DIR / "docker-compose.loginserver.yml"
    if compose_file.exists():
        run_compose(compose_file, "down")
    
    # Stop MariaDB
    print("\n  Parando MariaDB LoginServer...")
    compose_file = DOCKER_DIR / "docker-compose.yml"
    if compose_file.exists():
        run_compose(compose_file, "down")
    
    print("\n  Todos os servicos parados com sucesso!")

def start_game_server():
    print_header("Iniciar GameServer")
    
    servers = list_existing_game_servers()
    if not servers:
        print("  Nenhum GameServer configurado.")
        print("  Execute 'Criar GameServer' primeiro.")
        return
    
    options = [f"GameServer #{s.server_id} ({s.hostname})" for s in servers]
    options.append("Todos os GameServers")
    options.append("Cancelar")
    
    idx = choose_from_menu("Selecione o GameServer para iniciar", options)
    
    if idx == len(servers):
        # Start all
        print("\n  Iniciando todos os GameServers...")
        for server in servers:
            compose_file = server.compose_path
            env_file = server.env_path
            if compose_file.exists():
                run_compose(compose_file, "up", "-d", env_file=env_file if env_file.exists() else None)
        print("\n  Todos os GameServers iniciados!")
    elif idx == len(servers) + 1:
        print("  Operacao cancelada.")
    elif 0 <= idx < len(servers):
        server = servers[idx]
        compose_file = server.compose_path
        env_file = server.env_path
        if compose_file.exists():
            print(f"\n  Iniciando GameServer #{server.server_id}...")
            run_compose(compose_file, "up", "-d", env_file=env_file if env_file.exists() else None)
            print(f"\n  GameServer #{server.server_id} iniciado!")

def stop_game_server():
    print_header("Parar GameServer")
    
    servers = list_existing_game_servers()
    if not servers:
        print("  Nenhum GameServer configurado.")
        return
    
    options = [f"GameServer #{s.server_id} ({s.hostname})" for s in servers]
    options.append("Todos os GameServers")
    options.append("Cancelar")
    
    idx = choose_from_menu("Selecione o GameServer para parar", options)
    
    if idx == len(servers):
        # Stop all
        print("\n  Parando todos os GameServers...")
        for server in servers:
            compose_file = server.compose_path
            env_file = server.env_path
            if compose_file.exists():
                run_compose(compose_file, "down", env_file=env_file if env_file.exists() else None)
        print("\n  Todos os GameServers parados!")
    elif idx == len(servers) + 1:
        print("  Operacao cancelada.")
    elif 0 <= idx < len(servers):
        server = servers[idx]
        compose_file = server.compose_path
        env_file = server.env_path
        if compose_file.exists():
            print(f"\n  Parando GameServer #{server.server_id}...")
            run_compose(compose_file, "down", env_file=env_file if env_file.exists() else None)
            print(f"\n  GameServer #{server.server_id} parado!")

def bulk_edit_env():
    print_header("Edicao em Massa de Environment")
    
    servers = list_existing_game_servers()
    if not servers:
        print("  Nenhum GameServer encontrado.")
        input("\n  Pressione Enter para continuar...")
        return
    
    options = [f"GameServer #{s.server_id} ({s.hostname})" for s in servers]
    options.append("Cancelar")
    
    idx = choose_from_menu("Selecione o GameServer para editar", options)
    if idx == len(servers):
        return
    
    server = servers[idx]
    
    print(f"\n  Editando environment do GameServer #{server.server_id}")
    print("  Deixe em branco para manter o valor atual.\n")
    
    env_file = server.env_path
    config = {}
    if env_file.exists():
        for line in env_file.read_text().splitlines():
            if "=" in line and not line.startswith("#"):
                key, value = line.split("=", 1)
                config[key.strip()] = value.strip()
    
    new_config = {}
    for key, value in config.items():
        new_value = input(f"  {key} [{value}]: ").strip()
        new_config[key] = new_value if new_value else value
    
    # Write updated env file
    env_content = "\n".join(f"{key}={value}" for key, value in new_config.items())
    env_file.write_text(env_content, encoding='utf-8')
    
    print(f"\n  Environment atualizado: {env_file}")
    
    if confirm("  Reiniciar GameServer para aplicar mudancas?"):
        compose_file = server.compose_path
        if compose_file.exists():
            run_compose(compose_file, "restart", env_file=env_file)

def manage_config_profiles():
    print_header("Gerenciar Perfis de Configuracao")
    
    profiles_dir = DOCKER_DIR / "profiles"
    profiles_dir.mkdir(exist_ok=True)
    
    options = [
        "Listar perfis existentes",
        "Criar novo perfil",
        "Carregar perfil",
        "Salvar perfil atual",
        "Deletar perfil",
        "Voltar",
    ]
    
    while True:
        idx = choose_from_menu("Gerenciar Perfis", options)
        
        if idx == 0:
            list_profiles(profiles_dir)
        elif idx == 1:
            create_profile(profiles_dir)
        elif idx == 2:
            load_profile(profiles_dir)
        elif idx == 3:
            save_profile(profiles_dir)
        elif idx == 4:
            delete_profile(profiles_dir)
        elif idx == 5:
            break
        
        input("\n  Pressione Enter para continuar...")

def list_profiles(profiles_dir: Path):
    print("\n  Perfis existentes:")
    profiles = list(profiles_dir.glob("*.env"))
    if not profiles:
        print("    Nenhum perfil encontrado.")
    else:
        for i, profile in enumerate(profiles, 1):
            print(f"    [{i}] {profile.stem}")

def create_profile(profiles_dir: Path):
    print("\n  Criar novo perfil de configuracao")
    profile_name = input("  Nome do perfil: ").strip()
    
    if not profile_name:
        print("  Nome invalido.")
        return
    
    profile_file = profiles_dir / f"{profile_name}.env"
    if profile_file.exists():
        print(f"  Perfil '{profile_name}' ja existe.")
        if not confirm("  Sobrescrever?"):
            return
    
    print("\n  Configuracao do perfil:")
    config = collect_full_config("game")
    
    env_content = "\n".join(f"{key}={value}" for key, value in config.items())
    profile_file.write_text(env_content, encoding='utf-8')
    
    print(f"\n  Perfil '{profile_name}' criado com sucesso!")

def load_profile(profiles_dir: Path):
    profiles = list(profiles_dir.glob("*.env"))
    if not profiles:
        print("  Nenhum perfil encontrado.")
        return
    
    options = [p.stem for p in profiles]
    options.append("Cancelar")
    
    idx = choose_from_menu("Selecione o perfil para carregar", options)
    if idx == len(profiles):
        return
    
    profile = profiles[idx]
    print(f"\n  Perfil '{profile.stem}' selecionado.")
    print("  Este perfil sera usado na criacao do proximo GameServer.")

def save_profile(profiles_dir: Path):
    servers = list_existing_game_servers()
    if not servers:
        print("  Nenhum GameServer encontrado.")
        return
    
    options = [f"GameServer #{s.server_id}" for s in servers]
    options.append("Cancelar")
    
    idx = choose_from_menu("Selecione o GameServer para salvar como perfil", options)
    if idx == len(servers):
        return
    
    server = servers[idx]
    
    profile_name = input("  Nome do perfil: ").strip()
    if not profile_name:
        print("  Nome invalido.")
        return
    
    profile_file = profiles_dir / f"{profile_name}.env"
    
    if server.env_path.exists():
        shutil.copy2(server.env_path, profile_file)
        print(f"\n  Perfil '{profile_name}' salvo com sucesso!")
    else:
        print("  Arquivo .env do GameServer nao encontrado.")

def delete_profile(profiles_dir: Path):
    profiles = list(profiles_dir.glob("*.env"))
    if not profiles:
        print("  Nenhum perfil encontrado.")
        return
    
    options = [p.stem for p in profiles]
    options.append("Cancelar")
    
    idx = choose_from_menu("Selecione o perfil para deletar", options)
    if idx == len(profiles):
        return
    
    profile = profiles[idx]
    
    if not confirm(f"  Deletar perfil '{profile.stem}'?"):
        return
    
    profile.unlink()
    print(f"\n  Perfil '{profile.stem}' deletado com sucesso!")

# ============================================================
# Main Menu
# ============================================================

def main_menu():
    while True:
        options = [
            "1. Iniciar MariaDB LoginServer",
            "2. Iniciar LoginServer",
            "3. Criar GameServer",
            "4. Iniciar GameServer",
            "5. Parar GameServer",
            "6. Parar Todos os Serviços",
            "7. Listar servidores ativos",
            "8. Logs",
            "9. Edicao em massa de environment",
            "10. Gerenciar perfis de configuracao",
            "11. Sair",
        ]
        
        idx = choose_from_menu("Lineternity Stack Manager v2.0", options)
        
        if idx == 0:
            start_mariadb_login()
        elif idx == 1:
            start_loginserver()
        elif idx == 2:
            create_game_server()
        elif idx == 3:
            start_game_server()
        elif idx == 4:
            stop_game_server()
        elif idx == 5:
            stop_all_services()
        elif idx == 6:
            list_servers()
        elif idx == 7:
            show_logs_menu()
        elif idx == 8:
            bulk_edit_env()
        elif idx == 9:
            manage_config_profiles()
        elif idx == 10:
            print("\n  Saindo...")
            break
        else:
            continue
        
        input("\n  Pressione Enter para continuar...")

# ============================================================
# Entry Point
# ============================================================

if __name__ == "__main__":
    try:
        main_menu()
    except KeyboardInterrupt:
        print("\n\n  Saindo...")
        sys.exit(0)
