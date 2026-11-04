package ru.fastoran.mini

import android.os.Bundle
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.SubcomposeAsyncImage
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.net.URLDecoder

class MainActivity : ComponentActivity() {
    private lateinit var api: FastoranApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        api = FastoranApi(this)
        setContent { MaterialTheme(colorScheme = lightColorScheme(primary = androidx.compose.ui.graphics.Color(0xFFE85D25))) { FastoranRoot(api) } }
    }
}

data class ApiItem(val id: String, val title: String, val description: String = "", val price: String = "", val image: String = "", val raw: JSONObject = JSONObject())

private fun imageValue(json: JSONObject): String {
    val keys = listOf("image", "image_url", "images", "photo", "photo_url", "picture", "thumbnail", "cover", "avatar")
    for (key in keys) {
        val value = json.opt(key)
        val candidate = when (value) {
            is String -> {
                val parsed = try { if (value.trim().startsWith("[")) JSONArray(value) else null } catch (_: Exception) { null }
                if (parsed != null && parsed.length() > 0) {
                    val first = parsed.opt(0)
                    when (first) { is String -> first; is JSONObject -> first.optString("url", first.optString("path", first.optString("original"))); else -> "" }
                } else value
            }
            is JSONArray -> {
                val first = value.opt(0)
                when (first) { is String -> first; is JSONObject -> first.optString("url", first.optString("path", first.optString("original"))); else -> "" }
            }
            is JSONObject -> value.optString("url", value.optString("path", value.optString("original")))
            else -> ""
        }.trim()
        if (candidate.isNotBlank() && candidate != "null") return candidate
    }
    return ""
}

private fun absoluteImageUrl(baseUrl: String, path: String): String? {
    val value = path.trim()
    if (value.isBlank() || value == "null") return null
    if (value.startsWith("https://") || value.startsWith("http://")) return value
    val normalized = if (value.startsWith("/")) value else "/$value"
    return baseUrl.trimEnd('/') + normalized
}

