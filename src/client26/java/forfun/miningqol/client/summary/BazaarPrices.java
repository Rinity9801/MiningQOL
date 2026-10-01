package forfun.miningqol.client.summary;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gemstone prices off the Hypixel bazaar, refreshed in the background at most once a
 * minute. Only the {@code *_GEM} products are kept.
 */
public final class BazaarPrices {
    private static final Logger LOGGER = LoggerFactory.getLogger("MiningQOL");
    private static final String URL = "https://api.hypixel.net/v2/skyblock/bazaar";
    private static final long MIN_REFRESH_MS = 60_000L;

    private static final HttpClient CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();
    /** product id -> {sellPrice, buyPrice}. */
    private static final Map<String, double[]> PRICES = new ConcurrentHashMap<>();
    private static volatile long lastUpdate;
    private static volatile boolean inFlight;

    private BazaarPrices() {}

    public static void refresh() {
        long now = System.currentTimeMillis();
        if (inFlight || now - lastUpdate < MIN_REFRESH_MS) return;
        inFlight = true;
        HttpRequest request = HttpRequest.newBuilder(URI.create(URL))
            .timeout(Duration.ofSeconds(15))
            .GET()
            .build();
        CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenAccept(response -> {
                try {
                    if (response.statusCode() == 200) {
                        parse(response.body());
                        lastUpdate = System.currentTimeMillis();
                    } else {
                        LOGGER.warn("[ShaftSummary] Bazaar fetch failed: HTTP {}", response.statusCode());
                    }
                } finally {
                    inFlight = false;
                }
            })
            .exceptionally(e -> {
                inFlight = false;
                LOGGER.warn("[ShaftSummary] Bazaar fetch failed", e);
                return null;
            });
    }

    private static void parse(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonObject products = root.getAsJsonObject("products");
            if (products == null) return;
            for (Map.Entry<String, JsonElement> entry : products.entrySet()) {
                String id = entry.getKey();
                if (!id.endsWith("_GEM")) continue;
                JsonObject quick = entry.getValue().getAsJsonObject().getAsJsonObject("quick_status");
                if (quick == null) continue;
                double sell = quick.has("sellPrice") ? quick.get("sellPrice").getAsDouble() : 0.0;
                double buy = quick.has("buyPrice") ? quick.get("buyPrice").getAsDouble() : 0.0;
                PRICES.put(id, new double[]{sell, buy});
            }
        } catch (Exception e) {
            LOGGER.warn("[ShaftSummary] Bazaar parse failed", e);
        }
    }

    /**
     * Unit price for a gem product, or 0 when unknown.
     *
     * @param sellOffer true for the sell-offer price, false for the instant-sell price
     */
    public static double price(String productId, boolean sellOffer) {
        double[] p = PRICES.get(productId);
        if (p == null) return 0.0;
        return sellOffer ? p[0] : p[1];
    }

    /** {@code FINE_JASPER_GEM} style ids. */
    public static String productId(String gem, String quality) {
        return quality.toUpperCase(Locale.ROOT) + "_" + gem.replace(' ', '_').toUpperCase(Locale.ROOT) + "_GEM";
    }

    public static boolean hasPrices() {
        return !PRICES.isEmpty();
    }
}
