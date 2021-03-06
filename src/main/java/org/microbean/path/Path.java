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

import java.lang.StackWalker.StackFrame;

import java.lang.constant.ClassDesc;
import java.lang.constant.Constable;
import java.lang.constant.ConstantDesc;
import java.lang.constant.DynamicConstantDesc;
import java.lang.constant.MethodHandleDesc;
import java.lang.constant.MethodTypeDesc;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.TreeMap;
import java.util.TreeSet;

import java.util.function.BiFunction;
import java.util.function.BiPredicate;

import java.util.stream.Stream;

import org.microbean.constant.Constables;

import org.microbean.development.annotation.Experimental;

import org.microbean.qualifier.Qualified;
import org.microbean.qualifier.Qualifier;
import org.microbean.qualifier.Qualifiers;

import static java.lang.constant.ConstantDescs.BSM_INVOKE;
import static java.lang.constant.ConstantDescs.CD_List;
import static java.lang.constant.ConstantDescs.CD_Object;
import static java.lang.constant.ConstantDescs.CD_String;
import static java.lang.constant.ConstantDescs.CD_boolean;
import static java.lang.constant.ConstantDescs.DEFAULT_NAME;
import static java.lang.constant.ConstantDescs.FALSE;
import static java.lang.constant.ConstantDescs.NULL;
import static java.lang.constant.ConstantDescs.TRUE;

import static java.lang.constant.DirectMethodHandleDesc.Kind.STATIC;

import static org.microbean.path.ConstantDescs.CD_Path;
import static org.microbean.path.ConstantDescs.CD_PathElement;

import static org.microbean.qualifier.ConstantDescs.CD_Qualifiers;

/**
 * A {@linkplain Qualified qualified} sequence of {@linkplain
 * Path.Element named and qualified elements} that "points to" an
 * object and that can be used for many different purposes.
 *
 * <p>A {@link Path} can be used like a {@link javax.naming.Name
 * javax.naming.Name}, or like a {@link java.nio.file.Path
 * java.nio.file.Path}, or like a {@link java.net.URI java.net.URI}.
 * It differs from these other objects in that it combines some of
 * their concepts together.</p>
 *
 * @param <T> the type of the {@link Path}; most notably
 * <strong>not necessarily</strong> the type of the thing the {@link
 * Path} "points to"; more like an additional qualifier
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see Path.Element
 */
public final class Path<T> implements Iterable<Path.Element<?>>, Qualified<String, Object, T> {


  /*
   * Static fields.
   */


  private static final StackWalker stackWalker = StackWalker.getInstance();

  private static final Path<?> ROOT = new Path<>();

  private static final char PREFIX_SEPARATOR_CHAR = '.';

  private static final String PREFIX_SEPARATOR = "" + PREFIX_SEPARATOR_CHAR;


  /*
   * Instance fields.
   */


  private final Qualifiers<String, Object> qualifiers;

  private final List<Element<?>> elements;

  private final boolean transliterated;


  /*
   * Constructors.
   */


  // For use by ROOT only.
  @SuppressWarnings("unchecked")
  private Path() {
    this(Qualifiers.of(), List.of(), (Element<T>)Element.root(), true);
  }

  /**
   * Creates a new {@link Path}.
   *
   * @param lastElement the {@linkplain #lastElement last element} of
   * this {@link Path}; must not be {@code null}
   *
   * @exception NullPointerException if any parameter is {@code null}
   *
   * @see #Path(Qualifiers, List, Element)
   */
  public Path(final Element<? extends T> lastElement) {
    this(Qualifiers.of(), List.of(), lastElement, false);
  }

  /**
   * Creates a new {@link Path}.
   *
   * @param qualifiers the {@link Path}'s {@linkplain #qualifiers()
   * qualifiers}; must not be {@code null}
   *
   * @param lastElement the {@linkplain #lastElement last element} of
   * this {@link Path}; must not be {@code null}
   *
   * @exception NullPointerException if any parameter is {@code null}
   *
   * @see #Path(Qualifiers, List, Element)
   */
  public Path(final Qualifiers<? extends String, ?> qualifiers,
              final Element<? extends T> lastElement) {
    this(qualifiers, List.of(), lastElement, false);
  }

  /**
   * Creates a new {@link Path}.
   *
   * @param qualifiers the {@link Path}'s {@linkplain #qualifiers()
   * qualifiers}; must not be {@code null}
   *
   * @param elements the {@linkplain Element elements} of this {@link
   * Path}; must not be {@code null}; may be {@linkplain
   * List#isEmpty() empty}
   *
   * @param lastElement the {@linkplain #lastElement last element} of
   * this {@link Path}; must not be {@code null}
   *
   * @exception NullPointerException if any parameter is {@code null}
   */
  public Path(final Qualifiers<? extends String, ?> qualifiers,
              final List<? extends Element<?>> elements,
              final Element<? extends T> lastElement) {
    this(qualifiers, elements, lastElement, false);
  }

  @SuppressWarnings("unchecked")
  // Also used by #describeConstable()
  private Path(final Qualifiers<? extends String, ?> qualifiers,
               final List<? extends Element<?>> elements,
               final Element<? extends T> lastElement,
               final boolean transliterated) {
    super();
    final int size = elements.size();
    if (size > 0) {
      Set<Qualifier<String, Object>> pathQualifiers = null;
      final List<Element<?>> newList = new ArrayList<>(size + 1);
      StringBuilder prefix = null;
      for (int i = 0; i < size; i++) {
        final Element<?> e = elements.get(i);
        newList.add(e);
        final Qualifiers<String, Object> eQualifiers = e.qualifiers();
        if (!eQualifiers.isEmpty()) {
          if (prefix == null) {
            prefix = new StringBuilder();
          }
          if (pathQualifiers == null) {
            pathQualifiers = new TreeSet<>();
            for (final Qualifier<? extends String, ?> q : qualifiers) {
              pathQualifiers.add((Qualifier<String, Object>)q);
            }
          }
          prefix.append(e.name()).append(PREFIX_SEPARATOR_CHAR);
          final String finalPrefix = prefix.toString();
          for (final Qualifier<String, Object> q : eQualifiers.withPrefix(q -> finalPrefix + PREFIX_SEPARATOR + q.name())) {
            pathQualifiers.add(q);
          }
        }
      }
      newList.add(lastElement);
      this.elements = Collections.unmodifiableList(newList);
      final Qualifiers<? extends String, ?> lastElementQualifiers = lastElement.qualifiers();
      if (lastElementQualifiers.isEmpty()) {
        if (pathQualifiers == null) {
          this.qualifiers = (Qualifiers<String, Object>)qualifiers;
        } else {
          this.qualifiers = Qualifiers.of(pathQualifiers);
        }
      } else {
        if (prefix == null) {
          prefix = new StringBuilder();
        }
        if (pathQualifiers == null) {
          pathQualifiers = new TreeSet<>();
          for (final Qualifier<? extends String, ?> q : qualifiers) {
            pathQualifiers.add((Qualifier<String, Object>)q);
          }
        }
        final String finalPrefix = prefix.append(lastElement.name()).toString();
        for (final Qualifier<String, Object> q : lastElement.qualifiers().withPrefix(q -> finalPrefix + PREFIX_SEPARATOR + q.name())) {
          pathQualifiers.add(q);
        }
        this.qualifiers = Qualifiers.of(pathQualifiers);
      }
    } else {
      this.elements = List.of(lastElement);
      final Qualifiers<? extends String, ?> lastElementQualifiers = lastElement.qualifiers();
      if (lastElementQualifiers.isEmpty()) {
        this.qualifiers = (Qualifiers<String, Object>)qualifiers;
      } else {
        Set<Qualifier<String, Object>> pathQualifiers = new TreeSet<>();
        for (final Qualifier<? extends String, ?> q : qualifiers) {
          pathQualifiers.add((Qualifier<String, Object>)q);
        }
        for (final Qualifier<String, Object> q : lastElement.qualifiers().withPrefix(q -> lastElement.name() + PREFIX_SEPARATOR + q.name())) {
          pathQualifiers.add(q);
        }
        this.qualifiers = Qualifiers.of(pathQualifiers);
      }
    }
    this.transliterated = transliterated;
  }


