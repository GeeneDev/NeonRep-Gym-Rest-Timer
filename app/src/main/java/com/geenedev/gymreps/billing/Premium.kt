package com.geenedev.gymreps.billing

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

/**
 * The one-time "GymReps Full" unlock, sold through Google Play Billing as a non-consumable
 * in-app product. The owned state is cached on the device so the app stays unlocked offline;
 * Play is asked again on every launch, so a refund locks it again.
 */
class Premium private constructor(context: Context) {
    private val prefs = context.getSharedPreferences("freemium", Context.MODE_PRIVATE)

    /** True once the unlock is bought (cached). */
    var unlocked by mutableStateOf(prefs.getBoolean(KEY_UNLOCKED, false))
        private set

    /** Localized price from Play, e.g. "€2.99"; null until Play answers (or if not set up). */
    var price by mutableStateOf<String?>(null)
        private set

    /** Wall-clock ms of the last "Skip" on the popup; 0 if never. */
    var lastSkipAt: Long
        get() = prefs.getLong(KEY_LAST_SKIP, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SKIP, value).apply()

    private var product: ProductDetails? = null

    private val client: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) handle(purchases.orEmpty())
            else if (result.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) refresh()
        }
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    init {
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    refresh()
                    loadProduct()
                }
            }

            override fun onBillingServiceDisconnected() = Unit // auto-reconnects
        })
    }

    /**
     * Opens Play's purchase sheet. Returns false when the store isn't reachable or the product
     * isn't set up in Play Console yet (the caller then says so).
     */
    fun buy(activity: Activity): Boolean {
        val details = product ?: run { loadProduct(); return false }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).build()),
            )
            .build()
        return client.launchBillingFlow(activity, params).responseCode == BillingClient.BillingResponseCode.OK
    }

    /** Asks Play what this account owns (also restores the unlock after a reinstall). */
    fun refresh() {
        if (!client.isReady) return
        client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
        ) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryPurchasesAsync
            val owned = purchases.any { PRODUCT_ID in it.products && it.purchaseState == Purchase.PurchaseState.PURCHASED }
            storeUnlocked(owned)
            handle(purchases)
        }
    }

    private fun loadProduct() {
        if (!client.isReady) return
        val params = QueryProductDetailsParams.newBuilder().setProductList(
            listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PRODUCT_ID)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build(),
            ),
        ).build()
        client.queryProductDetailsAsync(params) { result, details ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryProductDetailsAsync
            product = details.productDetailsList.firstOrNull { it.productId == PRODUCT_ID }
            price = product?.oneTimePurchaseOfferDetails?.formattedPrice
        }
    }

    private fun handle(purchases: List<Purchase>) {
        for (p in purchases) {
            if (PRODUCT_ID !in p.products || p.purchaseState != Purchase.PurchaseState.PURCHASED) continue
            storeUnlocked(true)
            // Unacknowledged purchases are refunded by Play after 3 days.
            if (!p.isAcknowledged) {
                client.acknowledgePurchase(
                    AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build(),
                ) { }
            }
        }
    }

    // Billing callbacks can arrive on a background thread; Compose snapshot state is safe to set.
    private fun storeUnlocked(value: Boolean) {
        unlocked = value
        prefs.edit().putBoolean(KEY_UNLOCKED, value).apply()
    }

    companion object {
        @Volatile private var instance: Premium? = null

        /** One billing connection per app process (survives activity recreation). */
        fun get(context: Context): Premium =
            instance ?: synchronized(this) { instance ?: Premium(context.applicationContext).also { instance = it } }

        /** Must match the in-app product id created in Play Console. */
        const val PRODUCT_ID = "gymreps_full"
        private const val KEY_UNLOCKED = "unlocked"
        private const val KEY_LAST_SKIP = "last_skip_at"
    }
}
