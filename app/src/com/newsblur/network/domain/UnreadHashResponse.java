package com.newsblur.network.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;

public class UnreadHashResponse {

    @SerializedName("unread_feed_story_hashes")
    public Map<String, List<String>> unreadHashes;

    public Map<String, Long> flatHashList = new HashMap<String, Long>();

    public boolean authenticated;

    public UnreadHashResponse(String json, Gson gson) {
        unreadHashes = new HashMap<String, List<String>>();

        JsonParser parser = new JsonParser();
        JsonObject asJsonObject = parser.parse(json).getAsJsonObject();

        this.authenticated = asJsonObject.get("authenticated").getAsBoolean();

        JsonObject feedObject = (JsonObject) asJsonObject.get("unread_feed_story_hashes");
        Set<Entry<String, JsonElement>> jsonEntrySet = feedObject.entrySet();

        for (Entry<java.lang.String, JsonElement> entry : jsonEntrySet) {
            String key = entry.getKey();
            List<String> strings = new ArrayList<String>();

            for (JsonElement e : entry.getValue().getAsJsonArray()) {
                JsonArray arr = e.getAsJsonArray();
                strings.add(arr.get(0).getAsString());
                flatHashList.put(arr.get(0).getAsString(), (long) arr.get(1).getAsBigDecimal().intValue());
            }

            unreadHashes.put(key, strings);
        }
    }
}
