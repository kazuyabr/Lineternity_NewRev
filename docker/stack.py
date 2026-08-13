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
import xml.etree.ElementTree as ET
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
DISTRIBUTION_DIR = PROJECT_ROOT / "build" / "distribution"

# ============================================================
# Colors (ANSI)
# ============================================================

class C:
    """ANSI color codes for terminal output."""
    RESET   = "\033[0m"
    BOLD    = "\033[1m"
    DIM     = "\033[2m"
    # Text colors
    RED     = "\033[1;31m"
    GREEN   = "\033[1;32m"
    YELLOW  = "\033[1;33m"
    BLUE    = "\033[1;34m"
    MAGENTA = "\033[1;35m"
    CYAN    = "\033[1;36m"
    WHITE   = "\033[1;37m"
    GRAY    = "\033[2;37m"
    # Backgrounds
    BG_RED   = "\033[41m"
    BG_GREEN = "\033[42m"
    BG_BLUE  = "\033[44m"

# ============================================================
# Java Detection
# ============================================================

def detect_java_home():
    """Detecta JAVA_HOME a partir do comando java no PATH ou da variavel de ambiente."""
    # 1. Usar JAVA_HOME do ambiente se existir
    java_home = os.environ.get("JAVA_HOME")
    if java_home and Path(java_home).exists():
        return java_home
    
    # 2. Detectar a partir do executavel java no PATH
    try:
        if os.name == 'nt':
            result = subprocess.run(
                ["where", "java"],
                capture_output=True, text=True, timeout=10
            )
            if result.returncode == 0:
                java_exe = result.stdout.strip().splitlines()[0]
                # C:\Program Files\jdk\bin\java.exe -> C:\Program Files\jdk
                java_home = str(Path(java_exe).parent.parent)
                if Path(java_home).exists():
                    return java_home
        else:
            result = subprocess.run(
                ["readlink", "-f", "$(which java)"],
                shell=True, capture_output=True, text=True, timeout=10
            )
            if result.returncode == 0:
                java_exe = result.stdout.strip()
                java_home = str(Path(java_exe).parent.parent)
                if Path(java_home).exists():
                    return java_home
    except Exception:
        pass
    
    # 3. Nao encontrado
    return None

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
    print(f"{C.CYAN}{'=' * 60}{C.RESET}")
    print(f"  {C.WHITE}{C.BOLD}{title}{C.RESET}")
    print(f"{C.CYAN}{'=' * 60}{C.RESET}")
    print()

def choose_from_menu(title: str, options: list[str]) -> int:
    print_header(title)
    for i, option in enumerate(options, 1):
        print(f"  {C.CYAN}[{i}]{C.RESET} {option}")
    print()
    
    while True:
        try:
            choice = input(f"  {C.YELLOW}Selecione uma opcao: {C.RESET}").strip()
            if choice.isdigit():
                idx = int(choice) - 1
                if 0 <= idx < len(options):
                    return idx
            print(f"  {C.RED}Opcao invalida. Tente novamente.{C.RESET}")
        except (EOFError, KeyboardInterrupt):
            print(f"\n  {C.RED}Operacao cancelada.{C.RESET}")
            return -1

def confirm(message: str) -> bool:
    while True:
        choice = input(f"  {C.YELLOW}{message} (s/n): {C.RESET}").strip().lower()
        if choice in ('s', 'sim', 'y', 'yes'):
            return True
        if choice in ('n', 'nao', 'no'):
            return False
        print(f"  {C.RED}Resposta invalida. Digite 's' ou 'n'.{C.RESET}")

def prompt_property(config: PropertyConfig) -> str:
    if config.required:
        prompt = f"  {C.CYAN}{config.label}{C.RESET} [{C.DIM}{config.default}{C.RESET}]: "
    else:
        prompt = f"  {C.CYAN}{config.label}{C.RESET} [{C.DIM}{config.default}{C.RESET}] (enter para padrao): "
    
    value = input(prompt).strip()
    
    if not value and config.default:
        return config.default
    
    if config.required and not value:
        print(f"  {C.RED}ERRO: {config.label} e obrigatorio!{C.RESET}")
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
    # Copiar TODOS os arquivos de game/config/ (source é autoridade)
    source_config = PROJECT_ROOT / "game" / "config"
    for source_file in source_config.iterdir():
        if source_file.is_file():
            shutil.copy2(source_file, config_dir / source_file.name)

    # Aplicar overrides per-server (placeholders) em todos os .properties
    for prop_file in config_dir.glob("*.properties"):
        content = prop_file.read_text(encoding='utf-8')
        modified = False
        for key, value in config.items():
            placeholder = "{{" + key + "}}"
            if placeholder in content:
                content = content.replace(placeholder, value)
                modified = True
        if modified:
            prop_file.write_text(content, encoding='utf-8')

    print(f"  Config copiada de game/config/ para: {config_dir}")

# ============================================================
# Network Management
# ============================================================

def ensure_lineternity_network():
    """Garante que a rede lineternity-network existe"""
    result = subprocess.run(
        ["docker", "network", "ls", "--filter", "name=lineternity-network", "--format", "{{.Name}}"],
        capture_output=True, text=True
    )
    if "lineternity-network" in result.stdout:
        print("  Rede lineternity-network: existe")
        return True
    
    print("  Criando rede lineternity-network...")
    result = subprocess.run(
        ["docker", "network", "create", "lineternity-network"],
        capture_output=True, text=True
    )
    if result.returncode == 0:
        print("  Rede lineternity-network criada com sucesso!")
        return True
    else:
        print(f"  ERRO ao criar rede: {result.stderr.strip()}")
        return False

def connect_container_to_network(container_name: str):
    """Conecta um container à rede lineternity-network (se ainda não estiver)"""
    # Verificar redes atuais do container
    result = subprocess.run(
        ["docker", "inspect", "--format",
         "{{range $k, $v := .NetworkSettings.Networks}}{{$k}} {{end}}",
         container_name],
        capture_output=True, text=True
    )
    
    if "lineternity-network" in result.stdout:
        print(f"  Container '{container_name}' ja esta na rede lineternity-network")
        return True
    
    # Conectar
    print(f"  Conectando '{container_name}' à rede lineternity-network...")
    result = subprocess.run(
        ["docker", "network", "connect", "lineternity-network", container_name],
        capture_output=True, text=True
    )
    if result.returncode == 0:
        print(f"  '{container_name}' conectado à rede lineternity-network!")
        return True
    else:
        print(f"  ERRO ao conectar: {result.stderr.strip()}")
        return False

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
    
    if confirm("\n  Iniciar LoginServer agora?"):
        start_loginserver()

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
                status_color = C.GREEN if "Up" in parts[1] else C.RED
                print(f"    {parts[0]}: {status_color}{parts[1]}{C.RESET}")
    else:
        print(f"    {C.DIM}MariaDB LoginServer: nao iniciado{C.RESET}")
    
    # Check LoginServer
    result = subprocess.run(
        ["docker", "ps", "-a", "--filter", "name=lineternity-loginserver", "--format", "{{.Names}}\t{{.Status}}"],
        capture_output=True, text=True
    )
    if result.stdout.strip():
        for line in result.stdout.strip().splitlines():
            parts = line.split("\t")
            if len(parts) == 2:
                status_color = C.GREEN if "Up" in parts[1] else C.RED
                print(f"    {parts[0]}: {status_color}{parts[1]}{C.RESET}")
    else:
        print(f"    {C.DIM}LoginServer: nao iniciado{C.RESET}")
    
    # Check gameservers
    print(f"\n  {C.CYAN}--- GameServers ---{C.RESET}")
    servers = list_existing_game_servers()
    if servers:
        for server in servers:
            # Check MariaDB for this server
            mariadb_name = f"lineternity-mariadb-gs{server.server_id}"
            result = subprocess.run(
                ["docker", "ps", "-a", "--filter", f"name={mariadb_name}", "--format", "{{.Names}}\t{{.Status}}"],
                capture_output=True, text=True
            )
            mariadb_status = f"{C.DIM}nao iniciado{C.RESET}"
            if result.stdout.strip():
                parts = result.stdout.strip().split("\t")
                if len(parts) == 2:
                    status_color = C.GREEN if "Up" in parts[1] else C.RED
                    mariadb_status = f"{status_color}{parts[1]}{C.RESET}"
            
            # Check GameServer
            gs_name = f"lineternity-gameserver-{server.server_id}"
            result = subprocess.run(
                ["docker", "ps", "-a", "--filter", f"name={gs_name}", "--format", "{{.Names}}\t{{.Status}}"],
                capture_output=True, text=True
            )
            gs_status = f"{C.DIM}nao iniciado{C.RESET}"
            if result.stdout.strip():
                parts = result.stdout.strip().split("\t")
                if len(parts) == 2:
                    status_color = C.GREEN if "Up" in parts[1] else C.RED
                    gs_status = f"{status_color}{parts[1]}{C.RESET}"
            
            print(f"    {C.WHITE}GameServer #{server.server_id}{C.RESET}: {server.hostname} (port {server.public_port})")
            print(f"      MariaDB: {mariadb_status}")
            print(f"      GameServer: {gs_status}")
    else:
        print(f"    {C.DIM}Nenhum GameServer configurado.{C.RESET}")
    
    input(f"\n  {C.DIM}Pressione Enter para continuar...{C.RESET}")