  /*
   * Instance methods.
   */


  /**
   * Returns {@code true} if and only if this {@link Path} is the
   * result of a call to {@link #transliterate(BiFunction)}.
   *
   * @return {@code true} if this {@link Path} is {@linkplain
   * #transliterate(BiFunction) transliterated}; {@code false}
   * otherwise
   */
  public final boolean transliterated() {
    return this.transliterated;
  }

  /**
   * <em>Transliterates</em> this {@link Path} into another,
   * semantically equivalent {@link Path} by producing a {@link Path}
   * that is otherwise equal to this {@link Path} but that will return
   * {@code true} from its {@link #transliterated()} method.
   *
   * <p>Transliteration can be needed when a {@link Path} is defined
   * by a Java class and used by an application containing that Java
   * class&mdash;because another Java class may have used the same
   * element names to refer to different things.</p>
   *
   * <p>If this {@link Path} {@linkplain #transliterated() is
   * already transliterated} then it is returned.</p>
   *
   * <p>This method calls {@link #transliterate(BiFunction)} with
   * {@code null} as its sole argument.</p>
   *
   * @return the transliterated {@link Path}, which may be this {@link
   * Path}; never {@code null}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @see #transliterate(BiFunction)
   *
   * @see #transliterated()
   */
  public final Path<T> transliterate() {
    return transliterate(null);
  }

  /**
   * <em>Transliterates</em> this {@link Path} into another,
   * semantically equivalent {@link Path} by applying the supplied
   * {@link BiFunction}, and returns the transliterated {@link Path}.
   *
   * <p>The supplied {@link BiFunction} accepts a Java package name as
   * its first argument, which will be the first package name
   * {@linkplain StackWalker encountered in the current thread's
   * stack} that identifies a caller whose package name is not equal
   * to {@link Class#getPackageName() Path.class.getPackageName()}.
   * Its second argument is a {@link Element Element} from this
   * {@link Path}.  It must return a {@link Element} representing the
   * transliteration of its second argument (which may be the second
   * argument itself).</p>
   *
   * <p>Transliteration can be needed when a {@link Path} is defined
   * by a Java class and used by an application containing that Java
   * class&mdash;because another Java class may have used the same
   * element names to refer to different things.</p>
   *
   * <p>If this {@link Path} {@linkplain #transliterated() is
   * already transliterated} then it is returned.</p>
   *
   * @param f a {@link BiFunction} responsible for the
   * transliteration, element by element; may be {@code null}
   *
   * @return the transliterated {@link Path}, which may be this {@link
   * Path}; never {@code null}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic, but the
   * supplied {@link BiFunction} may not be.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads, but the supplied {@link BiFunction} may not be.
   *
   * @see #transliterated()
   */
  @Experimental
  @SuppressWarnings("unchecked")
  public final Path<T> transliterate(final BiFunction<? super String, ? super Element<?>, ? extends Element<?>> f) {
    if (this.transliterated()) {
      return this;
    } else {
      final int size = this.size();
      final int lastIndex = size - 1;
      if (f == null) {
        return
          new Path<T>(this.qualifiers(),
                      this.elements.subList(0, lastIndex),
                      (Element<? extends T>)this.elements.get(lastIndex),
                      true);
      } else {
        final String userPackageName = stackWalker.walk(Path::findUserPackageName);
        final List<Element<?>> newElements = new ArrayList<>(lastIndex);
        for (int i = 0; i < lastIndex; i++) {
          newElements.add(f.apply(userPackageName, this.elements.get(i)));
        }
        return
          new Path<T>(this.qualifiers(),
                      newElements,
                      (Element<? extends T>)f.apply(userPackageName, this.elements.get(lastIndex)),
                      true);
      }
    }
  }

  /**
   * Returns an {@link Optional} containing a {@link ConstantDesc}
   * representing this {@link Path}, or an {@linkplain
   * Optional#isEmpty() empty <code>Optional</code>} if this {@link
   * Path} cannot be represented as a {@link ConstantDesc}.
   *
   * <p>A {@link Path} instance may be represented by a {@link
   * ConstantDesc} only when the return value of an invocation of the
   * {@link Element#describeConstable()} method on its {@linkplain
   * #lastElement() last element} returns a non-{@linkplain
   * Optional#isEmpty() empty} {@link Optional}.</p>
   *
   * @return an {@link Optional} containing a {@link ConstantDesc}
   * representing this {@link Path}; never {@code null}; possibly
   * {@linkplain Optional#isEmpty() empty}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @see Element#describeConstable()
   */
  @Override // Constable
  public final Optional<? extends ConstantDesc> describeConstable() {
    if (this.isRoot()) {
      return
        Optional.of(DynamicConstantDesc.ofNamed(BSM_INVOKE,
                                                DEFAULT_NAME,
                                                CD_Path,
                                                MethodHandleDesc.ofMethod(STATIC,
                                                                          CD_Path,
                                                                          "root",
                                                                          MethodTypeDesc.of(CD_Path))));
    } else {
      final ConstantDesc qualifiers = this.qualifiers().describeConstable().orElse(null);
      if (qualifiers != null) {
        final ConstantDesc elements = Constables.describeConstable(this.elements.subList(0, this.size() - 1)).orElse(null);
        if (elements != null) {
          final ConstantDesc lastElement = this.elements.get(this.size() - 1).describeConstable().orElse(null);
          if (lastElement != null) {
            return
              Optional.of(DynamicConstantDesc.ofNamed(BSM_INVOKE,
                                                      DEFAULT_NAME,
                                                      CD_Path,
                                                      MethodHandleDesc.ofConstructor(CD_Path,
                                                                                     CD_Qualifiers,
                                                                                     CD_List,
                                                                                     CD_PathElement,
                                                                                     CD_boolean),
                                                      qualifiers,
                                                      elements,
                                                      lastElement,
                                                      this.transliterated ? TRUE : FALSE));
          }
        }
      }
    }
    return Optional.empty();
  }

  /**
   * Returns the {@link Qualifiers} that qualifies this {@link Path}.
   *
   * @return the {@link Qualifiers} that qualifies this {@link Path}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  @Override // Qualified<String, Object, T>
  public final Qualifiers<String, Object> qualifiers() {
    return this.qualifiers;
  }

  /**
   * Returns the {@link Path.Element} found at the supplied zero-based
   * index.
   *
   * @param index the index of the {@link Path.Element} to return;
   * must be {@code 0} or greater and less than this {@link Path}'s
   * {@linkplain #size() size}
   *
   * @return the {@link Path.Element} found at the supplied zero-based
   * index
   *
   * @exception IndexOutOfBoundsException if {@code index} does not
   * meet the requirements described above
   *
   * @nullability This method never returns {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency This method is idempotent and deterministic.
   */
  public final Element<?> get(final int index) {
    return this.elements.get(index);
  }

