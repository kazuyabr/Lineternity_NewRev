#!/bin/sh
# ============================================================
# Lineternity Entry Point
# Suporta: login, gameserver
# Cada container é independente com seu próprio MariaDB
# ============================================================

set +e

# --- Configurações do Banco de Dados ---
echo "=========================================="
echo "  Configurando banco de dados"
echo "=========================================="

# MariaDB LOCAL (para este container)
DB_HOST_VAL="${DB_HOST:-mariadb}"
DB_PORT_VAL="${DB_PORT:-3306}"
DB_USER_VAL="${DB_USER:-root}"
DB_PASSWORD_VAL="${DB_PASSWORD:-root}"
LOGIN_DB="${LOGIN_DB:-l2jdb_login}"
GAME_DB="${GAME_DB:-l2jdb_gs1}"

# MariaDB do LOGIN (remoto - para registro do gameserver)
LOGIN_DB_HOST="${LOGIN_DB_HOST:-mariadb-login}"
LOGIN_DB_PORT="${LOGIN_DB_PORT:-3306}"
LOGIN_DB_USER="${LOGIN_DB_USER:-root}"
LOGIN_DB_PASSWORD="${LOGIN_DB_PASSWORD:-root}"

echo "DB Host Local: $DB_HOST_VAL:$DB_PORT_VAL"
echo "DB Host Login: $LOGIN_DB_HOST:$LOGIN_DB_PORT"
echo "Login DB: $LOGIN_DB"
echo "Game DB: $GAME_DB"
echo ""

# --- Função para aguardar MySQL ---
wait_for_mysql() {
    local host=$1
    local port=$2
    local user=$3
    local pass=$4
    local label=$5
    
    echo "Aguardando MySQL ($label) estar pronto..."
    local retries=30
    while ! mysql -h "$host" -P "$port" -u "$user" -p"$pass" --skip-ssl -e "SELECT 1" &>/dev/null; do
        retries=$((retries - 1))
        if [ $retries -le 0 ]; do
            echo "ERRO: MySQL ($label) não ficou pronto após 30 tentativas"
            exit 1
        fi
        echo "  Tentativa $((30 - retries))/30..."
        sleep 2
    done
    echo "MySQL ($label) pronto!"
    echo ""
}

# --- Função para criar database ---
create_database() {
    local host=$1
    local port=$2
    local user=$3
    local pass=$4
    local db_name=$5
    local label=$6
    
    echo "=========================================="
    echo "  Criando Database: $db_name ($label)"
    echo "=========================================="
    mysql -h "$host" -P "$port" -u "$user" -p"$pass" --skip-ssl -e \
        "CREATE DATABASE IF NOT EXISTS \`$db_name\` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
    echo "  Database '$db_name' criado com sucesso!"
    echo ""
}

# --- Função para importar SQL ---
import_sql() {
    local host=$1
    local port=$2
    local user=$3
    local pass=$4
    local db_name=$5
    local sql_file=$6
    
    if [ ! -f "$sql_file" ]; then
        echo "  AVISO: Arquivo SQL não encontrado: $sql_file"
        return 1
    fi
    
    echo "  Importando: $sql_file -> $db_name"
    mysql -h "$host" -P "$port" -u "$user" -p"$pass" --skip-ssl "$db_name" < "$sql_file"
    echo "  Importação concluída!"
}

# --- Função para verificar se database tem tabelas ---
database_has_tables() {
    local host=$1
    local port=$2
    local user=$3
    local pass=$4
    local db_name=$5
    
    local table_count=$(mysql -h "$host" -P "$port" -u "$user" -p"$pass" --skip-ssl "$db_name" -N -e \
        "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$db_name';" 2>/dev/null)
    
    if [ "$table_count" = "0" ] 2>/dev/null; then
        return 1  # vazio
    else
        return 0  # tem tabelas
    fi
}

