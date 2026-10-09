plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
	id("com.google.devtools.ksp")
}

android {
    namespace = "ru.fo6osik.workjournal"
    compileSdk = 36

    defaultConfig {
        applicationId = "ru.fo6osik.workjournal"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
	implementation("androidx.room:room-runtime:2.8.5")
ksp("androidx.room:room-compiler:2.8.5")
implementation("androidx.room:room-ktx:2.8.5")
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
implementation(
    "com.infomaniak.pdfview:android-pdfview:3.2.18"
)
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("androidx.core:core-ktx:1.3.2")
    implementation("androidx.appcompat:appcompat:1.2.0")
    implementation("com.google.android.material:material:1.2.1")
    implementation("androidx.constraintlayout:constraintlayout:2.0.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")
}

