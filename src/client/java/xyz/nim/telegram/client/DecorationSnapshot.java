package xyz.nim.telegram.client;

import java.util.Objects;

public record DecorationSnapshot(String type, double x, double z, double rotation, String name) {
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DecorationSnapshot that)) return false;
        return Double.compare(x, that.x) == 0 &&
               Double.compare(z, that.z) == 0 &&
               Double.compare(rotation, that.rotation) == 0 &&
               Objects.equals(type, that.type) &&
               Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, x, z, rotation, name);
    }
}
