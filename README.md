# TBoard — клавиатура на Kotlin + Jetpack Compose

Сборка релизного APK через GitHub Actions:

1. Залей содержимое этого архива в корень нового репозитория на GitHub (папка `.github` должна попасть в репозиторий).
2. Открой вкладку **Actions** — сборка запускается сама при каждом push в `main`/`master`
   (или вручную: Actions → Build release APK → Run workflow).
3. Когда сборка зелёная, открой запуск и в разделе **Artifacts** скачай `TBoard-release-apk` — внутри `app-release.apk`.

Собирается только release-вариант (debug отключён в `app/build.gradle.kts`).

## Ключ подписи
Файл `app/tboard-release.jks`, алиас `tboard`, пароль хранилища и ключа `tboard-release-2026`
(прописаны в `app/build.gradle.kts`). Все обновления приложения нужно подписывать этим же ключом,
иначе Android не даст поставить апдейт поверх. Репозиторий лучше сделать приватным.

## Первый запуск на телефоне
Открой TBoard → «Включить клавиатуру» → «Выбрать TBoard».
