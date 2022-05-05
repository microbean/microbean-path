/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2022 microBean™.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.microbean.path;

import java.lang.constant.Constable;

import java.util.Iterator;
import java.util.Map.Entry;

import org.junit.jupiter.api.Test;

import org.microbean.qualifier.Qualifier;
import org.microbean.qualifier.Qualifiers;

import org.microbean.path.Path.Element;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TestPathQualification {

  private TestPathQualification() {
    super();
  }

  @Test
  final void testPathQualification() {
    final Path<Class<?>> p =
      new Path<>(Qualifiers.of(Qualifier.<String, String>of("env", "test")),
                 new Element<>(Qualifiers.of(Qualifier.<String, String>of("foo", "bar")),
                               String.class,
                               "c"));
    final Qualifiers<String, Object> pathQualifiers = p.qualifiers();
    assertNotNull(pathQualifiers);
    final Iterator<? extends Qualifier<String, Object>> iterator = pathQualifiers.iterator();
    assertTrue(iterator.hasNext());
    Qualifier<? extends String, ?> entry = iterator.next();
    assertTrue(iterator.hasNext());
    assertEquals("c.foo", entry.name());
    entry = iterator.next();
    assertEquals("env", entry.name());
    assertFalse(iterator.hasNext());
  }

}
