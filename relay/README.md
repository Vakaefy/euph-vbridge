# Relay Euph-VBridge

Небольшой UDP-over-WebSocket relay для клиентского аддона Euph-VBridge.

Релей принимает WebSocket на порту `8790`, проверяет приветствие `PVW2`, разрешает только адреса Euphorium и передаёт голосовые UDP-пакеты к Minecraft-серверу.

## Запуск

```bash
docker compose up -d --build
```

Публичный WebSocket публикуется через reverse proxy:

```text
wss://voice.femboyhouse.ru/voice
```

## Настройки

Ограничения задаются в `docker-compose.yml`:

- `VOICE_ALLOWED_HOSTS`: разрешённые домены и IP Minecraft-серверов.
- `VOICE_ALLOW_PORTS`: разрешённые UDP-порты.
- `VOICE_MAX_CLIENTS`: максимальное число клиентов.
- `VOICE_IDLE_TIMEOUT`: таймаут неактивного соединения в секундах.
- `VOICE_MAX_FRAME`: максимальный размер WebSocket-кадра.

Контейнер не публикует порт наружу напрямую и подключается к внешней Docker-сети `matrix_default`. Внешний TLS и маршрут `/voice` обслуживает Caddy.

## Проверка

```bash
docker compose ps
docker compose logs --tail=100 voice-relay
```
