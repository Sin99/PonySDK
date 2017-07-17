package com.ponysdk.core.ui.basic;

import com.google.gwt.event.dom.client.ClickHandler;
import com.ponysdk.core.model.PCheckBoxState;
import com.ponysdk.core.ui.basic.event.PClickEvent;
import com.ponysdk.core.ui.basic.event.PClickHandler;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class TestUnit extends PSuite {

    @Test
    public void testLabel() {
        PLabel label = Element.newPLabel("test");
        Assert.assertEquals("test", label.getText());
    }

    @Test
    public void testListbox() {
        PListBox listBox = Element.newPListBox();
        listBox.addItem("Item1");
        listBox.addItem("Item2");
        listBox.addItem("Item3");
        listBox.addItem("Item4");
        listBox.addItem("Item5");
        Assert.assertEquals(5, listBox.getItemCount());
    }

    @Test
    public void testButton() {
        PButton button = Element.newPButton("test");
        Assert.assertEquals("test", button.getText());
        button.setText("test2");
        Assert.assertEquals("test2", button.getText());

        PClickHandler handler = Mockito.mock(PClickHandler.class);
        button.addClickHandler(handler);
        button.fireEvent(new PClickEvent(this));
        Mockito.verify(handler);
    }

    @Test
    public void testCheckBox() {
        PCheckBox checkBox = Element.newPCheckBox();
        Assert.assertEquals(PCheckBoxState.UNCHECKED, checkBox.getState());
        checkBox.setState(PCheckBoxState.CHECKED);
        Assert.assertEquals(PCheckBoxState.CHECKED, checkBox.getState());
    }
}
