package com.darkona.tardigrade;

import com.darkona.tardigrade.configuration.TardigradeConfiguration;
import com.darkona.tardigrade.recording.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Servidor Tardigrade embebido")
class TardigradeServerTest {

    private static final String CUERPO_JSON = "{\"cliente\":42,\"nombre\":\"Nandu\"}";
    private static final String RESPUESTA_MOCK = "{\"id\":42,\"estado\":\"OK\"}";
    private static final String CABECERA_CORRELACION = "X-CORRELACION-ID";

    @TempDir
    Path carpeta;

    private TardigradeServer servidor;
    private HttpClient cliente;

    @BeforeEach
    void levantarServidor() throws IOException {
        Path entrada = Files.createDirectories(carpeta.resolve("input"));
        Files.writeString(entrada.resolve("cliente.json"), RESPUESTA_MOCK);
        Files.writeString(carpeta.resolve("configuration.yml"),
                "mocks:\n  - path: /api/clientes/42\n    file: cliente.json\n");

        servidor = new TardigradeServer(configuracion(entrada));
        servidor.start();
        cliente = HttpClient.newHttpClient();
    }

    @AfterEach
    void detenerServidor() {
        servidor.stop();
    }

    private TardigradeConfiguration configuracion(Path entrada) {
        try {
            return new TardigradeConfiguration(new String[]{
                    "-p", "0",
                    "-c", carpeta.resolve("configuration.yml").toString(),
                    "-i", entrada.toString(),
                    "-o", carpeta.resolve("output").toString(),
                    "-d", "color"
            });
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private HttpResponse<String> enviar(HttpRequest peticion) throws Exception {
        return cliente.send(peticion, HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest get(String ruta) {
        return HttpRequest.newBuilder(URI.create(servidor.baseUrl() + ruta)).build();
    }

    @Nested
    @DisplayName("Ciclo de vida")
    class CicloDeVida {

        @Test
        @DisplayName("Toma un puerto libre cuando se configura el puerto 0")
        void tomaPuertoLibre() {
            assertTrue(servidor.port() > 0, "Deberia haber tomado un puerto real");
            assertTrue(servidor.isRunning());
            assertEquals("http://localhost:" + servidor.port(), servidor.baseUrl());
        }

        @Test
        @DisplayName("Rechaza arrancar dos veces la misma instancia")
        void rechazaDobleArranque() {
            assertThrows(IllegalStateException.class, () -> servidor.start());
        }

        @Test
        @DisplayName("Libera el puerto al detenerse y permite volver a arrancar")
        void liberaElPuertoAlDetenerse() throws IOException {
            servidor.stop();

            assertFalse(servidor.isRunning());
            assertEquals(-1, servidor.port());

            servidor.start();

            assertTrue(servidor.isRunning());
            assertTrue(servidor.port() > 0, "Deberia haber vuelto a bindear");
        }

        @Test
        @DisplayName("Dos instancias conviven en la misma JVM con puertos distintos")
        void dosInstanciasConviven() throws IOException {
            TardigradeServer otro = new TardigradeServer(configuracion(carpeta.resolve("input")));
            try {
                otro.start();
                assertNotEquals(servidor.port(), otro.port());
            } finally {
                otro.stop();
            }
        }
    }

    @Nested
    @DisplayName("Registro de peticiones")
    class RegistroDePeticiones {

        @Test
        @DisplayName("Registra metodo, ruta y cuerpo de lo que recibe")
        void registraLaPeticion() throws Exception {
            enviar(HttpRequest.newBuilder(URI.create(servidor.baseUrl() + "/api/pagos"))
                    .POST(HttpRequest.BodyPublishers.ofString(CUERPO_JSON))
                    .build());

            RecordedRequest recibida = servidor.requests().last().orElseThrow();

            assertEquals("POST", recibida.method());
            assertEquals("/api/pagos", recibida.path());
            assertEquals(CUERPO_JSON, recibida.body());
        }

        @Test
        @DisplayName("Conserva las cabeceras y las busca sin distinguir mayusculas")
        void conservaLasCabeceras() throws Exception {
            enviar(HttpRequest.newBuilder(URI.create(servidor.baseUrl() + "/api/pagos"))
                    .header(CABECERA_CORRELACION, "abc-123")
                    .POST(HttpRequest.BodyPublishers.ofString(CUERPO_JSON))
                    .build());

            RecordedRequest recibida = servidor.requests().last().orElseThrow();

            assertTrue(recibida.hasHeader(CABECERA_CORRELACION));
            assertEquals(Optional.of("abc-123"), recibida.header("x-correlacion-id"));
        }

        @Test
        @DisplayName("Separa la query string de la ruta")
        void separaLaQueryString() throws Exception {
            enviar(get("/api/clientes?activo=true"));

            RecordedRequest recibida = servidor.requests().last().orElseThrow();

            assertEquals("/api/clientes", recibida.path());
            assertEquals("activo=true", recibida.query());
        }

        @Test
        @DisplayName("Cuenta las peticiones por ruta")
        void cuentaPorRuta() throws Exception {
            enviar(get("/api/uno"));
            enviar(get("/api/dos"));
            enviar(get("/api/uno"));

            assertEquals(3, servidor.requests().count());
            assertEquals(2, servidor.requests().count("/api/uno"));
            assertEquals(1, servidor.requests().count("/api/dos"));
        }

        @Test
        @DisplayName("Queda vacio despues de limpiarlo")
        void quedaVacioTrasLimpiar() throws Exception {
            enviar(get("/api/uno"));

            servidor.requests().clear();

            assertTrue(servidor.requests().isEmpty());
        }
    }

    @Nested
    @DisplayName("Respuestas definidas")
    class RespuestasDefinidas {

        @Test
        @DisplayName("Responde el archivo configurado para la ruta")
        void respondeElMock() throws Exception {
            HttpResponse<String> respuesta = enviar(get("/api/clientes/42"));

            assertEquals(200, respuesta.statusCode());
            assertEquals(RESPUESTA_MOCK, respuesta.body());
        }

        @Test
        @DisplayName("Registra tambien las peticiones que responde un mock")
        void registraLasPeticionesConMock() throws Exception {
            enviar(get("/api/clientes/42"));

            assertEquals(1, servidor.requests().count("/api/clientes/42"));
        }

        @Test
        @DisplayName("Acusa recibo en las rutas sin mock definido")
        void acusaReciboSinMock() throws Exception {
            HttpResponse<String> respuesta = enviar(get("/ruta/sin/mock"));

            assertEquals(200, respuesta.statusCode());
            assertTrue(respuesta.body().contains("logged"));
        }
    }
}
