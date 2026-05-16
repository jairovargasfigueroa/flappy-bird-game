# Flappy Bird 2P — Primer Parcial Programación Gráfica

**Integrante:** Jairo Moises Vargas Figueroa
**Asignatura:** Programación Gráfica
**Fecha:** 16 de mayo de 2026

---

## Controles

| Acción | Jugador 1 | Jugador 2 |
| ------ | --------- | --------- |
| Saltar | `ESPACIO` | `W`       |

| Acción                    | Tecla                                  |
| ------------------------- | -------------------------------------- |
| Navegar menú / game over  | `↑` `↓`                                |
| Confirmar opción          | `ENTER`                                |
| Selección directa en menú | `1` (un jugador) · `2` (dos jugadores) |
| Salir                     | `ESC`                                  |

---

## Compilación y ejecución

Requisitos: Java 17+, Maven 3.9+

```bash
mvn clean compile exec:exec
```

La clase principal es `com.graphics.FlappyGame` (configurada en `pom.xml`).

---

## Cambios realizados sobre la base

### 2.1 Pájaro compuesto por figuras geométricas

El pájaro ya no es un rectángulo simple — se construye con varias figuras geométricas dibujadas con OpenGL: cuerpo, cola, ala animada (oscila con `sin(tiempo)`), ojo con pupila y pico triangular. El cuerpo entero rota según la velocidad vertical del pájaro mediante un ángulo `uAngle` aplicado en el vertex shader (`cos/sin` sobre cada vértice).

### 2.2 Modo de dos jugadores simultáneos

Se agregó un segundo `Bird` con posición, velocidad, estado y puntaje independientes. Las tuberías son compartidas. La partida continúa mientras al menos un pájaro esté vivo. El modo se elige desde el menú (1 o 2 jugadores).

### 2.3 Incremento progresivo de dificultad

La velocidad de las tuberías y el tiempo entre spawns se ajustan cada 5 puntos del jugador líder. Hay un tope máximo para que el juego siga siendo jugable. El nivel actual se muestra en el HUD (esquina derecha) y en el título de la ventana.

### 2.4 Mejora de la interfaz

- **Fondo:** degradado de cielo con interpolación por vértice (OpenGL per-vertex color), nubes animadas que se desplazan de derecha a izquierda y franja de suelo verde.
- **Pantalla de inicio y game over:** imágenes PNG cargadas con STBImage y dibujadas como sprites (quad + textura). El selector de opciones se muestra como un borde amarillo que se mueve con las flechas.
- **HUD durante el juego:** puntaje de cada jugador mostrado con un sprite sheet de dígitos (`digits.png`) usando coordenadas UV, acompañado de un cuadradito del color de cada pájaro para identificarlos. Indicador de nivel en esquina derecha.
- **Sonido:** efectos de salto, punto anotado y game over usando `javax.sound.sampled` (archivos WAV en `src/main/resources/sounds/`).

---

## Estructura del proyecto

```
src/main/java/com/graphics/
├── FlappyGame.java      — bucle principal, orquesta todo
├── Bird.java            — física y estado de cada pájaro
├── GameState.java       — tuberías, colisiones, dificultad
├── Renderer.java        — todo lo visual (shaders, VAO/VBO, texturas)
├── Background.java      — fondo degradado y nubes
├── InputManager.java    — lectura del teclado con detección de flanco
└── SoundManager.java    — carga y reproducción de sonidos WAV

src/main/resources/
├── sounds/              — jump.wav, point.wav, gameover.wav
└── textures/            — menu.png, gameover.png, digits.png
```