  /**
   * Returns an {@link Iterator} that iterates over the {@linkplain
   * Element elements} of this {@link Path}, including the {@linkplain
   * #lastElement() last element}.
   *
   * @return an {@link Iterator} that iterates over the {@linkplain
   * Element elements} of this {@link Path}, including the {@linkplain
   * #lastElement() last element}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  @Override // Iterable<Element<?>>
  public final Iterator<Element<?>> iterator() {
    return this.elements.iterator();
  }

  /**
   * Returns a {@link Spliterator} for the {@linkplain Element
   * elements} of this {@link Path}, including the {@linkplain
   * #lastElement() last element}.
   *
   * @return a {@link Spliterator} for the {@linkplain Element
   * elements} of this {@link Path}, including the {@linkplain
   * #lastElement() last element}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  @Override // Iterable<Element<?>>
  public final Spliterator<Element<?>> spliterator() {
    return this.elements.spliterator();
  }

  /**
   * Returns a {@link Stream} of this {@link Path}'s {@link Element}s.
   *
   * @return a possibly parallel {@link Stream} of this {@link Path}'s
   * {@link Element}s
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  public final Stream<Element<?>> stream() {
    return this.elements.stream();
  }

  /**
   * Returns a possibly parallel {@link Stream} of this {@link Path}'s
   * {@link Element}s.
   *
   * @return a possibly parallel {@link Stream} of this {@link Path}'s
   * {@link Element}s
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  public final Stream<Element<?>> parallelStream() {
    return this.elements.parallelStream();
  }

  /**
   * Returns the number of {@link Element}s in this {@link Path},
   * which is always greater than or equal to {@code 1}.
   *
   * @return the number of {@link Element}s in this {@link Path}
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  public final int size() {
    return this.elements.size();
  }

  /**
   * Returns the zero-based index identifying the position of the
   * first occurrence of a {@link Path} {@linkplain #equals(Object)
   * equal to} the supplied {@link Path} within this {@link Path}, or
   * a negative value if the supplied {@link Path} does not occur
   * within this {@link Path}.
   *
   * @param other the other {@link Path}; must not be {@code null}
   *
   * @return the zero-based index identifying the position of the
   * first occurrence of a {@link Path} {@linkplain #equals(Object)
   * equal to} supplied {@link Path} within this {@link Path}, or a
   * negative value if the supplied {@link Path} does not occur within
   * this {@link Path}
   *
   * @exception NullPointerException if {@code other} is {@code null}
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  public final int indexOf(final Path<?> other) {
    return other == this ? 0 : Collections.indexOfSubList(this.elements, other.elements);
  }

  /**
   * Returns the zero-based index identifying the position of the
   * first occurrence of a {@link Path} {@linkplain #equals(Object)
   * equal to} supplied {@link Path} within this {@link Path}, or a
   * negative value if the supplied {@link Path} does not occur within
   * this {@link Path}.
   *
   * <p>The supplied {@link BiPredicate} is used to test each {@link
   * Element} of the supplied {@link Path} against {@link Element}s
   * from this {@link Path}.  The first argument is a {@link Element}
   * drawn from this {@link Path}.  The second argument is an {@link
   * Element} drawn from the supplied {@link Path}.  The {@link
   * BiPredicate} returns {@code true} if its arguments are deemed to
   * match.  The supplied {@link BiPredicate}'s {@link
   * BiPredicate#test(Object, Object)} method must be idempotent and
   * deterministic.</p>
   *
   * @param path the other {@link Path}; must not be {@code null}
   *
   * @param p the {@link BiPredicate} used to {@linkplain
   * BiPredicate#test(Object, Object) test} {@link Element}s; must not
   * be {@code null}
   *
   * @return the zero-based index identifying the position of the
   * first occurrence of a {@link Path} {@linkplain #equals(Object)
   * equal to} supplied {@link Path} within this {@link Path}, or a
   * negative value if the supplied {@link Path} does not occur within
   * this {@link Path}
   *
   * @exception NullPointerException if either {@code path} or {@code
   * p} is {@code null}
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  public final int indexOf(final Path<?> path, final BiPredicate<? super Element<?>, ? super Element<?>> p) {
    final int pathSize = path.size();
    final int sizeDiff = this.size() - pathSize;
    OUTER_LOOP:
    for (int i = 0; i <= sizeDiff; i++) {
      for (int j = 0, k = i; j < pathSize; j++, k++) {
        if (!p.test(this.elements.get(k), path.elements.get(j))) {
          continue OUTER_LOOP;
        }
      }
      return i;
    }
    return -1;
  }

  /**
   * Returns {@code true} if this {@link Path} starts with a {@link
   * Path} that is {@linkplain #equals(Object) equal to} the supplied
   * {@link Path}.
   *
   * <p>This method returns {@code true} if and only if:</p>
   *
   * <ul>
   *
   * <li>{@code other} is identical to this {@link Path}, or</li>
   *
   * <li>An invocation of the {@link #indexOf(Path)} method with
   * {@code other} as its sole argument returns {@code 0}</li>
   *
   * </ul>
   *
   * @param other the other {@link Path}; must not be {@code null}
   *
   * @return {@code true} if this {@link Path} starts with a {@link
   * Path} that is {@linkplain #equals(Object) equal to} the supplied
   * {@link Path}; {@code false} otherwise
   *
   * @exception NullPointerException if {@code other} is {@code null}
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @see #indexOf(Path)
   */
  public final boolean startsWith(final Path<?> other) {
    return other == this || this.indexOf(other) == 0;
  }

