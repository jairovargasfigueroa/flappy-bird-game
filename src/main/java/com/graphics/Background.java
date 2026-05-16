package com.graphics;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * Background: renderiza el fondo del juego.
 *
 * Responsabilidades:
 * - Cielo con degradado (color por vértice — OpenGL interpola automáticamente).
 * - Nubes que se desplazan lentamente hacia la izquierda.
 * - Franja de suelo verde en la parte inferior.
 *
 * Usa su propio shader porque necesita color por vértice,
 * a diferencia del shader principal que usa un color uniforme por figura.
 */
public class Background {

    private int programa;

    // VAO/VBO del quad de cielo — geometría fija, se sube una sola vez
    private int vaoGradiente;
    private int vboGradiente;

    // VAO/VBO reutilizable para suelo y nubes (se actualiza cada draw call)
    private int vaoRect;
    private int vboRect;

    void init() {
        crearShader();
        crearGradiente();
        crearVaoRect();
    }

    // -------------------------------------------------------------------------
    // Render
    // -------------------------------------------------------------------------

    /**
     * Dibuja el fondo completo: cielo degradado → nubes → suelo.
     * tiempo: segundos desde el inicio, usado para desplazar las nubes.
     */
    void draw(float tiempo) {
        GL20.glUseProgram(programa);

        // Cielo con degradado (quad estático)
        GL30.glBindVertexArray(vaoGradiente);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);

        // Nubes: aparecen por la derecha, salen por la izquierda (igual que tuberías)
        // cycle=3.0 cubre pantalla + margen fuera de pantalla → el salto es invisible
        float speed = 0.08f;
        float cycle = 3.0f;
        float c1x = 1.5f - (tiempo * speed)        % cycle;
        float c2x = 1.5f - (tiempo * speed + 1.2f) % cycle;
        float c3x = 1.5f - (tiempo * speed + 2.1f) % cycle;
        dibujarNube(c1x, 0.65f);
        dibujarNube(c2x, 0.50f);
        dibujarNube(c3x, 0.35f);

        // Suelo verde en la parte inferior
        dibujarRect(0f, -0.92f, 2f, 0.16f, 0.25f, 0.65f, 0.20f);
    }

    // -------------------------------------------------------------------------
    // Helpers de dibujo
    // -------------------------------------------------------------------------

    /** Una nube = 3 rectángulos blancos agrupados. */
    private void dibujarNube(float x, float y) {
        dibujarRect(x,        y,        0.25f, 0.08f, 1f, 1f, 1f);
        dibujarRect(x + 0.06f, y + 0.05f, 0.15f, 0.06f, 1f, 1f, 1f);
        dibujarRect(x - 0.06f, y + 0.05f, 0.15f, 0.06f, 1f, 1f, 1f);
    }

    /**
     * Dibuja un rectángulo sólido usando el shader de color por vértice.
     * Sube los vértices al VBO dinámico cada vez (permite posición variable).
     */
    private void dibujarRect(float x, float y, float w, float h,
                              float r, float g, float b) {
        float hw = w * 0.5f;
        float hh = h * 0.5f;
        float[] v = {
            x - hw, y - hh, 0f,  r, g, b,
            x + hw, y - hh, 0f,  r, g, b,
            x + hw, y + hh, 0f,  r, g, b,
            x - hw, y - hh, 0f,  r, g, b,
            x + hw, y + hh, 0f,  r, g, b,
            x - hw, y + hh, 0f,  r, g, b,
        };

        GL30.glBindVertexArray(vaoRect);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboRect);

        FloatBuffer buf = BufferUtils.createFloatBuffer(v.length);
        buf.put(v).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buf, GL15.GL_DYNAMIC_DRAW);

        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
    }

    // -------------------------------------------------------------------------
    // Inicialización
    // -------------------------------------------------------------------------

    private void crearShader() {
        String vertSrc = """
            #version 330 core
            layout(location = 0) in vec3 aPos;
            layout(location = 1) in vec3 aColor;
            out vec3 vColor;
            void main() {
                gl_Position = vec4(aPos, 1.0);
                vColor = aColor;
            }
            """;

        String fragSrc = """
            #version 330 core
            in vec3 vColor;
            out vec4 fragColor;
            void main() {
                fragColor = vec4(vColor, 1.0);
            }
            """;

        int vs = compilar(GL20.GL_VERTEX_SHADER,   vertSrc, "BG Vertex");
        int fs = compilar(GL20.GL_FRAGMENT_SHADER, fragSrc, "BG Fragment");

        programa = GL20.glCreateProgram();
        GL20.glAttachShader(programa, vs);
        GL20.glAttachShader(programa, fs);
        GL20.glLinkProgram(programa);
        GL20.glDeleteShader(vs);
        GL20.glDeleteShader(fs);
    }

    private int compilar(int tipo, String src, String nombre) {
        int shader = GL20.glCreateShader(tipo);
        GL20.glShaderSource(shader, src);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException(nombre + ": " + GL20.glGetShaderInfoLog(shader));
        }
        return shader;
    }

    /**
     * Quad de cielo: abajo celeste claro, arriba azul oscuro.
     * Los colores de los vértices superiores e inferiores son distintos →
     * OpenGL interpola automáticamente entre ellos al rasterizar.
     */
    private void crearGradiente() {
        float[] v = {
        //   x      y     z      r      g      b
            -1f,  -1f,  0f,  0.52f, 0.80f, 0.92f,  // abajo-izq  celeste
             1f,  -1f,  0f,  0.52f, 0.80f, 0.92f,  // abajo-der  celeste
             1f,   1f,  0f,  0.15f, 0.35f, 0.70f,  // arriba-der azul oscuro
            -1f,  -1f,  0f,  0.52f, 0.80f, 0.92f,
             1f,   1f,  0f,  0.15f, 0.35f, 0.70f,
            -1f,   1f,  0f,  0.15f, 0.35f, 0.70f,  // arriba-izq azul oscuro
        };

        vaoGradiente = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoGradiente);

        vboGradiente = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboGradiente);

        FloatBuffer buf = BufferUtils.createFloatBuffer(v.length);
        buf.put(v).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buf, GL15.GL_STATIC_DRAW);

        // aPos: location 0 — 3 floats, stride = 6 floats
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 6 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);

        // aColor: location 1 — 3 floats, offset = 3 floats después de aPos
        GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, 6 * Float.BYTES, 3L * Float.BYTES);
        GL20.glEnableVertexAttribArray(1);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
    }

    /** VAO/VBO vacío para rectángulos dinámicos (suelo y nubes). */
    private void crearVaoRect() {
        vaoRect = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoRect);

        vboRect = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboRect);

        // Reserva espacio para 6 vértices × 6 floats (posición + color)
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) 6 * 6 * Float.BYTES, GL15.GL_DYNAMIC_DRAW);

        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 6 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);

        GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, 6 * Float.BYTES, 3L * Float.BYTES);
        GL20.glEnableVertexAttribArray(1);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
    }

    // -------------------------------------------------------------------------
    // Limpieza
    // -------------------------------------------------------------------------

    void cleanup() {
        GL30.glDeleteVertexArrays(vaoGradiente);
        GL15.glDeleteBuffers(vboGradiente);
        GL30.glDeleteVertexArrays(vaoRect);
        GL15.glDeleteBuffers(vboRect);
        GL20.glDeleteProgram(programa);
    }
}
