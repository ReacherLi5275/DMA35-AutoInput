plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
 namespace = "com.example.dma35"
 compileSdk = 35
 defaultConfig { applicationId = "com.example.dma35"; minSdk = 26; targetSdk = 32; versionCode = 4; versionName = "3.1-pilot" }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
}
