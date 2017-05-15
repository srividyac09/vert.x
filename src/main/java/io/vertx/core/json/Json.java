/*
 * Copyright (c) 2011-2014 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 *     The Eclipse Public License is available at
 *     http://www.eclipse.org/legal/epl-v10.html
 *
 *     The Apache License v2.0 is available at
 *     http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */

package io.vertx.core.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.vertx.core.buffer.Buffer;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class Json {

  public static ObjectMapper mapper = new ObjectMapper();
  public static ObjectMapper prettyMapper = new ObjectMapper();

  static {
    // Non-standard JSON but we allow C style comments in our JSON
    mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);

    prettyMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
    prettyMapper.configure(SerializationFeature.INDENT_OUTPUT, true);

    SimpleModule module = new SimpleModule();
    // custom types
    module.addSerializer(JsonObject.class, new JsonObjectSerializer());
    module.addSerializer(JsonArray.class, new JsonArraySerializer());
    // he have 2 extensions: RFC-7493
    module.addSerializer(Instant.class, new InstantSerializer());
    module.addSerializer(byte[].class, new ByteArraySerializer());

    mapper.registerModule(module);
    prettyMapper.registerModule(module);
  }

  /**
   * Encode a POJO to JSON using the underlying Jackson mapper.
   *
   * @param obj a POJO
   * @return a String containing the JSON representation of the given POJO.
   * @throws EncodeException if a property cannot be encoded.
   */
  public static String encode(Object obj) throws EncodeException {
    try {
      return mapper.writeValueAsString(obj);
    } catch (Exception e) {
      throw new EncodeException("Failed to encode as JSON: " + e.getMessage());
    }
  }

  /**
   * Encode a POJO to JSON using the underlying Jackson mapper.
   *
   * @param obj a POJO
   * @return a Buffer containing the JSON representation of the given POJO.
   * @throws EncodeException if a property cannot be encoded.
   */
  public static Buffer encodeToBuffer(Object obj) throws EncodeException {
    try {
      Buffer buf = Buffer.buffer();
      mapper.writeValue(new ByteBufOutputStream(buf.getByteBuf()), obj);
      return buf;
    } catch (Exception e) {
      throw new EncodeException("Failed to encode as JSON: " + e.getMessage());
    }
  }

  /**
   * Encode a POJO to JSON with pretty indentation, using the underlying Jackson mapper.
   *
   * @param obj a POJO
   * @return a String containing the JSON representation of the given POJO.
   * @throws EncodeException if a property cannot be encoded.
   */
  public static String encodePrettily(Object obj) throws EncodeException {
    try {
      return prettyMapper.writeValueAsString(obj);
    } catch (Exception e) {
      throw new EncodeException("Failed to encode as JSON: " + e.getMessage());
    }
  }

  /**
   * Decode a given JSON string to a POJO of the given class type.
   * @param str the JSON string.
   * @param clazz the class to map to.
   * @param <T> the generic type.
   * @return an instance of T
   * @throws DecodeException when there is a parsing or invalid mapping.
   */
  public static <T> T decodeValue(String str, Class<T> clazz) throws DecodeException {
    try {
      return mapper.readValue(str, clazz);
    } catch (Exception e) {
      throw new DecodeException("Failed to decode:" + e.getMessage());
    }
  }

  /**
   * Decode a given JSON string to a POJO of the given type.
   * @param str the JSON string.
   * @param type the type to map to.
   * @param <T> the generic type.
   * @return an instance of T
   * @throws DecodeException when there is a parsing or invalid mapping.
   */
  public static <T> T decodeValue(String str, TypeReference<T> type) throws DecodeException {
    try {
      return mapper.readValue(str, type);
    } catch (Exception e) {
      throw new DecodeException("Failed to decode:" + e.getMessage(), e);
    }
  }

  /**
   * Decode a given JSON buffer to a POJO of the given class type.
   * @param buf the JSON buffer.
   * @param type the type to map to.
   * @param <T> the generic type.
   * @return an instance of T
   * @throws DecodeException when there is a parsing or invalid mapping.
   */
  public static <T> T decodeValue(Buffer buf, TypeReference<T> type) throws DecodeException {
    try {
      return mapper.readValue(new ByteBufInputStream(buf.getByteBuf()), type);
    } catch (Exception e) {
      throw new DecodeException("Failed to decode:" + e.getMessage(), e);
    }
  }

  /**
   * Decode a given JSON buffer to a POJO of the given class type.
   * @param buf the JSON buffer.
   * @param clazz the class to map to.
   * @param <T> the generic type.
   * @return an instance of T
   * @throws DecodeException when there is a parsing or invalid mapping.
   */
  public static <T> T decodeValue(Buffer buf, Class<T> clazz) throws DecodeException {
    try {
      return mapper.readValue(new ByteBufInputStream(buf.getByteBuf()), clazz);
    } catch (Exception e) {
      throw new DecodeException("Failed to decode:" + e.getMessage(), e);
    }
  }

  @SuppressWarnings("unchecked")
  static Object checkAndCopy(Object val, boolean copy) {
    if (val == null) {
      // OK
    } else if (val instanceof Number && !(val instanceof BigDecimal)) {
      // OK
    } else if (val instanceof Boolean) {
      // OK
    } else if (val instanceof String) {
      // OK
    } else if (val instanceof Character) {
      // OK
    } else if (val instanceof CharSequence) {
      val = val.toString();
    } else if (val instanceof JsonObject) {
      if (copy) {
        val = ((JsonObject) val).copy();
      }
    } else if (val instanceof JsonArray) {
      if (copy) {
        val = ((JsonArray) val).copy();
      }
    } else if (val instanceof Map) {
      if (copy) {
        val = (new JsonObject((Map)val)).copy();
      } else {
        val = new JsonObject((Map)val);
      }
    } else if (val instanceof List) {
      if (copy) {
        val = (new JsonArray((List)val)).copy();
      } else {
        val = new JsonArray((List)val);
      }
    } else if (val instanceof byte[]) {
      val = Base64.getEncoder().encodeToString((byte[])val);
    } else if (val instanceof Instant) {
      val = ISO_INSTANT.format((Instant) val);
    } else {
      throw new IllegalStateException("Illegal type in JsonObject: " + val.getClass());
    }
    return val;
  }

  static <T> Stream<T> asStream(Iterator<T> sourceIterator) {
    Iterable<T> iterable = () -> sourceIterator;
    return StreamSupport.stream(iterable.spliterator(), false);
  }

  private static class JsonObjectSerializer extends JsonSerializer<JsonObject> {
    @Override
    public void serialize(JsonObject value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
      jgen.writeObject(value.getMap());
    }
  }

  private static class JsonArraySerializer extends JsonSerializer<JsonArray> {
    @Override
    public void serialize(JsonArray value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
      jgen.writeObject(value.getList());
    }
  }

  private static class InstantSerializer extends JsonSerializer<Instant> {
    @Override
    public void serialize(Instant value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
      jgen.writeString(ISO_INSTANT.format(value));
    }
  }

  private static class ByteArraySerializer extends JsonSerializer<byte[]> {
    private final Base64.Encoder BASE64 = Base64.getEncoder();

    @Override
    public void serialize(byte[] value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
      jgen.writeString(BASE64.encodeToString(value));
    }
  }
}