  /**
   * Returns the last {@link Element} in this {@link Path}.
   *
   * @return the last {@link Element} in this {@link Path}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  @SuppressWarnings("unchecked")
  public final Element<T> lastElement() {
    return (Element<T>)this.elements.get(this.size() - 1);
  }

  /**
   * Returns the zero-based index identifying the position of the
   * last occurrence of a {@link Path} {@linkplain #equals(Object)
   * equal to} the supplied {@link Path} within this {@link Path}, or
   * a negative value if the supplied {@link Path} does not occur
   * within this {@link Path}.
   *
   * @param other the other {@link Path}; must not be {@code null}
   *
   * @return the zero-based index identifying the position of the
   * last occurrence of a {@link Path} {@linkplain #equals(Object)
   * equal to} supplied {@link Path} within this {@link Path}, or a
   * negative value if the supplied {@link Path} does not occur within
   * this {@link Path}
   *
   * @exception NullPointerException if {@code other} is {@code null}
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  public final int lastIndexOf(final Path<?> other) {
    return other == this ? 0 : Collections.lastIndexOfSubList(this.elements, other.elements);
  }

  /**
   * Returns the zero-based index identifying the position of the
   * last occurrence of a {@link Path} {@linkplain #equals(Object)
   * equal to} supplied {@link Path} within this {@link Path}, or a
   * negative value if the supplied {@link Path} does not occur within
   * this {@link Path}.
   *
   * <p>The supplied {@link BiPredicate} is used to test each {@link
   * Element} of the supplied {@link Path} against {@link Element}s
   * from this {@link Path}.  The first argument is a {@link Element}
   * drawn from this {@link Path}.  The second argument is an {@link
   * Element} drawn from the supplied {@link Path}.  The {@link
   * BiPredicate} returns {@code true} if its arguments are deemed to
   * match.  The supplied {@link BiPredicate}'s {@link
   * BiPredicate#test(Object, Object)} method must be idempotent and
   * deterministic.</p>
   *
   * @param path the other {@link Path}; must not be {@code null}
   *
   * @param p the {@link BiPredicate} used to {@linkplain
   * BiPredicate#test(Object, Object) test} {@link Element}s; must not
   * be {@code null}
   *
   * @return the zero-based index identifying the position of the
   * last occurrence of a {@link Path} {@linkplain #equals(Object)
   * equal to} supplied {@link Path} within this {@link Path}, or a
   * negative value if the supplied {@link Path} does not occur within
   * this {@link Path}
   *
   * @exception NullPointerException if either {@code path} or {@code
   * p} is {@code null}
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  public final int lastIndexOf(final Path<?> path, final BiPredicate<? super Element<?>, ? super Element<?>> p) {
    final int pathSize = path.size();
    final int sizeDiff = this.size() - pathSize;
    OUTER_LOOP:
    for (int i = sizeDiff; i >= 0; i--) {
      for (int j = 0, k = i; j < pathSize; j++, k++) {
        if (!p.test(this.elements.get(k), path.elements.get(j))) {
          continue OUTER_LOOP;
        }
      }
      return i;
    }
    return -1;
  }

/**
   * Returns {@code true} if this {@link Path} starts with a {@link
   * Path} that is {@linkplain #equals(Object) equal to} the supplied
   * {@link Path}.
   *
   * <p>The supplied {@link BiPredicate} is used to test each {@link
   * Element} of the supplied {@link Path} against {@link Element}s
   * from this {@link Path}.  The first argument is a {@link Element}
   * drawn from this {@link Path}.  The second argument is a {@link
   * Element} drawn from the supplied {@link Path}.  The {@link
   * BiPredicate} returns {@code true} if its arguments are deemed to
   * match.  The supplied {@link BiPredicate}'s {@link
   * BiPredicate#test(Object, Object)} method must be idempotent and
   * deterministic.</p>
   *
   * <p>This method returns {@code true} if and only if:</p>
   *
   * <ul>
   *
   * <li>{@code other} is identical to this {@link Path}, or</li>
   *
   * <li>An invocation of the {@link #indexOf(Path, BiPredicate)}
   * method with {@code other} and {@code p} as its arguments returns
   * {@code 0}</li>
   *
   * </ul>
   *
   * @param other the other {@link Path}; must not be {@code null}
   *
   * @param p the {@link BiPredicate} used to {@linkplain
   * BiPredicate#test(Object, Object) test} {@link Element}s; must not
   * be {@code null}
   *
   * @return {@code true} if this {@link Path} starts with a {@link
   * Path} that is {@linkplain #equals(Object) equal to} the supplied
   * {@link Path}; {@code false} otherwise
   *
   * @exception NullPointerException if {@code other} is {@code null}
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @see #indexOf(Path, BiPredicate)
   */
  public final boolean startsWith(final Path<?> other, final BiPredicate<? super Element<?>, ? super Element<?>> p) {
    return other == this || this.indexOf(other, p) == 0;
  }

  /**
   * Returns {@code true} if this {@link Path} ends with a {@link
   * Path} that is {@linkplain #equals(Object) equal to} the supplied
   * {@link Path}.
   *
   * <p>This method returns {@code true} if and only if:</p>
   *
   * <ul>
   *
   * <li>{@code other} is identical to this {@link Path}, or</li>
   *
   * <li>An invocation of the {@link #lastIndexOf(Path)} method with
   * {@code other} as its sole argument returns {@code 0} or a
   * positive {@code int}, and that number plus the supplied {@link
   * Path}'s {@linkplain #size() size} is equal to this {@link Path}'s
   * {@linkplain #size() size}</li>
   *
   * </ul>
   *
   * @param other the other {@link Path}; must not be {@code null}
   *
   * @return {@code true} if this {@link Path} ends with a {@link
   * Path} that is {@linkplain #equals(Object) equal to} the supplied
   * {@link Path}; {@code false} otherwise
   *
   * @exception NullPointerException if {@code other} is {@code null}
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @see #lastIndexOf(Path)
   */
  public final boolean endsWith(final Path<?> other) {
    if (other == this) {
      return true;
    } else {
      final int lastIndex = this.lastIndexOf(other);
      return lastIndex >= 0 && lastIndex + other.size() == this.size();
    }
  }

  /**
   * Returns {@code true} if this {@link Path} ends with a {@link
   * Path} that is {@linkplain #equals(Object) equal to} the supplied
   * {@link Path}.
   *
   * <p>The supplied {@link BiPredicate} is used to test each {@link
   * Element} of the supplied {@link Path} against {@link Element}s
   * from this {@link Path}.  The first argument is a {@link Element}
   * drawn from this {@link Path}.  The second argument is a {@link
   * Element} drawn from the supplied {@link Path}.  The {@link
   * BiPredicate} returns {@code true} if its arguments are deemed to
   * match.  The supplied {@link BiPredicate}'s {@link
   * BiPredicate#test(Object, Object)} method must be idempotent and
   * deterministic.</p>
   *
   * <p>This method returns {@code true} if and only if:</p>
   *
   * <ul>
   *
   * <li>{@code other} is identical to this {@link Path}, or</li>
   *
   * <li>An invocation of the {@link #indexOf(Path, BiPredicate)}
   * method with {@code other} and {@code p} as its arguments returns
   * {@code 0} or a positive {@code int}, and if that number plus the
   * supplied {@link Path}'s {@linkplain #size() size} is equal to
   * this {@link Path}'s {@linkplain #size() size}</li>
   *
   * </ul>
   *
   * @param other the other {@link Path}; must not be {@code null}
   *
   * @param p the {@link BiPredicate} used to {@linkplain
   * BiPredicate#test(Object, Object) test} {@link Element}s; must not
   * be {@code null}
   *
   * @return {@code true} if this {@link Path} ends with a {@link
   * Path} that is {@linkplain #equals(Object) equal to} the supplied
   * {@link Path}; {@code false} otherwise
   *
   * @exception NullPointerException if {@code other} is {@code null}
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @see #lastIndexOf(Path, BiPredicate)
   */
  public final boolean endsWith(final Path<?> other, final BiPredicate<? super Element<?>, ? super Element<?>> p) {
    if (other == this) {
      return true;
    } else {
      final int lastIndex = this.lastIndexOf(other, p);
      return lastIndex >= 0 && lastIndex + other.size() == this.size();
    }
  }

  /**
   * Calls the {@link Element#qualified()} method on the {@linkplain
   * lastElement() last element in this <code>Path</code>} and returns
   * the result.
   *
   * @return the result of invoking the {@link Element#qualified()}
   * method on the {@linkplain lastElement() last element in this
   * <code>Path</code>}
   *
   * @nullability This method may return {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  @Override // Qualified<String, Object, T>
  public final T qualified() {
    return this.lastElement().qualified();
  }

  /**
   * Returns {@code true} if this {@link Path} is a <em>root
   * path</em>, which is true when it has only one {@linkplain Element
   * element} and that {@linkplain Element element} {@linkplain
   * Element#isRoot() is root}.
   *
   * @return {@code true} if this {@link Path} is a <em>root
   * path</em>; {@code false} otherwise
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  public final boolean isRoot() {
    return this.size() == 1 && this.lastElement().isRoot();
  }

  /**
   * Returns {@code true} if and only if this {@link Path} is
   * <em>absolute</em>, which means that its first {@link Element} is
   * a {@linkplain Element#isRoot() root} {@link Element}.
   *
   * @return {@code true} if and only if this {@link Path} is absolute
   */
  public final boolean absolute() {
    return this.elements.get(0).isRoot();
  }

