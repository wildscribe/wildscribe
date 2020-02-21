/*
 * Copyright 2020 Red Hat, Inc.
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

package org.jboss.wildscribe.site;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class AlertMessage {

    private String header;
    private String message;
    private boolean dismissible;
    private String type;

    public AlertMessage() {
        header = null;
        message = null;
        dismissible = false;
        type = "primary";
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(final String header) {
        this.header = header;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public boolean isDismissible() {
        return dismissible;
    }

    public void setDismissible(final boolean dismissible) {
        this.dismissible = dismissible;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type == null ? "primary" : type;
    }
}
