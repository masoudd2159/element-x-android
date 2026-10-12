import com.android.build.api.artifact.SingleArtifact
import com.github.gradle.node.NodeExtension
import extension.BrandIconsTask
import extension.InstallBrandingToolsTask
import extension.ValidateBrandArtworkTask
import extension.VerifyBrandPackageTask
import extension.VerifyBrandSourceTask
import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("com.android.application")
    id("com.github.node-gradle.node")
    id("poopak.android-localization")
}

data class BrandConfig(
    val name: String,
    val appName: String,
    val applicationId: String,
    val shortDescription: String,
    val description: String,
    val webHost: String,
    val backendHost: String,
    val servicesHost: String,
    val isDefault: Boolean = false,
    val centralizedBranding: Boolean = false,
    val iconBackground: String = "#FFFFFF",
    val adaptiveIconExtent: Int = 60,
    val logoExtentFraction: Double = 314.0 / 330,
) {
    val firebasePushGateway: String
        get() = "https://$backendHost/_matrix/push/v1/notify"
}

val brands = listOf(
    BrandConfig(
        name = "apAksteel",
        appName = "پوپک امیرکبیر",
        applicationId = "ir.aksteel.poopak2",
        shortDescription = "aksteel",
        description = "poopak-aksteel",
        webHost = "poopak.aksteel.ir",
        backendHost = "pws.aksteel.ir",
        servicesHost = "pserv.aksteel.ir",
    ),
    BrandConfig(
        name = "apArmaniSanganco",
        appName = "خانواده آرمانی سنگان",
        applicationId = "ir.armanisanganco.poopak",
        shortDescription = "armani-sanganco",
        description = "poopak-armani-sanganco",
        webHost = "armani.sanganco.ir",
        backendHost = "armani-pws.sanganco.ir",
        servicesHost = "pserv.sanganco.ir",
    ),
    BrandConfig(
        name = "apBasa",
        appName = "باسای من",
        applicationId = "ir.basa.poopak",
        shortDescription = "basa",
        description = "poopak-basa",
        webHost = "my.basa.ir",
        backendHost = "pws.basa.ir",
        servicesHost = "pserv.basa.ir",
    ),
    BrandConfig(
        name = "apCbasco",
        appName = "پوپک ورق خودرو",
        applicationId = "ir.cbasco.poopak",
        shortDescription = "cbasco",
        description = "poopak-cbasco",
        webHost = "poopak.cbasco.ir",
        backendHost = "pws.cbasco.ir",
        servicesHost = "pserv.cbasco.ir",
    ),
    BrandConfig(
        name = "apGoharzamin",
        appName = "پوپک گهرزمین",
        applicationId = "com.goharzamin.popak",
        shortDescription = "goharzamin",
        description = "popak-goharzamin",
        webHost = "popak.goharzamin.com",
        backendHost = "pws.goharzamin.com",
        servicesHost = "pserv.goharzamin.com",
    ),
    BrandConfig(
        name = "apHoopoe",
        centralizedBranding = true,
        appName = "پوپک آمریکا",
        applicationId = "com.hoopoeim",
        shortDescription = "hoopoe",
        description = "hoopoe",
        webHost = "app.hoopoeim.com",
        backendHost = "hoopoeim.com",
        servicesHost = "hoopoeim.com",
    ),
    BrandConfig(
        name = "apHosco",
        appName = "پوپک فولاد هرمزگان",
        applicationId = "ir.hosco.poopak2",
        shortDescription = "hosco",
        description = "poopak-hosco",
        webHost = "poopak.hosco.ir",
        backendHost = "pws.hosco.ir",
        servicesHost = "pserv.hosco.ir",
    ),
    BrandConfig(
        name = "apJara",
        appName = "جهان آرای من",
        applicationId = "net.jahanara.poopak",
        shortDescription = "jahanara",
        description = "poopak-jahanara",
        webHost = "chat.jahanara.net",
        backendHost = "pws.jahanara.net",
        servicesHost = "pserv.jahanara.net",
    ),
    BrandConfig(
        name = "apMsc",
        appName = "فولاد من",
        applicationId = "ir.mymsc.poopak2",
        shortDescription = "msc",
        description = "poopak-msc",
        webHost = "my.msc.ir",
        backendHost = "ema.msc.ir",
        servicesHost = "pserv.msc.ir",
    ),
    BrandConfig(
        name = "apMyirisa",
        appName = "ایریسای من",
        applicationId = "com.irisaco.im2",
        shortDescription = "irisa",
        description = "poopak-irisa",
        webHost = "web.irisaco.com",
        backendHost = "im.irisaco.com",
        servicesHost = "web.irisaco.com",
    ),
    BrandConfig(
        name = "apNghsco",
        appName = "پیام‌رسان همکار",
        applicationId = "com.nghsco.hamkar",
        shortDescription = "hamkar",
        description = "hamkar-nghsco",
        webHost = "hamkar.nghsco.com",
        backendHost = "pws.nghsco.com",
        servicesHost = "pserv.nghsco.com",
    ),
    BrandConfig(
        name = "apNikandishco",
        appName = "نیک اندیش من",
        applicationId = "ir.nikandishco.my",
        shortDescription = "nikandishco",
        description = "poopak-nikandishco",
        webHost = "my.nikandishco.ir",
        backendHost = "pws.nikandishco.ir",
        servicesHost = "pserv.nikandishco.ir",
    ),
    BrandConfig(
        name = "apOxinsteel",
        appName = "اکسین من",
        applicationId = "ir.oxinsteel.im",
        shortDescription = "oxinsteel",
        description = "poopak-oxinsteel",
        webHost = "im.oxinsteel.ir",
        backendHost = "pws.oxinsteel.ir",
        servicesHost = "pserv.oxinsteel.ir",
    ),
    BrandConfig(
        name = "apPoopak",
        appName = "پوپک",
        applicationId = "ir.mypoopak",
        shortDescription = "poopak",
        description = "poopak",
        webHost = "mypoopak.ir",
        backendHost = "api.mypoopak.ir",
        servicesHost = "serv.mypoopak.ir",
        isDefault = true,
    ),
    BrandConfig(
        name = "apSanganco",
        appName = "پوپک سنگان",
        applicationId = "ir.sanganco.poopak2",
        shortDescription = "sanganco",
        description = "poopak-sanganco",
        webHost = "poopak.sanganco.ir",
        backendHost = "pws.sanganco.ir",
        servicesHost = "pserv.sanganco.ir",
    ),
    BrandConfig(
        name = "apSdsteel",
        appName = "پوپک سفید دشت",
        applicationId = "ir.sdsteel.poopak2",
        shortDescription = "sdsteel",
        description = "poopak-sdsteel",
        webHost = "poopak.sdsteel.ir",
        backendHost = "pws.sdsteel.ir",
        servicesHost = "pserv.sdsteel.ir",
    ),
    BrandConfig(
        name = "apSjsco",
        appName = "جهان فولاد من",
        applicationId = "ir.sjsco.poopak",
        shortDescription = "sjsco",
        description = "poopak-sjsco",
        webHost = "my.sjsco.ir",
        backendHost = "pws.sjsco.ir",
        servicesHost = "pserv.sjsco.ir",
    ),
    BrandConfig(
        name = "apWasco",
        appName = "پوپک غرب آسیا",
        applicationId = "ir.wasco.poopak",
        shortDescription = "wasco",
        description = "poopak-wasco",
        webHost = "poopak.wasco.ir",
        backendHost = "pws.wasco.ir",
        servicesHost = "pserv.wasco.ir",
    ),
    BrandConfig(
        name = "apZob",
        appName = "پیام‌رسان ذوب",
        applicationId = "app.zob",
        shortDescription = "zob",
        description = "poopak-zob",
        webHost = "zob.app",
        backendHost = "pws.zob.app",
        servicesHost = "pserv.zob.app",
    ),
)