  /**
   * Returns a hashcode for this {@link Path}.
   *
   * @return a hashcode for this {@link Path}
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  @Override // Object
  public final int hashCode() {
    int hashCode = 17;
    Object value = this.qualifiers();
    int c = value == null ? 0 : value.hashCode();
    hashCode = 37 * hashCode + c;
    value = this.elements;
    c = value == null ? 0 : value.hashCode();
    hashCode = 37 * hashCode + c;
    c = this.transliterated ? 1 : 0;
    hashCode = 37 * hashCode + c;
    return hashCode;
  }

  /**
   * Returns {@code true} if and only if the supplied {@link Object}
   * is equal to this {@link Path}.
   *
   * @param other the {@link Object} to test; may be {@code null} in
   * which case {@code false} will be returned
   *
   * @return {@code true} if and only if the supplied {@link Object}
   * is equal to this {@link Path}
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  @Override // Object
  public final boolean equals(final Object other) {
    if (other == this) {
      return true;
    } else if (other != null && other.getClass() == this.getClass()) {
      final Path<?> her = (Path<?>)other;
      return
        Objects.equals(this.qualifiers(), her.qualifiers()) &&
        Objects.equals(this.elements, her.elements) &&
        this.transliterated == her.transliterated;
    } else {
      return false;
    }
  }

  /**
   * Returns a <strong>new</strong> {@link Path} consisting of this
   * {@link Path}'s {@linkplain #qualifiers() qualifiers} and
   * {@linkplain Element elments} plus the supplied {@linkplain
   * Element element}.
   *
   * @param <U> the type of the new {@link Path}
   *
   * @param element the new {@link Path}'s {@linkplain #lastElement()
   * last element}; must not be {@code null}
   *
   * @return a <strong>new</strong> {@link Path} consisting of this
   * {@link Path}'s {@linkplain #qualifiers() qualifiers} and
   * {@linkplain Element elments} plus the supplied {@linkplain
   * Element element}
   *
   * @exception NullPointerException if {@code element} is {@code
   * null}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  public final <U> Path<U> plus(final Element<? extends U> element) {
    return new Path<>(this.qualifiers(), this.elements, element);
  }

  /**
   * Returns a <strong>new</strong> {@link Path} consisting of this
   * {@link Path}'s {@linkplain #qualifiers() qualifiers}, this {@link
   * Path}'s elements, the supplied {@code elements}, and the supplied
   * {@code lastElement}.
   *
   * @param <U> the type of the new {@link Path}
   *
   * @param elements additional {@linkplain Element elements}; must
   * not be {@code null}
   *
   * @param lastElement the new {@link Path}'s {@linkplain
   * #lastElement() last element}; must not be {@code null}
   *
   * @return a <strong>new</strong> {@link Path} consisting of this
   * {@link Path}'s {@linkplain #qualifiers() qualifiers}, this {@link
   * Path}'s elements, the supplied {@code elements}, and the supplied
   * {@code lastElement}
   *
   * @exception NullPointerException if {@code element} is {@code
   * null}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  public final <U> Path<U> plus(final Collection<? extends Element<?>> elements,
                                final Element<? extends U> lastElement) {
    final List<Element<?>> newElements = new ArrayList<>(this.size() + elements.size());
    newElements.addAll(elements);
    return new Path<>(this.qualifiers(), newElements, lastElement);
  }

  /**
   * Returns a <strong>new</strong> {@link Path} consisting of this
   * {@link Path}'s {@linkplain #qualifiers() qualifiers} combined
   * with a prefixed version of the supplied {@link Path}'s
   * {@linkplain #qualifiers() qualifiers}, this {@link Path}'s
   * elements, the supplied {@link Path}'s elements, and the supplied
   * {@link Path}'s {@linkplain #lastElement() last element}.
   *
   * <p>The new {@link Path}'s {@linkplain #qualifiers() qualifiers}
   * are those of this {@link Path} plus those of the supplied {@link
   * Path}, where each qualifier in the supplied {@link Path}'s
   * {@linkplain #qualifiers() qualifiers} has been prefixed with a
   * {@link String} formed from the {@linkplain Element#name() names}
   * of all the {@linkplain Element elements} in this {@link
   * Path}.</p>
   *
   * @param <U> the type of the new {@link Path}
   *
   * @param path the {@link Path} to logically append; must not be
   * {@code null}
   *
   * @return a <strong>new</strong> {@link Path} consisting of this
   * {@link Path}'s {@linkplain #qualifiers() qualifiers} combined
   * with a prefixed version of the supplied {@link Path}'s
   * {@linkplain #qualifiers() qualifiers}, this {@link Path}'s
   * elements, the supplied {@link Path}'s elements, and the supplied
   * {@link Path}'s {@linkplain #lastElement() last element}
   *
   * @exception NullPointerException if {@code path} is {@code null}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  @SuppressWarnings("unchecked")
  public final <U> Path<U> plus(final Path<? extends U> path) {
    final int pathSize = path.size();
    final List<Element<?>> newElements = new ArrayList<>(this.size() + pathSize);
    newElements.addAll(this.elements);
    final int lastIndex = pathSize - 1;
    for (int i = 0; i < lastIndex; i++) {
      newElements.add(path.elements.get(i));
    }
    final String prefix = this.prefix();
    return
      new Path<>(path.qualifiers().withPrefix(k -> prefix + k),
                 newElements,
                 (Element<? extends U>)path.elements.get(lastIndex));
  }

  private final String prefix() {
    final StringBuilder prefix = new StringBuilder();
    for (final Element<?> e : this) {
      prefix.append(e.name()).append(PREFIX_SEPARATOR_CHAR);
    }
    return prefix.toString();
  }

  /**
   * Returns a non-{@code null} {@link String} representation of this
   * {@link Path}.
   *
   * <p>The format of the returned {@link String} is deliberately
   * undefined and may change between revisions of this class without
   * notice.</p>
   *
   * @return a non-{@code null} {@link String} representation of this
   * {@link Path}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  @Override // Object
  public final String toString() {
    final StringBuilder sb = new StringBuilder();
    Iterator<Element<?>> i = this.iterator();
    while (i.hasNext()) {
      sb.append(i.next());
      if (i.hasNext()) {
        sb.append('/');
      }
    }
    sb.append(this.qualifiers());
    return sb.toString();
  }


  /*
   * Static methods.
   */


  /**
   * Returns a {@link Path} that {@linkplain #isRoot() is a <em>root
   * path</em>}.
   *
   * @return a {@link Path} that {@linkplain #isRoot() is a <em>root
   * path</em>}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @see #isRoot()
   */
  public static final Path<?> root() {
    return ROOT;
  }

  /**
   * Returns a (<strong>usually new</strong>) {@link Path} formed from
   * a {@link Element} formed from the supplied {@code qualified} and
   * an {@linkplain String#isEmpty() empty} {@linkplain Element#name()
   * name}.
   *
   * @param <T> the type of the {@link Path}
   *
   * @param qualified the {@linkplain Element#qualified() qualified
   * item} of what will be the last {@link Element}
   *
   * @return a {@link Path}
   *
   * @exception NullPointerException if {@code qualified} is {@code
   * null}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @see #of(Object, List)
   */
  public static final <T> Path<T> of(final T qualified) {
    return Path.of(qualified, List.of());
  }

