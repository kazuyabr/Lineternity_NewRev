#!/bin/bash
# ============================================================
# Script de inicialização do banco de dados
# Cria APENAS database de Login
# Game databases são criadas por cada gameserver na inicialização
# ============================================================

set -e

# Configurações do banco
DB_HOST="${DB_HOST:-mariadb}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USER:-root}"
DB_PASS="${DB_PASSWORD:-root}"

# Nome do database de Login
LOGIN_DB="${LOGIN_DB:-l2jdb_login}"

echo "=========================================="
echo "  Inicializando banco de dados de Login"
echo "=========================================="
echo "Host: $DB_HOST:$DB_PORT"
echo "User: $DB_USER"
echo "Login DB: $LOGIN_DB"
echo ""

# Função para aguardar MySQL estar pronto
wait_for_mysql() {
    echo "Aguardando MySQL estar pronto..."
    local retries=30
    while ! mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" --skip-ssl -e "SELECT 1" &>/dev/null; do
        retries=$((retries - 1))
        if [ $retries -le 0 ]; then
            echo "ERRO: MySQL não ficou pronto após 30 tentativas"
            exit 1
        fi
        echo "  Tentativa $((30 - retries))/30..."
        sleep 2
    done
    echo "MySQL pronto!"
    echo ""
}

# Função para criar database
create_database() {
    local db_name=$1
    echo "Criando database: $db_name"
    mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" --skip-ssl -e \
        "CREATE DATABASE IF NOT EXISTS \`$db_name\` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
    echo "  Database '$db_name' criado com sucesso!"
}

# Função para importar SQL
import_sql() {
    local db_name=$1
    local sql_file=$2
    
    if [ ! -f "$sql_file" ]; then
        echo "  AVISO: Arquivo SQL não encontrado: $sql_file"
        return 1
    fi
    
    echo "  Importando: $sql_file -> $db_name"
    mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" --skip-ssl "$db_name" < "$sql_file"
    echo "  Importação concluída!"
}

# ============================================================
# Execução principal
# ============================================================

wait_for_mysql

# Criar APENAS database de Login
echo "=== Database de Login ==="
create_database "$LOGIN_DB"
import_sql "$LOGIN_DB" "$(dirname "$0")/sql/login.sql"

echo ""
echo "=========================================="
echo "  Inicialização concluída!"
echo "=========================================="
echo ""
echo "Database criado:"
echo "  - $LOGIN_DB (LoginServer)"
echo ""
echo "Game databases são criados automaticamente"
echo "por cada gameserver na inicialização."
