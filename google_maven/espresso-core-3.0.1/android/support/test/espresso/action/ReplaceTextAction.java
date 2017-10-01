/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.support.test.espresso.action;

import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.hamcrest.Matchers.allOf;

import android.view.View;
import android.widget.EditText;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.remote.annotation.RemoteMsgConstructor;
import android.support.test.espresso.remote.annotation.RemoteMsgField;
import org.hamcrest.Matcher;

/**
 * Replaces view text by setting {@link EditText}s text property to given String.
 */
public final class ReplaceTextAction implements ViewAction {
  @RemoteMsgField(order = 0)
  final String stringToBeSet;

  @RemoteMsgConstructor
  public ReplaceTextAction(String value) {
    checkNotNull(value);
    this.stringToBeSet = value;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Matcher<View> getConstraints() {
    return allOf(isDisplayed(), isAssignableFrom(EditText.class));
  }

  @Override
  public void perform(UiController uiController, View view) {
    ((EditText) view).setText(stringToBeSet);
  }

  @Override
  public String getDescription() {
    return "replace text";
  }
}