  /**
   * Returns a (<strong>usually new</strong>) {@link Path} formed from
   * a {@link Element} formed from the supplied {@code qualified} and
   * the supplied name.
   *
   * @param <T> the type of the {@link Path}
   *
   * @param qualified the {@linkplain Element#qualified() qualified
   * item} of what will be the last {@link Element}
   *
   * @param name the {@linkplain Element#name() name} of what will be
   * the last {@link Element}
   *
   * @return a {@link Path}
   *
   * @exception NullPointerException if {@code name} is {@code null}
   *
   * @exception IllegalArgumentException if {@code qualified} is
   * {@code null} and name is {@code null} or an empty
   * <code>String</code>}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @see #of(Object, List)
   */
  public static final <T> Path<T> of(final T qualified, final String name) {
    return Path.of(qualified, List.of(name));
  }

  /**
   * Returns a (<strong>usually new</strong>) {@link Path} formed from
   * {@link Element}s formed from the supplied {@code qualified} and
   * the supplied array of {@linkplain Element#name() names}.
   *
   * @param <T> the type of the {@link Path}
   *
   * @param qualified the {@linkplain Element#qualified() qualified
   * item} of what will be the last {@link Element}
   *
   * @param names an array of {@linkplain Element#name() names} from
   * which {@link Element}s will be synthesized; must not be {@code
   * null}
   *
   * @return a {@link Path}
   *
   * @exception NullPointerException if {@code names} is {@code null}
   *
   * @exception IllegalArgumentException if {@code qualified} is
   * {@code null} and the last element of {@code names} {@linkplain
   * String#isEmpty() is an empty <code>String</code>}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @see #of(Object, List)
   */
  public static final <T> Path<T> of(final T qualified, final String... names) {
    return Path.of(qualified, Arrays.asList(names));
  }

  /**
   * Returns a (<strong>usually new</strong>) {@link Path} formed from
   * the supplied {@code lastElement}.
   *
   * @param <T> the type of the {@link Path}
   *
   * @param lastElement the last {@link Element}; must not be {@code
   * null}
   *
   * @return a {@link Path}
   *
   * @exception NullPointerException if {@code lastElement} is {@code
   * null}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  public static final <T> Path<T> of(final Element<? extends T> lastElement) {
    return new Path<>(lastElement);
  }

  /**
   * Returns a (<strong>usually new</strong>) {@link Path} formed from
   * {@link Element}s formed from the supplied {@code qualified} and
   * the supplied {@link List} of {@linkplain Element#name() names}.
   *
   * @param <T> the type of the {@link Path}
   *
   * @param qualified the {@linkplain Element#qualified() qualified
   * item} of what will be the last {@link Element}
   *
   * @param names a {@link List} of {@linkplain Element#name() names}
   * from which {@link Element}s will be synthesized; must not be
   * {@code null}
   *
   * @return a {@link Path}
   *
   * @exception NullPointerException if {@code names} is {@code null}
   *
   * @exception IllegalArgumentException if {@code qualified} is
   * {@code null} and the last element of {@code names} {@linkplain
   * String#isEmpty() is an empty <code>String</code>}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @see Element#Element(Qualifiers, Object, String)
   */
  public static final <T> Path<T> of(final T qualified, final List<? extends String> names) {
    final int lastIndex = names.size() - 1;
    final List<Element<Object>> elements;
    switch (lastIndex) {
    case -1:
      return new Path<>(Qualifiers.of(), List.of(), Element.of(qualified, null));
    case 0:
      return new Path<>(Qualifiers.of(), List.of(), Element.of(qualified, names.get(0)));
    default:
      elements = new ArrayList<>(lastIndex);
      for (int i = 0; i < lastIndex; i++) {
        final String name = names.get(i);
        elements.add(Element.of(name));
      }
      return new Path<>(Qualifiers.of(), elements, Element.of(qualified, names.get(lastIndex)));
    }
  }

  /**
   * Return a (<strong>usually new</strong>) {@link Path} formed from
   * the supplied arguments and returns it.
   *
   * @param <T> the type of the {@link Path}
   *
   * @param pathQualifiers the {@link Path}'s {@linkplain Qualifiers
   * qualifiers}; must not be {@code null}
   *
   * @param lastElement the {@linkplain #lastElement() last
   * <code>Element</code>} of the {@link Path}; must not be {@code
   * null}
   *
   * @return a (<strong>usually new</strong>) {@link Path}
   *
   * @exception NullPointerException if any argument is {@code null}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @see #Path(Qualifiers, List, Element)
   */
  public static final <T> Path<T> of(final Qualifiers<? extends String, ?> pathQualifiers,
                                     final Element<? extends T> lastElement) {
    return new Path<>(pathQualifiers, lastElement);
  }

  /**
   * Return a (<strong>usually new</strong>) {@link Path} formed from
   * the supplied arguments and returns it.
   *
   * @param <T> the type of the new {@link Path}
   *
   * @param pathQualifiers the {@link Path}'s {@linkplain Qualifiers qualifiers}; must
   * not be {@code null}
   *
   * @param elements the interior {@link Element}s of the {@link
   * Path}; must not be {@code null}
   *
   * @param lastElement the {@linkplain #lastElement() last
   * <code>Element</code>} of the {@link Path}; must not be {@code
   * null}
   *
   * @return a (<strong>usually new</strong>) {@link Path}
   *
   * @exception NullPointerException if any argument is {@code null}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @see #Path(Qualifiers, List, Element)
   */
  public static final <T> Path<T> of(final Qualifiers<? extends String, ?> pathQualifiers,
                                     final List<? extends Element<?>> elements,
                                     final Element<? extends T> lastElement) {
    return new Path<>(pathQualifiers, elements, lastElement);
  }

  private static final String findUserPackageName(final Stream<StackFrame> stream) {
    final String className = stream.sequential()
      .dropWhile(f -> f.getClassName().startsWith(Path.class.getPackageName()))
      .dropWhile(f -> f.getClassName().contains(".$Proxy")) // skip JDK proxies (and any other kind of proxies)
      .map(StackFrame::getClassName)
      .findFirst()
      .orElse(null);
    if (className == null) {
      return "";
    } else {
      final int lastIndex = className.lastIndexOf('.');
      if (lastIndex < 0) {
        return "";
      } else if (lastIndex == 0) {
        throw new AssertionError("className: " + className);
      } else {
        return className.substring(0, lastIndex);
      }
    }
  }


  /*
   * Inner and nested classes.
   */


  /**
   * An element normally {@linkplain Path#iterator() within} a {@link
   * Path}, consisting of a {@link #qualifiers() Qualifiers}, a
   * {@linkplain #name() name}, and an optional {@linkplain
   * #qualified() thing that it designates or points to}.
   *
   * @param <T> the type of this {@link Element}
   *
   * @author <a href="https://about.me/lairdnelson"
   * target="_parent">Laird Nelson</a>
   */
  public static final class Element<T> implements Qualified<String, Object, T> {


    /*
     * Static fields.
     */


    private static final Element<?> ROOT = new Element<>();


    /*
     * Instance fields.
     */


    private final Qualifiers<String, Object> qualifiers;

    private final T qualified;

    private final String name;


    /*
     * Constructors.
     */


    // For use by ROOT only.
    private Element() {
      super();
      this.qualifiers = Qualifiers.of();
      this.qualified = null;
      this.name = "";
    }

    /**
     * Creates a new {@link Element}.
     *
     * @param name the name of this {@link Element}; must not be
     * {@code null} or {@linkplain String#isEmpty() empty}
     *
     * @exception NullPointerException if {@code name} is {@code null}
     *
     * @exception IllegalArgumentException if {@code name} {@linkplain
     * String#isEmpty() is empty}
     *
     * @see #Element(Qualifiers, Object, String)
     */
    public Element(final String name) {
      this(Qualifiers.of(), null, name);
    }

