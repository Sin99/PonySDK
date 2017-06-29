
package com.ponysdk.core.ui.datagrid.impl;

import java.util.function.Function;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PLabel;

public class PLabelCellRenderer<DataType> extends TypedCellRenderer<DataType, PLabel> {

    private static final String EMPTY = "";
    private final Function<DataType, String> transform;

    public PLabelCellRenderer() {
        this(value -> value != null ? String.valueOf(value) : EMPTY);
    }

    public PLabelCellRenderer(final Function<DataType, String> transform) {
        this.transform = transform;
    }

    @Override
    public PLabel render(final DataType value) {
        return Element.newPLabel(transform.apply(value));
    }

    @Override
    protected PLabel update0(final DataType value, final PLabel widget) {
        widget.setText(transform.apply(value));
        return widget;
    }

    @Override
    protected void reset0(final PLabel widget) {
        if (widget != null) widget.setText(EMPTY);
    }

}
