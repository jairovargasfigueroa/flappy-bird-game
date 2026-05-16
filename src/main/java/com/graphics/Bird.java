package com.graphics;

/**
 * Bird: estado y física de un pájaro.
 * Cada jugador tiene su propia instancia con posición, velocidad y puntaje independientes.
 */
public class Bird {

    // Tamaño del pájaro en NDC
    static final float ANCHO = 0.10f;
    static final float ALTO  = 0.10f;

    // Parámetros de física
    static final float GRAVEDAD         = -1.9f;
    static final float IMPULSO_SALTO    =  0.85f;
    static final float VELOCIDAD_MAX_CAIDA = -1.8f;

    // Posición horizontal fija (ambos jugadores comparten el mismo x)
    final float x = -0.45f;

    // Estado que cambia cada frame
    float y;
    float velY;
    boolean vivo;
    int puntaje;

    // Color del pájaro (para distinguir visualmente a cada jugador)
    final float r, g, b;

    Bird(float r, float g, float b) {
        this.r = r;
        this.g = g;
        this.b = b;
        reset(0.0f);
    }

    /** Reinicia el pájaro a su estado inicial. */
    void reset(float yInicial) {
        y     = yInicial;
        velY  = 0.0f;
        vivo  = true;
        puntaje = 0;
    }

    /** Aplica el impulso de salto. */
    void saltar() {
        // Asigna (no suma): el salto siempre tiene la misma fuerza aunque el
        // pájaro venga cayendo rápido. Es el comportamiento del Flappy original.
        velY = IMPULSO_SALTO;
    }

    /**
     * Actualiza física vertical.
     * Si toca el techo o el suelo, marca al pájaro como muerto.
     */
    void actualizar(float dt) {
        if (!vivo) return;

        velY += GRAVEDAD * dt; // Para q caiga mas rapido segun la gravedad
        if (velY < VELOCIDAD_MAX_CAIDA) velY = VELOCIDAD_MAX_CAIDA;// para q no caiga tan
        y += velY * dt;

        float top    = y + (ALTO * 0.5f);
        float bottom = y - (ALTO * 0.5f);
        if (top >= 1.0f || bottom <= -1.0f) {
            vivo = false;
        }
    }

    /**
     * Colisión AABB entre este pájaro y una tubería.
     * Devuelve true si hay colisión.
     */
    boolean colisionaCon(Pipe p, float tubAncho, float gapAlto) {
        float birdLeft   = x - (ANCHO * 0.5f);
        float birdRight  = x + (ANCHO * 0.5f);
        float birdBottom = y - (ALTO  * 0.5f);
        float birdTop    = y + (ALTO  * 0.5f);

        // Fase 1 — descarte rápido: si no se solapan en X es imposible chocar,
        // así evito calcular el hueco (optimización).
        float pipeLeft  = p.x - (tubAncho * 0.5f);
        float pipeRight = p.x + (tubAncho * 0.5f);
        boolean overlapX = birdRight > pipeLeft && birdLeft < pipeRight;
        if (!overlapX) return false;

        // Fase 2 — en vez de detectar los dos tubos, detecto si el pájaro se
        // salió del hueco (por arriba o por abajo). Más simple que revisar
        // dos rectángulos por separado.
        float gapTop    = p.gapCentroY + (gapAlto * 0.5f);
        float gapBottom = p.gapCentroY - (gapAlto * 0.5f);
        return birdTop > gapTop || birdBottom < gapBottom;
    }
}