    /**
     * Creates a new {@link Element}.
     *
     * @param qualified the thing this {@link Element} describes; may
     * be {@code null}
     *
     * @param name the name of this {@link Element}; may be {@code
     * null} only if {@code qualified} is not {@code null}
     *
     * @exception NullPointerException if {@code qualified} is {@code
     * null} and {@code name} is {@code null}
     *
     * @exception IllegalArgumentException if {@code qualified} is
     * {@code null} and {@code name} {@linkplain String#isEmpty() is
     * empty}
     *
     * @see #Element(Qualifiers, Object, String)
     */
    public Element(final T qualified, final String name) {
      this(Qualifiers.of(), qualified, name);
    }

    /**
     * Creates a new {@link Element}.
     *
     * @param qualifiers the {@linkplain Qualifiers qualifiers}
     * qualifying this {@link Element}; may be {@code null} in which
     * case an {@linkplain Qualifiers#of() empty
     * <code>Qualifiers</code>} will be used instead
     *
     * @param name the name of this {@link Element}; may be {@code
     * null} only if {@code qualified} is not {@code null}
     *
     * @exception NullPointerException if {@code name} is {@code null}
     *
     * @exception IllegalArgumentException if {@code name} {@linkplain
     * String#isEmpty() is empty}
     *
     * @see #Element(Qualifiers, Object, String)
     */
    @SuppressWarnings("unchecked")
    public Element(final Qualifiers<? extends String, ?> qualifiers, final String name) {
      super();
      if (name == null) {
        throw new NullPointerException("name");
      } else if (name.isEmpty()) {
        throw new IllegalArgumentException("An empty name may not be paired with a null qualified");
      } else {
        this.name = name;
      }
      this.qualifiers = qualifiers == null || qualifiers.isEmpty() ? Qualifiers.of() : (Qualifiers<String, Object>)qualifiers;
      this.qualified = null;
    }

    /**
     * Creates a new {@link Element}.
     *
     * @param qualifiers the {@linkplain Qualifiers qualifiers}
     * qualifying this {@link Element}; may be {@code null} in which
     * case an {@linkplain Qualifiers#of() empty
     * <code>Qualifiers</code>} will be used instead
     *
     * @param qualified the thing this {@link Element} describes; may
     * be {@code null}
     *
     * @param name the name of this {@link Element}; may be {@code
     * null} only if {@code qualified} is not {@code null}
     *
     * @exception NullPointerException if {@code qualified} is {@code
     * null} and {@code name} is {@code null}
     *
     * @exception IllegalArgumentException if {@code qualified} is
     * {@code null} and {@code name} {@linkplain String#isEmpty() is
     * empty}
     */
    @SuppressWarnings("unchecked")
    public Element(final Qualifiers<? extends String, ?> qualifiers, final T qualified, final String name) {
      super();
      if (qualified == null) {
        if (name == null) {
          throw new NullPointerException("name");
        } else if (name.isEmpty()) {
          throw new IllegalArgumentException("An empty name may not be paired with a null qualified");
        } else {
          this.name = name;
        }
      } else {
        this.name = name == null ? "" : name;
      }
      this.qualifiers = qualifiers == null || qualifiers.isEmpty() ? Qualifiers.of() : (Qualifiers<String, Object>)qualifiers;
      this.qualified = qualified;
    }


    /*
     * Instance methods.
     */


    /**
     * Returns an {@link Optional} containing a {@link ConstantDesc}
     * representing this {@link Element}.
     *
     * @return an {@link Optional} containing a {@link ConstantDesc}
     * representing this {@link Element}; never {@code null}; possibly
     * {@linkplain Optional#isEmpty() empty}
     *
     * @nullability This method never returns {@code null}.
     *
     * @idempotency This method is idempotent and deterministic.
     *
     * @threadsafety This method is safe for concurrent use by
     * multiple threads.
     */
    @Override // Constable
    public final Optional<? extends ConstantDesc> describeConstable() {
      if (this.isRoot()) {
        return
          Optional.of(DynamicConstantDesc.ofNamed(BSM_INVOKE,
                                                  DEFAULT_NAME,
                                                  CD_PathElement,
                                                  MethodHandleDesc.ofMethod(STATIC,
                                                                            CD_PathElement,
                                                                            "root",
                                                                            MethodTypeDesc.of(CD_PathElement))));
      } else {
        final ConstantDesc qualifiersDesc = this.qualifiers().describeConstable().orElse(null);
        if (qualifiersDesc != null) {
          final ConstantDesc qualifiedDesc;
          final Object qualified = this.qualified();
          if (qualified == null) {
            qualifiedDesc = NULL;
          } else if (qualified instanceof Constable cq) {
            qualifiedDesc = cq.describeConstable().orElse(null);
          } else if (qualified instanceof ConstantDesc cd) {
            qualifiedDesc = cd;
          } else {
            qualifiedDesc = null;
          }
          if (qualifiedDesc != null) {
            final String name = this.name();
            return
              Optional.of(DynamicConstantDesc.ofNamed(BSM_INVOKE,
                                                      name,
                                                      CD_PathElement,
                                                      MethodHandleDesc.ofConstructor(CD_PathElement,
                                                                                     CD_Qualifiers,
                                                                                     CD_Object,
                                                                                     CD_String),
                                                      qualifiersDesc,
                                                      qualifiedDesc,
                                                      name));
          }
        }
      }
      return Optional.empty();
    }

    /**
     * Returns the {@link Qualifiers} that qualifies this {@link
     * Element}.
     *
     * @return the {@link Qualifiers} that qualifies this {@link
     * Element}
     *
     * @nullability This method never returns {@code null}.
     *
     * @idempotency This method is idempotent and deterministic.
     *
     * @threadsafety This method is safe for concurrent use by multiple
     * threads.
     */
    @Override // Qualified<String, Object, T>
    public final Qualifiers<String, Object> qualifiers() {
      return this.qualifiers;
    }

    /**
     * Returns the thing that this {@link Element} describes (which
     * may be nothing).
     *
     * @return the thing that this {@link Element} describes, which
     * may be nothing, in which case {@code null} will be returned
     *
     * @nullability This method may return {@code null}.
     *
     * @idempotency This method is idempotent and deterministic.
     *
     * @threadsafety This method is safe for concurrent use by
     * multiple threads.
     */
    @Override // Qualified<String, Object, T>
    public final T qualified() {
      return this.qualified;
    }

    /**
     * Returns the name of this {@link Element}.
     *
     * @return the name of this {@link Element}
     *
     * @nullability This method never returns {@code null}.
     *
     * @idempotency This method is idempotent and deterministic.
     *
     * @threadsafety This method is safe for concurrent use by
     * multiple threads.
     */
    public final String name() {
      return this.name;
    }

    /**
     * Returns {@code true} if this {@link Element} is a <em>root
     * element</em>, which is true when the {@link #qualified()}
     * method returns {@code null} and the {@link #name()} method
     * returns an {@linkplain String#isEmpty() empty
     * <code>String</code>}.
     *
     * @return {@code true} if and only if this {@link Element} is a
     * <em>root element</em>
     *
     * @idempotency This method is idempotent and deterministic.
     *
     * @threadsafety This method is safe for concurrent use by
     * multiple threads.
     */
    public final boolean isRoot() {
      return this.qualified() == null && this.name().isEmpty();
    }

