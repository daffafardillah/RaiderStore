package com.example.raiderstore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

// ---------- Palet pastel ----------
val Ink = Color(0xFF3B3552)
val Lavender = Color(0xFFDCD6F7)
val Mint = Color(0xFFCDEFE3)
val Peach = Color(0xFFFFD9C7)
val Sky = Color(0xFFCFE8FF)
val Butter = Color(0xFFFFF3C4)
val Pink = Color(0xFFFBD5E5)
val Cream = Color(0xFFFFFBF7)
val Pastels = listOf(Mint, Peach, Sky, Butter, Pink, Lavender)

// ---------- Data ----------
data class Product(
    val id: Int, val title: String, val description: String, val category: String,
    val price: Double, val discount: Double, val rating: Double, val stock: Int,
    val brand: String, val thumbnail: String, val images: List<String>, val tags: List<String>
)

fun parseProducts(json: String): List<Product> {
    val arr = JSONObject(json).getJSONArray("products")
    return List(arr.length()) { i ->
        val o = arr.getJSONObject(i)
        val imgs = o.optJSONArray("images")
        val tg = o.optJSONArray("tags")
        Product(
            o.getInt("id"), o.getString("title"), o.optString("description"), o.optString("category"),
            o.optDouble("price"), o.optDouble("discountPercentage"), o.optDouble("rating"),
            o.optInt("stock"), o.optString("brand", "Raider"), o.optString("thumbnail"),
            List(imgs?.length() ?: 0) { imgs!!.getString(it) },
            List(tg?.length() ?: 0) { tg!!.getString(it) }
        )
    }
}

suspend fun fetchProducts(q: String): List<Product> = withContext(Dispatchers.IO) {
    val url = if (q.isBlank()) "https://dummyjson.com/products?limit=0"
    else "https://dummyjson.com/products/search?q=" + java.net.URLEncoder.encode(q, "UTF-8")
    parseProducts(URL(url).openStream().bufferedReader().readText())
}

// ---------- Activity ----------
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { RaiderApp() } }
    }
}

@Composable
fun RaiderApp() {
    var query by remember { mutableStateOf("") }
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var category by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<Product?>(null) }
    var retry by remember { mutableStateOf(0) }

    LaunchedEffect(query, retry) {
        if (query.isNotBlank()) delay(400)
        loading = true; error = null
        try { products = fetchProducts(query); category = null }
        catch (e: Exception) { error = "Gagal memuat produk. Periksa koneksi internet." }
        loading = false
    }

    BackHandler(enabled = selected != null) { selected = null }

    Surface(Modifier.fillMaxSize(), color = Cream) {
        Box(Modifier.statusBarsPadding()) {
            val p = selected
            if (p != null) DetailScreen(p) { selected = null }
            else HomeScreen(query, { query = it }, products, loading, error, category,
                { category = it }, { selected = it }, { retry++ })
        }
    }
}

@Composable
fun Logo(size: Int) = Image(painterResource(R.drawable.raider_logo), "Logo Raider Store", Modifier.size(size.dp))

// ---------- Beranda ----------
@Composable
fun HomeScreen(
    query: String, onQuery: (String) -> Unit, products: List<Product>, loading: Boolean,
    error: String?, category: String?, onCategory: (String?) -> Unit,
    onOpen: (Product) -> Unit, onRetry: () -> Unit
) {
    val shown = if (category == null) products else products.filter { it.category == category }
    val cats = products.map { it.category }.distinct()

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(16.dp, 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Logo(48)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Raider Store", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text("Toko komputer & gadget pilihan", fontSize = 13.sp, color = Ink.copy(0.6f))
            }
        }
        OutlinedTextField(
            value = query, onValueChange = onQuery, singleLine = true,
            placeholder = { Text("Cari hp, kendaraan...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = { if (query.isNotEmpty()) IconButton({ onQuery("") }) { Icon(Icons.Default.Close, "Hapus") } },
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Lavender, unfocusedContainerColor = Lavender,
                focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )
        if (cats.isNotEmpty()) LazyRow(
            contentPadding = PaddingValues(16.dp, 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Chip("Semua", category == null) { onCategory(null) } }
            items(cats) { c -> Chip(c.replace("-", " "), category == c) { onCategory(c) } }
        }
        when {
            loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Ink) }
            error != null -> Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
                Text(error, color = Ink); Spacer(Modifier.height(8.dp))
                Button(onRetry, colors = ButtonDefaults.buttonColors(Peach, Ink)) { Text("Coba lagi") }
            }
            shown.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("Produk \"$query\" tidak ditemukan. Coba kata kunci lain.", color = Ink.copy(0.7f))
            }
            else -> LazyVerticalGrid(
                GridCells.Fixed(2), contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(shown.size) { i -> ProductCard(shown[i], Pastels[i % Pastels.size]) { onOpen(shown[i]) } }
            }
        }
    }
}