android {
    flavorDimensions += "brand"

    productFlavors {
        brands.forEach { brand ->
            create(brand.name) {
                dimension = "brand"
                applicationId = brand.applicationId
                manifestPlaceholders["brandLauncher"] = if (brand.centralizedBranding) "@mipmap/brand_launcher" else "@mipmap/ic_launcher"
                manifestPlaceholders["brandLauncherRound"] = if (brand.centralizedBranding) "@mipmap/brand_launcher_round" else "@mipmap/ic_launcher_round"

                if (brand.isDefault) {
                    isDefault = true
                }

                buildConfigField("String", "BRAND_ID", "\"${brand.name}\"")
                buildConfigField("String", "BRAND_SHORT_DESCRIPTION", "\"${brand.shortDescription}\"")
                buildConfigField("String", "BRAND_DESCRIPTION", "\"${brand.description}\"")
                buildConfigField("String", "SHORT_FLAVOR_DESCRIPTION", "\"${brand.shortDescription}\"")
                buildConfigField("String", "FLAVOR_DESCRIPTION", "\"${brand.description}\"")
                buildConfigField("String", "WEB_HOST", "\"${brand.webHost}\"")
                buildConfigField("String", "BACKEND_HOST", "\"${brand.backendHost}\"")
                buildConfigField("String", "SERVICES_HOST", "\"${brand.servicesHost}\"")

                resValue("string", "brand_web_host", brand.webHost)
                resValue(
                    "string",
                    "app_name",
                    brand.appName,
                )
                resValue("string", "brand_backend_host", brand.backendHost)
                resValue("string", "brand_services_host", brand.servicesHost)
                resValue("string", "brand_firebase_push_gateway", brand.firebasePushGateway)
            }
        }
    }
}