def show_logs_menu():
    print_header("Logs")
    
    options = [
        f"{C.CYAN}Logs do MariaDB LoginServer{C.RESET}",
        f"{C.CYAN}Logs do LoginServer{C.RESET}",
        f"{C.CYAN}Logs do GameServer{C.RESET} (selecionar)",
        f"{C.CYAN}Logs de todos os containers{C.RESET}",
        f"{C.DIM}Voltar{C.RESET}",
    ]
    
    idx = choose_from_menu("Selecione o tipo de log", options)
    
    if idx == 0:
        print(f"\n  {C.YELLOW}Logs do MariaDB LoginServer (Ctrl+C para sair):{C.RESET}")
        subprocess.run(["docker", "logs", "-f", "lineternity-mariadb-login"])
    elif idx == 1:
        print(f"\n  {C.YELLOW}Logs do LoginServer (Ctrl+C para sair):{C.RESET}")
        subprocess.run(["docker", "logs", "-f", "lineternity-loginserver"])
    elif idx == 2:
        servers = list_existing_game_servers()
        if not servers:
            print(f"  {C.RED}Nenhum GameServer encontrado.{C.RESET}")
        else:
            options = [f"{C.CYAN}GameServer #{s.server_id}{C.RESET}" for s in servers]
            idx = choose_from_menu("Selecione o GameServer", options)
            if 0 <= idx < len(servers):
                server = servers[idx]
                print(f"\n  {C.YELLOW}Logs do GameServer #{server.server_id} (Ctrl+C para sair):{C.RESET}")
                subprocess.run(["docker", "logs", "-f", f"lineternity-gameserver-{server.server_id}"])
    elif idx == 3:
        print(f"\n  {C.YELLOW}Logs de todos os containers (Ctrl+C para sair):{C.RESET}")
        subprocess.run(["docker", "logs", "-f", "lineternity-mariadb-login", "lineternity-loginserver"])
    elif idx == 4:
        return

# ============================================================
# Quick Data Update (docker cp for remote/production)
# ============================================================

def update_container_data():
    """Atualiza dados não compilados (HTML, XML, etc) em containers via docker cp"""
    print_header("Atualizar Dados nos Containers")
    
    # Listar containers Lineternity rodando
    result = subprocess.run(
        ["docker", "ps", "--filter", "name=lineternity", "--format", "{{.Names}}\t{{.Ports}}\t{{.Status}}"],
        capture_output=True, text=True
    )
    
    if not result.stdout.strip():
        print("  Nenhum container Lineternity rodando.")
        input("\n  Pressione Enter para voltar...")
        return
    
    containers = []
    for line in result.stdout.strip().splitlines():
        parts = line.split("\t")
        if len(parts) >= 2:
            containers.append({"name": parts[0], "ports": parts[1], "status": parts[2] if len(parts) > 2 else ""})
    
    # Filtrar: só GameServer e LoginServer (não MariaDB)
    updatable = [c for c in containers if "mariadb" not in c["name"]]
    
    if not updatable:
        print("  Nenhum container atualizável rodando (só GameServer/LoginServer).")
        input("\n  Pressione Enter para voltar...")
        return
    
    print("  Containers atualizáveis:")
    for i, c in enumerate(updatable, 1):
        print(f"    [{i}] {c['name']} ({c['ports']}) - {c['status']}")
    
    print(f"    [V] Voltar")
    
    choice = input("\n  Selecionar container: ").strip()
    if choice.lower() == 'v' or not choice.isdigit():
        return
    
    idx = int(choice) - 1
    if idx < 0 or idx >= len(updatable):
        print("  Opção inválida.")
        input("\n  Pressione Enter para voltar...")
        return
    
    container = updatable[idx]
    container_name = container["name"]
    
    # Determinar tipo de container
    if "gameserver" in container_name:
        _update_gameserver_data(container_name)
    elif "loginserver" in container_name:
        _update_loginserver_data(container_name)
    else:
        print(f"  Tipo de container desconhecido: {container_name}")
    
    input("\n  Pressione Enter para voltar...")


def _update_gameserver_data(container_name: str):
    """Atualiza dados de um GameServer específico via docker cp"""
    print(f"\n  Itens atualizáveis:")
    print(f"    [1] config/          (~2 MB) - Properties (//reload config)")
    print(f"    [2] serverNames.xml  (~0 MB)")
    print(f"    [3] Todos")
    
    choice = input("\n  Selecionar (ex: 1,2): ").strip()
    if not choice:
        return
    
    dir_map = {
        "1": "config",
        "2": "serverNames.xml",
    }
    
    selected = []
    if choice == "3":
        selected = ["config", "serverNames.xml"]
    else:
        for item in choice.split(","):
            item = item.strip()
            if item in dir_map:
                selected.append(dir_map[item])
    
    if not selected:
        print("  Nenhum item selecionado.")
        return
    
    print(f"\n  Copiando para {container_name}...")
    for item in selected:
        if item == "config":
            src = PROJECT_ROOT / "game" / "config"
            dest_container = "/lineternity/game/config"
        else:
            src = PROJECT_ROOT / "game" / "data" / item
            dest_container = f"/lineternity/game/data/{item}"
        
        if src.exists():
            dest = f"{container_name}:{dest_container}"
            result = subprocess.run(
                ["docker", "cp", str(src) + "/.", dest] if src.is_dir() else ["docker", "cp", str(src), dest],
                capture_output=True, text=True
            )
            if result.returncode == 0:
                print(f"    {C.GREEN}OK:{C.RESET} {item}")
            else:
                print(f"    {C.RED}ERRO:{C.RESET} {item} - {result.stderr.strip()}")
        else:
            print(f"    {C.YELLOW}AVISO:{C.RESET} {src} nao existe, pulando...")
    
    print(f"\n  Copia concluída!")
    
    if confirm("  Reiniciar GameServer para recarregar?"):
        _restart_game_server(container_name)


def _update_loginserver_data(container_name: str):
    """Atualiza dados de um LoginServer específico"""
    print(f"\n  Arquivos do LoginServer:")
    print(f"    [1] login/serverNames.xml")
    print(f"    [2] login/config/       - Properties (//reload config)")
    
    choice = input("\n  Selecionar (ex: 1,2): ").strip()
    if not choice:
        return
    
    items = [c.strip() for c in choice.split(",")]
    
    for item in items:
        if item == "1":
            src = PROJECT_ROOT / "login" / "serverNames.xml"
            dest = f"{container_name}:/lineternity/login/serverNames.xml"
            if src.exists():
                result = subprocess.run(
                    ["docker", "cp", str(src), dest],
                    capture_output=True, text=True
                )
                if result.returncode == 0:
                    print(f"    OK: serverNames.xml")
                else:
                    print(f"    ERRO: {result.stderr.strip()}")
            else:
                print(f"    AVISO: {src} não existe!")
        elif item == "2":
            src = PROJECT_ROOT / "login" / "config"
            dest = f"{container_name}:/lineternity/login/config"
            if src.exists():
                result = subprocess.run(
                    ["docker", "cp", str(src) + "/.", dest],
                    capture_output=True, text=True
                )
                if result.returncode == 0:
                    print(f"    OK: login/config/")
                else:
                    print(f"    ERRO: {result.stderr.strip()}")
            else:
                print(f"    AVISO: {src} não existe!")
    
    print(f"\n  Copia concluída!")
    
    # Perguntar se quer reiniciar
    if confirm("  Reiniciar LoginServer para recarregar?"):
        _restart_login_server(container_name)


def _restart_game_server(container_name: str):
    """Reinicia um GameServer específico"""
    print(f"  Reiniciando {container_name}...")
    result = subprocess.run(
        ["docker", "restart", container_name],
        capture_output=True, text=True
    )
    if result.returncode == 0:
        print(f"  {container_name} reiniciado com sucesso!")
    else:
        print(f"  ERRO ao reiniciar: {result.stderr.strip()}")


def _restart_login_server(container_name: str):
    """Reinicia um LoginServer específico"""
    print(f"  Reiniciando {container_name}...")
    result = subprocess.run(
        ["docker", "restart", container_name],
        capture_output=True, text=True
    )
    if result.returncode == 0:
        print(f"  {container_name} reiniciado com sucesso!")
    else:
        print(f"  ERRO ao reiniciar: {result.stderr.strip()}")

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
# Build
# ============================================================

def build_project():
    """Compila o projeto completo: Gradle build + patches Java + ASM + build/distribution/"""
    print_header("Compilar Projeto")
    
    # Detectar JAVA_HOME
    java_home = detect_java_home()
    if not java_home:
        print("  ERRO: JAVA_HOME nao encontrado!")
        print("  Instale o JDK 25 e configure JAVA_HOME no ambiente.")
        print("  Exemplo: set JAVA_HOME=C:\\Program Files\\jdk")
        return False
    
    java_exe = Path(java_home) / "bin" / "java.exe"
    if not java_exe.exists():
        print(f"  ERRO: java.exe nao encontrado: {java_exe}")
        return False
    
    wrapper_jar = PROJECT_ROOT / "gradle" / "wrapper" / "gradle-wrapper.jar"
    if not wrapper_jar.exists():
        print(f"  ERRO: gradle-wrapper.jar nao encontrado: {wrapper_jar}")
        return False
    
    # Preparar environment com JAVA_HOME
    build_env = os.environ.copy()
    build_env["JAVA_HOME"] = java_home
    
    print(f"  JAVA_HOME: {java_home}")
    
    server_jar = PROJECT_ROOT / "libs" / "server.jar"
    
    print("  Este comando ira:")
    print("    1. Compilar todo o codigo Java + Kotlin do projeto")
    print("    2. Gerar libs/server.jar (fat JAR)")
    print("    3. Aplicar patches Java (9 arquivos compilados contra server.jar)")
    print("    4. Aplicar ASM bytecode patches (CreatureMove + PlayerMove)")
    print("    5. Montar build/distribution/ com arquivos runtime para Docker")
    print()
    
    if server_jar.exists():
        import time
        mtime = time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(server_jar.stat().st_mtime))
        print(f"  server.jar atual: {server_jar.stat().st_size:,} bytes (modificado: {mtime})")
    
    if DISTRIBUTION_DIR.exists():
        import time
        mtime = time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(DISTRIBUTION_DIR.stat().st_mtime))
        print(f"  build/distribution/ existe (modificado: {mtime})")
    else:
        print("  build/distribution/ NAO EXISTE (sera criado pelo build)")
    
    print()
    print("  Executando: java.exe ... GradleWrapperMain --no-daemon build distribution -x test")
    print("  Aguarde...")
    print()
    
    # Rodar Gradle direto via java.exe (evita problemas com gradlew.bat no Windows)
    gradle_cmd = [
        str(java_exe), "-Xmx64m", "-Xms64m",
        "-classpath", str(wrapper_jar),
        "org.gradle.wrapper.GradleWrapperMain",
        "--no-daemon", "--rerun-tasks",
        "build", "distribution", "-x", "test"
    ]
    
    try:
        result = subprocess.run(
            gradle_cmd,
            cwd=str(PROJECT_ROOT),
            env=build_env,
            timeout=600
        )
    except subprocess.TimeoutExpired:
        print("  ERRO: Build excedeu timeout de 10 minutos!")
        return False
    except FileNotFoundError:
        print(f"  ERRO: Nao foi possivel executar: {java_exe}")
        return False
    
    if result.returncode != 0:
        print(f"\n  ERRO: Gradle build falhou (exit code: {result.returncode})")
        print("  Verifique os erros acima e corrija antes de tentar novamente.")
        return False
    
    # Verificar resultado
    if not server_jar.exists():
        print("\n  ERRO: server.jar nao foi gerado apos o build!")
        return False
    
    if not DISTRIBUTION_DIR.exists():
        print("\n  ERRO: build/distribution/ nao foi criado apos o build!")
        return False
    
    dist_files = [f for f in DISTRIBUTION_DIR.rglob("*") if f.is_file()]
    if len(dist_files) == 0:
        print("\n  ERRO: build/distribution/ existe mas esta VAZIO!")
        print("  A task 'distribution' do Gradle nao copiou arquivos.")
        print("  Verifique se os arquivos fonte existem (libs/, game/, login/, etc.)")
        return False
    
    import time
    mtime = time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(server_jar.stat().st_mtime))
    size = server_jar.stat().st_size
    
    dist_size = sum(f.stat().st_size for f in dist_files)
    dist_size_mb = dist_size / (1024 * 1024)
    
    print()
    print("  " + "=" * 50)
    print(f"  BUILD CONCLUIDO COM SUCESSO!")
    print(f"  server.jar: {size:,} bytes (modificado: {mtime})")
    print(f"  build/distribution/: {dist_size_mb:.1f} MB ({len(dist_files)} arquivos)")
    print("  " + "=" * 50)
    print()
    print("  Para iniciar os servidores, use as opcoes do menu.")
    
    return True


