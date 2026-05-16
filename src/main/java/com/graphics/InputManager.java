package com.graphics;

import org.lwjgl.glfw.GLFW;

/**
 * InputManager: lectura del teclado para ambos jugadores.
 *
 * Usa detección de flanco (previo vs actual) para que una pulsación
 * larga cuente como un solo evento, no como múltiples.
 *
 * Jugador 1: ESPACIO
 * Jugador 2: W
 */
public class InputManager {

    private final long window;

    private boolean prevSpace;
    private boolean prevW;
    private boolean prevR;
    private boolean prev1;
    private boolean prev2;
    private boolean prevUp;
    private boolean prevDown;
    private boolean prevEnter;

    InputManager(long window) {
        this.window = window;
    }

    /** Reinicia el estado previo de todas las teclas. */
    void reset() {
        prevSpace = false;
        prevW     = false;
        prevR     = false;
        prev1     = false;
        prev2     = false;
        prevUp    = false;
        prevDown  = false;
        // prevEnter NO se resetea — evita que ENTER se re-dispare al cambiar de pantalla
    }

    boolean escPresionado() {
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS;
    }

    /** Devuelve true solo en el frame en que ESPACIO se presionó. */
    boolean saltarJ1() {
        boolean ahora  = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS;
        boolean flanco = ahora && !prevSpace;
        prevSpace = ahora;
        return flanco;
    }

    /** Devuelve true solo en el frame en que W se presionó. */
    boolean saltarJ2() {
        boolean ahora  = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS;
        boolean flanco = ahora && !prevW;
        prevW = ahora;
        return flanco;
    }

    /** Devuelve true solo en el frame en que R se presionó. */
    boolean reiniciar() {
        boolean ahora  = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_R) == GLFW.GLFW_PRESS;
        boolean flanco = ahora && !prevR;
        prevR = ahora;
        return flanco;
    }

    /** Devuelve true solo en el frame en que 1 se presionó (menú: 1 jugador). */
    boolean elegir1Jugador() {
        boolean ahora  = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_1) == GLFW.GLFW_PRESS;
        boolean flanco = ahora && !prev1;
        prev1 = ahora;
        return flanco;
    }

    /** Devuelve true solo en el frame en que 2 se presionó (menú: 2 jugadores). */
    boolean elegir2Jugadores() {
        boolean ahora  = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_2) == GLFW.GLFW_PRESS;
        boolean flanco = ahora && !prev2;
        prev2 = ahora;
        return flanco;
    }

    /** Flecha arriba — navegar opciones del menú. */
    boolean arribaPresionado() {
        boolean ahora  = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_UP) == GLFW.GLFW_PRESS;
        boolean flanco = ahora && !prevUp;
        prevUp = ahora;
        return flanco;
    }

    /** Flecha abajo — navegar opciones del menú. */
    boolean abajoPresionado() {
        boolean ahora  = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_DOWN) == GLFW.GLFW_PRESS;
        boolean flanco = ahora && !prevDown;
        prevDown = ahora;
        return flanco;
    }

    /** ENTER — confirmar opción seleccionada. */
    boolean confirmar() {
        boolean ahora  = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_ENTER) == GLFW.GLFW_PRESS;
        boolean flanco = ahora && !prevEnter;
        prevEnter = ahora;
        return flanco;
    }
}
