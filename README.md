# We Raised Siren Head

Mod de Minecraft Java 1.20.1 (Forge 47.x, Java 17). Cinco días criando a Siren Head:
empieza como una aventura de crianza y termina en una noche de supervivencia.

## Cómo sacar el .jar (solo GitHub)

1. Creá un repositorio en GitHub y subí **todo el contenido de esta carpeta** (incluida la carpeta oculta `.github`).
2. Entrá a la pestaña **Actions**. El workflow *Build* arranca solo con cada push (o con *Run workflow*).
3. Cuando termine en verde, abrí la ejecución y bajá el artefacto **weraisedsirenhead-jar**. Adentro está el `.jar`.
4. Copialo a la carpeta `mods` de un Minecraft 1.20.1 con Forge 47.x. No necesita ningún otro mod.

Si el build falla, copiá el error del log de Actions y mandámelo para corregirlo.

## Cómo se juega

1. Creá un mundo (o usá uno existente) y escribí `/sirenhead start`. Aparece el bebé en un bosque cercano,
   a 28–60 bloques. Escuchá los chirridos y buscalo.
2. Construí tu casita. Clic derecho con comida lo alimenta; clic derecho con la mano vacía lo hace quedarse o seguirte.
3. La historia avanza sola: cada día de Minecraft (amanecer) pasa al siguiente. Son 5 días.
4. El día 5, al caer la tarde, se transforma. Aparece el cartel **SOBREVIVE HASTA EL AMANECER**, la noche pasa
   más rápido (config `finalNightSpeed`) y no se puede dormir. Llegá vivo al amanecer para terminar.

Comandos: `/sirenhead start | status | reset`. Para pruebas (operadores): `/sirenhead day <1-5>`, `/sirenhead locate`.

Si empezás después de la mitad del día, el día 1 dura hasta la mañana siguiente a la próxima.
Necesita la gamerule `doDaylightCycle` en true. El progreso se guarda por jugador en el mundo.

## Qué pasa cada día

| Día | Escenas de terror |
|---|---|
| 1 | El bebé está perdido en el bosque y chirría para guiarte. De noche duerme a tu lado y, ya dormido, **susurra tu nombre por radio**. |
| 2 | De día **devora una vaca** delante tuyo (si no hay ninguna a la vista, aparece una sola, de la nada). Al anochecer deja un **regalo frente a tu puerta**: un cadáver con rastro de sangre desde el bosque, huesos y carne podrida. De noche te llama una señal de estática y lo encontrás parado ahí. |
| 3 | Desaparece. Un **rastro de sangre y almas** lleva a su escondite: un **altar de huesos** con calaveras, antorchas de almas y cadáveres, donde lo encontrás comiendo. Voces pidiendo ayuda. De noche **devora a un aldeano** y deja otro regalo. |
| 4 | Al amanecer tu casa está **rodeada de ofrendas** (seis cadáveres con huesos y antorchas). Al anochecer te persigue; después golpea puertas y ventanas. Señuelos con voz humana. |
| 5 | Ofrenda enorme y un altar cerca. Al caer la tarde se transforma y te caza hasta el amanecer. |

Entre escenas hay transmisiones de **radio en el chat** (estática y mensajes con tu nombre) y eventos al azar: pisadas invisibles,
respiración a tu espalda, siluetas entre los árboles, sirenas, voces. Los cadáveres son de bloques (lana roja y de color, huesos,
calaveras, alfombra roja de sangre) y nunca rompen tus construcciones.

## Configuración (`config/weraisedsirenhead-common.toml`)

`finalNightSpeed`, `allowBlockDestruction`, `attackDamageDay4`, `attackDamageDay5`, `eventFrequency`,
`chaseSpeed`, `debugLogging`.

## Tu modelo de Siren Head

El modelo es el tuyo (`tools/source/siren.bbmodel`, una malla de ~10 mil caras). Como Minecraft/GeckoLib solo dibujan cubos, el mod
trae su propio sistema: `tools/build_siren_mesh.py` le coloca un esqueleto de 15 huesos (cadera, columna, cuello, cabeza, dos parlantes,
brazos y piernas con codo y rodilla) y calcula los pesos de piel. El mod dibuja la malla deformada hueso a hueso.

**Tamaño según el día** (la misma malla escalada): día 1 = 1,5 bloques, día 2 = 2,4, día 3 = 3,9, día 4 = 6, día 5 = 12.
Al cambiar de día crece de a poco, a la vista.

Animaciones (en `client/SirenPose.java`, retocables): reposo, caminar, correr, dormir, comer, devorar, observar, golpear y transformarse.

| Qué | Ruta (`src/main/resources/assets/weraisedsirenhead/`) |
|---|---|
| Malla + esqueleto + pesos | `models/siren_head.bin` (generada) |
| Textura | `textures/entity/siren_head.png` |
| Sonidos | `sounds/*.ogg` (mono, Vorbis) + `sounds.json` |

Si cambiás el modelo: reemplazá `tools/source/siren.bbmodel` y corré `python3 tools/build_siren_mesh.py` (numpy y Pillow).
Los sonidos se regeneran con `python3 tools/generate_sounds.py` (numpy, scipy y ffmpeg).
