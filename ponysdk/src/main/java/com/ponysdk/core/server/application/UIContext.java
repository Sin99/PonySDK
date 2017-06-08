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

package com.ponysdk.core.server.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.HandlerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.AlreadyDestroyedApplication;
import com.ponysdk.core.server.servlet.CommunicationSanityChecker;
import com.ponysdk.core.server.stm.Txn;
import com.ponysdk.core.server.stm.TxnContext;
import com.ponysdk.core.ui.basic.DataListener;
import com.ponysdk.core.ui.basic.PCookies;
import com.ponysdk.core.ui.basic.PHistory;
import com.ponysdk.core.ui.basic.PObject;
import com.ponysdk.core.ui.eventbus.BroadcastEventHandler;
import com.ponysdk.core.ui.eventbus.Event;
import com.ponysdk.core.ui.eventbus.EventHandler;
import com.ponysdk.core.ui.eventbus.HandlerRegistration;
import com.ponysdk.core.ui.eventbus.RootEventBus;
import com.ponysdk.core.ui.eventbus.StreamHandler;
import com.ponysdk.core.ui.eventbus2.EventBus;
import com.ponysdk.core.ui.statistic.TerminalDataReceiver;
import com.ponysdk.core.writer.ModelWriter;

/**
 * <p>
 * Provides a way to identify a user across more than one page request or visit to a Web site and to
 * store information about that user.
 * </p>
 * <p>
 * There is ONE unique UIContext for each screen displayed. Each UIContext is bound to the current
 * {@link Application} .
 * </p>
 */
public class UIContext {

    private static final ThreadLocal<UIContext> currentContext = new ThreadLocal<>();

    private static final Logger log = LoggerFactory.getLogger(UIContext.class);

    private static final AtomicInteger uiContextCount = new AtomicInteger();

    private final int ID;
    private final Application application;
    private final ReentrantLock lock = new ReentrantLock();
    private final Map<String, Object> attributes = new HashMap<>();

    private int objectCounter = 1;
    private final PObjectWeakHashMap pObjectWeakReferences = new PObjectWeakHashMap();

    private int streamRequestCounter = 0;
    private final Map<Integer, StreamHandler> streamListenerByID = new HashMap<>();

    private final PHistory history = new PHistory();
    private final RootEventBus rootEventBus = new RootEventBus();
    private final EventBus newEventBus = new EventBus();

    private final PCookies cookies = new PCookies();

    private final CommunicationSanityChecker communicationSanityChecker;
    private final List<UIContextListener> uiContextListeners = new ArrayList<>();
    private final TxnContext context;
    private final Set<DataListener> listeners = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private TerminalDataReceiver terminalDataReceiver;

    private boolean living = true;

    private final BoundedLinkedList<Long> pings = new BoundedLinkedList<>(10);

    public UIContext(final TxnContext context) {
        this.application = context.getApplication();
        this.ID = uiContextCount.incrementAndGet();
        this.context = context;

        this.communicationSanityChecker = new CommunicationSanityChecker(this);
        this.communicationSanityChecker.start();
    }

    public static final UIContext get() {
        return currentContext.get();
    }

    public static final void remove() {
        currentContext.remove();
    }

    public static final void setCurrent(final UIContext uiContext) {
        currentContext.set(uiContext);
    }

    public static final HandlerRegistration addHandler(final Event.Type type, final EventHandler handler) {
        return getRootEventBus().addHandler(type, handler);
    }

    public static final void removeHandler(final Event.Type type, final EventHandler handler) {
        getRootEventBus().removeHandler(type, handler);
    }

    public static final HandlerRegistration addHandlerToSource(final Event.Type type, final Object source,
                                                               final EventHandler handler) {
        return getRootEventBus().addHandlerToSource(type, source, handler);
    }

    public static final void removeHandlerFromSource(final Event.Type type, final Object source, final EventHandler handler) {
        getRootEventBus().removeHandlerFromSource(type, source, handler);
    }

    public static final void fireEvent(final Event<? extends EventHandler> event) {
        getRootEventBus().fireEvent(event);
    }

    public static final void fireEventFromSource(final Event<? extends EventHandler> event, final Object source) {
        getRootEventBus().fireEventFromSource(event, source);
    }

    public static final void addHandler(final BroadcastEventHandler handler) {
        getRootEventBus().addHandler(handler);
    }

    public static final void removeHandler(final BroadcastEventHandler handler) {
        getRootEventBus().removeHandler(handler);
    }

    public static final RootEventBus getRootEventBus() {
        return get().rootEventBus;
    }

    public static final EventBus getNewEventBus() {
        return get().newEventBus;
    }

    public int getID() {
        return ID;
    }

