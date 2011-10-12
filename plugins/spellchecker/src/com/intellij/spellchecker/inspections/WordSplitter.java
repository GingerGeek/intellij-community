/*
 * Copyright 2000-2010 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.spellchecker.inspections;

import com.intellij.openapi.util.TextRange;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WordSplitter extends BaseSplitter {

  @NonNls
  private static final Pattern SPECIAL = Pattern.compile("&\\p{Alnum}{4};?|#\\p{Alnum}{3,6}|0x\\p{Alnum}?");

  @Override
  public void split(@Nullable String text, @NotNull TextRange range, Consumer<TextRange> consumer) {
    if (text == null || range.getLength() <= 1) {
      return;
    }

    Matcher specialMatcher = SPECIAL.matcher(range.substring(text));
    if (specialMatcher.find()) {
      TextRange found = matcherRange(range, specialMatcher);
      addWord(consumer, true, found);
    }
    else {
      SplitterFactory.getInstance().getIdentifierSplitter().split(text, range, consumer);
    }
  }
}
