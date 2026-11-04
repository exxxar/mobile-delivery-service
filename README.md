# Фасторан.Мини — Android-клиент

Нативный Android-клиент на Kotlin + Jetpack Compose. Основной сценарий: список партнёров → категории и товары партнёра → серверная корзина → оформление заказа.

## Реализованные Laravel-запросы

Маршруты используются из присланного `routes` файла и контроллеров:

| Действие | Метод и путь |
|---|---|
| Инициализация tenant web-session | `GET /pwa/{tenantSlug}` |
| Партнёры | `POST /partners/` |
| Категории и товары выбранного партнёра | `GET /partners/products-by-category?partner_id={id}` |
| Добавление товара | `POST /basket/add-product` (`product_id`, `count`) |
| Получение корзины | `POST /basket/` |
| Увеличение/уменьшение позиции | `POST /basket/increment/{id}`, `POST /basket/decrement/{id}` |
| Расчёт доставки | `POST /basket/get-delivery-price-new` |
| Оформление | `POST /basket/checkout` |

Каталог приходит из `tenant_partner_id` партнёра на стороне backend. Android не переключает tenant контекст и не рассчитывает цены самостоятельно. Корзина и заказ остаются в основном tenant.

## Настройка

В GitHub → Settings → Secrets and variables → Actions добавьте:

- `FASTORAN_API_BASE_URL` — **корень tenant-сайта**, например `https://fastoran.example.ru` (без `/api/mobile/v1` и без завершающего `/`).
- `FASTORAN_TENANT_SLUG` — slug tenant, который передаётся в существующий маршрут `/pwa/{tenantSlug}` для инициализации сессии.
- `FASTORAN_TENANT_KEY` — необязательный ключ, только если ваш tenant middleware действительно проверяет заголовок `X-Tenant-Key`.

В приложении сохраняются Laravel session cookies между запусками. Для POST/DELETE передаётся `X-XSRF-TOKEN`, если сервер выдал cookie `XSRF-TOKEN`.

**Важно про backend-сессию:** в присланных маршрутах нет отдельного API endpoint для создания гостевого пользователя и выдачи Sanctum Bearer token. Поэтому приложение не выдумывает `/api/mobile/v1/guest` и не отправляет фиктивный Bearer token: оно открывает существующий `/pwa/{tenantSlug}` и использует tenant web-session. Автоматическое создание/привязка гостя зависит от `TenantUserResolver` и tenant middleware на сервере.

## Сборка

Workflow: `.github/workflows/android.yml`. После push в `main`/`master` или ручного запуска `Build Android APK` APK появится в Artifacts под именем `fastoran-mini-debug-apk`.

Локально: Android Studio, JDK 17, Android SDK 35, Gradle 8.9. Для локальной сборки задайте свойства Gradle `FASTORAN_API_BASE_URL`, `FASTORAN_TENANT_SLUG` и при необходимости `FASTORAN_TENANT_KEY`.

## Важное о checkout

Структура `deliveryForm` повторяет поля, найденные в присланном `CartPage.vue`: `name`, `phone`, `address`, `location_id`, `lng`, `lat`, `city`, `street`, `building`, `flat_number`, `entrance_number`, `floor_number`, `info`, `need_pickup`, `pick_up_type`, `payment_type`, `cash`, `persons`, `time`, `when_ready`, `delivery_price`, `distance`, `delivery_details` и связанные параметры. Значения способов оплаты взяты из веб-версии: СБП — `4`, наличные — `3`. Сервер остаётся источником окончательного расчёта.

Для адресов с координатами приложение пока предоставляет простое текстовое поле; геокодирование и выбор сохранённого адреса в эту компактную версию не включены. Если backend требует `lat/lng` или `location_id` для расчёта доставки, их необходимо получить через существующий адресный компонент/API веб-версии.
