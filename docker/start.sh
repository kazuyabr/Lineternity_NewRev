#!/bin/bash
# ============================================================
# Lineternity Stack Manager
# Wrapper para stack.py
# ============================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
STACK_SCRIPT="$SCRIPT_DIR/stack.py"

# Verificar se Python está instalado
if ! command -v python3 &> /dev/null; then
    echo "ERRO: Python3 não encontrado"
    echo "Instale Python3: https://www.python.org/downloads/"
    exit 1
fi

# Verificar se stack.py existe
if [ ! -f "$STACK_SCRIPT" ]; then
    echo "ERRO: stack.py não encontrado em $SCRIPT_DIR"
    exit 1
fi

# Executar stack.py
exec python3 "$STACK_SCRIPT" "$@"
