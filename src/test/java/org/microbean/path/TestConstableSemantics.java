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

import java.util.List;

import org.junit.jupiter.api.Test;

import org.microbean.qualifier.Qualifier;
import org.microbean.qualifier.Qualifiers;

import org.microbean.path.Path.Element;

import static java.lang.invoke.MethodHandles.lookup;
import static java.lang.invoke.MethodHandles.privateLookupIn;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TestConstableSemantics {

  private TestConstableSemantics() {
    super();
  }

  @Test
  final void testConstableSemantics() throws ReflectiveOperationException {
    final Path<String> p =
      new Path<>(Qualifiers.of(Qualifier.<String, String>of("env", "test")),
                 List.of(Element.of("a", "a"),
                         Element.of("b", "b")),
                 Element.of("c", "c"));
    assertEquals(p, p.describeConstable().orElseThrow().resolveConstantDesc(privateLookupIn(Path.class, lookup())));
  }

}