    public void addDataListener(final DataListener listener) {
        listeners.add(listener);
    }

    public void removeDataListener(final DataListener listener) {
        listeners.remove(listener);
    }

    public void execute(final Runnable runnable) {
        if (log.isDebugEnabled()) log.debug("Pushing to #" + this);
        if (UIContext.get() != this) {
            begin();
            try {
                final Txn txn = Txn.get();
                txn.begin(context);
                try {
                    runnable.run();
                    txn.commit();
                } catch (final Throwable e) {
                    log.error("Cannot process client instruction", e);
                    txn.rollback();
                }
            } finally {
                end();
            }
        } else {
            runnable.run();
        }
    }

    public void pushToClient(final List<Object> data) {
        execute(() -> fireOnData(data));
    }

    public void pushToClient(final Object data) {
        execute(() -> fireOnData(data));
    }

    private void fireOnData(final List<Object> data) {
        if (listeners.isEmpty()) return;
        try {
            listeners.forEach(listener -> data.forEach(listener::onData));
        } catch (final Throwable e) {
            log.error("Cannot send data", e);
        }
    }

    private void fireOnData(final Object data) {
        if (listeners.isEmpty()) return;
        try {
            listeners.forEach(listener -> listener.onData(data));
        } catch (final Throwable e) {
            log.error("Cannot send data", e);
        }
    }

    public void fireClientData(final JsonObject jsonObject) {
        if (jsonObject.containsKey(ClientToServerModel.TYPE_HISTORY.toStringValue())) {
            if (history != null) {
                history.fireHistoryChanged(jsonObject.getString(ClientToServerModel.TYPE_HISTORY.toStringValue()));
            }
        } else {
            final JsonValue jsonValue = jsonObject.get(ClientToServerModel.OBJECT_ID.toStringValue());
            int objectID;
            if (ValueType.NUMBER.equals(jsonValue.getValueType())) {
                objectID = ((JsonNumber) jsonValue).intValue();
            } else if (ValueType.STRING.equals(jsonValue.getValueType())) {
                objectID = Integer.parseInt(((JsonString) jsonValue).getString());
            } else {
                log.error("unknown reference from the browser. Unable to execute instruction: " + jsonObject);
                return;
            }

            //Cookies
            if (objectID == 0) {
                cookies.onClientData(jsonObject);
            } else {
                final PObject object = getObject(objectID);

                if (object == null) {
                    log.error("unknown reference from the browser. Unable to execute instruction: " + jsonObject);

                    if (jsonObject.containsKey(ClientToServerModel.PARENT_OBJECT_ID.toStringValue())) {
                        final int parentObjectID = jsonObject.getJsonNumber(ClientToServerModel.PARENT_OBJECT_ID.toStringValue())
                            .intValue();
                        final PObject gcObject = pObjectWeakReferences.get(parentObjectID);
                        log.warn(String.valueOf(gcObject));
                    }

                    return;
                }

                if (terminalDataReceiver != null) terminalDataReceiver.onDataReceived(object, jsonObject);

                object.onClientData(jsonObject);
            }
        }
    }

    public void setClientDataOutput(final TerminalDataReceiver clientDataOutput) {
        this.terminalDataReceiver = clientDataOutput;
    }

    public void begin() {
        lock.lock();
        UIContext.setCurrent(this);
    }

    public void end() {
        UIContext.remove();
        lock.unlock();
    }

    public int nextID() {
        return objectCounter++;
    }

    public void registerObject(final PObject object) {
        pObjectWeakReferences.put(object.getID(), object);
    }

    public <T> T getObject(final int objectID) {
        return (T) pObjectWeakReferences.get(objectID);
    }

    public void stackStreamRequest(final StreamHandler streamListener) {
        final int streamRequestID = nextStreamRequestID();

        final ModelWriter writer = Txn.get().getWriter();
        writer.beginObject();
        writer.write(ServerToClientModel.TYPE_ADD_HANDLER, -1);
        writer.write(ServerToClientModel.HANDLER_TYPE, HandlerModel.HANDLER_STREAM_REQUEST.getValue());
        writer.write(ServerToClientModel.STREAM_REQUEST_ID, streamRequestID);
        writer.write(ServerToClientModel.APPLICATION_ID, getApplication().getId());
        writer.endObject();

        streamListenerByID.put(streamRequestID, streamListener);
    }

    public void stackEmbededStreamRequest(final StreamHandler streamListener, final int objectID) {
        final int streamRequestID = nextStreamRequestID();

        final ModelWriter writer = Txn.get().getWriter();
        writer.beginObject();
        writer.write(ServerToClientModel.TYPE_ADD_HANDLER, objectID);
        writer.write(ServerToClientModel.HANDLER_TYPE, HandlerModel.HANDLER_EMBEDED_STREAM_REQUEST.getValue());
        writer.write(ServerToClientModel.STREAM_REQUEST_ID, streamRequestID);
        writer.write(ServerToClientModel.APPLICATION_ID, getApplication().getId());
        writer.endObject();

        streamListenerByID.put(streamRequestID, streamListener);
    }

