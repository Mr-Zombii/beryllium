package me.zombii.beryllium.client;

import finalforeach.cosmicreach.util.Identifier;

import java.util.HashMap;

public class IdentifierCache {
    public static HashMap<String, Identifier> cache = new HashMap<>();

    ///Full id for the identifier namespace : name
    public static Identifier getOrInsert(String id) {
        if (id == null) {
            return null;
        }
        Identifier ident = cache.get(id);
        if (ident == null) {
            Identifier n = Identifier.of(id);
            cache.put(id, n );
            return n;
        }
        return ident;

    }
    public static void insert(Identifier identifier) {
        if (identifier == null) {
            return;
        }
        cache.put(identifier.toString(), identifier);
    }
}
