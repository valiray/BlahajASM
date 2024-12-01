package mirror.blahajasm.api.datastructures.canonical;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import mirror.blahajasm.api.BlahajStringPool;

import java.util.Map;

public class AutoCanonizingArrayMap<K, V> extends Object2ObjectArrayMap<K, V> {

    public AutoCanonizingArrayMap() {
        super();
    }

    public AutoCanonizingArrayMap(Map<K, V> map) {
        super(map);
    }

    @Override
    public V put(K k, V v) {
        if (k instanceof String) {
            k = (K) BlahajStringPool.canonicalize((String) k);
        }
        if (v instanceof String) {
            v = (V) BlahajStringPool.canonicalize((String) v);
        }
        return super.put(k, v);
    }
}
