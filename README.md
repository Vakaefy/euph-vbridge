# Euph-VBridge

Связка из клиентского аддона Plasmo Voice и WebSocket-релея для голосового трафика Euphorium.

```text
Euph-VBridge
├── client/   Minecraft-аддон для Plasmo Voice
└── relay/    Серверный UDP-over-WebSocket relay
```

## Как это работает

Клиент переключает только адреса Euphorium на WebSocket-релей `voice.femboyhouse.ru`. Остальные серверы не затрагиваются. При недоступности релея клиент возвращается к прямому UDP-подключению.

## Проекты

- [`client/`](client): аддон `Euph-VBridge` версии `1.0.0`.
- [`relay/`](relay): Docker-сервис `voice-relay` с allowlist адресов Euphorium.

## Быстрый старт

```bash
cd client
./gradlew build
```

Готовый JAR появится в `client/build/libs/euph-vbridge-1.0.0.jar`.

Для запуска релея:

```bash
cd relay
docker compose up -d --build
```

## Публикация

Инструкция находится в [`docs/PUBLISHING.md`](docs/PUBLISHING.md).

## Лицензия

MIT.
