package laoqi123.util.config;

import laoqi123.value.Value;

import java.util.List;

public interface PropertyProvider {
    List<Value<?>> getAdditionalProperties();
}
