<div align="center">
  <img src="icon.png" alt="Euph-VBridge" width="150">
  <h1>Euph-VBridge</h1>
  <p>Клиентский аддон Plasmo Voice для Euphorium</p>
</div>

Euph-VBridge направляет голосовой трафик Euphorium через WebSocket-релей и оставляет остальные серверы без изменений.

## Установка

1. Установите Plasmo Voice под свой загрузчик Minecraft.
2. Скачайте `euph-vbridge-1.0.0.jar`.
3. Положите файл в папку `mods`.
4. Подключитесь к Euphorium.

Аддон работает на клиентской стороне. Поддерживаются Fabric и NeoForge.

## Поведение

- Для адресов Euphorium используется `wss://voice.femboyhouse.ru/voice`.
- Остальные серверы используют обычное подключение Plasmo Voice.
- При сбое релея клиент возвращается к прямому UDP.

## Сборка

Нужен JDK 25.

```bash
./gradlew build
```

JAR появится в `build/libs/euph-vbridge-1.0.0.jar`.

## Связанный проект

Серверная часть находится в [`../relay`](../relay).

## Лицензия

MIT. Подробности находятся в [`LICENSE`](LICENSE).
