/*
 * Copyright (C) 2014 Stefan Niederhauser (nidin@gmx.ch)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package guru.nidi.snippets;

import java.io.IOException;
import java.io.Reader;

final class IoUtils {
    private IoUtils() {
    }

    public static String read(Reader in) throws IOException {
        final StringBuilder s = new StringBuilder();
        final char[] buf = new char[1000];
        int read;
        while ((read = in.read(buf)) > 0) {
            s.append(buf, 0, read);
        }
        return s.toString();
    }

}
