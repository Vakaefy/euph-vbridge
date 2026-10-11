# Euph-VBridge

Клиентский аддон для Plasmo Voice, который прокладывает голосовой трафик Euphorium через WebSocket-релей.

На остальных серверах Plasmo Voice работает штатно. Если релей недоступен, аддон возвращается к прямому UDP-подключению.

## Установка

1. Установите Plasmo Voice под свой загрузчик Minecraft.
2. Скачайте `euph-vbridge-1.0.0.jar` из GitHub Releases или Modrinth.
3. Положите JAR-файл в папку `mods`.
4. Подключитесь к Euphorium.

Аддон рассчитан на клиентскую сторону и собирается единым JAR для Fabric и NeoForge.

## Сборка

```bash
./gradlew build
```

Артефакт появится в `build/libs/euph-vbridge-1.0.0.jar`.

## Связанный релей

Серверная часть находится в каталоге [`relay`](../relay) общего репозитория Euph-VBridge.

## Лицензия

MIT. Исходный код распространяется в [`LICENSE`](LICENSE).
