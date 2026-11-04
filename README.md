# Фасторан.Мини — Android Kotlin

## Структура экранов
Каждый экран вынесен в отдельный Kotlin-файл:
- `PartnerSelectionScreen.kt` — выбор партнёра
- `ProductsScreen.kt` — товары выбранного партнёра
- `CartScreen.kt` — корзина
- `CheckoutScreen.kt` — оформление
- `OrdersScreen.kt` — история заказов
- `ProfileScreen.kt` — профиль
Навигация между Compose-экранами настроена в `ui/FastoranApp.kt`. В Android используется одна MainActivity как контейнер приложения; отдельные экраны — это Compose destinations, а не отдельные Activity.

## Сборка в GitHub Actions
Файл workflow находится строго по пути `.github/workflows/android.yml`. GitHub автоматически обнаруживает его после push в `main` или `master`. Также можно запустить вручную: **Actions → Build Android APK → Run workflow**.

После успешной сборки скачайте `fastoran-mini-debug-apk` из Artifacts.

## GitHub Secrets
Можно задать:
- `FASTORAN_API_BASE_URL` — базовый URL API, например `https://example.ru/api/mobile/v1/`
- `FASTORAN_TENANT_KEY` — ключ/идентификатор tenant, согласованный с backend.

## Локальная среда
JDK 17, Android SDK 35, Gradle 8.9, Android Gradle Plugin 8.7.3. Откройте папку проекта в Android Studio.

## Иконка
Adaptive launcher icon:
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- `app/src/main/res/drawable/ic_fastoran_foreground.xml`
- `app/src/main/res/drawable/ic_fastoran_background.xml`

## Статус интеграции
Сейчас это UI-каркас с демонстрационными партнёрами и товарами. Подключение реальных Laravel/Sanctum API, сохранение корзины, адресов, расчёт доставки и checkout требуют привязки к фактическим маршрутам backend.