    private int nextStreamRequestID() {
        return streamRequestCounter++;
    }

    public StreamHandler removeStreamListener(final int streamID) {
        return streamListenerByID.remove(streamID);
    }

    public PHistory getHistory() {
        return history;
    }

    public PCookies getCookies() {
        return cookies;
    }

    public void close() {
        final ModelWriter writer = Txn.get().getWriter();
        writer.beginObject();
        writer.write(ServerToClientModel.DESTROY_CONTEXT, null);
        writer.endObject();
    }

    /**
     * Binds an object to this session, using the name specified. If an object
     * of the same name is already bound to the session, the object is replaced.
     * <p>
     * If the value passed in is null, this has the same effect as calling
     * <code>removeAttribute()<code>.
     *
     * @param name
     *            the name to which the object is bound; cannot be null
     * @param value
     *            the object to be bound
     */
    public void setAttribute(final String name, final Object value) {
        if (value == null) removeAttribute(name);
        else attributes.put(name, value);
    }

    /**
     * Removes the object bound with the specified name from this session. If
     * the session does not have an object bound with the specified name, this
     * method does nothing.
     *
     * @param name
     *            the name of the object to remove from this session
     */

    public Object removeAttribute(final String name) {
        return attributes.remove(name);
    }

    /**
     * Returns the object bound with the specified name in this session, or <code>null</code> if no
     * object is bound under the name.
     *
     * @param name
     *            a string specifying the name of the object
     * @return the object with the specified name
     */
    public <T> T getAttribute(final String name) {
        return (T) attributes.get(name);
    }

    public Application getApplication() {
        return application;
    }

    public void notifyMessageReceived() {
        communicationSanityChecker.onMessageReceived();
    }

    public void onDestroy() {
        begin();
        try {
            doDestroy();
        } finally {
            end();
        }
    }

    void destroyFromApplication() {
        begin();
        try {
            living = false;
            communicationSanityChecker.stop();
            uiContextListeners.forEach(listener -> {
                try {
                    listener.onUIContextDestroyed(this);
                } catch (final AlreadyDestroyedApplication e) {
                    if (log.isDebugEnabled()) log.debug("Exception while destroying UIContext #" + getID(), e);
                } catch (final Exception e) {
                    log.error("Exception while destroying UIContext #" + getID(), e);
                }
            });
            context.close();
        } finally {
            end();
        }
    }

    public void destroy() {
        begin();
        try {
            doDestroy();
            context.close();
        } finally {
            end();
        }
    }

    private void doDestroy() {
        living = false;
        communicationSanityChecker.stop();
        application.unregisterUIContext(ID);
        uiContextListeners.forEach(listener -> listener.onUIContextDestroyed(this));
    }

    public void sendHeartBeat() {
        begin();
        try {
            context.sendHeartBeat();
        } catch (final Throwable e) {
            log.error("Cannot send server heart beat to UIContext #" + getID(), e);
        } finally {
            end();
        }
    }

    public void sendRoundTrip() {
        begin();
        try {
            context.sendRoundTrip();
        } catch (final Throwable e) {
            log.error("Cannot send server round trip to UIContext #" + getID(), e);
        } finally {
            end();
        }
    }

    public void addUIContextListener(final UIContextListener listener) {
        uiContextListeners.add(listener);
    }

    public boolean isLiving() {
        return living;
    }

    public TxnContext getContext() {
        return context;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final UIContext uiContext = (UIContext) o;
        return ID == uiContext.ID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ID);
    }

    @Override
    public String toString() {
        return "UIContext [ID=" + ID + ", living=" + living + ", ApplicationID=" + application.getId() + "]";
    }

    public void enableCommunicationChecker(final boolean enable) {
        communicationSanityChecker.enableCommunicationChecker(enable);
    }

    public void addPingValue(final long pingValue) {
        pings.add(pingValue);
    }

    /**
     * Get an average latency from the last 10 measurements
     */
    public double getLatency() {
        return pings.stream().mapToLong(Long::longValue).average().orElse(0);
    }

    private static final class BoundedLinkedList<E> extends LinkedList<E> {

        private final int limit;

        public BoundedLinkedList(final int limit) {
            this.limit = limit;
        }

        @Override
        public boolean add(final E e) {
            if (this.size() == this.limit) this.removeFirst();
            return super.add(e);
        }
    }

}
