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

package com.ponysdk.ui.server.basic;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ponysdk.core.Parser;
import com.ponysdk.ui.model.ServerToClientModel;
import com.ponysdk.ui.server.basic.event.PHasHTML;
import com.ponysdk.ui.terminal.WidgetType;

/**
 * A widget that represents a simple &lt;a&gt; element.
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-Anchor { }</li>
 * </ul>
 */
public class PAnchor extends PFocusWidget implements PHasHTML {

    private static final Pattern PATTERN = Pattern.compile("\"", Pattern.LITERAL);
    private static final String REPLACEMENT = Matcher.quoteReplacement("\\\"");

    private String text;
    private String html;
    private String href;

    public PAnchor() {
    }

    /**
     * Creates an anchor with its text specified.
     *
     * @param text
     *            the anchor's text
     */
    public PAnchor(final String text) {
        this(text, null);
    }

    /**
     * Creates an anchor with its text and href (target URL) specified.
     *
     * @param text
     *            the anchor's text
     * @param href
     *            the url to which it will link
     */
    public PAnchor(final String text, final String href) {
        this.text = text;
        this.href = href;
    }

    @Override
    protected void enrichOnInit(final Parser parser) {
        super.enrichOnInit(parser);
        if (text != null) parser.parse(ServerToClientModel.TEXT, text);
        if (href != null) parser.parse(ServerToClientModel.HREF, href);
        if (html != null) parser.parse(ServerToClientModel.HTML, PATTERN.matcher(html).replaceAll(REPLACEMENT));
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.ANCHOR;
    }

    /**
     * Gets the anchor's href (the url to which it links).
     *
     * @return the anchor's href
     */
    public String getHref() {
        return href;
    }

    /**
     * Sets the anchor's href (the url to which it links).
     *
     * @param href
     *            the anchor's href
     */
    public void setHref(final String href) {
        if (Objects.equals(this.href, href)) return;
        this.href = href;

        saveUpdate((writer) -> {
            writer.writeModel(ServerToClientModel.HREF, this.href);
        });
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public void setText(final String text) {
        if (Objects.equals(this.text, text)) return;
        this.text = text;
        saveUpdate((writer) -> {
            writer.writeModel(ServerToClientModel.TEXT, this.text);
        });
    }

    @Override
    public String getHTML() {
        return html;
    }

    @Override
    public void setHTML(final String html) {
        if (Objects.equals(this.html, html)) return;
        this.html = html;
        saveUpdate((writer) -> {
            writer.writeModel(ServerToClientModel.HTML, PATTERN.matcher(html).replaceAll(REPLACEMENT));
        });
    }

}