def check_distribution():
    """Verifica se build/distribution/ existe e tem conteudo. Retorna True se OK."""
    if not DISTRIBUTION_DIR.exists():
        print("\n  AVISO: build/distribution/ nao existe!")
        print("  Execute a opcao 'Compilar Projeto (Build)' primeiro.")
        print("  Sem build/distribution/, os containers nao podem ser iniciados.")
        return False
    
    dist_files = [f for f in DISTRIBUTION_DIR.rglob("*") if f.is_file()]
    if len(dist_files) == 0:
        print("\n  AVISO: build/distribution/ existe mas esta VAZIO!")
        print("  Execute a opcao 'Compilar Projeto (Build)' primeiro.")
        print("  Sem arquivos em build/distribution/, os containers nao podem ser iniciados.")
        return False
    
    return True

# ============================================================
# Service Management
# ============================================================

def _stop_mariadb_login_if_running():
    """Para e remove container mariadb-login se estiver rodando (libera RAM quando usando MariaDB externo)."""
    result = subprocess.run(
        ["docker", "ps", "-a", "--filter", "name=lineternity-mariadb-login", "--format", "{{.Names}}\t{{.Status}}"],
        capture_output=True, text=True, timeout=10
    )
    if result.stdout.strip():
        print("\n  Parando container lineternity-mariadb-login (nao necessario com MariaDB externo)...")
        subprocess.run(["docker", "stop", "lineternity-mariadb-login"], capture_output=True, timeout=30)
        subprocess.run(["docker", "rm", "lineternity-mariadb-login"], capture_output=True, timeout=30)
        print("  Container lineternity-mariadb-login removido (RAM liberada).")

def _resolve_mariadb_choice(env_file: Path) -> bool:
    """
    Detecta MariaDB externo, SEMPRE pergunta ao usuario qual usar.
    Retorna True se deve usar externo, False se embedded.
    Atualiza .env conforme escolha.
    """
    config = _read_env_dict(env_file) if env_file.exists() else {}
    current_external = config.get("EXTERNAL_MARIADB", "false").lower() == "true"
    detection = detect_or_configure_mariadb()

    # ── Mostrar estado atual ──
    print("\n  ── Configuracao MariaDB ──")
    if current_external:
        db_host = config.get("DB_HOST", "localhost")
        db_port = config.get("DB_PORT", "3306")
        print(f"  Config atual: EXTERNO ({db_host}:{db_port})")
    else:
        print("  Config atual: EMBEDDED (mariadb-login)")

    # ── Mostrar MariaDB detectado ──
    if detection["found"]:
        db_exists = detection.get("database_exists", False)
        print(f"\n  MariaDB externo detectado: {detection['host']}:{detection['port']}")
        print(f"  Database l2jdb_login: {'EXISTE' if db_exists else 'NAO EXISTE (sera criado pelo entrypoint)'}")

    # ── SEMPRE perguntar ──
    print()
    if detection["found"]:
        print("  Opcoes:")
        print("    [1] Usar MariaDB externo (libera RAM do container)")
        print("    [2] Usar MariaDB embedded (cria container junto)")
        print("    [3] Configurar manualmente (host:porta)")

        while True:
            choice = input("\n  Selecione [1]: ").strip() or "1"

            if choice == "1":
                update_login_env(
                    detection["host"],
                    detection["port"],
                    detection.get("user", "root"),
                    detection.get("password", "root"),
                    external=True,
                )
                _stop_mariadb_login_if_running()
                return True
            elif choice == "2":
                update_login_env("mariadb-login", "3306", "root", "root", external=False)
                return False
            elif choice == "3":
                return _configure_mariadb_manual(env_file)
            else:
                print("  Opcao invalida. Tente novamente.")
    else:
        # Nenhum externo detectado
        print("  Nenhum MariaDB externo detectado.")
        print()
        print("  Opcoes:")
        print("    [1] Usar MariaDB embedded (cria container junto)")
        print("    [2] Configurar manualmente (host:porta)")

        while True:
            choice = input("\n  Selecione [1]: ").strip() or "1"

            if choice == "1":
                update_login_env("mariadb-login", "3306", "root", "root", external=False)
                return False
            elif choice == "2":
                return _configure_mariadb_manual(env_file)
            else:
                print("  Opcao invalida. Tente novamente.")


def _configure_mariadb_manual(env_file: Path) -> bool:
    """Configuracao manual de MariaDB externo."""
    print("\n  ── Configuracao Manual ──")

    while True:
        host = input("  Host do MariaDB [localhost]: ").strip() or "localhost"
        port = input("  Porta do MariaDB [3306]: ").strip() or "3306"
        user = input("  Usuario do MariaDB [root]: ").strip() or "root"
        password = input("  Senha do MariaDB [root]: ").strip() or "root"

        print(f"\n  Testando conexao com {user}@{host}:{port}...")
        if test_mariadb_connection(host, port, user, password):
            print("  Conexao OK!")
            update_login_env(host, port, user, password, external=True)
            _stop_mariadb_login_if_running()
            return True
        else:
            print("\n  Falha na conexao.")
            print()
            print("  Opcoes:")
            print("    [1] Tentar outras credenciais")
            print("    [2] Usar MariaDB embedded")
            print("    [3] Cancelar")

            choice = input("\n  Selecione: ").strip()

            if choice == "2":
                update_login_env("mariadb-login", "3306", "root", "root", external=False)
                return False
            elif choice == "3":
                return False

def _sync_gameservers_login_db(login_db_host: str, login_db_port: str):
    """Atualiza LOGIN_DB_HOST e LOGIN_DB_PORT no .env de todos os GameServers existentes."""
    if not GAMESERVERS_DIR.exists():
        return
    
    updated = 0
    for server_dir in GAMESERVERS_DIR.iterdir():
        if not server_dir.is_dir():
            continue
        env_file = server_dir / ".env"
        if not env_file.exists():
            continue
        
        config = _read_env_dict(env_file)
        old_host = config.get("LOGIN_DB_HOST", "")
        old_port = config.get("LOGIN_DB_PORT", "")
        
        if old_host == login_db_host and old_port == login_db_port:
            continue  # já está correto
        
        _update_env_key(env_file, "LOGIN_DB_HOST", login_db_host)
        _update_env_key(env_file, "LOGIN_DB_PORT", login_db_port)
        print(f"    {server_dir.name}: LOGIN_DB_HOST={login_db_host}:{login_db_port}")
        updated += 1
    
    if updated > 0:
        print(f"  {updated} GameServer(s) atualizado(s).")

