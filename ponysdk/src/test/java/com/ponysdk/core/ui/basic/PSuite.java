package com.ponysdk.core.ui.basic;

import com.ponysdk.core.server.application.Application;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.stm.Txn;
import com.ponysdk.core.server.stm.TxnContext;
import com.ponysdk.core.writer.ModelWriter;
import org.junit.BeforeClass;
import org.mockito.Mockito;

public class PSuite {
    @BeforeClass
    public static void beforeClass() {
        Element.f = new TestElementFactory();
        TxnContext context = Mockito.spy(new TxnContext(null));
        ModelWriter mw = Mockito.mock(ModelWriter.class);
        Mockito.when(context.getWriter()).thenReturn(mw);
        Mockito.when(context.getApplication()).thenReturn(Mockito.mock(Application.class, Mockito.RETURNS_MOCKS));
        Txn.get().begin(context);
        UIContext uiContext = Mockito.spy(new UIContext(context));
        UIContext.setCurrent(uiContext);
    }
}