// Opt-in migration: only enrolled flavors require the three canonical artwork files or strict package verification.
val activeBrands = brands.filter { it.centralizedBranding }
brands.filterNot { it.centralizedBranding }.forEach { brand ->
    dependencies.add("${brand.name}Implementation", project(":appicon:enterprise"))
    android.sourceSets.getByName(brand.name).res.directories.add("src/legacyBranding/res")
}
val managedNode = extensions.getByType<NodeExtension>().apply {
    version.set("24.12.0")
    download.set(true)
    distBaseUrl.set(null as String?) // Distribution repository is centrally declared in settings.
    enableTaskRules.set(false)
    workDir.set(rootProject.layout.projectDirectory.dir(".gradle/branding-node"))
}
val windowsNode = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
val brandingNode = managedNode.resolvedNodeDir.map { it.file(if (windowsNode) "node.exe" else "bin/node") }
val brandingNpm = managedNode.resolvedNodeDir.map {
    it.file(if (windowsNode) "node_modules/npm/bin/npm-cli.js" else "lib/node_modules/npm/bin/npm-cli.js")
}
// The bundled npm version is pinned by the Node distribution. No shell PATH lookup is used.
val brandingToolingFiles = rootProject.fileTree("tools/branding") {
    include("*.cjs", "*.json")
    exclude("*.test.cjs")
}
val brandingPlatform = "Node 24.12.0/${System.getProperty("os.name")}/${System.getProperty("os.arch")}"
val brandingCallAssets = configurations.create("brandingCallAssets") {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}
dependencies.add(
    brandingCallAssets.name,
    extensions.getByType<VersionCatalogsExtension>().named("libs").findLibrary("element-call-embedded").get(),
)
val installBrandingTools = tasks.register<InstallBrandingToolsTask>("installBrandingTools") {
    dependsOn("nodeSetup")
    nodeExecutable.set(brandingNode)
    npmCli.set(brandingNpm)
    toolingFiles.from(brandingToolingFiles)
    platform.set(brandingPlatform)
    outputDirectory.set(rootProject.layout.buildDirectory.dir("brandingTools"))
}
val verifyBrandSource = activeBrands.associate { brand ->
    brand.name to tasks.register<VerifyBrandSourceTask>("verify${brand.name.replaceFirstChar(Char::uppercaseChar)}BrandSource") {
        dependsOn(installBrandingTools)
        group = "verification"
        repositoryDirectory.set(rootProject.layout.projectDirectory)
        nodeExecutable.set(brandingNode)
        toolingFiles.from(brandingToolingFiles)
        toolingDirectory.set(installBrandingTools.flatMap { it.outputDirectory })
        sourceFiles.from(rootProject.fileTree(rootProject.projectDir) {
            listOf("main", "debug", "release", "nightly", brand.name).forEach { include("app/src/$it/**") }
            listOf("appnav", "appconfig", "libraries", "features", "services").forEach {
                include("$it/**/src/**", "$it/**/build.gradle.kts", "$it/build.gradle.kts")
            }
            include("app/build.gradle.kts", "plugins/src/main/kotlin/poopak.android-brands.gradle.kts")
            include("branding/${brand.name}/logo.svg", "branding/${brand.name}/monochrome.xml", "branding/${brand.name}/notification.xml")
            exclude("**/build/**", "**/.gradle/**", "**/.git/**", "**/node_modules/**")
            exclude("**/src/test/**", "**/src/testFixtures/**", "**/src/androidTest/**")
        })
        receipt.set(layout.buildDirectory.file("brandingVerification/${brand.name}/source.txt"))
    }
}
val generateBrandIcons = activeBrands.associate { brand ->
    brand.name to tasks.register<BrandIconsTask>("generate${brand.name.replaceFirstChar(Char::uppercaseChar)}BrandIcons") {
        dependsOn(installBrandingTools)
        embeddedCallAar.set(layout.file(brandingCallAssets.elements.map { it.single().asFile }))
        group = "branding"
        description = "Generate approved icons for ${brand.name}; missing artwork fails every build type."
        brandId.set(brand.name)
        nodeExecutable.set(brandingNode)
        background.set(brand.iconBackground)
        extent.set(brand.adaptiveIconExtent)
        logoFraction.set(brand.logoExtentFraction)
        sourceDirectory.set(rootProject.layout.projectDirectory.dir("branding/${brand.name}"))
        artwork.from(rootProject.fileTree("branding/${brand.name}") { include("logo.svg", "monochrome.xml", "notification.xml") })
        toolingFiles.from(brandingToolingFiles)
        toolingDirectory.set(installBrandingTools.flatMap { it.outputDirectory })
        platform.set(brandingPlatform)
        outputDirectory.set(layout.buildDirectory.dir("generated/brandIcons/${brand.name}"))
        assetsDirectory.set(layout.buildDirectory.dir("generated/brandAssets/${brand.name}"))
    }
}
val validateBrandArtwork = activeBrands.map { brand ->
    tasks.register<ValidateBrandArtworkTask>("validate${brand.name.replaceFirstChar(Char::uppercaseChar)}BrandArtwork") {
        dependsOn(installBrandingTools)
        sourceDirectory.set(rootProject.layout.projectDirectory.dir("branding/${brand.name}"))
        artwork.from(rootProject.fileTree("branding/${brand.name}") { include("logo.svg", "monochrome.xml", "notification.xml") })
        toolingFiles.from(brandingToolingFiles)
        toolingDirectory.set(installBrandingTools.flatMap { it.outputDirectory })
        nodeExecutable.set(brandingNode)
        receipt.set(layout.buildDirectory.file("brandingVerification/${brand.name}/artwork.txt"))
    }
}
tasks.register("validateAllBrandArtwork") {
    group = "branding"
    dependsOn(validateBrandArtwork)
}