def start_loginserver():
    print_header("Iniciar LoginServer")
    
    if not check_distribution():
        return False
    
    env_file = DOCKER_DIR / "login" / ".env"
    
    # ============================================================
    # Garantir rede
    # ============================================================
    print("  Verificando rede lineternity-network...")
    ensure_lineternity_network()
    
    # ============================================================
    # SEMPRE detectar MariaDB e perguntar ao usuario
    # ============================================================
    if not env_file.exists():
        # Primeira vez — criar .env basico
        print("  Primeira execucao - detectando MariaDB...")
        update_login_env("mariadb-login", "3306", "root", "root", external=False)
    
    # Salvar estado anterior antes de perguntar
    config_before = _read_env_dict(env_file) if env_file.exists() else {}
    was_external = config_before.get("EXTERNAL_MARIADB", "false").lower() == "true"
    
    # Resolver escolha do MariaDB (sempre detecta e pergunta)
    use_external = _resolve_mariadb_choice(env_file)
    
    # ============================================================
    # Ler config final do .env
    # ============================================================
    config = _read_env_dict(env_file)
    use_external = config.get("EXTERNAL_MARIADB", "false").lower() == "true"
    db_host = config.get("DB_HOST", "mariadb-login")
    db_port = config.get("DB_PORT", "3306")
    db_user = config.get("DB_USER", "root")
    db_password = config.get("DB_PASSWORD", "root")
    
    # ============================================================
    # Se externo: verificar conectividade e configurar rede
    # ============================================================
    if use_external:
        print(f"  Testando conexao com MariaDB externo: {db_host}:{db_port}...")
        
        if not test_mariadb_connection(db_host, db_port, db_user, db_password):
            print(f"\n  MariaDB externo ({db_host}:{db_port}) inacessivel!")
            if confirm("\n  Deseja usar MariaDB embedded (junto com o LoginServer)?"):
                update_login_env("mariadb-login", "3306", "root", "root", external=False)
                use_external = False
                db_host = "mariadb-login"
            else:
                return False
        
        if use_external:
            # .env mantem coordenadas acessíveis do host (localhost:porta)
            # O docker-compose.loginserver-external.yml usa host.docker.internal diretamente
            print(f"  Modo externo confirmado: localhost:{db_port}")
    
    # ============================================================
    # Sincronizar GameServers existentes com a escolha do LoginServer
    # .env dos GameServers é lido por containers Docker → usa host.docker.internal
    # ============================================================
    if use_external:
        print("\n  Sincronizando GameServers com MariaDB externo...")
        _sync_gameservers_login_db("host.docker.internal", db_port)
    elif was_external:
        # Estava externo, voltou para embedded — restaurar GameServers
        print("\n  Restaurando GameServers para MariaDB embedded...")
        _sync_gameservers_login_db("mariadb-login", "3306")
    
    # ============================================================
    # Escolher compose file
    # ============================================================
    if use_external:
        compose_file = DOCKER_DIR / "docker-compose.loginserver-external.yml"
        print("  Modo: MariaDB externo")
    else:
        compose_file = DOCKER_DIR / "docker-compose.loginserver.yml"
        print("  Modo: MariaDB embedded (mariadb-login junto com LoginServer)")
    
    if not compose_file.exists():
        print(f"  ERRO: {compose_file.name} nao encontrado em {compose_file}")
        return False
    
    # ============================================================
    # Regenerar properties com valores atuais do .env
    # ============================================================
    config_dir = DOCKER_DIR / "login" / "config"
    config_dir.mkdir(parents=True, exist_ok=True)
    login_config = _read_env_dict(env_file)
    create_login_properties(config_dir, login_config)
    
    # ============================================================
    # Build + Start
    # ============================================================
    print("  Iniciando LoginServer...")

    # Verificar se imagem ja existe
    img_check = subprocess.run(
        ["docker", "image", "inspect", "lineternity-newrev:latest"],
        capture_output=True, timeout=10,
    )
    if img_check.returncode != 0:
        print("  Imagem nao encontrada. Buildando...")
        run_compose(compose_file, "build", "--no-cache", env_file=env_file)
    else:
        print("  Imagem lineternity-newrev:latest ja existe. Use opcao [1] Build para reconstruir.")
    
    if not run_compose(compose_file, "up", "-d", env_file=env_file):
        print("  ERRO ao iniciar LoginServer!")
        return False
    
    print("\n  LoginServer iniciado com sucesso!")
    return True

def _update_env_key(env_file: Path, key: str, value: str):
    """Atualiza uma chave no arquivo .env"""
    content = env_file.read_text(encoding='utf-8')
    lines = content.splitlines()
    found = False
    for i, line in enumerate(lines):
        if line.startswith(f"{key}="):
            lines[i] = f"{key}={value}"
            found = True
            break
    if not found:
        lines.append(f"{key}={value}")
    env_file.write_text("\n".join(lines) + "\n", encoding='utf-8')

def _read_env_dict(env_file: Path) -> dict[str, str]:
    """ Lê arquivo .env e retorna dict de chave=valor"""
    config = {}
    if env_file.exists():
        for line in env_file.read_text(encoding='utf-8').splitlines():
            if "=" in line and not line.startswith("#"):
                key, value = line.split("=", 1)
                config[key.strip()] = value.strip()
    return config

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
    
    # Stop login server — usar compose correto baseado em EXTERNAL_MARIADB
    print("\n  Parando LoginServer...")
    env_file = DOCKER_DIR / "login" / ".env"
    if env_file.exists():
        config = _read_env_dict(env_file)
        use_external = config.get("EXTERNAL_MARIADB", "false").lower() == "true"
    else:
        use_external = False
    
    if use_external:
        compose_file = DOCKER_DIR / "docker-compose.loginserver-external.yml"
    else:
        compose_file = DOCKER_DIR / "docker-compose.loginserver.yml"
    
    if compose_file.exists():
        run_compose(compose_file, "down")
    
    # Parar mariadb-login se existir (pode ter ficado de sessao anterior)
    _stop_mariadb_login_if_running()
    
    # Stop base MariaDB (se existir como servico separado)
    print("\n  Parando MariaDB LoginServer...")
    compose_file = DOCKER_DIR / "docker-compose.yml"
    if compose_file.exists():
        run_compose(compose_file, "down")
    
    print("\n  Todos os servicos parados com sucesso!")

def update_images():
    print_header("Atualizar Imagens (Build + Docker)")
    
    # ── PASSO 1: Compilar Java/Kotlin (gera server.jar + build/distribution/) ──
    print(f"  {C.BLUE}[1/3]{C.RESET} {C.WHITE}Compilando projeto (Gradle build)...{C.RESET}")
    print()
    
    java_home = detect_java_home()
    if not java_home:
        print(f"  {C.RED}ERRO: JAVA_HOME nao encontrado!{C.RESET}")
        print(f"  {C.DIM}Instale o JDK 25 e configure JAVA_HOME no ambiente.{C.RESET}")
        return
    
    java_exe = Path(java_home) / "bin" / "java.exe"
    if not java_exe.exists():
        print(f"  {C.RED}ERRO: java.exe nao encontrado: {java_exe}{C.RESET}")
        return
    
    wrapper_jar = PROJECT_ROOT / "gradle" / "wrapper" / "gradle-wrapper.jar"
    if not wrapper_jar.exists():
        print(f"  {C.RED}ERRO: gradle-wrapper.jar nao encontrado: {wrapper_jar}{C.RESET}")
        return
    
    build_env = os.environ.copy()
    build_env["JAVA_HOME"] = java_home
    
    server_jar = PROJECT_ROOT / "libs" / "server.jar"
    
    print(f"  {C.DIM}JAVA_HOME: {java_home}{C.RESET}")
    print(f"  {C.DIM}Executando: java.exe ... GradleWrapperMain --no-daemon build distribution -x test{C.RESET}")
    print(f"  {C.YELLOW}Aguarde...{C.RESET}")
    print()
    
    gradle_cmd = [
        str(java_exe), "-Xmx64m", "-Xms64m",
        "-classpath", str(wrapper_jar),
        "org.gradle.wrapper.GradleWrapperMain",
        "--no-daemon", "--rerun-tasks",
        "build", "distribution", "-x", "test"
    ]
    
    try:
        result = subprocess.run(
            gradle_cmd,
            cwd=str(PROJECT_ROOT),
            env=build_env,
            timeout=600
        )
    except subprocess.TimeoutExpired:
        print(f"  {C.RED}ERRO: Build excedeu timeout de 10 minutos!{C.RESET}")
        return
    except FileNotFoundError:
        print(f"  {C.RED}ERRO: Nao foi possivel executar: {java_exe}{C.RESET}")
        return
    
    if result.returncode != 0:
        print(f"\n  {C.RED}ERRO: Gradle build falhou (exit code: {result.returncode}){C.RESET}")
        print(f"  {C.DIM}Corrija os erros antes de tentar novamente.{C.RESET}")
        return
    
    if not server_jar.exists():
        print(f"\n  {C.RED}ERRO: server.jar nao foi gerado apos o build!{C.RESET}")
        return
    
    if not DISTRIBUTION_DIR.exists():
        print(f"\n  {C.RED}ERRO: build/distribution/ nao foi criado apos o build!{C.RESET}")
        return
    
    dist_files = [f for f in DISTRIBUTION_DIR.rglob("*") if f.is_file()]
    if len(dist_files) == 0:
        print(f"\n  {C.RED}ERRO: build/distribution/ existe mas esta VAZIO!{C.RESET}")
        return
    
    import time
    mtime = time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(server_jar.stat().st_mtime))
    print(f"  {C.GREEN}Build Gradle OK:{C.RESET} server.jar {server_jar.stat().st_size:,} bytes ({mtime})")
    print(f"  {C.DIM}build/distribution/: {len(dist_files)} arquivos{C.RESET}")
    print()
    
    # ── PASSO 2: Selecionar servidores para atualizar ──
    print(f"  {C.BLUE}[2/3]{C.RESET} {C.WHITE}Selecionar servidores...{C.RESET}")
    print()
    
    # Verificar se Dockerfiles existem na distribution
    root_df = DISTRIBUTION_DIR / "Dockerfile"
    docker_df = DISTRIBUTION_DIR / "docker" / "Dockerfile"
    if not root_df.exists() or not docker_df.exists():
        print(f"\n  {C.YELLOW}AVISO: Dockerfiles nao encontrados em build/distribution/{C.RESET}")
        print(f"  {C.DIM}O build gerou a distribuicao mas os Dockerfiles estao faltando.{C.RESET}")
        return
    
    # Listar servidores disponiveis
    login_env = DOCKER_DIR / "login" / ".env"
    login_compose = None
    if login_env.exists():
        content = login_env.read_text(encoding='utf-8')
        use_external = any(
            line.startswith("EXTERNAL_MARIADB=") and line.split("=", 1)[1].strip().lower() == "true"
            for line in content.splitlines()
        )
        if use_external:
            login_compose = DOCKER_DIR / "docker-compose.loginserver-external.yml"
        else:
            login_compose = DOCKER_DIR / "docker-compose.loginserver.yml"
    
    game_servers = list_existing_game_servers()
    
    # Montar menu
    options = []
    if login_compose and login_compose.exists():
        options.append("LoginServer")
    for s in game_servers:
        options.append(f"GameServer #{s.server_id} ({s.hostname})")
    options.append("Todos")
    options.append("Cancelar")
    
    if not options or options == ["Cancelar"]:
        print("  Nenhum servidor configurado.")
        return
    
    idx = choose_from_menu("Selecione para atualizar imagem", options)
    
    total = len(options)
    if idx == total - 1:  # Cancelar
        print("  Operacao cancelada.")
        return
    
    # Determinar quais atualizar
    to_update = []
    if idx == total - 2:  # Todos
        if login_compose and login_compose.exists():
            to_update.append(("LoginServer", login_compose))
        for s in game_servers:
            to_update.append((f"GameServer #{s.server_id}", s.compose_path))
    elif login_compose and login_compose.exists() and idx == 0:
        to_update.append(("LoginServer", login_compose))
    else:
        # GameServer (ajustar indice)
        gs_idx = idx - (1 if login_compose and login_compose.exists() else 0)
        if 0 <= gs_idx < len(game_servers):
            s = game_servers[gs_idx]
            to_update.append((f"GameServer #{s.server_id}", s.compose_path))
    
    if not to_update:
        print("  Nenhum servidor selecionado.")
        return
    
    # Confirmar
    print(f"\n  {C.YELLOW}Imagens que serao atualizadas:{C.RESET}")
    for name, _ in to_update:
        print(f"    {C.CYAN}-{C.RESET} {name}")
    
    print(f"\n  {C.DIM}NOTA: Volumes de dados (MariaDB, xml, config, locale) NAO serao afetados.{C.RESET}")
    print(f"  {C.DIM}O container sera recriado com a imagem atualizada.{C.RESET}")
    
    if not confirm("\n  Continuar?"):
        print(f"  {C.RED}Operacao cancelada.{C.RESET}")
        return
    
    # ── PASSO 3: Rebuild Docker + recreate containers ──
    print()
    print(f"  {C.BLUE}[3/3]{C.RESET} {C.WHITE}Reconstruindo imagens Docker e recriando containers...{C.RESET}")
    
    success = 0
    fail = 0
    for name, compose_file in to_update:
        print(f"\n  {C.CYAN}--- Atualizando {name} ---{C.RESET}")
        print(f"  {C.DIM}Compose: {compose_file.name}{C.RESET}")
        
        env_file = compose_file.parent / ".env"
        if not env_file.exists():
            env_file = None
        
        # Build (forcar rebuild completo para garantir imagem atualizada)
        print(f"  {C.YELLOW}Reconstruindo imagem (build --no-cache)...{C.RESET}")
        if not run_compose(compose_file, "build", "--no-cache", env_file=env_file):
            print(f"  {C.RED}ERRO: Build falhou para {name}{C.RESET}")
            fail += 1
            continue
        
        # Recreate
        print(f"  {C.YELLOW}Recriando container...{C.RESET}")
        if not run_compose(compose_file, "up", "-d", "--force-recreate", env_file=env_file):
            print(f"  {C.RED}ERRO: Recreate falhou para {name}{C.RESET}")
            fail += 1
            continue
        
        print(f"  {C.GREEN}{name} atualizado com sucesso!{C.RESET}")
        success += 1
    
    print(f"\n  {C.CYAN}{'=' * 40}{C.RESET}")
    print(f"  {C.GREEN}Atualizacao concluida: {success} ok, {fail} falha(s){C.RESET}")
    print()
    print(f"  {C.YELLOW}Proximo passo no jogo: //reload config{C.RESET}")

