<div align="center">
  <img src="client/icon.png" alt="Euph-VBridge" width="180">
  <h1>Euph-VBridge</h1>
  <p>Клиентский мост Plasmo Voice для Euphorium</p>
  <p>
    <a href="https://github.com/Vakaefy/euph-vbridge/actions/workflows/build.yml"><img src="https://github.com/Vakaefy/euph-vbridge/actions/workflows/build.yml/badge.svg" alt="Build"></a>
    <img src="https://img.shields.io/badge/version-1.0.0-7c5cff" alt="Version 1.0.0">
    <img src="https://img.shields.io/badge/license-MIT-7c5cff" alt="MIT license">
  </p>
</div>

Euph-VBridge помогает Plasmo Voice подключаться к Euphorium через WebSocket-релей. На других серверах клиент работает как обычно. Если релей недоступен, соединение возвращается к прямому UDP.

## Состав проекта

| Каталог | Что внутри |
| --- | --- |
| [`client/`](client) | Minecraft-аддон `Euph-VBridge` версии `1.0.0` |
| [`relay/`](relay) | Серверный UDP-over-WebSocket relay |

## Клиент

Установите Plasmo Voice, скачайте `euph-vbridge-1.0.0.jar` и положите его в папку `mods`. Аддон работает на клиентской стороне и собирается одним JAR для Fabric и NeoForge.

Сборка:

```bash
cd client
./gradlew build
```

Готовый файл появится в `client/build/libs/euph-vbridge-1.0.0.jar`.

## Relay

Публичный маршрут релея:

```text
wss://voice.femboyhouse.ru/voice
```

Локальный запуск:

```bash
cd relay
docker compose up -d --build
```

Настройки и команды проверки собраны в [`relay/README.md`](relay/README.md).

## Лицензия

MIT. Подробности находятся в [`LICENSE`](LICENSE).
