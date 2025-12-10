pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        maven ("https://artifactory.appodeal.com/appodeal-public")
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        maven ("https://artifactory.appodeal.com/appodeal-public")
        mavenCentral()
    }
}

rootProject.name = "Azizahzi Official"
include(":app")
 