def start_game_server():
    print_header("Iniciar GameServer")
    
    if not check_distribution():
        return
    
    # Garantir que a rede lineternity-network existe
    print("  Verificando rede lineternity-network...")
    ensure_lineternity_network()
    print()
    
    servers = list_existing_game_servers()
    
    # Build options: existing servers + create new + cancel
    options = [f"GameServer #{s.server_id} ({s.hostname})" for s in servers]
    options.append("Criar novo GameServer")
    options.append("Todos os GameServers")
    options.append("Cancelar")
    
    idx = choose_from_menu("Selecione o GameServer", options)
    
    total = len(servers)
    
    if idx == total + 2:
        print("  Operacao cancelada.")
        return
    
    if idx == total + 1:
        # Start all existing
        if not servers:
            print("  Nenhum GameServer configurado.")
            return
        print("\n  Iniciando todos os GameServers...")
        for server in servers:
            compose_file = server.compose_path
            env_file = server.env_path
            if compose_file.exists():
                print(f"\n  --- GameServer #{server.server_id} ---")
                print(f"  Reconstruindo imagem...")
                run_compose(compose_file, "build", "--no-cache", env_file=env_file if env_file.exists() else None)
                run_compose(compose_file, "up", "-d", env_file=env_file if env_file.exists() else None)
        print("\n  Todos os GameServers iniciados!")
        return
    
    if idx == total:
        # Create new GameServer
        while True:
            server_id_str = input("\n  Server ID (1-255): ").strip()
            if server_id_str.isdigit():
                server_id = int(server_id_str)
                if 1 <= server_id <= 255:
                    break
            print("  ID invalido. Use um numero entre 1 e 255.")
        
        server_dir = GAMESERVERS_DIR / f"gameserver-{server_id}"
        
        if server_dir.exists():
            print(f"\n  GameServer #{server_id} ja existe.")
            if not confirm("  Sobrescrever configuracao?"):
                print("  Operacao cancelada.")
                return
        
        # Collect config (basic mode by default)
        config = collect_mandatory_config("game")
        
        # Apply defaults
        defaults = {
            "SERVER_ID": str(server_id),
            "SERVER_HOSTNAME": f"gameserver-{server_id}",
            "PUBLIC_PORT": str(7776 + server_id),
            "GAME_DB": f"l2jdb_gs{server_id}",
        }
        
        # Ler DB_HOST do LoginServer para LOGIN_DB_HOST do GameServer
        login_env = DOCKER_DIR / "login" / ".env"
        login_db_host = "mariadb-login"
        login_db_port = "3306"
        login_db_user = "root"
        login_db_password = "root"
        if login_env.exists():
            for line in login_env.read_text(encoding="utf-8").splitlines():
                line = line.strip()
                if line.startswith("DB_HOST="):
                    login_db_host = line.split("=", 1)[1].strip()
                elif line.startswith("DB_PORT="):
                    login_db_port = line.split("=", 1)[1].strip()
                elif line.startswith("DB_USER="):
                    login_db_user = line.split("=", 1)[1].strip()
                elif line.startswith("DB_PASSWORD="):
                    login_db_password = line.split("=", 1)[1].strip()
        
        defaults["LOGIN_DB_HOST"] = login_db_host
        defaults["LOGIN_DB_PORT"] = login_db_port
        defaults["LOGIN_DB_USER"] = login_db_user
        defaults["LOGIN_DB_PASSWORD"] = login_db_password
        defaults["LOGIN_DB"] = "l2jdb_login"
        defaults["LOGIN_HOSTNAME"] = "loginserver"
        defaults["LOGIN_PORT"] = "9014"
        
        for key, value in defaults.items():
            if key not in config or not config[key]:
                config[key] = value
        
        print("\n  Configuracao:")
        for key, value in config.items():
            print(f"    {key}: {value}")
        
        if not confirm(f"\n  Criar e iniciar GameServer #{server_id}?"):
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
        
        # Fix Hostname for Docker: client needs 127.0.0.1 (port mapped to host)
        server_props = config_dir / "server.properties"
        if server_props.exists():
            content = server_props.read_text(encoding='utf-8')
            content = re.sub(r'^Hostname\s*=.*$', 'Hostname = 127.0.0.1', content, flags=re.MULTILINE)
            server_props.write_text(content, encoding='utf-8')
        
        print(f"\n  GameServer #{server_id} criado!")
        
        # Start it
        print(f"  Iniciando GameServer #{server_id}...")
        print(f"  Reconstruindo imagem...")
        run_compose(compose_file, "build", "--no-cache", env_file=env_file if env_file.exists() else None)
        run_compose(compose_file, "up", "-d", env_file=env_file if env_file.exists() else None)
        print(f"\n  GameServer #{server_id} iniciado!")
        return
    
    # Start selected existing server
    if 0 <= idx < total:
        server = servers[idx]
        compose_file = server.compose_path
        env_file = server.env_path
        if compose_file.exists():
            print(f"\n  Iniciando GameServer #{server.server_id}...")
            print(f"  Reconstruindo imagem...")
            run_compose(compose_file, "build", "--no-cache", env_file=env_file if env_file.exists() else None)
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

def bulk_edit_config():
    print_header("Edicao de Configuracoes por Servidor")
    
    print("  Tipo de servidor:")
    print("    [1] GameServer")
    print("    [2] LoginServer")
    print("    [3] Cancelar")
    
    tipo = input("\n  Selecionar: ").strip()
    if tipo == "3" or not tipo:
        return
    
    if tipo == "1":
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
        config_dir = server.config_dir
        server_label = f"GameServer #{server.server_id}"
    elif tipo == "2":
        # LoginServer - listar arquivos de config
        config_dir = DOCKER_DIR / "login" / "config"
        server_label = "LoginServer"
    else:
        print("  Opcao invalida.")
        return
    
    if not config_dir.exists():
        print(f"  Diretorio de config nao encontrado: {config_dir}")
        input("\n  Pressione Enter para continuar...")
        return
    
    # Listar arquivos .properties
    props_files = sorted(config_dir.glob("*.properties"))
    if not props_files:
        print("  Nenhum arquivo .properties encontrado.")
        input("\n  Pressione Enter para continuar...")
        return
    
    print(f"\n  Arquivos .properties em {server_label}:")
    for i, f in enumerate(props_files, 1):
        size = f.stat().st_size
        print(f"    [{i}] {f.name} ({size} bytes)")
    
    print(f"    [V] Voltar")
    
    choice = input("\n  Selecionar arquivo: ").strip()
    if choice.lower() == 'v' or not choice.isdigit():
        return
    
    idx = int(choice) - 1
    if idx < 0 or idx >= len(props_files):
        print("  Opcao invalida.")
        return
    
    selected_file = props_files[idx]
    
    lines = selected_file.read_text(encoding='utf-8').splitlines()
    props = []
    for line in lines:
        stripped = line.strip()
        if stripped and not stripped.startswith("#") and "=" in stripped:
            key, value = stripped.split("=", 1)
            props.append({
                "key": key.strip(),
                "value": value.strip(),
                "original": line,
                "new_value": value.strip(),
                "modified": False,
            })
        else:
            props.append({
                "key": None, "value": None, "original": line,
                "new_value": None, "modified": False,
            })
    
    _edit_properties_interactive(selected_file, props, server_label)


