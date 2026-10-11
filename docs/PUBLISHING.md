# Публикация

## GitHub

Создайте публичный репозиторий `euph-vbridge` в аккаунте Vakaefy и выполните:

```bash
git remote add origin https://github.com/Vakaefy/euph-vbridge.git
git push -u origin main
git tag v1.0.0
git push origin v1.0.0
```

Для GitHub Release приложите `client/build/libs/euph-vbridge-1.0.0.jar`.

## Modrinth

Название: `Euph-VBridge`  
Slug: `euph-vbridge`  
Тип: `Mod`  
Сторона клиента: `Required`  
Сторона сервера: `Unsupported`  
Загрузчики: `Fabric`, `NeoForge`  
Зависимость: `Plasmo Voice`, обязательная  
Лицензия: `MIT`

Описание для карточки:

> Клиентский аддон для Plasmo Voice, который прокладывает голосовой трафик Euphorium через WebSocket-релей.

Загрузите JAR из `client/build/libs/` и логотип `client/icon.png`.
