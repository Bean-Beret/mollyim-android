// MOLLY: Edit Dockerfile to download the same SDK packages
val signalBuildToolsVersion by extra("34.0.0")
val signalCompileSdkVersion by extra("android-34")
val signalTargetSdkVersion by extra(31)
val signalMinSdkVersion by extra(24)
val signalJavaVersion by extra(JavaVersion.VERSION_17)
val signalKotlinJvmTarget by extra("17")