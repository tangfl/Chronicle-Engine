package net.openhft.chronicle.engine.map;

import net.openhft.chronicle.core.util.SerializableBiFunction;
import net.openhft.chronicle.engine.api.map.MapView;
import net.openhft.chronicle.wire.AbstractMarshallable;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author Rob Austin.
 */
public class VaadinLambda<K, V> {

    public enum Type {
        key, value
    }

    public static class MarshableFilter extends AbstractMarshallable {
        public final Type type;
        public final Object value;

        public MarshableFilter(Type type, Object value) {
            this.type = type;
            this.value = value;
        }
    }

    public static class MarshableOrderBy extends AbstractMarshallable {
        public final String column;
        public final boolean isAscending;

        public MarshableOrderBy(String column, boolean isAscending) {
            this.column = column;
            this.isAscending = isAscending;
        }
    }

    public static class Query<K> {
        public long fromIndex;
        public List<MarshableOrderBy> marshableOrderBy = new ArrayList<>();
        public List<MarshableFilter> marshableFilters = new ArrayList<>();

        public boolean filter(@NotNull Map.Entry<K, ?> entry) {
            for (MarshableFilter f : marshableFilters) {

                Object item;

                if (f.type == Type.key) {
                    item = entry.getKey();
                } else if (f.type == Type.value) {
                    item = entry.getValue();

                } else {
                    throw new UnsupportedOperationException();
                }

                if (!item.toString().toLowerCase().contains(f.value.toString().toLowerCase()))
                    return false;

            }
            return true;
        }

        public Comparator<Map.Entry<K, ?>> sorted() {
            return this::compare;
        }

        private int compare(Map.Entry<K, ?> o1, Map.Entry<K, ?> o2) {
            for (MarshableOrderBy order : marshableOrderBy) {

                int result = 0;
                if ("key".equals(order.column))
                    if (o1.getKey() instanceof Number)
                        result = ((Comparable) o1.getKey()).compareTo(o2.getKey());
                    else
                        result = ((Comparable) o1.getKey().toString().toLowerCase()).compareTo(o2.getKey().toString().toLowerCase());

                else if ("value".equals(order.column))
                    if (o1.getValue() instanceof Number)
                        result = ((Comparable) o1.getValue()).compareTo(o2.getValue());
                    else
                        result = ((Comparable) o1.getValue().toString().toLowerCase()).compareTo(o2.getValue().toString().toLowerCase());
                result *= order.isAscending ? 1 : -1;
                if (result != 0)
                    return result;

            }
            return 0;
        }
    }


    @NotNull
    public static <K,V>
    SerializableBiFunction<MapView<K, V>, Query<K>, Iterator<Map.Entry<K, V>>>
    iteratorFunction() {
        return (MapView<K, V> kvMapView, Query<K> q) -> {

            Iterator<Map.Entry<K, V>> result = (Iterator) kvMapView.entrySet().stream()
                    .filter(q::filter)
                    .sorted(q.sorted())
                    .iterator();
            long x = 0;
            while (x++ < q.fromIndex && result.hasNext()) {
                result.next();
            }

            return result;
        };



    }


    @NotNull
    public static <K, V>
    SerializableBiFunction<MapView<K, V>, Query<K>, Long> countFunction() {
        return (MapView<K, V> mapView, Query<K> q) ->
                mapView.entrySet().stream()
                        .filter(q::filter)
                        .count();
    }


}
