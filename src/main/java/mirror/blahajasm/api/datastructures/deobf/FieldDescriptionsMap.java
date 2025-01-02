package mirror.blahajasm.api.datastructures.deobf;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import mirror.blahajasm.api.datastructures.canonical.AutoCanonizingArrayMap;
import mirror.blahajasm.api.BlahajStringPool;

import java.util.Map;

/**
 * This replaces the fieldDescriptions map in the remapper, it canonicalizes the inner maps and the inner maps' strings
 */
public class FieldDescriptionsMap extends Object2ObjectOpenHashMap<String, Map<String, String>> {

    // TODO: Move to deduplicator
    private static final ObjectOpenHashSet<AutoCanonizingArrayMap<String, String>> innerMapCanonicalCache = new ObjectOpenHashSet<>();

    public FieldDescriptionsMap(Map<String, Map<String, String>> startingMap) {
        super(startingMap);
    }

    @Override
    public Map<String, String> put(String s, Map<String, String> innerMap) {
        s = BlahajStringPool.canonicalize(s);
        if (!(innerMap instanceof Object2ObjectArrayMap)) {
            innerMap = innerMapCanonicalCache.addOrGet(new AutoCanonizingArrayMap<>(innerMap));
        }
        return super.put(s, innerMap);
    }
}