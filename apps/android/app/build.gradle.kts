plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose") }
android { namespace="com.securestream.viewer"; compileSdk=35
  defaultConfig { applicationId="com.securestream.viewer"; minSdk=26; targetSdk=35; versionCode=1; versionName="0.1.0"; testInstrumentationRunner="androidx.test.runner.AndroidJUnitRunner" }
  buildTypes { release { isMinifyEnabled=true; isShrinkResources=true; proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"),"proguard-rules.pro") } }
  compileOptions { sourceCompatibility=JavaVersion.VERSION_17; targetCompatibility=JavaVersion.VERSION_17 }
  buildFeatures { compose=true; buildConfig=true }
}
kotlin { jvmToolchain(17) }
dependencies { implementation(platform("androidx.compose:compose-bom:2025.05.01")); implementation("androidx.activity:activity-compose:1.10.1"); implementation("androidx.compose.material3:material3"); implementation("androidx.navigation:navigation-compose:2.9.0"); implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.1"); implementation("androidx.media3:media3-exoplayer:1.7.1"); implementation("androidx.media3:media3-ui:1.7.1"); implementation("androidx.media3:media3-exoplayer-dash:1.7.1"); implementation("androidx.media3:media3-exoplayer-hls:1.7.1"); implementation("com.squareup.retrofit2:retrofit:3.0.0"); implementation("com.squareup.okhttp3:logging-interceptor:5.1.0"); implementation("io.coil-kt.coil3:coil-compose:3.2.0"); testImplementation("junit:junit:4.13.2") }
