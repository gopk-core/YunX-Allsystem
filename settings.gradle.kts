pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
        // 官方仓库不可达时再试国内镜像。镜像返回 5xx 会让 Gradle 放弃后续仓库，所以不能放在前面。
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/central")
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
        maven("https://maven.aliyun.com/repository/central")
        maven("https://maven.aliyun.com/repository/google")
    }
}

rootProject.name = "YunX-Allsystem"
include(":desktop")