# --- Função para registrar gameserver no login ---
register_gameserver() {
    local server_id=$1
    local hostname=$2
    local hexid_file="game/config/hexid.txt"
    
    echo "=========================================="
    echo "  Registrando GameServer #$server_id"
    echo "=========================================="
    
    # Verificar se hexid.txt já existe e é válido
    if [ -f "$hexid_file" ]; then
        local existing_hexid=$(grep -o "HexID=.*" "$hexid_file" 2>/dev/null | cut -d'=' -f2)
        local existing_server_id=$(grep -o "ServerID=.*" "$hexid_file" 2>/dev/null | cut -d'=' -f2)
        if [ -n "$existing_hexid" ] && [ ${#existing_hexid} -ge 32 ] && [ -n "$existing_server_id" ] && [ "$existing_server_id" = "$server_id" ]; then
            echo "HexID já existe: $existing_hexid"
            echo "Verificando registro no banco do Login..."
            
            # Verificar se já está registrado no MariaDB do LOGIN (remoto)
            local db_hexid=$(mysql -h "$LOGIN_DB_HOST" -P "$LOGIN_DB_PORT" -u "$LOGIN_DB_USER" -p"$LOGIN_DB_PASSWORD" --skip-ssl "$LOGIN_DB" -N -e \
                "SELECT hexid FROM gameservers WHERE server_id = $server_id;" 2>/dev/null)
            
            if [ -n "$db_hexid" ] && [ "$db_hexid" = "$existing_hexid" ]; then
                echo "GameServer #$server_id já está registrado corretamente!"
                return 0
            else
                echo "HexID não corresponde ao registro no banco. Atualizando..."
                mysql -h "$LOGIN_DB_HOST" -P "$LOGIN_DB_PORT" -u "$LOGIN_DB_USER" -p"$LOGIN_DB_PASSWORD" --skip-ssl "$LOGIN_DB" -e \
                    "INSERT INTO gameservers (server_id, hexid, host) VALUES ($server_id, '$existing_hexid', '$hostname') 
                     ON DUPLICATE KEY UPDATE hexid='$existing_hexid', host='$hostname';"
                echo "Registro atualizado no banco!"
                return 0
            fi
        fi
    fi
    
    # Gerar novo hexid
    echo "Gerando novo HexID..."
    local hexid=$(openssl rand -hex 16 | tr '[:lower:]' '[:upper:]')
    echo "HexID gerado: $hexid"
    
    # Inserir no banco de dados do LOGIN (remoto)
    echo "Inserindo no banco do Login ($LOGIN_DB_HOST)..."
    mysql -h "$LOGIN_DB_HOST" -P "$LOGIN_DB_PORT" -u "$LOGIN_DB_USER" -p"$LOGIN_DB_PASSWORD" --skip-ssl "$LOGIN_DB" -e \
        "INSERT INTO gameservers (server_id, hexid, host) VALUES ($server_id, '$hexid', '$hostname') 
         ON DUPLICATE KEY UPDATE hexid='$hexid', host='$hostname';"
    echo "Registro inserido com sucesso!"
    
    # Criar diretório se não existir
    mkdir -p "$(dirname "$hexid_file")"
    
    # Salvar hexid.txt
    cat > "$hexid_file" << EOF
#HexID Auto-generated by Lineternity
ServerID=$server_id
HexID=$hexid
EOF
    
    echo "HexID salvo em: $hexid_file"
    echo ""
}

# --- Função para aplicar custom.properties ---
apply_custom_overrides() {
    local config_dir=$1
    
    local custom_file="$config_dir/custom.properties"
    if [ ! -f "$custom_file" ]; then
        return 0
    fi
    
    echo "=========================================="
    echo "  Aplicando custom.properties"
    echo "=========================================="
    
    while IFS='=' read -r key value; do
        # Pular comentários e linhas vazias
        [[ "$key" =~ ^#.*$ || -z "$key" ]] && continue
        
        # Remover espaços
        key=$(echo "$key" | xargs)
        value=$(echo "$value" | xargs)
        
        echo "  Override: $key = $value"
        
        # Aplicar em todos os arquivos .properties do diretório
        for props_file in "$config_dir"/*.properties; do
            if [ -f "$props_file" ]; then
                # Usar | como delimitador para evitar problemas com / no valor
                sed -i "s|^${key} = .*|${key} = ${value}|" "$props_file" 2>/dev/null
                sed -i "s|^${key}=.*|${key}=${value}|" "$props_file" 2>/dev/null
            fi
        done
    done < "$custom_file"
    
    echo "  Custom overrides aplicados!"
    echo ""
}

# --- Função para configurar properties com env vars atuais ---
configure_properties() {
    local mode=$1
    
    echo "=========================================="
    echo "  Configurando properties"
    echo "=========================================="
    
    # LoginServer properties
    if [ "$mode" = "login" ] || [ "$mode" = "both" ]; then
        local props="login/config/loginserver.properties"
        if [ -f "$props" ]; then
            echo "  Atualizando $props"
            sed -i "s|^sql\.url = jdbc:mariadb://.*|sql.url = jdbc:mariadb://${DB_HOST_VAL}:${DB_PORT_VAL}/${LOGIN_DB}?useUnicode=true\&characterEncoding=UTF-8|" "$props"
            sed -i "s|^sql\.login = .*|sql.login = ${DB_USER_VAL}|" "$props"
            sed -i "s|^sql\.password = .*|sql.password = ${DB_PASSWORD_VAL}|" "$props"
            echo "    sql.url = jdbc:mariadb://${DB_HOST_VAL}:${DB_PORT_VAL}/${LOGIN_DB}"
            echo "    sql.login = ${DB_USER_VAL}"
        else
            echo "  AVISO: $props nao encontrado"
        fi
    fi
    
    # GameServer properties
    if [ "$mode" = "gameserver" ] || [ "$mode" = "both" ]; then
        local props="game/config/server.properties"
        if [ -f "$props" ]; then
            echo "  Atualizando $props"
            sed -i "s|^sql\.url = jdbc:mariadb://.*|sql.url = jdbc:mariadb://${DB_HOST_VAL}:${DB_PORT_VAL}/${GAME_DB}?useUnicode=true\&characterEncoding=UTF-8|" "$props"
            sed -i "s|^sql\.login = .*|sql.login = ${DB_USER_VAL}|" "$props"
            sed -i "s|^sql\.password = .*|sql.password = ${DB_PASSWORD_VAL}|" "$props"
            echo "    sql.url = jdbc:mariadb://${DB_HOST_VAL}:${DB_PORT_VAL}/${GAME_DB}"
            echo "    sql.login = ${DB_USER_VAL}"
        else
            echo "  AVISO: $props nao encontrado"
        fi
    fi
    
    echo ""
}

# --- Configurações de variáveis ---
L2_EMAIL=${L2_EMAIL:-"contato@jogatinando.com.br"}
PASSWORD=${PASSWORD:-"12345678"}
KEY=$(cat /proc/sys/kernel/random/uuid 2>/dev/null | tr -d '-' | head -c 32 || openssl rand -hex 16)
SERVER_ID=${SERVER_ID:-1}
SERVER_HOSTNAME=${SERVER_HOSTNAME:-"gameserver-${SERVER_ID}"}

# --- Classpath (baseado no Brproject_Distribution) ---
BASE_DIR=$(cd "$(dirname "$0")/.." && pwd)
LIBS_DIR="$BASE_DIR/libs"
CLASSPATH="$LIBS_DIR/server.jar"
for jar in $(ls "$LIBS_DIR"/*.jar 2>/dev/null | sort); do
  base=$(basename "$jar")
  case "$base" in
    server.jar|*.encrypted|kotlin-stdlib-2.0.0.jar|kotlin-reflect-2.0.0.jar|kotlinx-coroutines-core-jvm-1.8.1.jar) ;;
    *) CLASSPATH="$CLASSPATH:$jar" ;;
  esac
done
echo "Classpath: $(echo $CLASSPATH | tr ':' '\n' | wc -l) jars"

# --- Flags JVM ---
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | sed 's/^1\.//' | cut -d'.' -f1)
BASE_JVM_FLAGS="-XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:G1HeapRegionSize=16m -XX:+G1UseAdaptiveIHOP -XX:G1ReservePercent=20 -XX:InitiatingHeapOccupancyPercent=45 -XX:+UseStringDeduplication -XX:+UseCompressedOops -XX:+UseCompressedClassPointers -XX:+TieredCompilation -XX:TieredStopAtLevel=4 -XX:G1PeriodicGCInterval=30000 -XX:+G1PeriodicGCInvokesConcurrent -XX:MinHeapFreeRatio=10 -XX:MaxHeapFreeRatio=30"

if [ "$JAVA_VERSION" -ge 25 ] 2>/dev/null; then
  BASE_JVM_FLAGS="$BASE_JVM_FLAGS -XX:+UseCompactObjectHeaders"
fi

LOGIN_JAVA_OPTS="$BASE_JVM_FLAGS"
GAME_JAVA_OPTS="-Xms1g -Xmx2g -Djava.awt.headless=true $BASE_JVM_FLAGS"

# ============================================================
# Iniciar Servidor
# ============================================================
START_TYPE=${1:-both}

if [ "$START_TYPE" = "login" ]; then
    echo "=== Iniciando LoginServer ==="
    
    # Aguardar MariaDB LOCAL estar pronto
    wait_for_mysql "$DB_HOST_VAL" "$DB_PORT_VAL" "$DB_USER_VAL" "$DB_PASSWORD_VAL" "local"
    
    # Criar database de Login se não existe
    create_database "$DB_HOST_VAL" "$DB_PORT_VAL" "$DB_USER_VAL" "$DB_PASSWORD_VAL" "$LOGIN_DB" "login"
    
    # Importar schema se database estiver vazio
    if ! database_has_tables "$DB_HOST_VAL" "$DB_PORT_VAL" "$DB_USER_VAL" "$DB_PASSWORD_VAL" "$LOGIN_DB"; then
        echo "Database vazio, importando schema..."
        import_sql "$DB_HOST_VAL" "$DB_PORT_VAL" "$DB_USER_VAL" "$DB_PASSWORD_VAL" "$LOGIN_DB" "$(dirname "$0")/sql/login.sql"
    else
        echo "Database já contém tabelas, pulando importação."
    fi
    
    # Aplicar custom.properties se existir
    apply_custom_overrides "login/config"
    
    # Configurar properties com env vars atuais
    configure_properties "login"
    
    cd login || exit 1
    exec java $LOGIN_JAVA_OPTS -cp "$CLASSPATH" ext.mods.loginserver.LoginServer

elif [ "$START_TYPE" = "gameserver" ]; then
    SERVER_NUM=${2:-$SERVER_ID}
    echo "=== Iniciando GameServer #$SERVER_NUM ==="
    
    # Aguardar MariaDB LOCAL estar pronto
    wait_for_mysql "$DB_HOST_VAL" "$DB_PORT_VAL" "$DB_USER_VAL" "$DB_PASSWORD_VAL" "local"
    
    # Criar game database se não existe
    create_database "$DB_HOST_VAL" "$DB_PORT_VAL" "$DB_USER_VAL" "$DB_PASSWORD_VAL" "$GAME_DB" "game"
    
    # Importar schema se database estiver vazio
    if ! database_has_tables "$DB_HOST_VAL" "$DB_PORT_VAL" "$DB_USER_VAL" "$DB_PASSWORD_VAL" "$GAME_DB"; then
        echo "Database vazio, importando schema..."
        import_sql "$DB_HOST_VAL" "$DB_PORT_VAL" "$DB_USER_VAL" "$DB_PASSWORD_VAL" "$GAME_DB" "$(dirname "$0")/sql/gameserver.sql"
    else
        echo "Database já contém tabelas, pulando importação."
    fi
    
    # Aguardar MariaDB do LOGIN estar pronto (remoto)
    wait_for_mysql "$LOGIN_DB_HOST" "$LOGIN_DB_PORT" "$LOGIN_DB_USER" "$LOGIN_DB_PASSWORD" "login (remoto)"
    
    # Registrar gameserver no banco do LOGIN (remoto)
    register_gameserver "$SERVER_NUM" "$SERVER_HOSTNAME"
    
    # Aplicar custom.properties se existir
    apply_custom_overrides "game/config"
    
    # Configurar properties com env vars atuais
    configure_properties "gameserver"
    
    cd game || exit 1
    echo "DEBUG: CLASSPATH jars = $(echo $CLASSPATH | tr ':' '\n' | wc -l)"
    exec java $GAME_JAVA_OPTS -cp "$CLASSPATH" ext.mods.gameserver.GameServer "$KEY" "$L2_EMAIL"

else
    echo "Modo '$START_TYPE' não suportado. Use: login ou gameserver"
    exit 1
fi