def _edit_properties_interactive(selected_file, props, server_label):
    editable = [(i, p) for i, p in enumerate(props) if p["key"]]
    if not editable:
        print("\n  Nenhuma propriedade editavel encontrada.")
        return
    
    history = []
    
    def show_props():
        print(f"\n  {'=' * 55}")
        print(f"  {selected_file.name} — Editando")
        print(f"  {'=' * 55}")
        for idx_e, (i, p) in enumerate(editable, 1):
            marker = " *" if p["modified"] else ""
            print(f"    [{idx_e:2d}] {p['key']} = {p['new_value']}{marker}")
        print(f"  {'=' * 55}")
        c_reset = "\033[0m"
        c_cmd = "\033[1;33m"   # amarelo bold
        c_green = "\033[1;32m" # verde bold
        c_cyan = "\033[1;36m"  # ciano bold
        c_dim = "\033[2m"      # cinza
        print(f"  {c_dim}{'─' * 55}{c_reset}")
        print(f"  {c_cmd}(<){c_reset} Desfazer   "
              f"{c_cmd}(!){c_reset} Salvar+Sair   "
              f"{c_cmd}(q){c_reset} Sair   "
              f"{c_cmd}(enter){c_reset} Menu")
        print(f"  {c_dim}{'─' * 55}{c_reset}")
        print(f"  {c_cyan}Exemplos:{c_reset} 1,3,5  |  1-5  |  all")
    
    while True:
        editable = [(i, p) for i, p in enumerate(props) if p["key"]]
        show_props()
        
        choice = input("\n  Editar: ").strip().lower()
        
        if not choice:
            break
        
        if choice == "!":
            _save_and_copy(selected_file, props, server_label)
            return
        
        if choice == "q":
            if any(p["modified"] for p in props):
                if confirm("  Alteracoes nao salvas. Sair sem salvar?"):
                    print("  Alteracoes descartadas.")
                    return
            else:
                return
        
        if choice == "<":
            if history:
                state = history.pop()
                props[state["idx"]]["new_value"] = state["old_value"]
                props[state["idx"]]["modified"] = state["was_modified"]
                print(f"  Desfeito: {props[state['idx']]['key']}")
            else:
                print("  Nada para desfazer.")
            continue
        
        indices = _parse_selection(choice, len(editable))
        if not indices:
            print("  Selecao invalida. Use numeros (1,3,5), faixas (1-5), ou 'all'.")
            continue
        
        for idx_e in indices:
            if idx_e < 1 or idx_e > len(editable):
                continue
            real_idx, prop = editable[idx_e - 1]
            
            print(f"\n  {prop['key']}:")
            print(f"    Atual: {prop['new_value']}")
            if prop["modified"]:
                print(f"    Original: {prop['value']}")
            
            new_val = input(f"    Novo valor [{prop['new_value']}]: ").strip()
            
            if new_val == "<":
                if history:
                    state = history.pop()
                    props[state["idx"]]["new_value"] = state["old_value"]
                    props[state["idx"]]["modified"] = state["was_modified"]
                    print(f"  Desfeito: {props[state['idx']]['key']}")
                continue
            
            if new_val:
                history.append({
                    "idx": real_idx,
                    "old_value": prop["new_value"],
                    "was_modified": prop["modified"],
                })
                prop["new_value"] = new_val
                prop["modified"] = True
            else:
                print("  (mantido)")


def _parse_selection(text: str, max_val: int) -> list:
    result = set()
    text = text.strip().lower()
    
    if text == "all":
        return list(range(1, max_val + 1))
    
    for part in text.split(","):
        part = part.strip()
        if "-" in part:
            try:
                start, end = part.split("-", 1)
                start, end = int(start.strip()), int(end.strip())
                for i in range(start, end + 1):
                    if 1 <= i <= max_val:
                        result.add(i)
            except ValueError:
                continue
        else:
            try:
                val = int(part)
                if 1 <= val <= max_val:
                    result.add(val)
            except ValueError:
                continue
    
    return sorted(result)


def _save_and_copy(selected_file, props, server_label):
    if not any(p["modified"] for p in props):
        print("\n  Nenhuma alteracao feita.")
        return
    
    print(f"\n  Alteracoes em {selected_file.name}:")
    for p in props:
        if p["modified"]:
            print(f"    {p['key']}: {p['value']} -> {p['new_value']}")
    
    if not confirm("\n  Confirmar alteracoes?"):
        print("  Alteracoes canceladas.")
        return
    
    new_lines = []
    for p in props:
        if p["key"] is None:
            new_lines.append(p["original"])
        elif p["modified"]:
            new_lines.append(f"{p['key']} = {p['new_value']}")
        else:
            new_lines.append(p["original"])
    
    selected_file.write_text("\n".join(new_lines) + "\n", encoding='utf-8')
    print(f"\n  {selected_file.name} atualizado com sucesso!")
    
    containers = _find_running_containers(server_label)
    if containers:
        if confirm(f"  Copiar {selected_file.name} para container rodando?"):
            for container_name in containers:
                dest = f"{container_name}:{selected_file.parent}"
                result = subprocess.run(
                    ["docker", "cp", str(selected_file), dest],
                    capture_output=True, text=True
                )
                if result.returncode == 0:
                    print(f"    OK: {container_name}")
                else:
                    print(f"    ERRO: {container_name} - {result.stderr.strip()}")
    
    print("\n  Use //reload config no jogo para aplicar as mudancas.")


def _find_running_containers(server_label: str):
    """Encontra containers rodando baseado no label do servidor"""
    result = subprocess.run(
        ["docker", "ps", "--filter", "name=lineternity", "--format", "{{.Names}}"],
        capture_output=True, text=True
    )
    if not result.stdout.strip():
        return []
    
    containers = []
    for name in result.stdout.strip().splitlines():
        if "GameServer" in server_label:
            server_id = server_label.split("#")[1].strip()
            if f"gameserver-{server_id}" in name:
                containers.append(name)
        elif "LoginServer" in server_label:
            if "loginserver" in name:
                containers.append(name)
    
    return containers

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
# Create Base (Full Setup)
# ============================================================

