# We Raised Siren Head — Arquitectura

Forge 47.x · Minecraft 1.20.1 · Java 17 · mappings oficiales de Mojang.
Paquete base: `com.weraisedsirenhead` · mod id: `weraisedsirenhead`.

## 1. Principios

1. **La historia vive en datos guardados, no en la entidad.** La entidad es solo la "cara visible"; el día, la etapa y las misiones se guardan en un `SavedData` del Overworld. Así sobreviven a cerrar el mundo, a que la entidad se descargue con el chunk o a que haya que reaparecerla.
2. **Una clase, una responsabilidad.** Nada de lógica de día/misiones/sonido dentro de `SirenHeadEntity`.
3. **Servidor manda, cliente muestra.** Día, IA, eventos y misiones se calculan en servidor. El cliente solo recibe paquetes para el HUD y reproduce sonido/animación.
4. **El director decide, la IA ejecuta.** Un "director de terror" elige *qué* puede pasar según el día; la IA de la entidad decide *cómo* hacerlo.

## 2. Paquetes

```
com.weraisedsirenhead
├─ SirenHeadMod            punto de entrada (registros, config, red)
├─ config/                 SirenHeadConfig
├─ registry/               ModEntities, ModSounds
├─ story/                  PlayerStory + StoryData (guardado), StoryManager (inicio/dia/victoria),
│   │                      StoryEvents (eventos de Forge), StoryClock, StoryRuntime, StoryContext, GrowthStage
│   └─ script/             Day1Script .. Day5Script (eventos programados y misiones de cada dia)
├─ quest/                  QuestManager (completar misiones + sync al HUD)
├─ director/               HorrorDirector (silencios, sorteo ponderado) + events/ (13 eventos de terror)
├─ entity/                 SirenHeadEntity, SirenBones, SirenState, CreatureActions
│   └─ ai/                 SirenBrain (decide estados), BlockBreaker, goal/ (un Goal por estado)
├─ sound/                  SirenSounds (distancia y direccion)
├─ network/                ModNetwork + SyncStoryPacket
├─ command/                /sirenhead
├─ util/                   Shelter (detector de refugio), Sight (vision)
├─ world/                  SpawnSpots
└─ client/                 renderer de malla con huesos (SirenMesh, SirenPose, SirenHeadRenderer), HUD de misiones, estado del cliente
```

## 3. Sistemas y decisiones técnicas

**Persistencia (`StorySavedData`).** `SavedData` guardado en el `DimensionDataStorage` del Overworld. En 1.20.1 se obtiene con `computeIfAbsent(Function<CompoundTag,T>, Supplier<T>, String)` (la variante con `SavedData.Factory` es de 1.20.2+). Guarda, por jugador: día de historia, día de Minecraft en el que empezó, UUID de la criatura, última posición conocida, misiones completadas y qué eventos ya ocurrieron. Funciona en mundos nuevos y existentes porque no depende de estructuras de worldgen: se crea al iniciar la historia.

**Reloj de días (`DayClock`).** Tick del servidor sobre el Overworld; día de Minecraft = `dayTime / 24000`. El día de historia solo avanza (nunca retrocede con `/time set`) y como máximo +1 por comprobación, para que dormir o saltar tiempo no se coma días de la historia.

**Entidad.** Una sola `SirenHeadEntity`. La etapa (`GrowthStage`) y el estado de IA son `EntityDataAccessor` sincronizados. Cambiar de etapa llama a `refreshDimensions()`, y el renderer escala la misma malla al alto de la etapa (1,5 a 12 bloques) y la anima con un esqueleto propio. Se marca como persistente y se re-vincula por UUID guardado, de modo que si se descarga o se pierde, el sistema puede volver a aparecerla.

**IA.** `SirenStateMachine` con los 11 estados pedidos. Cada estado tiene un `Goal` y una lista de días en los que está permitido. El `HorrorDirector` propone transiciones con probabilidades por día, y la máquina las valida con reglas de justicia: tiempo mínimo entre persecuciones, nunca atacar al jugador recién reaparecido, avisos sonoros antes de un ataque, silencios obligatorios entre eventos.

**Eventos de terror.** Cada evento es una clase con condiciones, duración y cooldown. El director los elige con aleatoriedad ponderada y semilla distinta por jugador, para que no ocurran siempre en el mismo momento.

**Misiones.** 3 por día (15 en total), definidas en `QuestRegistry`. Cada objetivo escucha un evento de Forge concreto (interactuar con la criatura, colocar bloques, recoger ítems, distancia, supervivencia...). El progreso se guarda en `StorySavedData` y se sincroniza al cliente con un paquete; el aviso de misión completada es otro paquete.

**HUD.** Overlay del cliente registrado en `RegisterGuiOverlaysEvent` (bus del mod, solo cliente): lista de 3 misiones en una esquina, tilde verde al completar y mensaje temporal en pantalla.

**Sonido.** `SoundEvent`s registrados con `DeferredRegister` y definidos en `sounds.json`. Para dar sensación de dirección, el servidor reproduce el sonido en una posición desplazada respecto del jugador (detrás, a un costado, lejos) y no en la posición de la criatura. El alcance se ajusta con el volumen y/o con eventos de rango fijo. Se verifican las firmas exactas contra el código de 1.20.1 al llegar a esa etapa. El silencio se controla desde el director (ventanas sin sonido).

**Red.** `SimpleChannel` de Forge (`NetworkRegistry.newSimpleChannel`) con versión de protocolo propia.

## 4. Archivos externos que tendrás que aportar (se piden etapa por etapa)

| Qué | Ruta exacta |
|---|---|
| Modelo bebé | `src/main/resources/assets/weraisedsirenhead/geo/siren_head_baby.geo.json` |
| Modelo adulto | `src/main/resources/assets/weraisedsirenhead/geo/siren_head_adult.geo.json` |
| Animaciones bebé | `.../animations/siren_head_baby.animation.json` |
| Animaciones adulto | `.../animations/siren_head_adult.animation.json` |
| Texturas | `.../textures/entity/siren_head_baby.png`, `.../textures/entity/siren_head_adult.png` |
| Sonidos (`.ogg` mono) | `.../sounds/*.ogg` + `.../sounds.json` (lista de nombres en la etapa de sonidos) |

(`...` = `src/main/resources/assets/weraisedsirenhead`)

## 5. Estado

Todas las etapas estan implementadas en el codigo (ver README). Falta compilar y probar en el juego.
