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

package com.ponysdk.core.ui.basic;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import javax.json.JsonObject;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.server.application.UIContext;

/**
 * This class allows to execute native Java-script code.
 */
public class PScript extends PObject {

    private static final String SCRIPT_KEY = PScript.class.getCanonicalName();
    private final Map<Long, ExecutionCallback> callbacksByID = new HashMap<>();
    private long executionID = 0;
    private String key;

    private PScript() {
    }

    private static final PScript get(final PWindow window) {
        if (window != null) {
            final UIContext uiContext = UIContext.get();
            final String key = SCRIPT_KEY + window.getID();
            final PScript script = uiContext.getAttribute(SCRIPT_KEY + window.getID());
            if (script == null) {
                final PScript newScript = new PScript();
                newScript.key = key;
                uiContext.setAttribute(key, newScript);
                newScript.attach(window, null);
                return newScript;
            }
            return script;
        } else {
            throw new IllegalArgumentException("PScript need to be executed on a window");
        }
    }

    @Override
    protected boolean attach(final PWindow window, final PFrame frame) {
        final boolean result = super.attach(window, frame);
        if (result) window.addDestroyListener(event -> onDestroy());
        return result;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        UIContext.get().removeAttribute(key);
    }

    public static void execute(final PWindow window, final String js) {
        execute(window, js, null, null);
    }

    public static void execute(final PWindow window, final String js, final ExecutionCallback callback) {
        execute(window, js, callback, null);
    }

    public static void execute(final PWindow window, final String js, final Duration period) {
        execute(window, js, null, period);
    }

    public static void execute(final PWindow window, final String js, final ExecutionCallback callback, final Duration period) {
        get(window).executeScript(js, callback, period);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.SCRIPT;
    }

    private void executeScript(final String js, final ExecutionCallback callback, final Duration period) {
        saveUpdate((writer) -> {
            writer.write(ServerToClientModel.EVAL, js);
            if (callback != null) {
                callbacksByID.put(++executionID, callback);
                writer.write(ServerToClientModel.COMMAND_ID, executionID);
            }
            if (period != null) {
                writer.write(ServerToClientModel.FIXDELAY, period.toMillis());
            }
        });
    }

    @Override
    public void onClientData(final JsonObject instruction) {
        if (instruction.containsKey(ClientToServerModel.ERROR_MSG.toStringValue())) {
            final ExecutionCallback callback = callbacksByID
                .remove(instruction.getJsonNumber(ClientToServerModel.COMMAND_ID.toStringValue()).longValue());
            if (callback != null) callback.onFailure(instruction.getString(ClientToServerModel.ERROR_MSG.toStringValue()));
        } else if (instruction.containsKey(ClientToServerModel.RESULT.toStringValue())) {
            final ExecutionCallback callback = callbacksByID
                .remove(instruction.getJsonNumber(ClientToServerModel.COMMAND_ID.toStringValue()).longValue());
            if (callback != null) callback.onSuccess(instruction.getString(ClientToServerModel.RESULT.toStringValue()));
        } else {
            super.onClientData(instruction);
        }
    }

    public interface ExecutionCallback {

        void onFailure(String msg);

        void onSuccess(String msg);
    }

}