def create_base():
    """Wizard de setup completo para máquina nova - MariaDB + LoginServer + GameServer #1"""
    print_header("Criar Base (Setup Completo)")
    
    if not check_distribution():
        return
    
    print("  Este wizard ira configurar:")
    print("    1. MariaDB (detectar ou usar embedded)")
    print("    2. LoginServer + MariaDB (iniciar juntos)")
    print("    3. GameServer #1 (configuracao padrao)")
    print()
    print("  Resultado: servidor pronto para teste do cliente!")
    print()
    
    if not confirm("  Iniciar setup completo?"):
        return
    
    # ============================================================
    # 1. Detectar/configurar MariaDB
    # ============================================================
    print("\n" + "=" * 60)
    print("  PASSO 1/4: Configurar MariaDB")
    print("=" * 60)
    
    detection = detect_or_configure_mariadb()
    
    use_external = detection["found"]
    
    if use_external:
        # MariaDB detectado
        mariadb_host = detection["host"]
        mariadb_port = detection["port"]
        mariadb_user = detection.get("user", "root")
        mariadb_password = detection.get("password", "root")
        mariadb_container = detection.get("container_name", "")
        
        print(f"\n  MariaDB detectado: {mariadb_host}:{mariadb_port}")
        print(f"  Usuario: {mariadb_user}")
        
        if not detection["database_exists"]:
            print(f"  Database l2jdb_login: sera criado pelo entrypoint")
        
        # Conectar container MariaDB à rede lineternity-network
        if mariadb_container:
            print(f"  Conectando MariaDB '{mariadb_container}' à rede lineternity-network...")
            ensure_lineternity_network()
            connect_container_to_network(mariadb_container)
            mariadb_host_for_containers = mariadb_container
        else:
            mariadb_host_for_containers = "host.docker.internal"
        
        update_login_env(mariadb_host, mariadb_port, mariadb_user, mariadb_password, external=True)
    else:
        # Usar MariaDB embedded (junto com o LoginServer)
        if detection.get("create_container"):
            mariadb_user = detection.get("user", "root")
            mariadb_password = detection.get("password", "root")
        else:
            print("\n  Nenhum MariaDB externo. Usando MariaDB embedded.")
            mariadb_user = "root"
            mariadb_password = "root"
        
        mariadb_host = "mariadb-login"
        mariadb_port = "3306"
        
        update_login_env(mariadb_host, mariadb_port, mariadb_user, mariadb_password, external=False)
    
    # ============================================================
    # 2. Configurar LoginServer
    # ============================================================
    print("\n" + "=" * 60)
    print("  PASSO 2/4: Configurar LoginServer")
    print("=" * 60)
    
    login_config = {
        "DB_HOST": mariadb_host,
        "DB_PORT": mariadb_port,
        "DB_USER": mariadb_user,
        "DB_PASSWORD": mariadb_password,
        "LOGIN_DB": "l2jdb_login",
        "HOSTNAME": "localhost",
        "LOGIN_PORT": "2106",
        "L2_EMAIL": "contato@jogatinando.com.br",
    }
    
    print(f"  DB_HOST: {mariadb_host}")
    print(f"  DB_PORT: {mariadb_port}")
    print(f"  DB_USER: {mariadb_user}")
    print(f"  DB_PASSWORD: ***")
    print(f"  HOSTNAME: localhost")
    print(f"  LOGIN_PORT: 2106")
    
    config_dir = DOCKER_DIR / "login" / "config"
    config_dir.mkdir(parents=True, exist_ok=True)
    create_login_properties(config_dir, login_config)
    
    print("  LoginServer configurado com sucesso!")
    
    # ============================================================
    # 3. Iniciar LoginServer (handle MariaDB via depends_on)
    # ============================================================
    print("\n" + "=" * 60)
    print("  PASSO 3/4: Iniciar LoginServer + MariaDB")
    print("=" * 60)
    
    print("  Iniciando LoginServer...")
    if not start_loginserver():
        print("  ERRO ao iniciar LoginServer!")
        return
    
    print("  LoginServer iniciado com sucesso!")
    print("  Aguardando LoginServer ficar pronto...")
    import time
    time.sleep(5)
    
    # ============================================================
    # 4. Criar GameServer #1
    # ============================================================
    print("\n" + "=" * 60)
    print("  PASSO 4/4: Criar e Iniciar GameServer #1")
    print("=" * 60)
    
    server_id = 1
    server_dir = GAMESERVERS_DIR / f"gameserver-{server_id}"
    
    # Verificar se já existe
    if server_dir.exists():
        print(f"  AVISO: GameServer #{server_id} ja existe em {server_dir}")
        if not confirm("  Sobrescrever?"):
            print("  Pulando criacao do GameServer.")
            game_config = None
        else:
            # Limpar diretório existente
            import shutil as shutil_module
            shutil_module.rmtree(server_dir)
            game_config = None
    
    if game_config is None or not server_dir.exists():
        # Usar host acessível de dentro dos containers
        login_db_host = mariadb_host_for_containers if use_external else "mariadb-login"
        login_db_port = mariadb_port if use_external else "3306"
        
        game_config = {
            "SERVER_ID": str(server_id),
            "SERVER_HOSTNAME": f"gameserver-{server_id}",
            "PUBLIC_PORT": str(7776 + server_id),
            "GAME_DB": f"l2jdb_gs{server_id}",
            # MariaDB local do GameServer
            "DB_HOST": f"mariadb-gs{server_id}",
            "DB_PORT": "3306",
            "DB_USER": mariadb_user,
            "DB_PASSWORD": mariadb_password,
            # MariaDB do Login (remoto)
            "LOGIN_DB_HOST": login_db_host,
            "LOGIN_DB_PORT": login_db_port,
            "LOGIN_DB_USER": mariadb_user,
            "LOGIN_DB_PASSWORD": mariadb_password,
            "LOGIN_DB": "l2jdb_login",
            # Identidade
            "LOGIN_HOSTNAME": "loginserver",
            "LOGIN_PORT": "9014",
        }
        
        print(f"  Server ID: {server_id}")
        print(f"  Hostname: gameserver-{server_id}")
        print(f"  Port: {7776 + server_id}")
        print(f"  Database: l2jdb_gs{server_id}")
        print(f"  LoginDB Host: {login_db_host}:{login_db_port}")
        
        # Criar estrutura de diretórios
        server_dir.mkdir(parents=True, exist_ok=True)
        config_dir = server_dir / "config"
        config_dir.mkdir(parents=True, exist_ok=True)
        
        # Copiar docker-compose template
        compose_template = GAMESERVERS_DIR / "template" / "docker-compose.yml"
        compose_file = server_dir / "docker-compose.yml"
        shutil.copy2(compose_template, compose_file)
        
        # Criar .env
        env_file = server_dir / ".env"
        env_content = "\n".join(f"{key}={value}" for key, value in game_config.items())
        env_file.write_text(env_content, encoding='utf-8')
        
        # Gerar properties
        create_game_properties(config_dir, game_config)
        
        print("  GameServer #1 configurado com sucesso!")
    
    # ============================================================
    # Iniciar GameServer #1
    # ============================================================
    print("\n" + "=" * 60)
    print("  Iniciando GameServer #1...")
    print("=" * 60)
    
    if game_config is not None:
        compose_file = server_dir / "docker-compose.yml"
        env_file = server_dir / ".env"
        
        if compose_file.exists():
            print("  Iniciando GameServer #1...")
            print("  Reconstruindo imagem...")
            run_compose(compose_file, "build", "--no-cache", env_file=env_file if env_file.exists() else None)
            if run_compose(compose_file, "up", "-d", env_file=env_file if env_file.exists() else None):
                print("  GameServer #1 iniciado com sucesso!")
            else:
                print("  ERRO ao iniciar GameServer #1!")
        else:
            print("  ERRO: docker-compose.yml nao encontrado!")
    
    # ============================================================
    # Resumo
    # ============================================================
    print("\n" + "=" * 60)
    print("  SETUP COMPLETO!")
    print("=" * 60)
    print()
    print("  Servidores configurados:")
    print(f"    LoginServer:  localhost:2106")
    print(f"    GameServer #1: localhost:7777")
    print()
    print("  Databases:")
    print(f"    Login:  l2jdb_login ({mariadb_host}:{mariadb_port})")
    print(f"    Game:   l2jdb_gs1 (mariadb-gs1:3306)")
    print()
    print("  Credenciais MariaDB:")
    print(f"    Usuario: {mariadb_user}")
    print(f"    Senha:   {mariadb_password}")
    print()
    print("  Proximos passos:")
    print("    1. Aguarde alguns segundos para os servidores iniciarem")
    print("    2. Conecte o cliente L2 em localhost:2106")
    print("    3. Crie uma conta no jogo")
    print()
    print("  Comandos uteis:")
    print("    - Listar servidores: opcao 8 no menu")
    print("    - Ver logs: opcao 9 no menu")
    print("    - Parar tudo: opcao 7 no menu")

# ============================================================
# GM Access Level Management
# ============================================================

def parse_access_levels(xml_path: Path) -> list[dict]:
    """Parse accessLevels.xml e retorna lista de niveis de acesso"""
    levels = []
    if not xml_path.exists():
        return levels
    try:
        tree = ET.parse(xml_path)
        root = tree.getroot()
        for access in root.findall('access'):
            level = int(access.get('level', 0))
            name = access.get('name', f'Level {level}')
            is_gm = access.get('isGM', 'false').lower() == 'true'
            levels.append({
                'level': level,
                'name': name,
                'is_gm': is_gm,
            })
    except Exception as e:
        print(f"  ERRO ao parsear accessLevels.xml: {e}")
    return levels

def get_running_gameserver_containers() -> list[dict]:
    """Lista containers GameServer ativos via docker ps"""
    result = subprocess.run(
        ["docker", "ps", "--filter", "name=lineternity-gameserver", "--format", "{{.Names}}\t{{.Status}}"],
        capture_output=True, text=True
    )
    containers = []
    if not result.stdout.strip():
        return containers
    for line in result.stdout.strip().splitlines():
        parts = line.split("\t")
        if len(parts) == 2:
            name = parts[0]
            status = parts[1]
            # Extrair server_id do nome (lineternity-gameserver-1 -> 1)
            try:
                server_id = int(name.split("-")[-1])
            except (ValueError, IndexError):
                continue
            containers.append({
                'name': name,
                'server_id': server_id,
                'status': status,
            })
    return containers

def get_mariadb_container_for_gameserver(server_id: int) -> str:
    """Retorna nome do container MariaDB de um GameServer"""
    return f"lineternity-mariadb-gs{server_id}"

def run_sql_on_mariadb(container_name: str, database: str, sql: str, fetch: bool = False) -> tuple[bool, str]:
    """Executa SQL em um container MariaDB via docker exec"""
    cmd = [
        "docker", "exec", container_name,
        "mysql", "-u", "root", "-proot", "--skip-ssl",
        database, "-N", "-e", sql
    ]
    result = subprocess.run(cmd, capture_output=True, text=True, timeout=15)
    if fetch:
        return result.returncode == 0, result.stdout.strip()
    return result.returncode == 0, result.stdout.strip()

# ============================================================
# SQL Migrations
# ============================================================

MIGRATIONS_DIR = PROJECT_ROOT / "tools" / "sql" / "migrations"

def _ensure_schema_migrations_table(container_name: str, database: str):
    """Cria a tabela schema_migrations se nao existir"""
    sql = """
    CREATE TABLE IF NOT EXISTS schema_migrations (
        id INT(10) NOT NULL AUTO_INCREMENT,
        filename VARCHAR(255) NOT NULL,
        applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        PRIMARY KEY (id),
        UNIQUE KEY uk_filename (filename)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
    """
    run_sql_on_mariadb(container_name, database, sql)

def _get_applied_migrations(container_name: str, database: str) -> set:
    """Retorna conjunto de migracoes ja aplicadas"""
    ok, output = run_sql_on_mariadb(
        container_name, database,
        "SELECT filename FROM schema_migrations ORDER BY id;",
        fetch=True
    )
    if not ok or not output:
        return set()
    return set(output.splitlines())

def _apply_migration(container_name: str, database: str, migration_file: Path):
    """Aplica um arquivo de migracao e registra na tabela schema_migrations"""
    sql_content = migration_file.read_text(encoding="utf-8")
    
    # Executar cada statement separadamente (para ALTER TABLE multiplas colunas)
    for statement in sql_content.split(";"):
        statement = statement.strip()
        if not statement or statement.startswith("--"):
            continue
        ok, err = run_sql_on_mariadb(container_name, database, statement)
        if not ok:
            print(f"    ERRO ao executar: {statement[:60]}...")
            print(f"    Detalhe: {err}")
            return False
    
    # Registrar migracao aplicada
    run_sql_on_mariadb(
        container_name, database,
        f"INSERT IGNORE INTO schema_migrations (filename) VALUES ('{migration_file.name}');"
    )
    return True

