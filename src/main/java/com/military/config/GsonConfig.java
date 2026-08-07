package com.military.config;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.springframework.boot.autoconfigure.gson.GsonBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDate;

@Configuration
public class GsonConfig {

  /**
   * Gson has no built-in adapter for java.time types and its reflective fallback
   * cannot access java.time internals on Java 17+ (module java.base does not open
   * java.time), which throws JsonIOException at serialization time. Serialize
   * LocalDate as an ISO-8601 string instead.
   */
  @Bean
  public GsonBuilderCustomizer localDateGsonCustomizer() {
    return builder -> builder.registerTypeAdapter(LocalDate.class, new LocalDateAdapter().nullSafe());
  }

  private static final class LocalDateAdapter extends TypeAdapter<LocalDate> {
    @Override
    public void write(JsonWriter out, LocalDate value) throws IOException {
      out.value(value.toString());
    }

    @Override
    public LocalDate read(JsonReader in) throws IOException {
      return LocalDate.parse(in.nextString());
    }
  }
}
