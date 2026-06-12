plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
}

// Keep build output out of iCloud sync (iCloud duplicates build files mid-build)
layout.buildDirectory.set(layout.projectDirectory.dir("build.nosync"))
