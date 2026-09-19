plugins {
    id("com.android.application")
}

data class BrandConfig(
    val name: String,
    val applicationId: String,
    val shortDescription: String,
    val description: String,
    val webHost: String,
    val backendHost: String,
    val servicesHost: String,
    val isDefault: Boolean = false,
) {
    val firebasePushGateway: String
        get() = "https://$backendHost/_matrix/push/v1/notify"
}

val brands = listOf(
    BrandConfig(
        name = "apAksteel",
        applicationId = "ir.aksteel.poopak2",
        shortDescription = "aksteel",
        description = "poopak-aksteel",
        webHost = "poopak.aksteel.ir",
        backendHost = "pws.aksteel.ir",
        servicesHost = "pserv.aksteel.ir",
    ),
    BrandConfig(
        name = "apArmaniSanganco",
        applicationId = "ir.armanisanganco.poopak",
        shortDescription = "armani-sanganco",
        description = "poopak-armani-sanganco",
        webHost = "armani.sanganco.ir",
        backendHost = "armani-pws.sanganco.ir",
        servicesHost = "pserv.sanganco.ir",
    ),
    BrandConfig(
        name = "apBasa",
        applicationId = "ir.basa.poopak",
        shortDescription = "basa",
        description = "poopak-basa",
        webHost = "my.basa.ir",
        backendHost = "pws.basa.ir",
        servicesHost = "pserv.basa.ir",
    ),
    BrandConfig(
        name = "apCbasco",
        applicationId = "ir.cbasco.poopak",
        shortDescription = "cbasco",
        description = "poopak-cbasco",
        webHost = "poopak.cbasco.ir",
        backendHost = "pws.cbasco.ir",
        servicesHost = "pserv.cbasco.ir",
    ),
    BrandConfig(
        name = "apGoharzamin",
        applicationId = "com.goharzamin.popak",
        shortDescription = "goharzamin",
        description = "popak-goharzamin",
        webHost = "popak.goharzamin.com",
        backendHost = "pws.goharzamin.com",
        servicesHost = "pserv.goharzamin.com",
    ),
    BrandConfig(
        name = "apHosco",
        applicationId = "ir.hosco.poopak2",
        shortDescription = "hosco",
        description = "poopak-hosco",
        webHost = "poopak.hosco.ir",
        backendHost = "pws.hosco.ir",
        servicesHost = "pserv.hosco.ir",
    ),
    BrandConfig(
        name = "apJara",
        applicationId = "net.jahanara.poopak",
        shortDescription = "jahanara",
        description = "poopak-jahanara",
        webHost = "chat.jahanara.net",
        backendHost = "pws.jahanara.net",
        servicesHost = "pserv.jahanara.net",
    ),
    BrandConfig(
        name = "apMsc",
        applicationId = "ir.mymsc.poopak2",
        shortDescription = "msc",
        description = "poopak-msc",
        webHost = "my.msc.ir",
        backendHost = "ema.msc.ir",
        servicesHost = "pserv.msc.ir",
    ),
    BrandConfig(
        name = "apMyirisa",
        applicationId = "com.irisaco.im2",
        shortDescription = "irisa",
        description = "poopak-irisa",
        webHost = "web.irisaco.com",
        backendHost = "im.irisaco.com",
        servicesHost = "web.irisaco.com",
    ),
    BrandConfig(
        name = "apNghsco",
        applicationId = "com.nghsco.hamkar",
        shortDescription = "hamkar",
        description = "hamkar-nghsco",
        webHost = "hamkar.nghsco.com",
        backendHost = "pws.nghsco.com",
        servicesHost = "pserv.nghsco.com",
    ),
    BrandConfig(
        name = "apNikandishco",
        applicationId = "ir.nikandishco.my",
        shortDescription = "nikandishco",
        description = "poopak-nikandishco",
        webHost = "my.nikandishco.ir",
        backendHost = "pws.nikandishco.ir",
        servicesHost = "pserv.nikandishco.ir",
    ),
    BrandConfig(
        name = "apOxinsteel",
        applicationId = "ir.oxinsteel.im",
        shortDescription = "oxinsteel",
        description = "poopak-oxinsteel",
        webHost = "im.oxinsteel.ir",
        backendHost = "pws.oxinsteel.ir",
        servicesHost = "pserv.oxinsteel.ir",
    ),
    BrandConfig(
        name = "apPoopak",
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
        applicationId = "ir.sanganco.poopak2",
        shortDescription = "sanganco",
        description = "poopak-sanganco",
        webHost = "poopak.sanganco.ir",
        backendHost = "pws.sanganco.ir",
        servicesHost = "pserv.sanganco.ir",
    ),
    BrandConfig(
        name = "apSdsteel",
        applicationId = "ir.sdsteel.poopak2",
        shortDescription = "sdsteel",
        description = "poopak-sdsteel",
        webHost = "poopak.sdsteel.ir",
        backendHost = "pws.sdsteel.ir",
        servicesHost = "pserv.sdsteel.ir",
    ),
    BrandConfig(
        name = "apSjsco",
        applicationId = "ir.sjsco.poopak",
        shortDescription = "sjsco",
        description = "poopak-sjsco",
        webHost = "my.sjsco.ir",
        backendHost = "pws.sjsco.ir",
        servicesHost = "pserv.sjsco.ir",
    ),
    BrandConfig(
        name = "apWasco",
        applicationId = "ir.wasco.poopak",
        shortDescription = "wasco",
        description = "poopak-wasco",
        webHost = "poopak.wasco.ir",
        backendHost = "pws.wasco.ir",
        servicesHost = "pserv.wasco.ir",
    ),
    BrandConfig(
        name = "apZob",
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
                resValue("string", "brand_backend_host", brand.backendHost)
                resValue("string", "brand_services_host", brand.servicesHost)
                resValue("string", "brand_firebase_push_gateway", brand.firebasePushGateway)
            }
        }
    }
}
