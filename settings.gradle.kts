pluginManagement {
    repositories {
                // 官方仓库优先，确保Lint工具能正确下载
        // 阿里云Maven镜像
        maven { url = uri("https://maven.aliyun.com/repository/public/") }
        maven { url = uri("https://maven.aliyun.com/repository/google/") }
        maven { url = uri("https://maven.aliyun.com/repository/central/") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin/") }
        maven { url = uri("https://maven.aliyun.com/repository/jcenter/") }
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
        // 腾讯云Maven镜像
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }

        // 华为云Maven镜像
        maven { url = uri("https://mirrors.huaweicloud.com/repository/maven/") }

        google()
        mavenCentral()
        gradlePluginPortal()
        
        // 阿里云Maven镜像
        maven { url = uri("https://maven.aliyun.com/repository/public/") }
        maven { url = uri("https://maven.aliyun.com/repository/google/") }
        maven { url = uri("https://maven.aliyun.com/repository/central/") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin/") }
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
        // 腾讯云Maven镜像
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
        
        // 华为云Maven镜像
        maven { url = uri("https://mirrors.huaweicloud.com/repository/maven/") }
        
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
                // 官方仓库优先，确保Lint工具能正确下载
        // 阿里云Maven镜像
        maven { url = uri("https://maven.aliyun.com/repository/public/") }
        maven { url = uri("https://maven.aliyun.com/repository/google/") }
        maven { url = uri("https://maven.aliyun.com/repository/central/") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin/") }
        maven { url = uri("https://maven.aliyun.com/repository/jcenter/") }
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
        // 腾讯云Maven镜像
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }

        // 华为云Maven镜像
        maven { url = uri("https://mirrors.huaweicloud.com/repository/maven/") }

        google()
        mavenCentral()
        gradlePluginPortal()
        
        // 阿里云Maven镜像
        maven { url = uri("https://maven.aliyun.com/repository/public/") }
        maven { url = uri("https://maven.aliyun.com/repository/google/") }
        maven { url = uri("https://maven.aliyun.com/repository/central/") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin/") }
        maven { url = uri("https://maven.aliyun.com/repository/jcenter/") }
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
        // 腾讯云Maven镜像
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
        
        // 华为云Maven镜像
        maven { url = uri("https://mirrors.huaweicloud.com/repository/maven/") }
        


        
        // 官方仓库作为后备
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "Yhchat Canary"
include(":app")
