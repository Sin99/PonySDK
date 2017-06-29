/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
 *
 *  WebSite:
 *  http://code.google.com/p/pony-sdk/
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.ponysdk.core.ui.form.formfield;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.form.dataconverter.DataConverter;

public class TextBoxFormField<T> extends AbstractFormField<T, PTextBox> {

    protected T initialValue;

    public TextBoxFormField(final DataConverter<String, T> dataProvider) {
        this(Element.newPTextBox(), dataProvider);
    }

    public TextBoxFormField(final PTextBox widget, final DataConverter<String, T> dataProvider) {
        super(widget, dataProvider);
        widget.addValueChangeHandler(event -> fireValueChange(getValue()));
    }

    @Override
    public void reset0() {
        widget.setText(null);
    }

    @Override
    public T getValue() {
        return dataProvider.to(widget.getText());
    }

    @Override
    public void setValue(final T value) {
        initialValue = value;
        widget.setValue(dataProvider.from(value));
    }

    @Override
    protected String getStringValue() {
        return widget.getText();
    }

    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);
        widget.setEnabled(enabled);
    }

    @Override
    public void commit() {
        initialValue = getValue();
    }

    @Override
    public void rollback() {
        setValue(initialValue);
    }

}
