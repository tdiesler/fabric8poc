package io.fabric8.test.smoke.sub.c;

import java.beans.ConstructorProperties;

public class Bean {

    private final String name;
    private final String value;

    @ConstructorProperties({ "name", "value" })
    public Bean(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof Bean))
            return false;
        Bean other = (Bean) obj;
        return name.equals(other.name) && value.equals(other.value);
    }

    @Override
    public String toString() {
        return "Bean[name=" + name + ",value=" + value + "]";
    }
}