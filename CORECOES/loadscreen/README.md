<p align="center">
  <img src="ARQUIVOS_MOD/images/brproject-logo-readme.png" alt="BrProject" width="160" />
</p>

<h1 align="center">BrProject — Load Screen</h1>

<p align="center">
  Splash moderno · Start silencioso · Encerramento limpo<br/>
  <em>Desenvolvido por Dev ⩽ A.L.N/⩾ para o BrProject</em>
</p>

<p align="center">
  <img alt="Java 25" src="https://img.shields.io/badge/Java-25-orange?style=for-the-badge&logo=openjdk&logoColor=white" />
  <img alt="Swing" src="https://img.shields.io/badge/Interface-Java%20Swing-blue?style=for-the-badge&logo=java&logoColor=white" />
  <img alt="Graphics2D" src="https://img.shields.io/badge/Render-Graphics2D-8A2BE2?style=for-the-badge&logo=adobephotoshop&logoColor=white" />
  <img alt="C#" src="https://img.shields.io/badge/Start.exe-C%23%20%2F%20.NET-512BD4?style=for-the-badge&logo=csharp&logoColor=white" />
  <img alt="Windows" src="https://img.shields.io/badge/Plataforma-Windows-0078D6?style=for-the-badge&logo=windows&logoColor=white" />
  <img alt="Autor" src="https://img.shields.io/badge/Autor-Dev%20A.L.N-a855f7?style=for-the-badge&logo=github&logoColor=white" />
</p>

---

## Sobre

Pack de integração para a source **BrProject** (`ext.mods.security`) que troca o bootstrap com console por um load screen profissional e um `Start.exe` silencioso no Windows.

| Módulo | Função |
|:-------|:-------|
| **Load Screen** | Splash animado com marca, barra de progresso e assinatura |
| **Start silencioso** | `Start.exe` sobe com `javaw` — sem janela preta de CMD |
| **Encerramento limpo** | “Fechar tudo” para Login/Game e a JVM do launcher |

---

## Stack técnica

### Interface e load screen

| Camada | Tecnologia | Função |
|:-------|:-----------|:-------|
| Runtime | Java 25 (OpenJDK) | Processo do launcher / splash |
| Interface | Java Swing (`JWindow`, `JPanel`, `JFrame`) | Janelas |
| Renderização | `Graphics2D` | Logo, título neon, barra, assinatura |
| Efeitos | `LinearGradientPaint`, `RadialGradientPaint`, `AlphaComposite` | Glow / shimmer |
| Animação | `javax.swing.Timer` (~16 ms) | Órbitas e progresso |

### Bootstrap e processos

| Camada | Tecnologia | Função |
|:-------|:-----------|:-------|
| Processo | `javaw.exe` | Início sem console |
| Wrapper nativo | C# / .NET Framework (`csc /target:winexe`) | Gera `Start.exe` a partir de `StartSilent.cs` |
| Encerramento | `ProcessHandle` + `taskkill /F /T` | Encerra só `java` / `javaw` do BrProject |

---

## Recursos

**Load Screen (`BootSplash`)**
- Moldura escura com borda pulsante
- Título **BR PROJECT** + logo da engrenagem
- Assinatura **Dev ⩽ A.L.N/⩾** (nome em destaque)
- Animação em órbita e progresso `0→100%`
- ~2,8 s de load + pausa em 100% antes de abrir o painel

**Interface (`MainFrame`)**
- Shell Swing customizado (`ModernUI`, `CardLayout`)
- Dashboard Login / Game Server
- Diálogo de saída: Fechar tudo · Só esconder · Cancelar
- Sons opcionais de start / shutdown (WAV)

---

## Estrutura

```text
loadscreen/
├── README.md
├── README_PREVIEW.html
├── abrir-readme-preview.bat
├── LEIAME.txt
├── DIFFS/
│   ├── 01-LicenseInit.diff
│   ├── 02-MainFrame-fechar-tudo.diff
│   └── 03-JavaProcessInspector.diff
└── ARQUIVOS_MOD/
    ├── java/ext/mods/...
    ├── tools/StartSilent.cs
    └── images/
```

---

## Instalação

### 1. Copiar fontes

```bash
cp ARQUIVOS_MOD/java/ext/mods/security/gui/BootSplash.java   <source>/java/ext/mods/security/gui/
cp ARQUIVOS_MOD/java/ext/mods/security/LicenseInit.java      <source>/java/ext/mods/security/
cp ARQUIVOS_MOD/java/ext/mods/commons/util/JavaProcessInspector.java \
                                                         <source>/java/ext/mods/commons/util/
```

Aplique as alterações do `MainFrame` em `DIFFS/02-MainFrame-fechar-tudo.diff` ou `MainFrame_PATCH.txt`.

Copie as imagens para a pasta de runtime:

```text
Brproject_Distribution/images/
```

### 2. Compilar Java

```bash
gradlew.bat jar
copy libs\server.jar Brproject_Distribution\libs\server.jar
```

### 3. Gerar Start.exe silencioso

```bat
ARQUIVOS_MOD\tools\compilar-StartSilent.bat
```

### 4. Validar

1. Abra `Start.exe` → splash → painel (sem CMD)
2. Escolha **Fechar tudo** → nenhum OpenJDK residual

Guia completo: [`LEIAME.txt`](LEIAME.txt)

---

## Arquivos principais

| Arquivo | Função |
|:--------|:-------|
| [`BootSplash.java`](ARQUIVOS_MOD/java/ext/mods/security/gui/BootSplash.java) | Load screen |
| [`LicenseInit.java`](ARQUIVOS_MOD/java/ext/mods/security/LicenseInit.java) | Entrada: splash → MainFrame |
| [`MainFrame_PATCH.txt`](ARQUIVOS_MOD/java/ext/mods/security/gui/MainFrame_PATCH.txt) | Diálogo de saída |
| [`JavaProcessInspector.java`](ARQUIVOS_MOD/java/ext/mods/commons/util/JavaProcessInspector.java) | Limpeza de processos |
| [`StartSilent.cs`](ARQUIVOS_MOD/tools/StartSilent.cs) | Start.exe silencioso |

---

## Descrição para YouTube

<details>
<summary>Bloco para copiar e colar</summary>

```text
BrProject — Load Screen + Start silencioso

Desenvolvido por Dev ⩽ A.L.N/⩾ para o BrProject

• Splash com logo, neon e barra 0→100%
• Assinatura Dev ⩽ A.L.N/⩾
• Start.exe sem janela CMD
• Fechar tudo encerra Login/Game + JVM

Stack: Java 25 · Swing · Graphics2D · javaw · C#/.NET · ProcessHandle

#BrProject #L2 #Java #Swing #LoadScreen #Launcher #DevALN
```

</details>

---

## Requisitos

- Windows
- JDK 25 (`javaw.exe`)
- Source BrProject com `ext.mods.security.LicenseInit`
- .NET Framework (`csc.exe`) apenas se for regenerar o `Start.exe`

---

## Autor

**Dev ⩽ A.L.N/⩾**

Desenvolvido por Dev ⩽ A.L.N/⩾ para o **BrProject**.  
Todos os direitos reservados.
