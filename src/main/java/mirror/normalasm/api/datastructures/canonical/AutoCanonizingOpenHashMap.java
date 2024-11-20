package mirror.normalasm.api.datastructures.canonical;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import mirror.normalasm.api.NormalStringPool;

import java.util.Map;

public class AutoCanonizingOpenHashMap<K, V> extends Object2ObjectOpenHashMap<K, V> {

    public AutoCanonizingOpenHashMap() {
        super();
    }

    public AutoCanonizingOpenHashMap(Map<K, V> map) {
        super(map);
    }

    @Override
    public V put(K k, V v) {
        if (k instanceof String) {
            k = (K) NormalStringPool.canonicalize((String) k);
        }
        if (v instanceof String) {
            v = (V) NormalStringPool.canonicalize((String) v);
        }
        return super.put(k, v);
    }
}