androidComponents {
    onVariants { variant ->
        val brand = variant.productFlavors.single { it.first == "brand" }.second
        if (brand !in generateBrandIcons) return@onVariants
        check(variant.minSdk.apiLevel >= 26) { "Brand icons require adaptive icon support (minSdk >= 26)." }
        variant.sources.res?.addGeneratedSourceDirectory(generateBrandIcons.getValue(brand), BrandIconsTask::outputDirectory)
        variant.sources.assets?.addGeneratedSourceDirectory(generateBrandIcons.getValue(brand), BrandIconsTask::assetsDirectory)
        val capitalizedVariant = variant.name.replaceFirstChar(Char::uppercaseChar)
        tasks.matching { it.name == "pre${capitalizedVariant}Build" }.configureEach { dependsOn(verifyBrandSource.getValue(brand)) }
        listOf(
            "Apk" to variant.artifacts.get(SingleArtifact.APK),
            "Bundle" to variant.artifacts.get(SingleArtifact.BUNDLE),
        ).forEach { (type, artifact) ->
            val verifyPackage = tasks.register<VerifyBrandPackageTask>("verify${capitalizedVariant}Brand$type") {
                group = "verification"
                dependsOn(verifyBrandSource.getValue(brand))
                packages.from(artifact)
                generatedResources.set(generateBrandIcons.getValue(brand).flatMap { it.outputDirectory })
                generatedAssets.set(generateBrandIcons.getValue(brand).flatMap { it.assetsDirectory })
                androidJar.set(androidComponents.sdkComponents.bootClasspath.map { it.first() })
                aapt2.set(androidComponents.sdkComponents.aapt2.flatMap { it.executable })
                script.set(rootProject.layout.projectDirectory.file("tools/branding/verify-apk.py"))
            }
            val buildTask = if (type == "Apk") "assemble$capitalizedVariant" else "bundle$capitalizedVariant"
            tasks.matching { it.name == buildTask }.configureEach { dependsOn(verifyPackage) }
        }
    }
}
