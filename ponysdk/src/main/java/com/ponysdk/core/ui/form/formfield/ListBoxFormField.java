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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PListBox;
import com.ponysdk.core.ui.form.dataconverter.DataConverter;

public class ListBoxFormField<T> extends AbstractFormField<T, PListBox> {

    protected Set<String> initialSelectedIndexes = new HashSet<>();

    public ListBoxFormField() {
        this(Element.newPListBox(), null);
    }

    public ListBoxFormField(final PListBox widget) {
        this(widget, null);
    }

    public ListBoxFormField(final Map<String, T> datas) {
        this(Element.newPListBox(), null);
        datas.entrySet().forEach(entry -> widget.addItem(entry.getKey(), entry.getValue()));
    }

    public ListBoxFormField(final PListBox widget, final DataConverter<String, T> dataProvider) {
        super(widget, dataProvider);
        widget.addChangeHandler(event -> fireValueChange(getValue()));
        initialSelectedIndexes.addAll(widget.getSelectedItems());
    }

    @Override
    public void reset0() {
        widget.selectIndex(-1);
    }

    @Override
    public T getValue() {
        return (T) widget.getSelectedValue();
    }

    @Override
    protected String getStringValue() {
        final Object value = widget.getSelectedValue();
        if (value != null) String.valueOf(value);
        return null;
    }

    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);
        widget.setEnabled(enabled);
    }

    @Override
    public void setValue(final T value) {
        widget.selectValue(value);
    }

    public void addItem(final String item) {
        widget.addItem(item);
    }

    public void addItem(final String item, final T value) {
        widget.addItem(item, value);
    }

    public String getSelectedItem() {
        return widget.getSelectedItem();
    }

    @Override
    public void commit() {
        initialSelectedIndexes.clear();
        initialSelectedIndexes.addAll(widget.getSelectedItems());
    }

    @Override
    public void rollback() {
        initialSelectedIndexes.forEach(widget::selectItem);
    }

}
