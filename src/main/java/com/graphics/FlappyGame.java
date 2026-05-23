package com.graphics;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;

/**
 * FlappyGame: director del juego.
 *
 * Responsabilidades:
 * - Crear la ventana (GLFW).
 * - Instanciar y conectar todas las demás clases.
 * - Ejecutar el bucle principal (input → lógica → render).
 *
 * Esta clase NO contiene lógica de física ni de dibujo;
 * solo orquesta a Bird, GameState, InputManager y Renderer.
 *
 * Controles:
 *   Jugador 1 → ESPACIO para saltar
 *   Jugador 2 → W para saltar
 *   R         → reiniciar (en game over)
 *   ESC       → salir
 */
public class FlappyGame {

    private static final int ANCHO = 900;
    private static final int ALTO  = 700;

    private long window;

    // Las piezas del juego
    private Bird         bird1;
    private Bird         bird2;
    private Bird         bird3;
    private GameState    state;
    private InputManager input;
    private Renderer     renderer;
    private SoundManager sound;

    // 0 = menú, 1 = un jugador, 2 = dos jugadores, 3 = tres jugadores
    private int jugadores = 0;

    // Opción resaltada en cada pantalla (0 = primera, 1 = segunda)
    private int opcionMenu     = 0; // 0=1 jugador, 1=2 jugadores, 2=3 jugadores
    private int opcionGameOver = 0; // 0=jugar de nuevo, 1=volver al menú

    // -------------------------------------------------------------------------
    // Flujo principal
    // -------------------------------------------------------------------------

    public void run() {
        init();
        loop();
        cleanup();
    }

    private void init() {
        // Ventana GLFW
        if (!GLFW.glfwInit()) throw new IllegalStateException("No se pudo iniciar GLFW");

        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE,                GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE,              GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR,  3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR,  3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE,         GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT,  GLFW.GLFW_TRUE);

        window = GLFW.glfwCreateWindow(ANCHO, ALTO, "Flappy Bird 2P", 0, 0);
        if (window == 0) throw new RuntimeException("No se pudo crear la ventana");

        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwSwapInterval(1);
        GLFW.glfwShowWindow(window);
        GL.createCapabilities();

        // Jugador 1: amarillo | Jugador 2: rojo-rosado | Jugador 3: celeste
        bird1    = new Bird(0.98f, 0.85f, 0.20f);
        bird2    = new Bird(0.90f, 0.25f, 0.35f);
        bird3    = new Bird(0.30f, 0.60f, 0.95f); // Agrego el 3cer jugador
        state    = new GameState();
        input    = new InputManager(window);
        renderer = new Renderer();
        renderer.init();
        sound    = new SoundManager();

