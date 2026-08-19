package laoqi123.util.config;

import laoqi123.property.Property;

import java.util.List;

public interface PropertyProvider {
    List<Property<?>> getAdditionalProperties();
}
