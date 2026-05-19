# 🎱 BingoPatataPlugin

Plugin de Minecraft para jugar al **Bingo** en eventos **UHC**. Los equipos compiten por recolectar 25 ítems específicos distribuidos en una carta 5×5, ganando puntos por ítems conseguidos, líneas completadas y bingos completos.

---

## Características

- **Carta 5×5** — Cada equipo tiene su propia carta de 25 ítems para recolectar.
- **Sistema de puntos** — +1 por ítem, +2 por línea, +5 por bingo completo.
- **Scoreboard en tiempo real** — Visualización de la carta con símbolos Unicode, ranking y timer.
- **Timer configurable** — Contador descendente con avisos automáticos (30, 10, 5 y 1 minuto).
- **Modo speedrun** — Tiempo ascendente con objetivo de línea o bingo completo.
- **Gestión de equipos** — Crea equipos dinámicamente o carga los predefinidos en `teams.yml`.
- **Mundos por equipo** — Opción de mundos separados para cada equipo con carga/descarga automática.
- **Mensajes personalizables** — Todos los textos del plugin en formato MiniMessage con soporte de colores, gradientes y placeholders.
- **Detección automática** — Detecta ítems conseguidos por recogida del suelo, crafteo o inventario.

---

## Requisitos

| Requisito | Versión mínima |
|-----------|---------------|
| Java | 21 |
| Paper (Minecraft) | 1.21.11 |

> El plugin usa la Paper API. **No es compatible con Spigot/Bukkit**.

---

## Instalación

1. Descarga el `.jar` desde [Releases](../../releases).
2. Colócalo en la carpeta `plugins/` de tu servidor Paper.
3. Reinicia el servidor.
4. Edita `plugins/BingoPatataPlugin/config.yml` y `teams.yml` según tus preferencias.

---

## Comandos

Todos los comandos comienzan por `/bingo`. Requieren permisos de operador.

### Gestión del juego

| Comando | Descripción |
|---------|-------------|
| `/bingo start` | Inicia la partida |
| `/bingo stop` | Detiene la partida |
| `/bingo pause` | Pausa el juego y el timer |
| `/bingo resume` | Reanuda el juego |
| `/bingo reset` | Reinicia completamente la partida |
| `/bingo reload` | Recarga la configuración sin reiniciar |

### Equipos

| Comando | Descripción |
|---------|-------------|
| `/bingo team create <nombre> <color>` | Crea un nuevo equipo |
| `/bingo team delete <nombre>` | Elimina un equipo |
| `/bingo team add <equipo> <jugador>` | Añade un jugador a un equipo |
| `/bingo team remove <equipo> <jugador>` | Saca a un jugador de su equipo |
| `/bingo team rename <equipo> <nuevo_nombre>` | Cambia el nombre del equipo |
| `/bingo team list` | Lista todos los equipos y sus miembros |

### Timer

| Comando | Descripción |
|---------|-------------|
| `/bingo timer start` | Inicia el timer |
| `/bingo timer pause` | Pausa el timer |
| `/bingo timer resume` | Reanuda el timer |
| `/bingo timer set <segundos>` | Establece la duración del timer |

### Carta

| Comando | Descripción |
|---------|-------------|
| `/bingo card show` | Muestra tu carta de bingo |
| `/bingo card hide` | Oculta la carta |
| `/bingo card reset` | Reinicia la carta de todos los equipos |
| `/bingo card give <equipo> <ítem>` | Marca un ítem como conseguido para un equipo |
| `/bingo card take <equipo> <ítem>` | Elimina un ítem marcado |

### Otros

| Comando | Descripción |
|---------|-------------|
| `/bingo points` | Muestra la puntuación de todos los equipos |
| `/bingo load-worlds` | Carga manualmente los mundos de los equipos |

---

## Configuración

El archivo `config.yml` permite personalizar el comportamiento completo del plugin:

```yaml
# Mundos: true = todos comparten mundo, false = mundo separado por equipo
share_world: true

# Duración del timer en segundos (10800 = 3 horas)
timer_duration: 10800

# Anunciar ítems conseguidos a todos los jugadores (false = solo al equipo)
broadcast_item_announcements: true

# Terminar la partida automáticamente al completar un bingo
end_game_on_bingo_complete: false

# Modo speedrun: el timer sube en lugar de bajar
speedrun_mode: false

# Objetivo del speedrun: "line" (completar una línea) o "bingo" (carta completa)
speedrun_goal: "line"
```

Todos los mensajes del plugin son editables en la sección `messages:` del `config.yml`, en formato [MiniMessage](https://docs.advntr.dev/minimessage/format.html).

### Equipos predefinidos (`teams.yml`)

Define los equipos disponibles antes de iniciar el juego:

```yaml
teams:
  rojo:
    name: "Rojo"
    color: "red"
  azul:
    name: "Azul"
    color: "blue"
  # ...
```

---

## Flujo de una partida típica

1. El administrador crea los equipos con `/bingo team create` o carga los predefinidos.
2. Asigna jugadores a equipos con `/bingo team add`.
3. Inicia la partida con `/bingo start`.
4. Los jugadores recolectan los 25 ítems de su carta.
5. Cada ítem obtenido actualiza el scoreboard en tiempo real.
6. La partida termina cuando se acaba el timer o un equipo completa el bingo (según configuración).

---

## Compilar desde el código fuente

```bash
git clone https://github.com/Meliory/Bingo-Patata-Plugin.git
cd Bingo-Patata-Plugin
./gradlew build
```

El `.jar` resultante se genera en `build/libs/`.

Para lanzar un servidor de prueba directamente:

```bash
./gradlew runServer
```

---

## Estructura del proyecto

```
src/main/java/org/
├── BingoPatataPlugin.java     # Punto de entrada del plugin
├── BingoCard.java             # Definición de los 25 ítems de la carta
├── BingoCommands.java         # Gestión de todos los comandos
├── BingoProcess.java          # Lógica de detección y puntuación de ítems
├── BingoScoreboard.java       # Scoreboard visual en tiempo real
├── BingoTimer.java            # Sistema de timer y avisos automáticos
├── BingoConfig.java           # Lectura y gestión de la configuración
├── BingoData.java             # Estado de ítems por equipo
├── BingoListener.java         # Listeners de eventos (pickup, crafting...)
├── BingoWorldManager.java     # Carga/descarga de mundos por equipo
├── BingoPortalManager.java    # Control de portales Nether/End
├── BingoRespawnManager.java   # Gestión de puntos de spawn
├── BingoDisplayManager.java   # Renderizado de la carta
├── TeamManager.java           # CRUD de equipos
├── Team.java                  # Modelo de equipo
├── MessageManager.java        # Sistema de mensajes MiniMessage
└── BingoLogger.java           # Registro de eventos del juego
```

---

## Tecnologías

- **Java 21**
- **Paper API 1.21.11**
- **Adventure / MiniMessage** (incluido en Paper)
- **Gradle**

---

## Autor

Desarrollado por **Meliory** para los eventos **UHC - PATATA**.