        volverAlMenu();
    }

    private void loop() {
        float ultimoTiempo = (float) GLFW.glfwGetTime();

        while (!GLFW.glfwWindowShouldClose(window)) {
            float ahora = (float) GLFW.glfwGetTime();
            float dt    = Math.min(ahora - ultimoTiempo, 0.033f);
            ultimoTiempo = ahora;

            procesarInput();
            actualizar(dt);
            actualizarTitulo();
            renderer.render(state, bird1, bird2, bird3, ahora, jugadores, opcionMenu, opcionGameOver);

            GLFW.glfwSwapBuffers(window);
            GLFW.glfwPollEvents();
        }
    }

    // -------------------------------------------------------------------------
    // Lógica de input
    // -------------------------------------------------------------------------

    private void procesarInput() {
        if (input.escPresionado()) {
            GLFW.glfwSetWindowShouldClose(window, true);
            return;
        }

        // En menú: navegar con flechas y confirmar con ENTER, o teclas directas 1/2/3
        if (jugadores == 0) {
            if (input.arribaPresionado())  opcionMenu = Math.max(0, opcionMenu - 1);
            if (input.abajoPresionado())   opcionMenu = Math.min(2, opcionMenu + 1);
            if (input.confirmar())         iniciarJuego(opcionMenu + 1);
            if (input.elegir1Jugador())    iniciarJuego(1);
            if (input.elegir2Jugadores())  iniciarJuego(2);
            if (input.elegir3Jugadores())  iniciarJuego(3);
            return;
        }

        // En game over: navegar con flechas y confirmar con ENTER
        if (state.gameOver) {
            if (input.arribaPresionado()) opcionGameOver = 0;
            if (input.abajoPresionado())  opcionGameOver = 1;
            if (input.confirmar()) {
                if (opcionGameOver == 0) iniciarJuego(jugadores);
                else                     volverAlMenu();
            }
            if (input.reiniciar()) volverAlMenu();
            return;
        }

        // Durante el juego
        if (input.saltarJ1() && bird1.vivo) {
            state.started = true;
            bird1.saltar();
            sound.playJump();
        }
        if (jugadores >= 2 && input.saltarJ2() && bird2.vivo) {
            state.started = true;
            bird2.saltar();
            sound.playJump();
        }
        if (jugadores == 3 && input.saltarJ3() && bird3.vivo) {
            state.started = true;
            bird3.saltar();
            sound.playJump();
        }
    }

    // -------------------------------------------------------------------------
    // Lógica de actualización
    // -------------------------------------------------------------------------

    private void actualizar(float dt) {
        if (!state.started || state.gameOver) return;

        // Si algún jugador llegó al puntaje umbral, los vivos se elevan
        // hacia el techo hasta morir (termina la partida).
        int maxPuntaje   = Math.max(Math.max(bird1.puntaje, bird2.puntaje), bird3.puntaje);
        boolean elevarse = maxPuntaje >= Bird.PUNTAJE_ELEVACION;

        bird1.actualizar(dt, elevarse);
        if (jugadores >= 2) bird2.actualizar(dt, elevarse);
        if (jugadores == 3) bird3.actualizar(dt, elevarse);

        state.actualizarDificultad(maxPuntaje);

        // GameState no conoce a SoundManager (separación de responsabilidades).
        // Detecto los cambios comparando puntaje/gameOver ANTES y DESPUÉS de
        // state.actualizar: si crecieron, disparo el sonido correspondiente.
        int puntosBefore    = bird1.puntaje + bird2.puntaje + bird3.puntaje;
        boolean goBefore    = state.gameOver;

        state.actualizar(dt, bird1, bird2, bird3);

        if (bird1.puntaje + bird2.puntaje + bird3.puntaje > puntosBefore) sound.playPoint();
        if (!goBefore && state.gameOver)                                  sound.playGameOver();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Arranca una partida nueva con el modo elegido. */
    private void iniciarJuego(int modo) {
        jugadores      = modo;
        opcionGameOver = 0;

        // Resetear SIEMPRE los tres pájaros (puntaje=0, vivo=true) aunque no
        // jueguen, para que no quede puntaje viejo de la partida anterior.
        if (jugadores == 1) {
            bird1.reset(0.15f);
            bird2.reset(-0.15f);
            bird3.reset(-0.25f);
            bird2.vivo = false;   // en modo 1 jugador bird2/bird3 no existen
            bird3.vivo = false;
        } else if (jugadores == 2) {
            bird1.reset(0.15f);
            bird2.reset(-0.15f);
            bird3.reset(-0.25f);
            bird3.vivo = false;   // en modo 2 jugadores bird3 no existe
        } else { // 3 jugadores: posiciones repartidas para que no se encimen
            bird1.reset(0.25f);
            bird2.reset(0.0f);
            bird3.reset(-0.25f);
        }
        state.reset();
        input.reset();
        actualizarTitulo();
    }

    /** Vuelve al menú principal. */
    private void volverAlMenu() {
        jugadores  = 0;
        opcionMenu = 0;
        input.reset();
        actualizarTitulo();
    }

    private void actualizarTitulo() {
        if (jugadores == 0) {
            GLFW.glfwSetWindowTitle(window, "Flappy Bird  |  Presiona 1, 2 o 3 (jugadores)");
            return;
        }

        String base;
        if (jugadores == 1) {
            base = String.format("Flappy Bird  |  P1: %d  |  Nivel: %d", bird1.puntaje, state.nivel);
        } else if (jugadores == 2) {
            base = String.format("Flappy Bird 2P  |  P1: %d  |  P2: %d  |  Nivel: %d",
                    bird1.puntaje, bird2.puntaje, state.nivel);
        } else {
            base = String.format("Flappy Bird 3P  |  P1: %d  |  P2: %d  |  P3: %d  |  Nivel: %d",
                    bird1.puntaje, bird2.puntaje, bird3.puntaje, state.nivel);
        }

        if (!state.started) {
            GLFW.glfwSetWindowTitle(window, base + "  |  ESPACIO para empezar");
        } else if (state.gameOver) {
            GLFW.glfwSetWindowTitle(window, base + "  |  GAME OVER  —  cualquier tecla para volver");
        } else {
            GLFW.glfwSetWindowTitle(window, base);
        }
    }

    private void cleanup() {
        sound.cleanup();
        renderer.cleanup();
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }

    public static void main(String[] args) {
        new FlappyGame().run();
    }
}
