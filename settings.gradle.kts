pluginManagement {
    repositories {
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
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "berry-music"

include(":composeApp")
include(":androidApp")

includeBuild("../../../Ripple Design/rui-compose") {
    dependencySubstitution {
        substitute(module("art.rippledesign:compose-rui")).using(project(":"))
        substitute(module("art.rippledesign:compose-rui-navigation3")).using(project(":navigation3"))
    }
}