@Composable
fun Chip(text: String, active: Boolean, onClick: () -> Unit) {
    Text(
        text, fontSize = 13.sp, color = Ink, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(if (active) Peach else Mint)
            .clickable(onClick = onClick).padding(14.dp, 8.dp)
    )
}

@Composable
fun ProductCard(p: Product, bg: Color, onClick: () -> Unit) {
    Column(Modifier.clip(RoundedCornerShape(24.dp)).background(bg).clickable(onClick = onClick).padding(10.dp)) {
        AsyncImage(p.thumbnail, p.title, Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(0.5f)), contentScale = ContentScale.Fit)
        Spacer(Modifier.height(8.dp))
        Text(p.title, fontWeight = FontWeight.SemiBold, color = Ink, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$${"%.2f".format(p.price)}", fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
            Icon(Icons.Default.Star, null, Modifier.size(14.dp), tint = Color(0xFFE8A23B))
            Text(" ${"%.1f".format(p.rating)}", fontSize = 12.sp, color = Ink)
        }
    }
}

// ---------- Detail ----------
@Composable
fun DetailScreen(p: Product, onBack: () -> Unit) {
    var img by remember(p.id) { mutableStateOf(p.images.firstOrNull() ?: p.thumbnail) }
    val finalPrice = p.price * (1 - p.discount / 100)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(28.dp)).background(Lavender)) {
            AsyncImage(img, p.title, Modifier.fillMaxWidth().aspectRatio(1f).padding(16.dp), contentScale = ContentScale.Fit)
            IconButton(onBack, Modifier.padding(8.dp).clip(CircleShape).background(Color.White.copy(0.8f))) {
                Icon(Icons.Default.ArrowBack, "Kembali", tint = Ink)
            }
        }
        if (p.images.size > 1) LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(p.images) { u ->
                AsyncImage(u, null, Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(if (u == img) Peach else Mint)
                    .clickable { img = u }.padding(4.dp))
            }
        }
        Column(Modifier.padding(20.dp)) {
            Text(p.category.replace("-", " "), color = Ink.copy(0.6f), fontSize = 13.sp)
            Text(p.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text("oleh ${p.brand}", color = Ink.copy(0.7f), fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$${"%.2f".format(finalPrice)}", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                Spacer(Modifier.width(10.dp))
                Text("$${"%.2f".format(p.price)}", color = Ink.copy(0.5f), textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                Spacer(Modifier.width(10.dp))
                Text("-${"%.0f".format(p.discount)}%", Modifier.clip(RoundedCornerShape(50)).background(Pink).padding(10.dp, 4.dp), color = Ink, fontSize = 12.sp)
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoBox("Rating", "★ ${"%.1f".format(p.rating)}", Butter, Modifier.weight(1f))
                InfoBox("Stok", "${p.stock}", Mint, Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
            Text("Deskripsi", fontWeight = FontWeight.Bold, color = Ink)
            Text(p.description, color = Ink.copy(0.8f), lineHeight = 22.sp)
            if (p.tags.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { p.tags.forEach { Chip(it, false) {} } }
            }
            Spacer(Modifier.height(20.dp))
            Button({}, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(Peach, Ink)) { Text("Tambah ke keranjang", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun InfoBox(label: String, value: String, bg: Color, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(bg).padding(14.dp)) {
        Text(label, fontSize = 12.sp, color = Ink.copy(0.6f))
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
    }
}
