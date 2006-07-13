/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cocoon.servlet;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

/**
 * 
 * @version $Id$
 * @since 2.2
 */
public class RequestUtil {

    public static String getCompleteUri(HttpServletRequest request,
                                        HttpServletResponse response)
    throws IOException {
        // We got it... Process the request
        String uri = request.getServletPath();
        if (uri == null) {
            uri = "";
        }
        String pathInfo = request.getPathInfo();
        if (pathInfo != null) {
            // VG: WebLogic fix: Both uri and pathInfo starts with '/'
            // This problem exists only in WL6.1sp2, not in WL6.0sp2 or WL7.0b.
            if (uri.length() > 0 && uri.charAt(0) == '/') {
                uri = uri.substring(1);
            }
            uri += pathInfo;
        }

        if (uri.length() == 0) {
            /* empty relative URI
                 -> HTTP-redirect from /cocoon to /cocoon/ to avoid
                    StringIndexOutOfBoundsException when calling
                    "".charAt(0)
               else process URI normally
            */
            String prefix = request.getRequestURI();
            if (prefix == null) {
                prefix = "";
            }

            response.sendRedirect(response.encodeRedirectURL(prefix + "/"));
            return null;
        }

        if (uri.charAt(0) == '/') {
            uri = uri.substring(1);
        }
        return uri;
    }

    public static HttpServletRequest createRequestForUri(HttpServletRequest request, String uri) {
        return new HttpServletRequestImpl(request, uri);
    }

    /** TODO - we have to check the return values with the servlet spec! */
    protected static final class HttpServletRequestImpl extends HttpServletRequestWrapper {

        final private String uri;

        public HttpServletRequestImpl(HttpServletRequest request, String uri) {
            super(request);
            this.uri = uri;
        }

        public String getPathInfo() {
            return this.uri;
        }

        public String getRequestURI() {
            return this.uri;
        }

        public StringBuffer getRequestURL() {
            return new StringBuffer(this.uri);
        }

        public String getServletPath() {
            return null;
        }

    }
}
