/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *	Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *	Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

package com.ponysdk.ui.server.list.event;

import com.ponysdk.core.event.PSystemEvent;
import com.ponysdk.core.query.ComparatorType;

public class ComparatorTypeChangeEvent extends PSystemEvent<ComparatorTypeChangeHandler> {

    public static final Type<ComparatorTypeChangeHandler> TYPE = new Type<ComparatorTypeChangeHandler>();

    private final ComparatorType comparatorType;

    private final String pojoPropertyKey;

    public ComparatorTypeChangeEvent(Object sourceComponent, ComparatorType comparatorType, String pojoPropertyKey) {
        super(sourceComponent);
        this.comparatorType = comparatorType;
        this.pojoPropertyKey = pojoPropertyKey;
    }

    @Override
    protected void dispatch(ComparatorTypeChangeHandler handler) {
        handler.onComparatorTypeChange(this);
    }

    @Override
    public Type<ComparatorTypeChangeHandler> getAssociatedType() {
        return TYPE;
    }

    public ComparatorType getComparatorType() {
        return comparatorType;
    }

    public String getPojoPropertyKey() {
        return pojoPropertyKey;
    }

}
