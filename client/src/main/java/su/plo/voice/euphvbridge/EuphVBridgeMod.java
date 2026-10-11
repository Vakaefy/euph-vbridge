package su.plo.voice.euphvbridge;

import su.plo.voice.api.addon.AddonInitializer;
import su.plo.voice.api.addon.AddonLoaderScope;
import su.plo.voice.api.addon.InjectPlasmoVoice;
import su.plo.voice.api.addon.annotation.Addon;
import su.plo.voice.api.client.PlasmoVoiceClient;
import su.plo.voice.api.client.connection.ServerConnection;
import su.plo.voice.api.client.event.socket.UdpClientConnectEvent;
import su.plo.voice.api.client.socket.UdpClient;
import su.plo.voice.api.event.EventSubscribe;
import su.plo.voice.proto.packets.tcp.clientbound.ConnectionPacket;

import java.util.Locale;
import java.util.Set;

@Addon(
        id = "euph-vbridge",
        name = "Euph-VBridge",
        scope = AddonLoaderScope.CLIENT,
        version = "1.0.0",
        authors = {"Vakaefy"}
)
public final class EuphVBridgeMod implements AddonInitializer {

    private static final String RELAY_URL = "wss://voice.femboyhouse.ru/voice";
    private static final Set<String> EUPHORIUM_HOSTS = Set.of(
            "euphorium.fun",
            "euphorium.ru",
            "mc.euphorium.fun",
            "mc.euphorium.ru",
            "rc.euphorium.ru",
            "euphorium.20tps.ru"
    );
    private static final Set<String> EUPHORIUM_IPS = Set.of(
            "31.57.117.1",
            "95.81.102.20",
            "193.58.122.127",
            "26.178.46.168",
            "195.58.152.18",
            "195.58.152.19"
    );

    @InjectPlasmoVoice
    private PlasmoVoiceClient plasmoVoice;

    @Override
    public void onAddonInitialize() {
    }

    @EventSubscribe
    public void priletel(UdpClientConnectEvent event) {
        UdpClient old = event.getClient();
        if (old instanceof Trubach) return;
        if (!euphorium(event.getConnectionPacket())) {
            return;
        }

        event.setClient(new Trubach(old, RELAY_URL));
    }

    private boolean euphorium(ConnectionPacket packet) {
        if (packet != null && isEuphoriumAddress(packet.getIp())) return true;

        return plasmoVoice.getServerConnection()
                .map(ServerConnection::getRemoteIp)
                .map(this::isEuphoriumAddress)
                .orElse(false);
    }

    private boolean isEuphoriumAddress(String address) {
        if (address == null || address.isBlank()) return false;

        String normalized = address.trim().toLowerCase(Locale.ROOT);
        return EUPHORIUM_HOSTS.contains(normalized)
                || normalized.endsWith(".euphorium.fun")
                || normalized.endsWith(".euphorium.ru")
                || EUPHORIUM_IPS.contains(normalized);
    }
}
