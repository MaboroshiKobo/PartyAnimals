package org.maboroshi.partyanimals.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.CompletableFuture;
import org.maboroshi.partyanimals.PartyAnimals;

public class UpdateChecker {
    private final PartyAnimals plugin;

    public UpdateChecker(PartyAnimals plugin) {
        this.plugin = plugin;
    }

    public void checkForUpdates() {
        CompletableFuture.supplyAsync(() -> {
                    try {
                        HttpClient client = HttpClient.newHttpClient();
                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(
                                        "https://api.github.com/repos/MaboroshiKobo/PartyAnimals/releases/latest"))
                                .build();
                        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
                        if (response.statusCode() != 200) {
                            throw new RuntimeException("GitHub returned code " + response.statusCode());
                        }
                        return response.body();
                    } catch (Exception e) {
                        plugin.getPluginLogger().warn("Update check failed: " + e.getMessage());
                        return null;
                    }
                })
                .thenAccept(jsonResponse -> {
                    if (jsonResponse == null) return;
                    JsonObject json = JsonParser.parseString(jsonResponse).getAsJsonObject();
                    if (json.has("tag_name")) {
                        String tagName = json.get("tag_name").getAsString().replace("v", "");
                        String currentVersion = plugin.getPluginMeta().getVersion();

                        if (isNewer(currentVersion, tagName)) {
                            plugin.getPluginLogger()
                                    .warn("A new version is available! (Current: " + currentVersion + " | Latest: "
                                            + tagName + ")");
                            plugin.getPluginLogger()
                                    .warn(
                                            "Download it at: https://github.com/MaboroshiKobo/PartyAnimals/releases/latest");
                        }
                    }
                });
    }

    private boolean isNewer(String current, String remote) {
        current = current.replace("v", "").split("-")[0];
        remote = remote.replace("v", "").split("-")[0];
        String[] currentParts = current.split("\\.");
        String[] remoteParts = remote.split("\\.");

        int length = Math.max(currentParts.length, remoteParts.length);
        for (int i = 0; i < length; i++) {
            int v1 = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
            int v2 = i < remoteParts.length ? Integer.parseInt(remoteParts[i]) : 0;
            if (v2 > v1) return true;
            if (v2 < v1) return false;
        }
        return false;
    }
}
