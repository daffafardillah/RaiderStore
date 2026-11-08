# Raider Store
Aplikasi toko komputer Android (Kotlin + Jetpack Compose), data dari DummyJSON.

## Video Penjelasan & Demo
https://youtu.be/gPd0rjRBDJw

## Fitur
- Daftar seluruh produk
- Detail produk
- Pencarian

## Identitas
Nama: Daffaa Fardillah
NIM: 240102021

## Backend & CRUD
Aplikasi terhubung ke backend PHP + MySQL (folder `backend/`) lewat REST API.
Fitur: Create, Read (search & filter kategori), Update, dan Delete produk.

### Menjalankan backend
cd backend
copy .env.example .env
docker compose up -d --build
docker compose exec php php /var/www/html/seed.php

Cek: http://localhost:8080/products

### Menjalankan aplikasi
Buka folder ini di Android Studio, jalankan di emulator.
Base URL API: `http://10.0.2.2:8080` (atur di `app/build.gradle.kts`, `API_BASE_URL`).