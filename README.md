# Batalla Naval con Alexa y LEGO EV3

Juega a la clásica **Batalla Naval** contra Alexa. Un robot **LEGO MINDSTORMS EV3** marca tus disparos en el tablero de Alexa mientras ella te responde por voz. ¿Estás listo para la batalla?

![Robot Batalla Naval: tablero de Alexa (arriba) y tablero del jugador (abajo)](docs/img/portada.jpg)

> **Versión 2026.** Este proyecto participó en el *LEGO MINDSTORMS Voice Challenge* de 2019–2020 ([documentación original en Hackster, en inglés](https://www.hackster.io/jcoronelcortes/battleship-interactive-game-using-amazon-alexa-and-lego-ev3-8ac604)). Amazon retiró en 2021 el *Alexa Gadgets Toolkit*, que conectaba el Echo con el EV3 por Bluetooth. Por eso esta versión lo reemplaza por **AWS IoT Core**: el robot recibe las jugadas por WiFi. El robot físico y las reglas del juego no cambian.

## Contenido

1. [Qué cambió respecto de la versión 2020](#qué-cambió-respecto-de-la-versión-2020)
2. [Cómo se juega](#cómo-se-juega)
3. [El robot](#el-robot)
4. [Construcción](#construcción)
5. [Arquitectura del software](#arquitectura-del-software)
6. [La inteligencia artificial de Alexa](#la-inteligencia-artificial-de-alexa)
7. [Instalación paso a paso](#instalación-paso-a-paso)
8. [Pruebas](#pruebas)
9. [Próximos pasos](#próximos-pasos)
10. [Créditos y referencias](#créditos-y-referencias)

**Repositorios:**

| Repositorio | Rama | Contenido |
|---|---|---|
| [battleship-alexa](https://github.com/jcoronelcortes/battleship-alexa/tree/smart-mode) (este) | `smart-mode` | Skill de Alexa (Java), modelos de voz, despliegue en AWS y esta documentación |
| [battleship-ev3](https://github.com/jcoronelcortes/battleship-ev3/tree/aws-iot) | `aws-iot` | Programa del robot EV3 (Python) |

---

## Qué cambió respecto de la versión 2020

| | 2020 | 2026 |
|---|---|---|
| Conexión Alexa → robot | Alexa Gadgets Toolkit (Bluetooth con el Echo), retirado por Amazon en 2021 | **AWS IoT Core (MQTT)** por WiFi, con un adaptador USB WiFi en el EV3 |
| Backend | AWS Lambda con Java 8 | AWS Lambda con **Java 21** y SnapStart |
| Flota | 10 barcos (4 de 1, 3 de 2, 2 de 3 y 1 de 4) | **5 barcos clásicos** (2, 3, 3, 4 y 5 casillas) |
| Turnos | El jugador decía "tu turno" para que Alexa atacara | Alexa **contraataca sola** después de cada disparo tuyo |
| Dificultad | Un nivel (disparos al azar) | **3 niveles**: básico, medio y avanzado |
| IA avanzada | — | Densidad de probabilidad según el paper de Kent State (ver [IA](#la-inteligencia-artificial-de-alexa)) |
| Hundir barcos | No existía | "Hundiste mi submarino" (o solo "hundido") |
| Idiomas | Inglés y español (es-MX) | Inglés (en-US) y español (es-US, es-MX y es-ES) |
| Instalación | Unos 30 pasos manuales en las consolas web | Dos scripts: `deploy/deploy.sh` y `deploy/skill.sh` |
| Pruebas | — | 34 tests automáticos, un simulador de partidas por línea de comandos y un **simulador web** con robot virtual |

---

## Cómo se juega

El objetivo es hundir todos los barcos del rival antes de que él hunda los tuyos. El juego tiene **dos tableros de 9 × 9**: filas de la **A a la I** y columnas del **1 al 9**.

### Tablero de Alexa (arriba)

Muestra tus disparos sobre los barcos de Alexa. Al empezar no sabes dónde están sus barcos, así que el tablero se cubre completo con bloques Duplo **azules**, que representan el mar.

| Al comenzar | Durante la partida |
|---|---|
| ![Tablero de Alexa vacío](docs/img/tablero-alexa-vacio.jpg) | ![Tablero de Alexa con disparos](docs/img/tablero-alexa.jpg) |

En cada ataque tuyo:
1. Dices la casilla, por ejemplo **"C cinco"**.
2. El robot se mueve a esa casilla y empuja el bloque azul. Mientras tanto, Alexa dice si fue **agua** o **impacto**.
3. Si fue impacto, colocas una **marca de impacto** en esa casilla.

![Marcas de impacto](docs/img/marcas-impacto.jpg)

### Tablero del jugador (abajo)

Aquí ubicas tus barcos con bloques Duplo de colores y rellenas el resto con bloques azules. También llevas el registro de los disparos de Alexa.

| Al comenzar | Con los barcos ubicados |
|---|---|
| ![Tablero del jugador vacío](docs/img/tablero-jugador-vacio.jpg) | ![Tablero del jugador con barcos](docs/img/tablero-jugador.jpg) |

Cuando Alexa dispara, dice la casilla dos veces (por ejemplo "E 5, E 5"). Tú le respondes cómo le fue y marcas el impacto en tu tablero:

![Marca de impacto en el tablero del jugador](docs/img/marca-impacto.jpg)

### La flota

Cada jugador tiene 5 barcos, que suman 17 casillas. Los barcos pueden ir en horizontal o en vertical, y no pueden superponerse.

| Barco | Casillas | Color Duplo sugerido |
|---|---|---|
| Destructor | 2 | Amarillo |
| Submarino | 3 | Naranja |
| Crucero | 3 | Verde |
| Acorazado | 4 | Rojo oscuro |
| Portaaviones | 5 | Gris (nuevo) |

> La versión 2020 usaba 10 barcos más pequeños. Los colores sugeridos reutilizan los bloques de esa versión; solo el portaaviones necesita un color nuevo.

### Comandos de voz

| Para… | Di (español) | Di (inglés) |
|---|---|---|
| Abrir el juego | "Alexa, abre **batalla naval**" | "Alexa, open **robot battleship**" |
| Empezar una partida | "comenzar juego" / "nuevo juego" | "start game" |
| Elegir la dificultad | "modo de juego **básico / medio / avanzado**" (también "fácil" o "difícil") | "game mode basic / medium / advanced" |
| Atacar | "**C cinco**" / "disparo a C cinco" / "fila C columna cinco" | "C five" / "attack C five" |
| Responder a Alexa: agua | "al agua" / "agua" / "fallaste" | "you fail" / "miss" |
| Responder a Alexa: impacto | "tocado" / "impacto" / "le diste" | "hit" / "impact" |
| Responder a Alexa: hundido | "**hundiste mi submarino**" (o solo "hundido") | "you sunk my submarine" |
| Rendirte (gana Alexa) | "tú ganas" | "you win" |
| Ayuda | "ayuda" | "help" |
| Ver los barcos de Alexa (pruebas) | "activar modo prueba" | "enable test mode" |
| Salir | "salir" / "cancelar" | "stop" / "cancel" |

**Consejo:** cuando Alexa te hunda un barco, nómbralo ("hundiste mi crucero"). Si solo dices "hundido", Alexa tiene que adivinar qué barco era y puede equivocarse cuando hay dos barcos pegados.

Hay un [cartel de instrucciones para imprimir](docs/cartel-instrucciones.md) y poner al lado del tablero, como en la versión original.

### Flujo de una partida

```mermaid
flowchart TD
    A["Alexa, abre batalla naval"] --> B["comenzar juego<br/>El robot vuelve a la posición inicial<br/>y Alexa ubica sus barcos"]
    B --> C["modo de juego básico / medio / avanzado"]
    C --> D["Tu ataque: «C cinco»<br/>El robot empuja el bloque y Alexa dice agua o impacto"]
    D -->|"Hundiste toda su flota"| G["Ganas tú 🎉"]
    D --> E["Alexa contraataca: «E 5, E 5»"]
    E --> F["Respondes: al agua / tocado / hundiste mi …"]
    F -->|"Hundió tu último barco"| H["Gana Alexa"]
    F --> D
```

Alexa cuida el orden del juego:
- Si atacas antes de responder a su disparo, te pide primero la respuesta.
- Si respondes "tocado" dos veces, el segundo no se cuenta.
- Si dices una casilla que no existe ("diez"), te explica cómo decirla.

### Niveles de dificultad

| Nivel | Cómo dispara Alexa | Disparos promedio para ganar* |
|---|---|---|
| **Básico** | Al azar | ~77 |
| **Medio** | *Hunt & target*: busca en patrón de tablero de ajedrez y, al impactar, ataca las casillas vecinas | ~48 |
| **Avanzado** | Densidad de probabilidad (ver [IA](#la-inteligencia-artificial-de-alexa)) | ~38 |

\* Promedio de 3.000 partidas simuladas contra tableros al azar, de un máximo de 81 casillas.

---

## El robot

El robot mueve un carro sobre el tablero de Alexa y empuja el bloque azul de la casilla atacada. Usa **3 motores grandes, 1 motor mediano y 2 sensores táctiles**.

### Movimiento vertical (filas)

Dos motores grandes, uno a cada lado del tablero, suben y bajan el carro juntos. Un sensor táctil en la parte inferior marca la posición inicial: antes de cada ataque los motores bajan hasta presionarlo.

| Motor derecho (puerto A) | Motor izquierdo (puerto D) | Sensor vertical (puerto 4) |
|---|---|---|
| ![Motor vertical derecho](docs/img/motor-vertical-derecho.jpg) | ![Motor vertical izquierdo](docs/img/motor-vertical-izquierdo.jpg) | ![Sensor táctil vertical](docs/img/sensor-vertical.jpg) |

![Sensor táctil vertical, otra vista](docs/img/sensor-vertical-2.jpg)

### Movimiento horizontal (columnas) y empuje

Un tercer motor grande mueve horizontalmente el carro con el **motor mediano**. El motor mediano empuja el bloque Duplo usando una **cremallera 1 × 13 con agujeros para eje y pasador** (*Gear Rack 1 x 13 with Axle and Pin Holes*). Un segundo sensor táctil marca la posición inicial horizontal.

| | | |
|---|---|---|
| ![Carro con el motor mediano](docs/img/carro-empujador-1.jpg) | ![Carro, vista lateral](docs/img/carro-empujador-2.jpg) | ![Carro, detalle de la cremallera](docs/img/carro-empujador-3.jpg) |

| Cremallera que empuja el bloque | Sensor horizontal (puerto 1) |
|---|---|
| ![Cremallera 1x13](docs/img/pieza-cremallera.jpg) | ![Sensor táctil horizontal](docs/img/sensor-horizontal.jpg) |

### Conexiones del EV3

El robot usa 6 puertos del EV3 y **cinco cables de 50 cm**.

| Puerto | Elemento |
|---|---|
| **A** | Motor grande, movimiento vertical (el de la derecha en la foto de abajo) |
| **D** | Motor grande, movimiento vertical (el de la izquierda en la foto) |
| **C** | Motor grande, movimiento horizontal |
| **B** | Motor mediano, empuja el bloque |
| **1** | Sensor táctil, posición inicial horizontal |
| **4** | Sensor táctil, posición inicial vertical |

![Vista trasera con el EV3 y las conexiones](docs/img/conexiones-ev3.jpg)

### Cómo calcula el robot el movimiento

La **posición inicial es la casilla I9** (abajo a la derecha). Desde ahí, cada movimiento se calcula en grados del motor (`run_to_rel_pos`, velocidad 400 °/s):

| Movimiento | Fórmula | Ejemplo para C5 |
|---|---|---|
| Vertical (filas) | `(8 − fila) × 140°` | C = fila 2 → 6 × 140 = **840°** |
| Horizontal (columnas) | `(8 − columna) × 280°` | 5 = columna 4 → 4 × 280 = **1120°** |
| Empuje del bloque | −350° y luego +350° | El largo de la cremallera |
| Vuelta a la posición inicial | Motores al 40% hasta presionar cada sensor táctil | |

Estos valores están en [`config.py`](https://github.com/jcoronelcortes/battleship-ev3/blob/aws-iot/config.py) del robot. Después de armarlo, se ajustan con `calibrate.py`.

**Mejora 2026 de seguridad:** si un sensor táctil no se presiona en 20 segundos, el motor se detiene y las luces del EV3 se ponen rojas. Antes, un sensor desconectado podía dejar el motor girando y romper el armado.

### Sincronización entre la voz y el robot

Mientras el robot se mueve, Alexa habla: dice "ejecutando ataque", espera a que el robot llegue, suena el cañón justo cuando empuja el bloque y anuncia el resultado. Para lograrlo, la skill calcula cuánto tarda el robot en cada casilla e inserta pausas SSML (`<break>`) en la respuesta. El cálculo está en [`AttackBoardTime.java`](src/main/java/jose/coronel/cl/battleship/util/AttackBoardTime.java):

| Factor | Tiempo |
|---|---|
| Cada fila o columna recorrida | 700 ms |
| Movimiento horizontal (si no está en la columna 9 ni en la fila A) | +1.200 ms |
| Empujar el bloque | 4.000 ms |
| Sonido del cañón | 3.680 ms |
| Volver a la posición inicial | 4.000 ms |

Alexa no acepta pausas de más de 10 segundos seguidos. Para las casillas lejanas (como A1), la pausa se divide y Alexa dice "apuntando…" entre los tramos.

---

## Construcción

El robot está hecho con piezas de **LEGO Technic, LEGO System y LEGO Duplo**, más los motores, sensores y cables de un set **EV3**.

### Lista de piezas

La lista completa está en formato BrickLink: [`docs/bricklink-piezas.xml`](docs/bricklink-piezas.xml) tiene 98 tipos de pieza y se puede importar en BrickLink para comprarlas. Estas son las capturas de la lista (los nombres de las piezas vienen del catálogo de BrickLink):

<details>
<summary>Ver las 12 páginas de la lista de piezas</summary>

![Piezas 1](docs/img/piezas-01.jpg)
![Piezas 2](docs/img/piezas-02.jpg)
![Piezas 3](docs/img/piezas-03.jpg)
![Piezas 4](docs/img/piezas-04.jpg)
![Piezas 5](docs/img/piezas-05.jpg)
![Piezas 6](docs/img/piezas-06.jpg)
![Piezas 7](docs/img/piezas-07.jpg)
![Piezas 8](docs/img/piezas-08.jpg)
![Piezas 9](docs/img/piezas-09.jpg)
![Piezas 10](docs/img/piezas-10.jpg)
![Piezas 11](docs/img/piezas-11.jpg)
![Piezas 12](docs/img/piezas-12.jpg)

</details>

Piezas del set EV3:

![Piezas del set EV3](docs/img/piezas-ev3.jpg)

Además necesitas:
- **5 cables de 50 cm**.
- Una **microSD** de 4 a 32 GB para ev3dev.
- Un **adaptador WiFi USB** compatible con ev3dev, por ejemplo el Edimax EW-7811Un. Es nuevo en la versión 2026.

### Videos de armado

Los videos muestran paso a paso cómo armar el robot (de 2019; la construcción no cambió):

| | | | |
|---|---|---|---|
| [![Parte 1](https://img.youtube.com/vi/Rhy8FBoz5-o/mqdefault.jpg)](https://www.youtube.com/watch?v=Rhy8FBoz5-o) | [![Parte 2](https://img.youtube.com/vi/55-pwYZL0XU/mqdefault.jpg)](https://www.youtube.com/watch?v=55-pwYZL0XU) | [![Parte 3](https://img.youtube.com/vi/a5-HZEO3hCs/mqdefault.jpg)](https://www.youtube.com/watch?v=a5-HZEO3hCs) | [![Parte 4](https://img.youtube.com/vi/rKj35Y0dqLo/mqdefault.jpg)](https://www.youtube.com/watch?v=rKj35Y0dqLo) |
| Parte 1 | Parte 2 | Parte 3 | Parte 4 |
| [![Parte 5](https://img.youtube.com/vi/G22nA6VdCBA/mqdefault.jpg)](https://www.youtube.com/watch?v=G22nA6VdCBA) | [![Parte 6](https://img.youtube.com/vi/3jG25oBYJvo/mqdefault.jpg)](https://www.youtube.com/watch?v=3jG25oBYJvo) | [![Parte 7](https://img.youtube.com/vi/CIrRZmkOZnk/mqdefault.jpg)](https://www.youtube.com/watch?v=CIrRZmkOZnk) | [![Parte 8](https://img.youtube.com/vi/LDHeU8XUXl4/mqdefault.jpg)](https://www.youtube.com/watch?v=LDHeU8XUXl4) |
| Parte 5 | Parte 6 | Parte 7 | Parte 8 |

### El juego en video (versión 2020)

| Partida en inglés | Partida en español |
|---|---|
| [![Partida en inglés](https://img.youtube.com/vi/wr5hHsV9W6M/hqdefault.jpg)](https://www.youtube.com/watch?v=wr5hHsV9W6M) | [![Partida en español](https://img.youtube.com/vi/UcNnPs_PIMo/hqdefault.jpg)](https://www.youtube.com/watch?v=UcNnPs_PIMo) |

> Estos videos son de la versión 2020. El robot y el tablero son los mismos; cambian la flota, que ahora tiene 5 barcos, algunos comandos (ya no se dice "tu turno") y los niveles de dificultad.

---

## Arquitectura del software

```mermaid
flowchart LR
    J(["Jugador"]) -- voz --> E["Echo<br/>(Alexa)"]
    E --> S["Skill «batalla naval»<br/>modelos es / en"]
    S --> L["AWS Lambda<br/>Java 21 · SnapStart"]
    L -- "respuesta con voz (SSML)" --> E
    L -- "comando JSON<br/>tópico battleship/ev3" --> I["AWS IoT Core<br/>(MQTT)"]
    I -- "WiFi · TLS con certificado" --> R["EV3 con ev3dev<br/>Python · main.py"]
    R --> M["Motores y sensores"]
```

1. **Skill de Alexa.** Los modelos de voz [`alexa-skill/Battleship_es.json`](alexa-skill/Battleship_es.json) y [`Battleship_en.json`](alexa-skill/Battleship_en.json) convierten lo que dices en *intents* (por ejemplo `AtaqueJugadorIntent` con los slots `Fila` y `Columna`).
2. **Lambda en Java 21.** Tiene la lógica del juego y la IA. La partida se guarda en la sesión de Alexa como JSON (clave `jsonGame`) entre un turno y el siguiente.
3. **AWS IoT Core.** Cada jugada que mueve al robot se publica como JSON en el tópico `battleship/ev3`.
4. **EV3.** El robot se suscribe a ese tópico con certificados propios y ejecuta los comandos **en orden**, uno a la vez.

Si la Lambda no tiene configurada la variable `IOT_ENDPOINT`, el juego funciona en **modo dummy**: se puede jugar por voz sin el robot, y los comandos solo quedan en el log.

### Intents de la skill

| Intent (es / en) | Qué hace | Comando al robot |
|---|---|---|
| `ComenzarJuegoIntent` / `StartGameIntent` | Nueva partida; Alexa ubica sus barcos al azar | `start_game` (vuelve a la posición inicial) |
| `ModoDeJuegoIntent` / `GameModeIntent` | Elige el nivel | `game_mode_configured` (pitido) |
| `AtaqueJugadorIntent` / `PlayerAttackIntent` | Tu disparo y el contraataque de Alexa | `player_attack` (va a la casilla y empuja) |
| `AlAguaAlexaIntent` / `YouFailAlexaIntent` | El disparo de Alexa fue agua | `fail_attack` (luz verde) |
| `ImpactoAlexaIntent` / `ImpactAlexaIntent` | El disparo de Alexa fue impacto | `alexa_impact` (tono y luz roja) |
| `DestruidoIntent` / `DestroyedIntent` | Alexa hundió un barco | `alexa_ship_destroyed`, o `finish_game` si era el último |
| `AlexaGanaEljuegoIntent` / `AlexaWinsTheGameIntent` | Te rindes | `finish_game` (celebración) |
| `ModoTestIntent` / `TestModeIntent` | Alexa lee dónde están sus barcos | `test_mode` |

El formato JSON de los comandos es un **contrato** entre Java y Python, y lo verifican tests en los dos repositorios:

```json
{"type":"command","command":"player_attack","attackResult":2,"shipDestroyed":"Destroyer",
 "row":"2","column":"4","alexaRow":"E","alexaColumn":"5","logZones":null}
```

`attackResult` puede ser: 1 impacto · 2 barco hundido · 3 agua · 4 ganaste · 5 casilla ya atacada (el robot no se mueve).

### Organización del código Java

| Paquete | Contenido |
|---|---|
| `handlers` | `Turn` (todo lo de un turno: idioma, textos, slots, sesión y robot) y las clases base `GameIntentHandler`, `ActiveGameHandler` (exige una partida iniciada) y `AlexaShotAnswerHandler` (exige un disparo de Alexa sin responder) |
| `handlers.game` | Un handler por intent del juego |
| `handlers.common` | Bienvenida, ayuda, salir, *fallback* y errores |
| `game` | `BattleshipGame`: las reglas, sin nada de voz |
| `game.board` | `Game`, `BoardGame`, `Zone`, `Ship`, `ShipType` y `Attack` |
| `game.ai` | `BasicStrategy`, `MediumStrategy` y `AdvancedStrategy` |
| `session` | `GameSession`: guarda y carga la partida en la sesión de Alexa |
| `robot` | `RobotCommand` (el JSON) y `MqttRobotChannel` (AWS IoT) / `LoggingRobotChannel` (modo dummy) |
| `util` | `AttackBoardTime` (pausas), `Coordinates` ("C", "cinco" → fila y columna) e `I18NUtil` |

### Voz con emoción (SSML)

Las respuestas usan **SSML** (*Speech Synthesis Markup Language*) para que Alexa "viva" la partida, con efectos de sonido, pausas y, en inglés, emociones (`<amazon:emotion>`). Por ejemplo, cuando Alexa hunde uno de tus barcos:

```xml
<audio src="soundbank://soundlibrary/explosions/explosions/explosions_10"/> Fabuloso.
Solamente me queda hundir dos barcos para ganar el juego. <break time="1s"/> Es tu turno de atacar.
```

Todos los textos están en [`translations_es.properties`](src/main/resources/i18n/translations_es.properties) y [`translations_en.properties`](src/main/resources/i18n/translations_en.properties). Cada mensaje tiene varias variantes, y Alexa elige una al azar para no repetirse.

### Agregar un idioma

1. Revisa el código del idioma (*locale*) en la [documentación de Alexa](https://developer.amazon.com/en-US/docs/alexa/custom-skills/develop-skills-in-multiple-languages.html), por ejemplo `pt-BR`.
2. Copia `alexa-skill/Battleship_en.json` a `Battleship_pt.json` y traduce el nombre de invocación, las frases de ejemplo y los valores de los slots. Los **nombres de los intents y de los slots** pueden cambiar, pero deben coincidir con las claves del paso 3.
3. Crea `src/main/resources/i18n/translations_pt.properties` a partir del inglés. Las claves `xxxIntent`, `row`, `column`, `ship` y `gameMode` deben tener exactamente los nombres de intents y slots del paso 2.
4. En `deploy/skill.sh`, agrega el *locale* en el bloque `publishingInformation` y en la copia de modelos.
5. Ejecuta `mvn test`. `InteractionModelTest` falla si un handler espera un intent o slot que no existe en el modelo.

---

## La inteligencia artificial de Alexa

El nivel **avanzado** sigue el paper *"Battleship Playing Program Utilizing Probability Density Functions"* (Jacob Boyd, Jonathan Boyd y Darwin L. Boyd, Kent State University, 2016), basado en el análisis de [DataGenetics (2011)](http://www.datagenetics.com/blog/december32011/).

**Modo Hunt (buscar).** Para cada barco que sigue a flote, se cuentan todas las formas en que podría ubicarse en el tablero sin pasar por casillas de agua ni por barcos ya hundidos. Cada casilla suma un punto por cada ubicación que la cubre, y Alexa dispara a la casilla con más puntos. Con el tablero vacío, el centro vale 34 y las esquinas 10, igual que en la figura 4 del paper. Por eso Alexa suele abrir en el centro (E5).

**Modo Destroy (hundir).** Después de un impacto, solo se cuentan las ubicaciones que pasan por ese impacto. Alrededor de un impacto aislado los valores son 12, 7, 3 y 1, como en la figura 6 del paper. Si hay varios impactos alineados, las ubicaciones que los cubren a todos valen 20 veces más por cada impacto extra, así que Alexa sigue la línea del barco.

**Barcos hundidos.** Cuando le dices "hundiste mi crucero", Alexa busca la fila de impactos que forma ese barco, la marca como hundida y vuelve al modo Hunt. Los impactos de otro barco que quedaron pendientes no se olvidan.

| IA avanzada | Versión 2020 | Versión 2026 |
|---|---|---|
| Nombras el barco hundido | 44,0 disparos | **38,2 disparos** |
| Solo dices "hundido" | 46,7 disparos | **40,1 disparos** |

Promedio de 3.000 partidas con los mismos tableros. Para repetir la medición:

```bash
mvn test -Dtest=AlexaAiSimulationTest#benchmark -Dai.benchmark=true
```

---

## Instalación paso a paso

### 1. Requisitos

En tu computador (las instrucciones son para macOS con Homebrew):

```bash
brew install maven node awscli
```

```bash
npm install -g ask-cli
```

También necesitas:
- **Java 21 o superior.**
- Una **cuenta de AWS**.
- Una **cuenta de Amazon Developer**, la misma de tu Echo para probar la skill en él.

### 2. Iniciar sesión

```bash
aws login --region us-east-1
```

```bash
ask configure
```

En `ask configure`, inicia sesión en el navegador y vuelve **de inmediato** a la terminal: el código expira en pocos minutos. Cuando te pregunte si quieres vincular una cuenta de AWS, responde **No**.

### 3. Compilar y desplegar la Lambda y AWS IoT

```bash
mvn clean package
```

```bash
./deploy/deploy.sh
```

El script crea o actualiza:
- El rol IAM de la Lambda, que solo puede publicar en el tópico del robot.
- La Lambda `battleship-alexa` (Java 21, arm64, SnapStart), con el alias `live`.
- El *thing* de AWS IoT `battleship-ev3`, su política y sus **certificados**, que quedan en `deploy/ev3-certs/` y **no se suben a git**.

### 4. Crear la skill de Alexa

```bash
./deploy/skill.sh
```

Crea o actualiza la skill con la ASK CLI en 4 idiomas, la conecta a la Lambda y la habilita para pruebas en tu cuenta. Al final vuelve a ejecutar `deploy.sh` con el ID de la skill, para que la Lambda solo responda a tu skill.

> El script reutiliza la skill de 2020 si existe. Para crear otra, borra `deploy/.ask/` y cambia `EXISTING_SKILL_ID`.

### 5. Probar sin el robot

```bash
python3 deploy/simulate.py es-US "abre batalla naval" "comenzar juego" "modo de juego avanzado" "C cinco" "al agua"
```

Este comando juega contra el simulador real de Alexa y muestra qué *intent* entendió y qué respondió en cada frase. También puedes probar en tu Echo: "Alexa, abre batalla naval".

#### Simulador web

Para jugar con tableros en pantalla y ver un robot virtual que hace los mismos movimientos que el EV3:

```bash
./deploy/web.sh
```

Luego abre <http://localhost:8080>. Puedes ubicar tus barcos con clics (con vista previa y giro con la tecla R) y jugar contra la skill local (sin AWS), contra la Lambda real o en modo **Alexa completa**, donde escribes o dices frases libres y ves qué intent entendió Alexa. En los modos con AWS, el simulador se puede conectar a AWS IoT **como si fuera el robot**. Ver la guía en [simulador/README.md](simulador/README.md).

![Simulador web](docs/img/simulador.jpg)

### 6. Preparar el robot

Sigue el [README de battleship-ev3](https://github.com/jcoronelcortes/battleship-ev3/tree/aws-iot#readme). En resumen:
1. Graba **ev3dev-stretch** en la microSD.
2. Conecta el adaptador WiFi.
3. Desde tu computador, ejecuta `./install.sh`, que copia el programa y los certificados.
4. Ejecuta `python3 calibrate.py` para comprobar que el robot llega bien a las casillas.
5. Ejecuta `python3 main.py`. Cuando las luces del EV3 se pongan **verdes**, el robot está conectado.

> Para probar el robot sin el EV3: `python3 main.py --simulate --certs <carpeta de certificados>` se conecta a AWS IoT y muestra en pantalla los movimientos que haría.

---

## Pruebas

```bash
mvn test
```

| Test | Qué verifica |
|---|---|
| `SkillConversationTest` | Conversaciones completas en español e inglés, incluidos los resguardos del juego (atacar sin partida, responder dos veces, casilla inválida) |
| `InteractionModelTest` | Que los modelos de voz tengan todos los intents y slots que esperan los handlers |
| `AdvancedStrategyPaperTest` | Que la IA reproduzca las figuras 4 y 6 del paper |
| `AiGoldenMasterTest` | 600 situaciones de juego: la IA no cambia sin que se note |
| `AlexaAiSimulationTest` | Partidas completas en los 3 niveles, sin repetir disparos, y el benchmark |
| `AttackBoardTimeTest` | Que ninguna casilla genere pausas que Alexa rechace |
| `RobotCommandContractTest` | El JSON exacto que lee el robot |
| `GameSetupTest` | Que los barcos de Alexa no se superpongan y aparezcan en las dos orientaciones |

El robot tiene sus propios tests (`python3 -m unittest discover tests` en battleship-ev3), entre ellos el mismo contrato JSON.

---

## Próximos pasos

De la lista de 2020:
- ✅ **Nivel medio de dificultad** e intent **"hundido"**. Ahora hay 3 niveles y la IA avanzada.
- ✅ **Quitar la instrucción "tu turno"**: Alexa contraataca sola.
- ⬜ Mejorar el movimiento del motor horizontal.
- ⬜ Reducir la vibración de la estructura.
- ⬜ Aumentar la velocidad de los motores verticales. Hay que ajustar también `AttackBoardTime`.
- ⬜ Instrucciones de armado profesionales, siguiendo las [recomendaciones de Nico71](https://www.nico71.fr/faq/).
- ⬜ Modo multijugador.

Nuevos:
- ⬜ Probar la versión 2026 con el robot físico y ajustar la calibración.
- ⬜ Guardar la partida en DynamoDB para retomarla si la sesión de Alexa se cierra.
- ⬜ Mostrar el tablero en pantalla en los Echo Show (APL).

---

## Créditos y referencias

- **Autor:** José Coronel ([Hackster](https://www.hackster.io/jcoronelcortes)).
- **Documentación original (2020, en inglés):** [Battleship. Interactive game using Amazon Alexa and LEGO EV3](https://www.hackster.io/jcoronelcortes/battleship-interactive-game-using-amazon-alexa-and-lego-ev3-8ac604).
- **IA:** Boyd, J., Boyd, J. y Boyd, D. L. (2016). *Battleship Playing Program Utilizing Probability Density Functions*. Kent State University.
- Berry, N. (2011). [*Battleship*](http://www.datagenetics.com/blog/december32011/). DataGenetics.
- [ev3dev](https://www.ev3dev.org/) y la librería [python-ev3dev2](https://github.com/ev3dev/ev3dev-lang-python).
- **Licencia:** GPL-3.0, la misma de la publicación original.
