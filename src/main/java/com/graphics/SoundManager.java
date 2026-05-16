package com.graphics;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

/**
 * SoundManager: reproduce efectos de sonido del juego.
 *
 * Carga los WAV una sola vez al arrancar y los reproduce bajo demanda.
 * Si un archivo no existe o falla, el juego sigue funcionando sin sonido.
 */
public class SoundManager {

    private Clip jump;
    private Clip point;
    private Clip gameOver;

    SoundManager() {
        jump     = cargar("/sounds/jump.wav");
        point    = cargar("/sounds/point.wav");
        gameOver = cargar("/sounds/gameover.wav");
    }

    void playJump()     { tocar(jump);     }
    void playPoint()    { tocar(point);    }
    void playGameOver() { tocar(gameOver); }

    private void tocar(Clip clip) {
        if (clip == null) return;
        clip.setFramePosition(0); // rebobina para poder repetir el sonido
        clip.start();
    }

    private Clip cargar(String ruta) {
        try {
            var url    = getClass().getResource(ruta);
            var stream = AudioSystem.getAudioInputStream(url);
            var clip   = AudioSystem.getClip();
            clip.open(stream);
            return clip;
        } catch (Exception e) {
            System.out.println("SoundManager: no se pudo cargar " + ruta + " → " + e.getMessage());
            return null;
        }
    }

    void cleanup() {
        cerrar(jump);
        cerrar(point);
        cerrar(gameOver);
    }

    private void cerrar(Clip clip) {
        if (clip != null) clip.close();
    }
}
