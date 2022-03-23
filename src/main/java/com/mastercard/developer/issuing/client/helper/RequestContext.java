/**
 * Copyright (c) 2022 Mastercard
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mastercard.developer.issuing.client.helper;

import java.util.HashMap;
import java.util.Map;

/** The Class RequestContext. */
public class RequestContext {

  private RequestContext() {}

  /** The thread context. */
  /**
   * private static ThreadLocal<HashMap<String, Object>> threadContext = new
   * ThreadLocal<HashMap<String, Object>>() { @Override protected HashMap<String, Object>
   * initialValue() { return new HashMap<>(); } };
   */
  private static ThreadLocal<HashMap<String, Object>> threadContext =
      ThreadLocal.withInitial(HashMap::new);

  /**
   * Gets the context map.
   *
   * @return the context map
   */
  public static Map<String, Object> getContextMap() {
    return threadContext.get();
  }

  /**
   * Gets the.
   *
   * @param key the key
   * @return the object
   */
  public static Object get(String key) {
    return threadContext.get().get(key);
  }

  /**
   * Put.
   *
   * @param key the key
   * @param value the value
   */
  public static void put(String key, Object value) {
    threadContext.get().put(key, value);
  }

  /** Clear. */
  public static void clear() {
    threadContext.remove();
  }
}
