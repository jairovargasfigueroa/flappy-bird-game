package com.graphics;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.stb.STBImage;

/**
 * Renderer: todo lo visual del juego.
 *
 * Responsabilidades:
 * - Compilar y mantener los shaders.
 * - Crear y mantener el quad base (VAO/VBO) reutilizable.
 * - Dibujar fondo, tuberías, pájaros y overlays cada frame.
 *
 * El truco central: un solo quad unitario en GPU se reutiliza para
 * todo lo que se dibuja, cambiando offset/scale/color por uniforms.
 */
public class Renderer {

    private int programa;
    private Background background;

    // Shader y geometría para dibujar texturas (imágenes PNG)
    private int programaTextura;
    private int vaoTextura, vboTextura;

    // IDs de las texturas cargadas en GPU
    private int texMenu, texGameOver, texDigits;

    // VAO/VBO dinámico para dibujar dígitos con UV personalizadas
    private int vaoDigito, vboDigito;

    // VAO/VBO del quad (rectángulo) — usado para cuerpo, ala, ojo, cola, tuberías
    private int vao;
    private int vbo;

    // VAO/VBO del triángulo — usado para el pico del pájaro
    private int vaoTriangulo;
    private int vboTriangulo;

    // Locations de los uniforms en el shader
    private int uOffset;
    private int uScale;
    private int uColor;
    private int uAngle;

    /** Inicializa shaders y geometría. Llamar una sola vez al arrancar. */
    void init() {
        crearShaders();
        crearQuadBase();
        crearTrianguloBase();
        background = new Background();
        background.init();
        crearShaderTextura();
        crearQuadTextura();
        texMenu     = cargarTextura("/textures/menu.png");
        texGameOver = cargarTextura("/textures/gameover.png");
        texDigits   = cargarTextura("/textures/digits.png");
        crearVaoDigito();
    }

    // -------------------------------------------------------------------------
    // Shaders
    // -------------------------------------------------------------------------

    private void crearShaders() {
        String vertexSrc = """
            #version 330 core
            layout (location = 0) in vec3 aPos;
            uniform vec2 uOffset;
            uniform vec2 uScale;
            uniform float uAngle;
            void main() {
                vec2 scaled = aPos.xy * uScale;
                float c = cos(uAngle);
                float s = sin(uAngle);
                vec2 rotated = vec2(c * scaled.x - s * scaled.y,
                                    s * scaled.x + c * scaled.y);
                gl_Position = vec4(rotated + uOffset, aPos.z, 1.0);
            }
            """;

        String fragmentSrc = """
            #version 330 core
            uniform vec3 uColor;
            out vec4 fragColor;
            void main() {
                fragColor = vec4(uColor, 1.0);
            }
            """;

        int vs = compilarShader(GL20.GL_VERTEX_SHADER,   vertexSrc,   "Vertex");
        int fs = compilarShader(GL20.GL_FRAGMENT_SHADER, fragmentSrc, "Fragment");

        programa = GL20.glCreateProgram();
        GL20.glAttachShader(programa, vs);
        GL20.glAttachShader(programa, fs);
        GL20.glLinkProgram(programa);

        if (GL20.glGetProgrami(programa, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException("Error al enlazar shaders: " + GL20.glGetProgramInfoLog(programa));
        }

        uOffset = GL20.glGetUniformLocation(programa, "uOffset");
        uScale  = GL20.glGetUniformLocation(programa, "uScale");
        uColor  = GL20.glGetUniformLocation(programa, "uColor");
        uAngle  = GL20.glGetUniformLocation(programa, "uAngle");

        GL20.glDeleteShader(vs);
        GL20.glDeleteShader(fs);
    }

    private int compilarShader(int tipo, String src, String nombre) {
        int shader = GL20.glCreateShader(tipo);
        GL20.glShaderSource(shader, src);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException(nombre + " shader: " + GL20.glGetShaderInfoLog(shader));
        }
        return shader;
    }

    // -------------------------------------------------------------------------
    // Geometría base
    // -------------------------------------------------------------------------

    /**
     * Crea un rectángulo unitario centrado en el origen (-0.5 a +0.5 en x e y).
     * Todos los objetos del juego se dibujan escalando y moviendo este quad.
     */
    private void crearQuadBase() {
        float[] vertices = {
            -0.5f, -0.5f, 0.0f,
             0.5f, -0.5f, 0.0f,
             0.5f,  0.5f, 0.0f,
            -0.5f, -0.5f, 0.0f,
             0.5f,  0.5f, 0.0f,
            -0.5f,  0.5f, 0.0f
        };

        vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);

        vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);

        FloatBuffer buf = BufferUtils.createFloatBuffer(vertices.length);
        buf.put(vertices).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buf, GL15.GL_STATIC_DRAW);

        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
    }

    /**
     * Triángulo unitario apuntando hacia la derecha — usado para el pico.
     * Vértices: izquierda-arriba, izquierda-abajo, derecha-centro.
     * Al escalarlo con uScale queda como un pico puntiagudo.
     */
    private void crearTrianguloBase() {
        float[] vertices = {
            -0.5f,  0.5f, 0.0f,   // arriba-izquierda
            -0.5f, -0.5f, 0.0f,   // abajo-izquierda
             0.5f,  0.0f, 0.0f    // punta derecha
        };

        vaoTriangulo = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoTriangulo);

        vboTriangulo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboTriangulo);

        FloatBuffer buf = BufferUtils.createFloatBuffer(vertices.length);
        buf.put(vertices).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buf, GL15.GL_STATIC_DRAW);

        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
    }

    /** VAO/VBO vacío para dígitos — la geometría cambia cada dígito (UV distintas). */
    private void crearVaoDigito() {
        vaoDigito = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoDigito);
        vboDigito = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboDigito);
        // 6 vértices × 5 floats (x, y, z, u, v)
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, 6 * 5 * Float.BYTES, GL15.GL_DYNAMIC_DRAW);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 5 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 5 * Float.BYTES, 3L * Float.BYTES);
        GL20.glEnableVertexAttribArray(1);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
    }

    // -------------------------------------------------------------------------
    // Render principal
    // -------------------------------------------------------------------------

    /**
     * Dibuja un frame completo: fondo → tuberías → pájaros → overlay.
     * tiempo: segundos desde que arrancó la app, usado para animar el ala.
     */
    void render(GameState state, Bird bird1, Bird bird2, float tiempo, int jugadores,
                int opcionMenu, int opcionGameOver) {
        GL11.glClearColor(0.52f, 0.80f, 0.92f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        // Fondo: degradado + nubes + suelo (shader propio)
        background.draw(tiempo);

        // Vuelve al shader principal para todo lo demás
        GL20.glUseProgram(programa);
        GL30.glBindVertexArray(vao);

        // Menú principal — no dibuja el juego
        if (jugadores == 0) {
            dibujarMenu(opcionMenu);
            return;
        }

        dibujarTuberias(state);
        dibujarPajaro(bird1, tiempo);
        if (jugadores == 2) dibujarPajaro(bird2, tiempo);

        if (!state.gameOver) {
            GL30.glBindVertexArray(vao);
            dibujarHUD(bird1, bird2, jugadores, state.nivel);
        }

        if (state.gameOver) {
            GL30.glBindVertexArray(vao);
            dibujarGameOver(bird1, bird2, jugadores, opcionGameOver);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers de dibujo
    // -------------------------------------------------------------------------

    /**
     * Pantalla de menú: imagen de fondo + resaltado sobre la opción seleccionada.
     * Si no hay textura cargada muestra el fallback de rectángulos.
     * AJUSTA las coordenadas Y del resaltado según donde pongas los botones en tu imagen.
     */
    private void dibujarMenu(int opcion) {
        if (texMenu != 0) {
            dibujarTextura(texMenu);
            // Borde amarillo sobre la opción activa — ajusta Y según tu imagen
            float y = (opcion == 0) ? 0.07f : -0.21f;
            dibujarBorde(0.0f, y, 0.70f, 0.12f, 1.0f, 0.85f, 0.0f);
        } else {
            // Fallback sin textura
            dibujarRect(0.0f,  0.0f,  2.0f,  2.0f,  0.08f, 0.10f, 0.14f);
            dibujarRect(-0.45f, 0.05f, 0.70f, 0.40f, 0.98f, 0.85f, 0.20f);
            dibujarRect( 0.45f, 0.05f, 0.70f, 0.40f, 0.90f, 0.25f, 0.35f);
            dibujarRect(0.0f,  0.05f,  0.015f, 0.45f, 1.0f,  1.0f,  1.0f);
        }
    }

    /**
     * Pantalla de game over: imagen de fondo + puntajes + resaltado de opción.
     * Si no hay textura muestra el fallback de rectángulos.
     * AJUSTA las coordenadas Y del resaltado según donde pongas los botones en tu imagen.
     */
    private void dibujarGameOver(Bird bird1, Bird bird2, int jugadores, int opcion) {
        if (texGameOver != 0) {
            dibujarTextura(texGameOver);

            // Puntaje como número real — ajusta Y según tu imagen
            dibujarNumero(bird1.puntaje, 0.0f, 0.05f, 0.12f);
            if (jugadores == 2)
                dibujarNumero(bird2.puntaje, 0.0f, -0.10f, 0.12f);

            // Borde amarillo sobre la opción activa — ajusta Y según tu imagen
            float y = (opcion == 0) ? -0.14f : -0.41f;
            dibujarBorde(0.0f, y, 0.60f, 0.10f, 1.0f, 0.85f, 0.0f);
        } else {
            // Fallback sin textura
            dibujarRect(0.0f, 0.0f, 1.30f, 0.55f, 0.10f, 0.12f, 0.15f);
            if (jugadores == 1) {
                dibujarRect(0.0f, 0.0f, 0.35f, 0.28f, bird1.r, bird1.g, bird1.b);
            } else {
                boolean p1Gana    = bird1.puntaje >= bird2.puntaje;
                Bird ganador      = p1Gana ? bird1 : bird2;
                Bird perdedor     = p1Gana ? bird2 : bird1;
                float ladoGanador  = p1Gana ? -0.18f :  0.18f;
                float ladoPerdedor = p1Gana ?  0.38f : -0.38f;
                dibujarRect(ladoGanador,  0.0f, 0.38f, 0.30f, ganador.r,  ganador.g,  ganador.b);
                dibujarRect(ladoPerdedor, 0.0f, 0.22f, 0.18f, perdedor.r, perdedor.g, perdedor.b);
            }
        }
    }

    /**
     * HUD en pantalla durante el juego.
     * 1 jugador: número del puntaje en la esquina superior izquierda.
     * 2 jugadores: cuadradito del color del pájaro + número por cada jugador.
     * Nivel: cuadrados amarillos apilados en la esquina derecha.
     */
    private void dibujarHUD(Bird bird1, Bird bird2, int jugadores, int nivel) {
        GL20.glUseProgram(programa);
        GL30.glBindVertexArray(vao);

        if (jugadores == 1) {
            dibujarNumero(bird1.puntaje, -0.70f, 0.87f, 0.10f);
        } else {
            float tam = 0.07f;
            // P1: cuadradito de color + número
            dibujarRect(-0.85f, 0.87f, tam, tam, bird1.r, bird1.g, bird1.b);
            dibujarNumero(bird1.puntaje, -0.68f, 0.87f, 0.10f);
            // P2: cuadradito de color + número
            dibujarRect(-0.85f, 0.76f, tam, tam, bird2.r, bird2.g, bird2.b);
            dibujarNumero(bird2.puntaje, -0.68f, 0.76f, 0.10f);
        }

        // Nivel: cuadrados apilados en esquina derecha
        for (int i = 0; i < nivel && i < 10; i++)
            dibujarRect(0.90f, 0.87f - i * 0.065f, 0.04f, 0.04f, 1.0f, 0.90f, 0.0f);
    }

    private void dibujarTuberias(GameState state) {
        for (Pipe p : state.tuberias) {
            float gapTop    = p.gapCentroY + (GameState.GAP_ALTO * 0.5f);
            float gapBottom = p.gapCentroY - (GameState.GAP_ALTO * 0.5f);

            float altoSup = 1.0f - gapTop;
            if (altoSup > 0.0f) {
                dibujarRect(p.x, gapTop + altoSup * 0.5f, GameState.TUBERIA_ANCHO, altoSup, 0.18f, 0.70f, 0.25f);
            }

            float altoInf = gapBottom + 1.0f;
            if (altoInf > 0.0f) {
                dibujarRect(p.x, -1.0f + altoInf * 0.5f, GameState.TUBERIA_ANCHO, altoInf, 0.18f, 0.70f, 0.25f);
            }
        }
    }

    /**
     * Dibuja el pájaro compuesto por varias figuras.
     * Todas las partes usan bird.y como base → se mueven juntas.
     * inclinacion: desplaza el pico y el ojo según la velocidad vertical,
     *              dando sensación de que el pájaro "mira" hacia donde va.
     */
    private void dibujarPajaro(Bird bird, float tiempo) {
        if (!bird.vivo) return;

        // Rotación real del cuerpo entero según velocidad vertical
        GL20.glUniform1f(uAngle, bird.velY * 0.35f);
        GL30.glBindVertexArray(vao);

        // Cuerpo principal
        dibujarRect(bird.x, bird.y, 0.10f, 0.09f, bird.r, bird.g, bird.b);

        // Cola (rectángulo oscuro a la izquierda del cuerpo)
        dibujarRect(bird.x - 0.065f, bird.y, 0.03f, 0.04f,
                bird.r * 0.7f, bird.g * 0.7f, bird.b * 0.7f);

        // Ala (oscila arriba/abajo con seno — efecto de aleteo)
        float alaY = bird.y + 0.045f + (float) Math.sin(tiempo * 8.0f) * 0.018f;
        dibujarRect(bird.x - 0.01f, alaY, 0.07f, 0.022f,
                bird.r * 0.85f, bird.g * 0.85f, bird.b * 0.85f);

        // Ojo (blanco)
        dibujarRect(bird.x + 0.02f, bird.y + 0.02f, 0.028f, 0.028f, 1f, 1f, 1f);

        // Pupila (negra, dentro del ojo)
        dibujarRect(bird.x + 0.026f, bird.y + 0.02f, 0.013f, 0.013f, 0f, 0f, 0f);



        // Pico (triángulo apuntando a la derecha)
        GL30.glBindVertexArray(vaoTriangulo);
        dibujarRect(bird.x + 0.07f, bird.y - 0.005f, 0.04f, 0.025f, 1.0f, 0.55f, 0.0f);

        dibujarRect(bird.x - 0.01f, bird.y - 0.055f, 0.02f, 0.03f, 1.0f, 0.55f, 0.0f);


        // Resetear ángulo para que tuberías y demás no roten
        GL20.glUniform1f(uAngle, 0f);
        GL30.glBindVertexArray(vao);
    }

    /**
     * Dibuja un rectángulo usando el quad base.
     * x, y   → centro del rectángulo en NDC
     * ancho, alto → tamaño en NDC
     * r, g, b → color
     */
    void dibujarRect(float x, float y, float ancho, float alto, float r, float g, float b) {
        GL20.glUniform2f(uOffset, x, y);
        GL20.glUniform2f(uScale,  ancho, alto);
        GL20.glUniform3f(uColor,  r, g, b);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
    }

    /**
     * Dibuja un número entero centrado en (cx, cy).
     * Cada dígito se toma de digits.png usando la región UV correspondiente:
     * dígito N → UV x de N*0.1 a (N+1)*0.1.
     * tamDigito controla el ancho de cada dígito en NDC.
     */
    private void dibujarNumero(int numero, float cx, float cy, float tamDigito) {
        if (texDigits == 0) return;
        String s = String.valueOf(numero);
        float startX = cx - (s.length() * tamDigito) * 0.5f + tamDigito * 0.5f;

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL20.glUseProgram(programaTextura);
        GL30.glBindVertexArray(vaoDigito);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texDigits);

        for (int i = 0; i < s.length(); i++) {
            int d = s.charAt(i) - '0';
            subirYDibujarDigito(d, startX + i * tamDigito, cy, tamDigito * 0.85f, tamDigito * 1.8f);
        }

        GL11.glDisable(GL11.GL_BLEND);
        GL20.glUseProgram(programa);
        GL30.glBindVertexArray(vao);
    }

    /** Sube la geometría del dígito al VBO dinámico y lo dibuja. */
    private void subirYDibujarDigito(int d, float x, float y, float w, float h) {
        float u0 = d * 0.1f, u1 = u0 + 0.1f;
        float hw = w * 0.5f, hh = h * 0.5f;
        float[] v = {
            x - hw, y - hh, 0f,  u0, 0f,
            x + hw, y - hh, 0f,  u1, 0f,
            x + hw, y + hh, 0f,  u1, 1f,
            x - hw, y - hh, 0f,  u0, 0f,
            x + hw, y + hh, 0f,  u1, 1f,
            x - hw, y + hh, 0f,  u0, 1f,
        };
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboDigito);
        FloatBuffer buf = BufferUtils.createFloatBuffer(30);
        buf.put(v).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buf, GL15.GL_DYNAMIC_DRAW);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
    }

    /** Dibuja solo el marco (4 líneas) de un rectángulo sin rellenar el interior. */
    private void dibujarBorde(float x, float y, float ancho, float alto, float r, float g, float b) {
        float grosor = 0.008f;
        dibujarRect(x,              y + alto * 0.5f, ancho,  grosor, r, g, b); // arriba
        dibujarRect(x,              y - alto * 0.5f, ancho,  grosor, r, g, b); // abajo
        dibujarRect(x - ancho * 0.5f, y,             grosor, alto,   r, g, b); // izquierda
        dibujarRect(x + ancho * 0.5f, y,             grosor, alto,   r, g, b); // derecha
    }

    // -------------------------------------------------------------------------
    // Sistema de texturas
    // -------------------------------------------------------------------------

    private void crearShaderTextura() {
        String vertSrc = """
            #version 330 core
            layout(location = 0) in vec3 aPos;
            layout(location = 1) in vec2 aTexCoord;
            out vec2 vTexCoord;
            void main() {
                gl_Position = vec4(aPos, 1.0);
                vTexCoord = aTexCoord;
            }
            """;
        String fragSrc = """
            #version 330 core
            in vec2 vTexCoord;
            uniform sampler2D uTextura;
            out vec4 fragColor;
            void main() {
                fragColor = texture(uTextura, vTexCoord);
            }
            """;
        int vs = compilarShader(GL20.GL_VERTEX_SHADER,   vertSrc, "Tex Vertex");
        int fs = compilarShader(GL20.GL_FRAGMENT_SHADER, fragSrc, "Tex Fragment");
        programaTextura = GL20.glCreateProgram();
        GL20.glAttachShader(programaTextura, vs);
        GL20.glAttachShader(programaTextura, fs);
        GL20.glLinkProgram(programaTextura);
        GL20.glDeleteShader(vs);
        GL20.glDeleteShader(fs);
    }

    /** Quad de pantalla completa con coordenadas UV para mostrar la textura. */
    private void crearQuadTextura() {
        float[] v = {
        //   x      y     z    u    v
            -1f,  -1f,  0f,  0f,  0f,
             1f,  -1f,  0f,  1f,  0f,
             1f,   1f,  0f,  1f,  1f,
            -1f,  -1f,  0f,  0f,  0f,
             1f,   1f,  0f,  1f,  1f,
            -1f,   1f,  0f,  0f,  1f,
        };
        vaoTextura = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoTextura);
        vboTextura = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboTextura);
        FloatBuffer buf = BufferUtils.createFloatBuffer(v.length);
        buf.put(v).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buf, GL15.GL_STATIC_DRAW);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 5 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 5 * Float.BYTES, 3L * Float.BYTES);
        GL20.glEnableVertexAttribArray(1);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
    }

    /** Carga un PNG desde resources y lo sube a la GPU. Devuelve 0 si falla. */
    private int cargarTextura(String ruta) {
        try {
            InputStream is = getClass().getResourceAsStream(ruta);
            if (is == null) { System.out.println("Textura no encontrada: " + ruta); return 0; }
            byte[] bytes = is.readAllBytes();
            ByteBuffer buf = BufferUtils.createByteBuffer(bytes.length);
            buf.put(bytes).flip();
            int[] w = new int[1], h = new int[1], comp = new int[1];
            STBImage.stbi_set_flip_vertically_on_load(true);
            ByteBuffer data = STBImage.stbi_load_from_memory(buf, w, h, comp, 4);
            if (data == null) { System.out.println("Error textura " + ruta + ": " + STBImage.stbi_failure_reason()); return 0; }
            int id = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, w[0], h[0], 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, data);
            STBImage.stbi_image_free(data);
            return id;
        } catch (Exception e) {
            System.out.println("Error cargando textura " + ruta + ": " + e.getMessage());
            return 0;
        }
    }

    /** Dibuja la textura en pantalla completa y restaura el shader principal. */
    private void dibujarTextura(int texId) {
        GL20.glUseProgram(programaTextura);
        GL30.glBindVertexArray(vaoTextura);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
        GL20.glUseProgram(programa);
        GL30.glBindVertexArray(vao);
    }

    // -------------------------------------------------------------------------
    // Limpieza
    // -------------------------------------------------------------------------

    /** Libera recursos de GPU. */
    void cleanup() {
        background.cleanup();
        if (texMenu     != 0) GL11.glDeleteTextures(texMenu);
        if (texGameOver != 0) GL11.glDeleteTextures(texGameOver);
        if (texDigits   != 0) GL11.glDeleteTextures(texDigits);
        GL30.glDeleteVertexArrays(vaoDigito);
        GL15.glDeleteBuffers(vboDigito);
        GL30.glDeleteVertexArrays(vaoTextura);
        GL15.glDeleteBuffers(vboTextura);
        GL20.glDeleteProgram(programaTextura);
        GL30.glDeleteVertexArrays(vao);
        GL15.glDeleteBuffers(vbo);
        GL30.glDeleteVertexArrays(vaoTriangulo);
        GL15.glDeleteBuffers(vboTriangulo);
        GL20.glDeleteProgram(programa);
    }
}