def apply_migrations():
    """Menu para aplicar migracoes SQL pendentes em GameServers"""
    print_header("Aplicar Migrations SQL")
    
    if not MIGRATIONS_DIR.exists():
        print(f"  Pasta de migracoes nao encontrada: {MIGRATIONS_DIR}")
        return
    
    migration_files = sorted(MIGRATIONS_DIR.glob("*.sql"))
    if not migration_files:
        print("  Nenhuma migracao encontrada em tools/sql/migrations/")
        return
    
    # Listar GameServers ativos
    containers = get_running_gameserver_containers()
    if not containers:
        print("  Nenhum GameServer ativo encontrado.")
        print("  Inicie um GameServer primeiro (opcao 4 do menu).")
        return
    
    options = [f"GameServer #{c['server_id']} ({c['name']}) - {c['status']}" for c in containers]
    options.append("Aplicar em TODOS")
    options.append("Voltar")
    
    idx = choose_from_menu("Selecione o GameServer", options)
    if idx == len(options) - 1:
        return
    
    if idx < 0 or idx >= len(options):
        return
    
    # Determinar alvos
    if idx == len(containers):
        targets = containers
    else:
        targets = [containers[idx]]
    
    total_applied = 0
    total_errors = 0
    
    for target in targets:
        server_id = target['server_id']
        container_name = target['name']
        game_db = f"l2jdb_gs{server_id}"
        mariadb_container = get_mariadb_container_for_gameserver(server_id)
        
        print(f"\n  GameServer #{server_id} ({game_db}):")
        
        # Verificar se MariaDB esta rodando
        check = subprocess.run(
            ["docker", "ps", "--filter", f"name={mariadb_container}", "--format", "{{.Names}}"],
            capture_output=True, text=True
        )
        if mariadb_container not in check.stdout:
            print(f"    MariaDB '{mariadb_container}' nao esta rodando. Pulando.")
            continue
        
        # Criar tabela schema_migrations se necessario
        _ensure_schema_migrations_table(mariadb_container, game_db)
        
        # Obter migracoes ja aplicadas
        applied = _get_applied_migrations(mariadb_container, game_db)
        
        # Filtrar pendentes
        pending = [f for f in migration_files if f.name not in applied]
        
        if not pending:
            print(f"    Nenhuma migracao pendente. Todas ja foram aplicadas.")
            continue
        
        print(f"    {len(pending)} migracao(oes) pendente(s):")
        for f in pending:
            print(f"      - {f.name}")
        
        if not confirm(f"    Aplicar migracoes no GameServer #{server_id}?"):
            continue
        
        for migration_file in pending:
            print(f"    Aplicando {migration_file.name}...", end=" ")
            if _apply_migration(mariadb_container, game_db, migration_file):
                print("OK")
                total_applied += 1
            else:
                print("FALHOU")
                total_errors += 1
    
    print(f"\n  Resultado: {total_applied} aplicada(s), {total_errors} erro(s)")

def set_gm_access():
    """Funcao para setar nivel de acesso de um personagem"""
    print_header("Setar GM / Access Level")

    # 1. Listar GameServers ativos
    containers = get_running_gameserver_containers()
    if not containers:
        print("  Nenhum GameServer ativo encontrado.")
        print("  Inicie um GameServer primeiro (opcao 4 do menu).")
        return

    options = [f"GameServer #{c['server_id']} ({c['name']}) - {c['status']}" for c in containers]
    options.append("Voltar")

    idx = choose_from_menu("Selecione o GameServer", options)
    if idx == len(containers):
        return
    if idx < 0 or idx >= len(containers):
        return

    selected = containers[idx]
    mariadb_container = get_mariadb_container_for_gameserver(selected['server_id'])
    game_db = f"l2jdb_gs{selected['server_id']}"

    # Verificar se MariaDB esta rodando
    check = subprocess.run(
        ["docker", "ps", "--filter", f"name={mariadb_container}", "--format", "{{.Names}}"],
        capture_output=True, text=True
    )
    if mariadb_container not in check.stdout:
        print(f"  ERRO: Container MariaDB '{mariadb_container}' nao esta rodando.")
        return

    # 2. Pedir conta
    print()
    account_name = input("  Conta do jogador: ").strip()
    if not account_name:
        print("  Conta invalida.")
        return

    # 3. Pedir personagem
    char_name = input("  Nome do personagem: ").strip()
    if not char_name:
        print("  Nome invalido.")
        return

    # 4. Verificar se personagem existe
    check_sql = f"SELECT obj_Id, accesslevel FROM characters WHERE char_name='{char_name}' AND account_name='{account_name}';"
    ok, result = run_sql_on_mariadb(mariadb_container, game_db, check_sql, fetch=True)

    if not ok:
        print(f"  ERRO ao consultar banco de dados.")
        return

    if not result:
        print(f"  Personagem '{char_name}' na conta '{account_name}' nao encontrado.")
        print(f"  Verifique se o nome esta correto (sensivel a maiusculas/minusculas).")
        return

    parts = result.split()
    char_obj_id = parts[0]
    current_level = int(parts[1]) if len(parts) > 1 else 0

    print(f"\n  Personagem encontrado: {char_name} (ID: {char_obj_id})")
    print(f"  Access Level atual: {current_level}")

    # 5. Parsear accessLevels.xml
    xml_path = PROJECT_ROOT / "game" / "data" / "xml" / "accessLevels.xml"
    levels = parse_access_levels(xml_path)

    if not levels:
        print(f"  AVISO: accessLevels.xml nao encontrado em {xml_path}")
        print(f"  Usando lista padrao de niveis.")
        levels = [
            {'level': -1, 'name': 'Banned', 'is_gm': False},
            {'level': 0, 'name': 'User', 'is_gm': False},
            {'level': 1, 'name': 'Chat Moderator', 'is_gm': False},
            {'level': 2, 'name': 'Test GM', 'is_gm': False},
            {'level': 3, 'name': 'General GM', 'is_gm': False},
            {'level': 4, 'name': 'Support GM', 'is_gm': False},
            {'level': 5, 'name': 'Event GM', 'is_gm': False},
            {'level': 6, 'name': 'Head GM', 'is_gm': False},
            {'level': 7, 'name': 'Admin', 'is_gm': True},
            {'level': 8, 'name': 'Master', 'is_gm': True},
        ]

    # 6. Mostrar niveis e permitir selecao
    print()
    print("  Niveis de Acesso:")
    print()

    # Ordenar por level
    levels_sorted = sorted(levels, key=lambda x: x['level'])

    for lv in levels_sorted:
        marker = " ◄─ recomendado para GM" if lv['level'] == 8 else ""
        gm_marker = " [GM]" if lv['is_gm'] else ""
        print(f"  [{lv['level']:>2}] {lv['name']}{gm_marker}{marker}")

    print(f"  [ V] Voltar")
    print()

    level_input = input("  Selecione o nivel (numero): ").strip()

    if level_input.lower() in ('v', 'voltar', ''):
        return

    try:
        new_level = int(level_input)
    except ValueError:
        print(f"  Valor invalido: '{level_input}'. Use um numero.")
        return

    # Verificar se o nivel existe
    valid_levels = [lv['level'] for lv in levels]
    if new_level not in valid_levels:
        print(f"  Nivel {new_level} invalido. Valores validos: {valid_levels}")
        return

    # Encontrar nome do nivel
    level_name = next((lv['name'] for lv in levels if lv['level'] == new_level), f'Level {new_level}')

    # 7. Confirmar
    print()
    print(f"  Conta:         {account_name}")
    print(f"  Personagem:    {char_name}")
    print(f"  Nivel:         {new_level} - {level_name}")
    print(f"  Level atual:   {current_level}")
    print()

    if not confirm("  Confirmar alteracao?"):
        print("  Operacao cancelada.")
        return

    # 8. Executar UPDATE
    update_sql = f"UPDATE characters SET accesslevel = {new_level} WHERE char_name='{char_name}' AND account_name='{account_name}';"
    ok, _ = run_sql_on_mariadb(mariadb_container, game_db, update_sql)

    if ok:
        # Verificar se realmente alterou
        verify_sql = f"SELECT accesslevel FROM characters WHERE char_name='{char_name}' AND account_name='{account_name}';"
        ok2, new_val = run_sql_on_mariadb(mariadb_container, game_db, verify_sql, fetch=True)
        if ok2 and new_val.strip() == str(new_level):
            print(f"\n  ✅ Access level alterado com sucesso!")
            print(f"     {char_name}: {current_level} → {new_level} ({level_name})")
        else:
            print(f"\n  ⚠️ Comando executado, mas verificacao retornou valor inesperado: '{new_val}'")
    else:
        print(f"\n  ❌ ERRO ao executar UPDATE no banco de dados.")

# ============================================================
# Main Menu
# ============================================================

def main_menu():
    while True:
        options = [
            f"{C.GREEN}1.{C.RESET} Compilar Projeto (Build)",
            f"{C.GREEN}2.{C.RESET} Criar Base (Setup Completo)",
            f"{C.GREEN}3.{C.RESET} Iniciar LoginServer",
            f"{C.GREEN}4.{C.RESET} Iniciar GameServer",
            f"{C.RED}5.{C.RESET} Parar GameServer",
            f"{C.RED}6.{C.RESET} Parar Todos os Servicos",
            f"{C.CYAN}7.{C.RESET} Listar servidores ativos",
            f"{C.CYAN}8.{C.RESET} Logs",
            f"{C.YELLOW}9.{C.RESET} Editar Config por Servidor",
            f"{C.YELLOW}10.{C.RESET} Gerenciar perfis de configuracao",
            f"{C.MAGENTA}11.{C.RESET} Setar GM / Access Level",
            f"{C.BLUE}12.{C.RESET} Atualizar Imagens",
            f"{C.BLUE}13.{C.RESET} Atualizar Dados nos Containers",
            f"{C.BLUE}14.{C.RESET} Aplicar Migrations SQL",
            f"{C.RED}15.{C.RESET} Sair",
        ]
        
        idx = choose_from_menu(f"{C.BOLD}Lineternity Stack Manager v2.4{C.RESET}", options)
        
        if idx == 0:
            build_project()
        elif idx == 1:
            create_base()
        elif idx == 2:
            start_loginserver()
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
            bulk_edit_config()
        elif idx == 9:
            manage_config_profiles()
        elif idx == 10:
            set_gm_access()
        elif idx == 11:
            update_images()
        elif idx == 12:
            update_container_data()
        elif idx == 13:
            apply_migrations()
        elif idx == 14:
            print(f"\n  {C.GREEN}Saindo...{C.RESET}")
            break
        else:
            continue
        
        input(f"\n  {C.DIM}Pressione Enter para continuar...{C.RESET}")

# ============================================================
# Entry Point
# ============================================================

if __name__ == "__main__":
    try:
        main_menu()
    except KeyboardInterrupt:
        print("\n\n  Saindo...")
        sys.exit(0)