@Composable
private fun ImagePlaceholder(partner: Boolean) {
    Box(Modifier.fillMaxSize().background(Color(0xFFF1F2F4)), contentAlignment = Alignment.Center) {
        Text(if (partner) "🍽" else "🥗", style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
private fun ImageOrPlaceholder(baseUrl: String, path: String, modifier: Modifier = Modifier, partner: Boolean = false) {
    val url = remember(baseUrl, path) { absoluteImageUrl(baseUrl, path) }
    if (url != null) {
        SubcomposeAsyncImage(
            model = url,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop,
            loading = { ImagePlaceholder(partner) },
            error = { ImagePlaceholder(partner) }
        )
    } else {
        Box(modifier) { ImagePlaceholder(partner) }
    }
}

class FastoranApi(private val activity: ComponentActivity) {
    private val prefs = activity.getSharedPreferences("fastoran_session", 0)
    private val cookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val old = prefs.getString("cookies", "") ?: ""
            val map = old.split(";").filter { it.contains("=") }.associate { it.substringBefore("=") to it.substringAfter("=") }.toMutableMap()
            cookies.forEach { map[it.name] = it.value }
            prefs.edit().putString("cookies", map.entries.joinToString(";") { "${it.key}=${it.value}" }).apply()
        }
        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val raw = prefs.getString("cookies", "") ?: ""
            return raw.split(";").filter { it.contains("=") }.mapNotNull {
                val n = it.substringBefore("=").trim(); val v = it.substringAfter("=").trim()
                try { Cookie.Builder().name(n).value(v).domain(url.host).path("/").build() } catch (_: Exception) { null }
            }
        }
    }
    private val client = OkHttpClient.Builder().cookieJar(cookieJar).connectTimeout(25, TimeUnit.SECONDS).readTimeout(35, TimeUnit.SECONDS).build()
    private val base: String get() = BuildConfig.API_BASE_URL.trim().trimEnd('/')
    private val tenantSlug: String get() = BuildConfig.TENANT_SLUG
    private val media = "application/json; charset=utf-8".toMediaType()

    private fun call(method: String, path: String, data: JSONObject? = null): JSONObject {
        if (base.contains("example.com") || base.isBlank()) throw IOException("Укажите FASTORAN_API_BASE_URL в GitHub Secrets / gradle.properties")
        val url = base + path
        val b = Request.Builder().url(url).header("Accept", "application/json").header("X-Requested-With", "XMLHttpRequest")
        if (BuildConfig.TENANT_KEY.isNotBlank() && BuildConfig.TENANT_KEY != "SET_TENANT_KEY") b.header("X-Tenant-Key", BuildConfig.TENANT_KEY)
        if (method != "GET") {
            val xsrf = prefs.getString("cookies", "")!!.split(";").firstOrNull { it.trim().startsWith("XSRF-TOKEN=") }
                ?.substringAfter("=")?.trim()
            if (!xsrf.isNullOrBlank()) b.header("X-XSRF-TOKEN", try { URLDecoder.decode(xsrf, "UTF-8") } catch (_: Exception) { xsrf })
        }
        when (method) {
            "GET" -> b.get()
            "POST" -> b.post((data ?: JSONObject()).toString().toRequestBody(media))
            "DELETE" -> b.delete((data ?: JSONObject()).toString().toRequestBody(media))
        }
        val response = client.newCall(b.build()).execute()
        val body = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
            val message = try { JSONObject(body).optString("message", body) } catch (_: Exception) { body }
            throw IOException("HTTP ${response.code}: $message")
        }
        return try { JSONObject(body) } catch (_: Exception) { JSONObject().put("raw", body) }
    }
    suspend fun warmup() = withContext(Dispatchers.IO) {
        // Laravel tenant route handler initializes tenant/session context for the selected tenant slug.
        if (tenantSlug.isNotBlank()) {
            val req = Request.Builder().url("$base/pwa/$tenantSlug").header("Accept", "text/html").build()
            client.newCall(req).execute().use { if (!it.isSuccessful) throw IOException("Не удалось открыть tenant-сессию: HTTP ${it.code}") }
        }
    }
    suspend fun partners() = withContext(Dispatchers.IO) { extractItems(call("POST", "/partners/"), listOf("data", "partners")) }
    suspend fun catalog(partnerId: String) = withContext(Dispatchers.IO) {
        val result = call("GET", "/partners/products-by-category?partner_id=$partnerId")
        val categories = extractItems(result, listOf("data", "categories"))
        categories.map { c ->
            val products = c.raw.optJSONArray("products") ?: JSONArray()
            c.raw.put("_products", products)
            // Если сервер не прислал флаг, но вернул непустую первую страницу,
            // кнопку дозагрузки показываем только при явном has_more=true.
            val totalProducts = c.raw.optInt("products_count", products.length())
            c.raw.put("_has_more", totalProducts > products.length())
            c.raw.put("_loading_more", false)
            c
        }
    }
    suspend fun loadMoreProducts(partnerId: String, category: ApiItem) = withContext(Dispatchers.IO) {
        val products = category.raw.optJSONArray("_products") ?: JSONArray()
        val offset = products.length()
        val url = (base + "/partners/products-by-category/more").toHttpUrl().newBuilder()
            .addQueryParameter("category_id", category.id)
            .addQueryParameter("partner_id", partnerId)
            .addQueryParameter("offset", offset.toString())
            .build()
        val request = Request.Builder().url(url).header("Accept", "application/json")
            .header("X-Requested-With", "XMLHttpRequest").apply {
                if (BuildConfig.TENANT_KEY.isNotBlank() && BuildConfig.TENANT_KEY != "SET_TENANT_KEY") header("X-Tenant-Key", BuildConfig.TENANT_KEY)
            }.get().build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}: $body")
            val result = try { JSONObject(body) } catch (_: Exception) { JSONObject() }
            val newItems = extractItems(result, listOf("data", "products"))
            JSONObject().put("items", JSONArray().also { a -> newItems.forEach { a.put(it.raw) } })
                .put("has_more", newItems.size >= 20)
                .put("next_offset", offset + newItems.size)
        }
    }
    suspend fun addProduct(productId: String) = withContext(Dispatchers.IO) { call("POST", "/basket/add-product", JSONObject().put("product_id", productId).put("count", 1)) }
    suspend fun basket() = withContext(Dispatchers.IO) { call("POST", "/basket/") }
    suspend fun changeCount(id: String, inc: Boolean) = withContext(Dispatchers.IO) { call("POST", "/basket/${if (inc) "increment" else "decrement"}/$id") }
    suspend fun delivery(payload: JSONObject) = withContext(Dispatchers.IO) { call("POST", "/basket/get-delivery-price-new", payload) }
    suspend fun checkout(payload: JSONObject) = withContext(Dispatchers.IO) { call("POST", "/basket/checkout", payload) }

    private fun extractItems(root: JSONObject, keys: List<String>): List<ApiItem> {
        var node: Any = root
        for (key in keys) {
            if (node is JSONObject && node.has(key)) node = node.get(key)
            else if (node is JSONObject && node.has("data")) node = node.get("data")
        }
        val arr = when (node) { is JSONArray -> node; is JSONObject -> node.optJSONArray("data") ?: node.optJSONArray("items") ?: JSONArray(); else -> JSONArray() }
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            ApiItem(o.optString("id"), o.optString("title", o.optString("name", "Без названия")), o.optString("description"), o.optString("price", o.optString("current_price")), imageValue(o), o)
        }
    }
    fun rows(root: JSONObject): List<ApiItem> {
        fun find(o: JSONObject): JSONArray? {
            listOf("items", "products", "basket_items", "basket", "data").forEach { k ->
                val a = o.optJSONArray(k); if (a != null) return a
                val child = o.optJSONObject(k); if (child != null) find(child)?.let { return it }
            }; return null
        }
        val arr = find(root) ?: JSONArray()
        return (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let { o -> ApiItem(o.optString("basket_id", o.optString("id", "")), o.optString("name", o.optString("title", o.optJSONObject("product")?.optString("name", "Товар") ?: "Товар")), o.optString("description", o.optJSONObject("product")?.optString("description", "") ?: ""), o.optString("final_price", o.optString("price", o.optString("total_price", ""))), raw = o) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FastoranRoot(api: FastoranApi) {
    var screen by remember { mutableStateOf("partners") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var partners by remember { mutableStateOf(emptyList<ApiItem>()) }
    var categories by remember { mutableStateOf(emptyList<ApiItem>()) }
    var basket by remember { mutableStateOf(JSONObject()) }
    var selectedPartner by remember { mutableStateOf<ApiItem?>(null) }
    var selectedProduct by remember { mutableStateOf<ApiItem?>(null) }
    val scope = rememberCoroutineScope()
    fun run(block: suspend () -> Unit) { scope.launch { busy = true; error = ""; try { block() } catch (e: Exception) { error = e.message ?: "Ошибка запроса" } finally { busy = false } } }
    LaunchedEffect(Unit) { run { api.warmup(); partners = api.partners() } }
    Scaffold(topBar = { TopAppBar(title = { Text("Фасторан.Мини") }, navigationIcon = { if (screen != "partners") IconButton(onClick = { screen = when (screen) { "checkout" -> "cart"; "product" -> "catalog"; "cart" -> "partners"; else -> "partners" } }) { Icon(Icons.Default.ArrowBack, null) } }, actions = { IconButton(onClick = { run { basket = api.basket(); screen = "cart" } }) { Icon(Icons.Default.ShoppingCart, null) } }) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp)) {
            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            when (screen) {
                "partners" -> {
                    Text("Выберите партнёра", style = MaterialTheme.typography.headlineSmall)
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { items(partners) { p -> Card(Modifier.fillMaxWidth().clickable { selectedPartner = p; run { categories = api.catalog(p.id); screen = "catalog" } }, shape = RoundedCornerShape(16.dp)) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { ImageOrPlaceholder(BuildConfig.API_BASE_URL, p.image.ifBlank { imageValue(p.raw) }, Modifier.size(104.dp), partner = true); Column(Modifier.weight(1f).padding(14.dp)) { Text(p.title, style = MaterialTheme.typography.titleLarge); if (p.description.isNotBlank()) Text(p.description) } } } } }
                }
                "product" -> {
                    val product = selectedProduct
                    if (product != null) {
                        val productName = product.raw.optString("name", product.raw.optString("title", product.title))
                        val productDescription = product.raw.optString("description", product.description)
                        val productPrice = product.raw.optString("price", product.raw.optString("current_price", product.price))
                        var added by remember(product.id) { mutableStateOf(false) }
                        val addColor by animateColorAsState(if (added) Color(0xFF2E8B57) else MaterialTheme.colorScheme.primary, label = "addButtonColor")
                        Column(Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            ImageOrPlaceholder(BuildConfig.API_BASE_URL, product.image.ifBlank { imageValue(product.raw) }, Modifier.fillMaxWidth().height(230.dp))
                            Text(productName, style = MaterialTheme.typography.headlineSmall)
                            Text("${productPrice} ₽", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                            if (productDescription.isNotBlank()) Text(productDescription, style = MaterialTheme.typography.bodyLarge)
                            Button(onClick = { run { api.addProduct(product.id); basket = api.basket(); added = true } }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = addColor)) {
                                Icon(if (added) Icons.Default.ShoppingCart else Icons.Default.Add, null)
                                Spacer(Modifier.width(8.dp))
                                Text(if (added) "Добавлено в корзину" else "Добавить в корзину")
                            }
                        }
                    }
                }
                "catalog" -> {
                    Text(selectedPartner?.title ?: "Меню", style = MaterialTheme.typography.headlineSmall)
                    val partnerId = selectedPartner?.id.orEmpty()
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(categories.filter { c ->
                            val a = c.raw.optJSONArray("_products") ?: JSONArray()
                            a.length() > 0
                        }, key = { it.id }) { c ->
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(c.title, style = MaterialTheme.typography.titleLarge)
                                val arr = c.raw.optJSONArray("_products") ?: JSONArray()
                                for (i in 0 until arr.length()) {
                                    val p = arr.optJSONObject(i) ?: JSONObject()
                                    val productName = p.optString("name", p.optString("title", "Без названия"))
                                    val productImage = imageValue(p)
                                    Card(Modifier.fillMaxWidth().clickable {
                                        selectedProduct = ApiItem(p.optString("id"), productName, p.optString("description"), p.optString("price", p.optString("current_price")), productImage, p)
                                        screen = "product"
                                    }, shape = RoundedCornerShape(14.dp)) {
                                        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                            ImageOrPlaceholder(BuildConfig.API_BASE_URL, productImage, Modifier.size(88.dp), partner = false)
                                            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                                Text(productName, style = MaterialTheme.typography.titleMedium)
                                                Text("${p.optString("price", p.optString("current_price"))} ₽", color = MaterialTheme.colorScheme.primary)
                                                Text("Подробнее", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            var justAdded by remember(p.optString("id")) { mutableStateOf(false) }
                                            val addColor by animateColorAsState(if (justAdded) Color(0xFF2E8B57) else MaterialTheme.colorScheme.primary, label = "catalogAddColor")
                                            Button(onClick = { run { api.addProduct(p.optString("id")); basket = api.basket(); justAdded = true } }, colors = ButtonDefaults.buttonColors(containerColor = addColor)) {
                                                Icon(if (justAdded) Icons.Default.ShoppingCart else Icons.Default.Add, null)
                                            }
                                        }
                                    }
                                }
                                val totalCount = c.raw.optInt("products_count", arr.length())
                                if (arr.length() > 0 && arr.length() < totalCount) {
                                    var loadingMore by remember(c.id) { mutableStateOf(false) }
                                    OutlinedButton(
                                        enabled = !loadingMore,
                                        onClick = {
                                            loadingMore = true
                                            run {
                                                try {
                                                    val page = api.loadMoreProducts(partnerId, c)
                                                    val newArray = page.optJSONArray("items") ?: JSONArray()
                                                    val current = c.raw.optJSONArray("_products") ?: JSONArray()
                                                    val seen = (0 until current.length()).mapNotNull { current.optJSONObject(it)?.optString("id") }.toMutableSet()
                                                    for (j in 0 until newArray.length()) {
                                                        val item = newArray.optJSONObject(j) ?: continue
                                                        if (seen.add(item.optString("id"))) current.put(item)
                                                    }
                                                    c.raw.put("_products", current)
                                                    c.raw.put("_has_more", current.length() < totalCount)
                                                } finally { loadingMore = false }
                                            }
                                        }, modifier = Modifier.fillMaxWidth()
                                    ) { Text(if (loadingMore) "Загружаем…" else "Загрузить ещё") }
                                }
                            }
                        }
                        if (categories.all { (it.raw.optJSONArray("_products")?.length() ?: 0) == 0 && !it.raw.optBoolean("_has_more", false) }) {
                            item { Text("В этом меню пока нет товаров", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp)) }
                        }
                    }
                    Button(onClick = { run { basket = api.basket(); screen = "cart" } }, modifier = Modifier.fillMaxWidth()) { Text("Перейти в корзину") }
                }
                "cart" -> {
                    Text("Корзина", style = MaterialTheme.typography.headlineSmall)
                    val rows = remember(basket.toString()) { api.rows(basket) }
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(rows) { item -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) { Text(item.title, style = MaterialTheme.typography.titleMedium); Text("${item.price} ₽"); Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.animateContentSize()) {
                            OutlinedButton(onClick = { run { api.changeCount(item.id, false); basket = api.basket() } }) { Icon(Icons.Default.Remove, null) }
                            val quantity = item.raw.optString("count", item.raw.optString("quantity", item.raw.optString("amount", "1")))
                            AnimatedContent(targetState = quantity, label = "basketQuantity") { value ->
                                Text(value, modifier = Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.titleMedium)
                            }
                            OutlinedButton(onClick = { run { api.changeCount(item.id, true); basket = api.basket() } }) { Icon(Icons.Default.Add, null) }
                        } } } } }
                    Button(onClick = { screen = "checkout" }, modifier = Modifier.fillMaxWidth(), enabled = rows.isNotEmpty()) { Text("К оформлению") }
                }
                "checkout" -> CheckoutForm(api, onDone = { basket = JSONObject(); screen = "partners" }, onBack = { screen = "cart" }, onError = { error = it })
            }
        }
    }
}

