package com.graphics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * GameState: estado global de la partida.
 * Maneja las tuberías, el nivel de dificultad y las banderas de inicio/fin.
 */
public class GameState {

    // Parámetros de tuberías
    static final float TUBERIA_ANCHO          = 0.18f;
    static final float GAP_ALTO               = 0.48f;
    static final float GAP_MIN_CENTRO         = -0.45f;
    static final float GAP_MAX_CENTRO         =  0.45f;

    // Dificultad base y límites
    static final float VELOCIDAD_BASE         = 0.62f;
    static final float VELOCIDAD_MAX          = 1.20f;
    static final float TIEMPO_SPAWN_BASE      = 1.5f;
    static final float TIEMPO_SPAWN_MIN       = 0.8f;

    // Lista de tuberías activas (compartidas por ambos jugadores)
    final List<Pipe> tuberias = new ArrayList<>();
    private final Random random = new Random();

    // Estado de la partida
    boolean started;
    boolean gameOver;

    // Dificultad progresiva
    float velocidadActual;
    float tiempoEntreSpawns;
    int   nivel;

    private float timerSpawn;

    GameState() {
        reset();
    }

    /** Reinicia todo al estado inicial. */
    void reset() {
        tuberias.clear();
        started            = false;
        gameOver           = false;
        timerSpawn         = 0.0f;
        velocidadActual    = VELOCIDAD_BASE;
        tiempoEntreSpawns  = TIEMPO_SPAWN_BASE;
        nivel              = 1;
    }

    /**
     * Sube el nivel y ajusta velocidad/frecuencia cada 5 puntos del jugador líder.
     * Tiene un tope para que el juego siga siendo jugable.
     */
    void actualizarDificultad(int maxPuntaje) {
        int nuevoNivel = 1 + maxPuntaje / 5;
        if (nuevoNivel == nivel) return;

        nivel             = nuevoNivel;
        //escoge el minimo para q la velocidad no crezca infinitamente
        velocidadActual   = Math.min(VELOCIDAD_BASE + (nivel - 1) * 0.12f, VELOCIDAD_MAX);
        tiempoEntreSpawns = Math.max(TIEMPO_SPAWN_BASE - (nivel - 1) * 0.10f, TIEMPO_SPAWN_MIN);
    }

    /**
     * Actualiza tuberías cada frame:
     * - Genera nuevas según el timer.
     * - Mueve todas hacia la izquierda.
     * - Puntúa a los pájaros vivos que superen cada tubería.
     * - Detecta colisiones y marca pájaros muertos.
     * - Elimina tuberías fuera de pantalla.
     * - Declara game over cuando ambos pájaros están muertos.
     */
    void actualizar(float dt, Bird bird1, Bird bird2) {
        timerSpawn += dt;
        if (timerSpawn >= tiempoEntreSpawns) {
            timerSpawn = 0.0f;
            spawnTuberia();
        }

        Iterator<Pipe> it = tuberias.iterator();
        while (it.hasNext()) {
            Pipe p = it.next();
            p.x -= velocidadActual * dt;

            // Puntuar cuando la tubería queda atrás del pájaro
            if (p.x + (TUBERIA_ANCHO * 0.5f) < bird1.x && !p.puntuada) {
                p.puntuada = true;
                if (bird1.vivo) bird1.puntaje++;
                if (bird2.vivo) bird2.puntaje++;
            }

            // Colisión con cada pájaro
            if (bird1.vivo && bird1.colisionaCon(p, TUBERIA_ANCHO, GAP_ALTO)) bird1.vivo = false;
            if (bird2.vivo && bird2.colisionaCon(p, TUBERIA_ANCHO, GAP_ALTO)) bird2.vivo = false;

            // Remover tuberías que ya salieron de pantalla
            if (p.x + (TUBERIA_ANCHO * 0.5f) < -1.3f) it.remove();
        }

        // Game over solo cuando ambos pájaros están muertos
        if (!bird1.vivo && !bird2.vivo) gameOver = true;
    }

    private void spawnTuberia() {
        float gapCentro = GAP_MIN_CENTRO + random.nextFloat() * (GAP_MAX_CENTRO - GAP_MIN_CENTRO);
        tuberias.add(new Pipe(1.2f, gapCentro));
    }
}
