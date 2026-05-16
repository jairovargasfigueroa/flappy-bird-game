package com.graphics;

/**
 * Pipe: datos de una tubería (obstáculo).
 * Las tuberías son compartidas por ambos jugadores.
 */
public class Pipe {

    float x;           // posición horizontal, se mueve de derecha a izquierda
    float gapCentroY;  // centro vertical del hueco por donde pasan los pájaros
    boolean puntuada;  // true cuando ya se sumó el punto, evita contarlo dos veces

    Pipe(float x, float gapCentroY) {
        this.x = x;
        this.gapCentroY = gapCentroY;
    }
}