    /**
     * Returns a {@link Element}, <strong>usually newly
     * created</strong>, whose {@linkplain #qualifiers() qualifiers}
     * have keys that are prefixed with the supplied {@code prefix}.
     *
     * @param prefix the prefix to apply; may be {@code null} or
     * {@linkplain String#isEmpty() empty} in which case {@code this}
     * will be returned
     *
     * @return a {@link Element}, <strong>usually newly
     * created</strong>, whose {@linkplain #qualifiers() qualifiers}
     * have keys that are prefixed with the supplied {@code prefix}
     *
     * @nullability This method never returns {@code null}.
     *
     * @idempotency This method is idempotent and deterministic.
     *
     * @threadsafety This method is safe for concurrent use by
     * multiple threads.
     */
    public final Element<T> withQualifiersPrefix(final String prefix) {
      if (prefix == null || prefix.isEmpty()) {
        return this;
      }
      return Element.of(this.qualifiers().withPrefix(k -> prefix + k),
                        this.qualified(),
                        this.name());
    }

    /**
     * Returns a hashcode for this {@link Element}.
     *
     * @return a hashcode for this {@link Element}
     *
     * @idempotency This method is idempotent.
     *
     * @threadsafety This method is safe for concurrent use by
     * multiple threads.
     */
    @Override // Object
    public final int hashCode() {
      int hashCode = 17;
      Object value = this.qualified();
      int c = value == null ? 0 : value.hashCode();
      hashCode = 37 * hashCode + c;
      value = this.name();
      c = value == null ? 0 : value.hashCode();
      hashCode = 37 * hashCode + c;
      value = this.qualifiers();
      c = value == null ? 0 : value.hashCode();
      hashCode = 37 * hashCode + c;
      return hashCode;
    }

    /**
     * Returns {@code true} if the supplied {@link Object} is equal to
     * this {@link Element}.
     *
     * @param other the {@link Object} to test; may be {@code null} in
     * which case {@code false} will be returned
     *
     * @return {@code true} if the supplied {@link Object} is equal to
     * this {@link Element}; {@code false} otherwise
     *
     * @idempotency This method is idempotent and deterministic.
     *
     * @threadsafety This method is safe for concurrent use by
     * multiple threads.
     */
    @Override // Object
    public final boolean equals(final Object other) {
      if (this == other) {
        return true;
      } else if (other != null && this.getClass() == other.getClass()) {
        final Element<?> her = (Element<?>)other;
        return
          Objects.equals(this.qualified(), her.qualified()) &&
          Objects.equals(this.name(), her.name()) &&
          Objects.equals(this.qualifiers(), her.qualifiers());
      } else {
        return false;
      }
    }

    /**
     * Returns a non-{@code null} {@link String} representation of
     * this {@link Element}.
     *
     * <p>The format of the {@link String} that is returned is
     * deliberately unspecified and subject to change without notice
     * from version to version of this class.</p>
     *
     * @return a {@link String} representation of this {@link Element}
     *
     * @nullability This method never returns {@code null}
     *
     * @idempotency This method is idempotent and deterministic.
     *
     * @threadsafety This method is safe for concurrent use by
     * multiple threads.
     */
    @Override // Object
    public final String toString() {
      final StringBuilder sb = new StringBuilder();
      final T qualified = this.qualified();
      if (qualified != null) {
        // Handle the extremely common case that qualified is a Type.
        sb.append(qualified instanceof Type t ? t.getTypeName() : qualified).append(':');
      }
      sb.append(name);
      final Qualifiers<?, ?> q = this.qualifiers();
      if (!q.isEmpty()) {
        sb.append(q.toString());
      }
      return sb.toString();
    }


    /*
     * Static methods.
     */


    /**
     * Returns a {@link Element} that {@linkplain #isRoot() is a
     * <em>root element</em>}.
     *
     * @return a {@link Element} that {@linkplain #isRoot() is a
     * <em>root element</em>}
     *
     * @nullability This method never returns {@code null}.
     *
     * @idempotency This method is idempotent and deterministic.
     *
     * @threadsafety This method is safe for concurrent use by
     * multiple threads.
     */
    public static final Element<?> root() {
      return ROOT;
    }

    /**
     * Returns a {@link Element} representing the supplied arguments.
     *
     * <p>The returned {@link Element} may be newly created or it may
     * be cached.  No assumption must be made about its identity.</p>
     *
     * @param <T> the type of this {@link Element}
     *
     * @param qualifiers the {@link Qualifiers} qualifying this {@link
     * Element}; may be {@code null} in which case an {@linkplain
     * Qualifiers#of() empty <code>Qualifiers</code>} will be used
     * instead
     *
     * @param qualified the thing this {@link Element} describes; may
     * be {@code null}
     *
     * @param name the name of this {@link Element}; may be {@code
     * null} only if {@code qualified} is not {@code null}
     *
     * @return a {@link Element} representing the supplied arguments
     *
     * @exception NullPointerException if {@code qualified} is {@code
     * null} and {@code name} is {@code null}
     *
     * @exception IllegalArgumentException if {@code qualified} is
     * {@code null} and {@code name} {@linkplain String#isEmpty() is
     * empty}
     *
     * @nullability This method never returns {@code null}.
     *
     * @idempotency This method is idempotent and deterministic.
     *
     * @threadsafety This method is safe for concurrent use by
     * multiple threads.
     */
    public static final <T> Element<T> of(final Qualifiers<? extends String, ?> qualifiers,
                                          final T qualified,
                                          final String name) {
      return new Element<>(qualifiers, qualified, name);
    }

    /**
     * Returns a {@link Element} representing the supplied arguments.
     *
     * <p>The returned {@link Element} may be newly created or it may
     * be cached.  No assumption must be made about its identity.</p>
     *
     * @param <T> the type of this {@link Element}
     *
     * @param qualified the thing this {@link Element} describes; may
     * be {@code null}
     *
     * @param name the name of this {@link Element}; may be {@code
     * null} only if {@code qualified} is not {@code null}
     *
     * @return a {@link Element} representing the supplied arguments
     *
     * @exception NullPointerException if {@code qualified} is {@code
     * null} and {@code name} is {@code null}
     *
     * @exception IllegalArgumentException if {@code qualified} is
     * {@code null} and {@code name} {@linkplain String#isEmpty() is
     * empty}
     *
     * @nullability This method never returns {@code null}.
     *
     * @idempotency This method is idempotent and deterministic.
     *
     * @threadsafety This method is safe for concurrent use by
     * multiple threads.
     */
    public static final <T> Element<T> of(final T qualified, final String name) {
      return new Element<>(qualified, name);
    }

    /**
     * Returns a {@link Element} representing the supplied arguments.
     *
     * <p>The returned {@link Element} may be newly created or it may
     * be cached.  No assumption must be made about its identity.</p>
     *
     * @param <T> the type of this {@link Element}
     *
     * @param name the name of this {@link Element}; may be {@code
     * null} only if {@code qualified} is not {@code null}
     *
     * @return a {@link Element} representing the supplied arguments
     *
     * @exception NullPointerException if {@code qualified} is {@code
     * null} and {@code name} is {@code null}
     *
     * @exception IllegalArgumentException if {@code qualified} is
     * {@code null} and {@code name} {@linkplain String#isEmpty() is
     * empty}
     *
     * @nullability This method never returns {@code null}.
     *
     * @idempotency This method is idempotent and deterministic.
     *
     * @threadsafety This method is safe for concurrent use by
     * multiple threads.
     */
    public static final <T> Element<T> of(final String name) {
      return new Element<>(name);
    }

  }

}
