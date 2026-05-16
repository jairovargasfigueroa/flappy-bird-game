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
    private GameState    state;
    private InputManager input;
    private Renderer     renderer;
    private SoundManager sound;

    // 0 = menú, 1 = un jugador, 2 = dos jugadores
    private int jugadores = 0;

    // Opción resaltada en cada pantalla (0 = primera, 1 = segunda)
    private int opcionMenu     = 0; // 0=1 jugador, 1=2 jugadores
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

        // Jugador 1: amarillo | Jugador 2: rojo-rosado
        bird1    = new Bird(0.98f, 0.85f, 0.20f);
        bird2    = new Bird(0.90f, 0.25f, 0.35f);
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
            renderer.render(state, bird1, bird2, ahora, jugadores, opcionMenu, opcionGameOver);

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

        // En menú: navegar con flechas y confirmar con ENTER, o teclas directas 1/2
        if (jugadores == 0) {
            if (input.arribaPresionado())  opcionMenu = 0;
            if (input.abajoPresionado())   opcionMenu = 1;
            if (input.confirmar())         iniciarJuego(opcionMenu + 1);
            if (input.elegir1Jugador())    iniciarJuego(1);
            if (input.elegir2Jugadores())  iniciarJuego(2);
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
        if (jugadores == 2 && input.saltarJ2() && bird2.vivo) {
            state.started = true;
            bird2.saltar();
            sound.playJump();
        }
    }

    // -------------------------------------------------------------------------
    // Lógica de actualización
    // -------------------------------------------------------------------------

    private void actualizar(float dt) {
        if (!state.started || state.gameOver) return;

        bird1.actualizar(dt);
        if (jugadores == 2) bird2.actualizar(dt);

        int maxPuntaje = Math.max(bird1.puntaje, bird2.puntaje);
        state.actualizarDificultad(maxPuntaje);

        // GameState no conoce a SoundManager (separación de responsabilidades).
        // Detecto los cambios comparando puntaje/gameOver ANTES y DESPUÉS de
        // state.actualizar: si crecieron, disparo el sonido correspondiente.
        int puntosBefore    = bird1.puntaje + bird2.puntaje;
        boolean goBefore    = state.gameOver;

        state.actualizar(dt, bird1, bird2);

        if (bird1.puntaje + bird2.puntaje > puntosBefore) sound.playPoint();
        if (!goBefore && state.gameOver)                  sound.playGameOver();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Arranca una partida nueva con el modo elegido. */
    private void iniciarJuego(int modo) {
        jugadores      = modo;
        opcionGameOver = 0;
        bird1.reset(0.15f);
        if (jugadores == 2) {
            bird2.reset(-0.15f);
        } else {
            bird2.vivo = false; // en modo 1 jugador bird2 no existe
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
            GLFW.glfwSetWindowTitle(window, "Flappy Bird  |  Presiona 1 (un jugador) o 2 (dos jugadores)");
            return;
        }

        String base = jugadores == 1
            ? String.format("Flappy Bird  |  P1: %d  |  Nivel: %d", bird1.puntaje, state.nivel)
            : String.format("Flappy Bird 2P  |  P1: %d  |  P2: %d  |  Nivel: %d", bird1.puntaje, bird2.puntaje, state.nivel);

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