@Composable
fun CheckoutForm(api: FastoranApi, onDone: () -> Unit, onBack: () -> Unit, onError: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var address by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    var lat by remember { mutableStateOf<Double?>(null) }
    var lng by remember { mutableStateOf<Double?>(null) }
    var needPickup by remember { mutableStateOf(false) }
    var paymentType by remember { mutableStateOf("cash") }
    var loading by remember { mutableStateOf(false) }
    var deliveryPrice by remember { mutableStateOf<Double?>(null) }
    val scope = rememberCoroutineScope()
    val mapLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            lat = data?.getDoubleExtra(MapPickerActivity.EXTRA_LAT, Double.NaN)?.takeUnless { it.isNaN() }
            lng = data?.getDoubleExtra(MapPickerActivity.EXTRA_LNG, Double.NaN)?.takeUnless { it.isNaN() }
            address = data?.getStringExtra(MapPickerActivity.EXTRA_ADDRESS).orEmpty().ifBlank { address }
            deliveryPrice = null
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Оформление заказа", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(name, { name = it }, label = { Text("Имя") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(phone, { phone = it }, label = { Text("Телефон") }, modifier = Modifier.fillMaxWidth())
        Text("Адрес доставки", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(address, { address = it }, label = { Text("Адрес по карте") }, modifier = Modifier.fillMaxWidth(), readOnly = true)
        OutlinedButton(onClick = { mapLauncher.launch(Intent(context, MapPickerActivity::class.java)) }, modifier = Modifier.fillMaxWidth()) {
            Text(if (lat != null && lng != null) "Изменить точку на карте" else "Выбрать адрес на карте")
        }
        if (lat != null && lng != null) {
            Text("Координаты: ${"%.6f".format(lat)}, ${"%.6f".format(lng)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else if (!needPickup) {
            Text("Для доставки обязательно выберите точку на карте", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        OutlinedTextField(comment, { comment = it }, label = { Text("Комментарий") }, modifier = Modifier.fillMaxWidth())
        Text("Получение")
        Row { FilterChip(selected = !needPickup, onClick = { needPickup = false }, label = { Text("Доставка") }); Spacer(Modifier.width(8.dp)); FilterChip(selected = needPickup, onClick = { needPickup = true }, label = { Text("Самовывоз") }) }
        Text("Оплата")
        Row { FilterChip(selected = paymentType == "cash", onClick = { paymentType = "cash" }, label = { Text("Наличными") }); Spacer(Modifier.width(8.dp)); FilterChip(selected = paymentType == "sbp", onClick = { paymentType = "sbp" }, label = { Text("СБП") }) }
        deliveryPrice?.let { Text("Стоимость доставки: ${"%.2f".format(it)} ₽", color = MaterialTheme.colorScheme.primary) }
        Button(enabled = !loading && name.isNotBlank() && phone.isNotBlank() && (needPickup || (address.isNotBlank() && lat != null && lng != null)), onClick = {
            loading = true
            scope.launch { try {
                val payload = JSONObject()
                    .put("name", name).put("phone", phone).put("address", address)
                    .put("city", "").put("street", "").put("building", "")
                    .put("flat_number", "").put("entrance_number", "").put("floor_number", "")
                    .put("info", comment).put("need_pickup", needPickup).put("pick_up_type", 1)
                    .put("payment_type", if (paymentType == "sbp") 4 else 3)
                    .put("cash", paymentType == "cash").put("persons", 1)
                    .put("when_ready", true).put("time", JSONObject.NULL)
                    .put("delivery_price", 0).put("distance", 0).put("delivery_details", JSONObject())
                    .put("location_id", JSONObject.NULL).put("lng", lng ?: JSONObject.NULL).put("lat", lat ?: JSONObject.NULL)
                    .put("discount", 0).put("use_cashback", false).put("has_disability", false)
                    .put("disabilities", JSONArray()).put("money", JSONObject.NULL).put("allergy", JSONObject.NULL)
                    .put("action_prize", JSONObject.NULL)
                if (!needPickup) {
                    // Веб-клиент отправляет в get-delivery-price-new ровно address, lat, lng.
                    val quote = api.delivery(JSONObject().put("address", address).put("lat", lat).put("lng", lng))
                    val price = quote.optDouble("price", 0.0)
                    val distance = quote.optDouble("distance", 0.0)
                    payload.put("delivery_price", price).put("distance", distance)
                    val details = quote.opt("config")
                    if (details != null && details != JSONObject.NULL) payload.put("delivery_details", details)
                    // Адрес/координаты должны остаться в checkout даже если расчёт вернул неполный ответ.
                    payload.put("address", address).put("lat", lat).put("lng", lng)
                    deliveryPrice = price
                }
                val result = api.checkout(payload)
                if (result.optBoolean("success", false)) onDone() else onError(result.optString("message", "Сервер не подтвердил создание заказа"))
            } catch (e: Exception) { onError(e.message ?: "Не удалось оформить заказ") } finally { loading = false } }
        }, modifier = Modifier.fillMaxWidth()) { Text(if (loading) "Отправка…" else "Подтвердить заказ") }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Назад в корзину") }
    }
}
