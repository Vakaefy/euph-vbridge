package su.plo.voice.euphvbridge;

import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

final class Provodnik {

    private static final byte[] MAGIYA = {'P', 'V', 'W', '2'};
    private static final int VERSIYA = 2;
    private static final int SKOLKO = 255;
    private static final int RAZMER_SOOBSHENIYA = 65535;
    private static final int LIMIT_OCHEREDI = 64;

    private final String kuda;
    private final String chuzhoyHost;
    private final int chuzhoyPort;
    private final Runnable priObryve;
    private final DatagramSocket dyrka;
    private final HttpClient setka;
    private final AtomicInteger vOchere = new AtomicInteger();

    private volatile WebSocket soedinenie;
    private volatile InetSocketAddress ktoTam;
    private volatile long posledniyPing;
    private volatile long pingMs;
    private volatile boolean umer;
    private Thread niti;
    private ScheduledExecutorService tiktak;

    Provodnik(String kuda, String chuzhoyHost, int chuzhoyPort, Runnable priObryve) throws Exception {
        this.kuda = kuda;
        this.chuzhoyHost = chuzhoyHost;
        this.chuzhoyPort = chuzhoyPort;
        this.priObryve = priObryve;
        this.dyrka = new DatagramSocket(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        this.setka = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    int kakoyPort() {
        return dyrka.getLocalPort();
    }

    void zapuskay(long skolkoZhdat) throws Exception {
        CompletableFuture<WebSocket> budet = setka.newWebSocketBuilder()
                .connectTimeout(Duration.ofMillis(skolkoZhdat))
                .buildAsync(URI.create(kuda), new Slukatel());

        WebSocket ws = budet.get(skolkoZhdat, TimeUnit.MILLISECONDS);
        this.soedinenie = ws;

        byte[] host = chuzhoyHost == null ? new byte[0]
                : chuzhoyHost.getBytes(StandardCharsets.UTF_8);
        if (host.length > SKOLKO) {
            host = Arrays.copyOf(host, SKOLKO);
        }

        ByteBuffer hello = ByteBuffer.allocate(MAGIYA.length + 4 + host.length);
        hello.put(MAGIYA).put((byte) VERSIYA)
                .putShort((short) chuzhoyPort)
                .put((byte) host.length)
                .put(host);
        hello.flip();
        ws.sendBinary(hello, true).join();

        this.niti = new Thread(this::kruzhit, "euph-vbridge-udp");
        this.niti.setDaemon(true);
        this.niti.start();

        this.tiktak = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "euph-vbridge-ping");
            t.setDaemon(true);
            return t;
        });
        this.tiktak.scheduleAtFixedRate(this::puk, 500L, 1000L, TimeUnit.MILLISECONDS);
    }

    private void puk() {
        WebSocket ws = soedinenie;
        if (ws == null || umer) return;

        try {
            long metka = System.nanoTime();
            posledniyPing = metka;
            ByteBuffer payload = ByteBuffer.allocate(Long.BYTES).putLong(metka);
            payload.flip();
            ws.sendPing(payload).whenComplete((rezultat, oshibka) -> {
                if (oshibka != null) zamri(true);
            });
        } catch (Exception ignored) {
            zamri(true);
        }
    }

    private void kruzhit() {
        byte[] korobka = new byte[65535];
        while (!umer) {
            try {
                DatagramPacket p = new DatagramPacket(korobka, korobka.length);
                dyrka.receive(p);

                ktoTam = new InetSocketAddress(p.getAddress(), p.getPort());

                WebSocket ws = soedinenie;
                if (ws != null) {
                    otprav(ws, Arrays.copyOf(p.getData(), p.getLength()));
                }
            } catch (Exception e) {
                if (umer) return;
            }
        }
    }

    private void otprav(WebSocket ws, byte[] paket) {
        if (umer || vOchere.incrementAndGet() > LIMIT_OCHEREDI) {
            vOchere.decrementAndGet();
            return;
        }

        try {
            ws.sendBinary(ByteBuffer.wrap(paket), true).whenComplete((rezultat, oshibka) -> {
                vOchere.decrementAndGet();
                if (oshibka != null) zamri(true);
            });
        } catch (Exception ignored) {
            vOchere.decrementAndGet();
            zamri(true);
        }
    }

    void zamri() {
        zamri(false);
    }

    private void zamri(boolean sboy) {
        if (umer) return;
        umer = true;

        ScheduledExecutorService t = tiktak;
        if (t != null) t.shutdownNow();

        dyrka.close();

        WebSocket ws = soedinenie;
        if (ws != null) {
            try {
                ws.abort();
            } catch (Exception ignored) {
            }
        }

        if (sboy && priObryve != null) priObryve.run();
    }

    boolean umer() {
        return umer;
    }

    private final class Slukatel implements WebSocket.Listener {

        private final ByteArrayOutputStream kopilka = new ByteArrayOutputStream();

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
            if (message.remaining() == Long.BYTES) {
                long metka = message.getLong(message.position());
                long seychas = System.nanoTime();
                if (metka == posledniyPing && seychas >= metka) {
                    pingMs = TimeUnit.NANOSECONDS.toMillis(seychas - metka);
                }
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            if (kopilka.size() + data.remaining() > RAZMER_SOOBSHENIYA) {
                zamri(true);
                return null;
            }

            byte[] kusok = new byte[data.remaining()];
            data.get(kusok);
            kopilka.write(kusok, 0, kusok.length);

            if (last) {
                byte[] message = kopilka.toByteArray();
                kopilka.reset();

                InetSocketAddress addr = ktoTam;
                if (addr != null) {
                    try {
                        dyrka.send(new DatagramPacket(message, message.length, addr));
                    } catch (Exception ignored) {
                        zamri(true);
                    }
                }
            }

            webSocket.request(1);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            zamri(true);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            zamri(true);
            return null;
        }
    }
}
