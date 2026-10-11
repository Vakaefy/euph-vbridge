package su.plo.voice.euphvbridge;

import org.jetbrains.annotations.NotNull;
import su.plo.voice.api.client.event.socket.UdpClientClosedEvent;
import su.plo.voice.api.client.socket.UdpClient;
import su.plo.voice.proto.packets.Packet;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

final class Trubach implements UdpClient {

    private final UdpClient vnutri;
    private final String kuda;
    private final AtomicLong pokolenie = new AtomicLong();
    private final AtomicLong otkatDlya = new AtomicLong();
    private volatile Provodnik mostik;
    private volatile boolean closed;

    Trubach(@NotNull UdpClient vnutri, @NotNull String kuda) {
        this.vnutri = vnutri;
        this.kuda = kuda;
    }

    @Override
    public void connect(@NotNull String ip, int port) {
        long nomer = pokolenie.incrementAndGet();
        closed = false;
        Provodnik staryy = mostik;
        mostik = null;
        if (staryy != null) staryy.zamri();

        Thread potok = new Thread(() -> {
            Provodnik provodnik = null;
            try {
                provodnik = new Provodnik(kuda, ip, port, () -> pereytiNaUdp(nomer, ip, port));
                provodnik.zapuskay(4000L);
                if (closed || nomer != pokolenie.get()) {
                    provodnik.zamri();
                    return;
                }
                this.mostik = provodnik;
                if (provodnik.umer()) {
                    this.mostik = null;
                    return;
                }

                vnutri.connect("127.0.0.1", provodnik.kakoyPort());
            } catch (Exception e) {
                if (provodnik != null) provodnik.zamri();
                this.mostik = null;

                pereytiNaUdp(nomer, ip, port);
            }
        }, "euph-vbridge-relay");

        potok.setDaemon(true);
        potok.start();
    }

    @Override
    public void close(@NotNull UdpClientClosedEvent.Reason reason) {
        closed = true;
        pokolenie.incrementAndGet();
        Provodnik provodnik = mostik;
        if (provodnik != null) provodnik.zamri();
        mostik = null;

        vnutri.close(reason);
    }

    private void pereytiNaUdp(long nomer, String ip, int port) {
        if (closed || nomer != pokolenie.get() || !otkatDlya.compareAndSet(nomer - 1, nomer)) return;

        mostik = null;
        try {
            vnutri.connect(ip, port);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void sendPacket(Packet<?> packet) {
        vnutri.sendPacket(packet);
    }

    @Override
    public @NotNull UUID getSecret() {
        return vnutri.getSecret();
    }

    @Override
    public Optional<InetSocketAddress> getRemoteAddress() {
        return vnutri.getRemoteAddress();
    }

    @Override
    public boolean isClosed() {
        return vnutri.isClosed();
    }

    @Override
    public boolean isConnected() {
        return vnutri.isConnected();
    }

    @Override
    public boolean isTimedOut() {
        return vnutri.isTimedOut();
    }

    @Override
    public long getKeepAlive() {
        return vnutri.getKeepAlive();
    }
}
