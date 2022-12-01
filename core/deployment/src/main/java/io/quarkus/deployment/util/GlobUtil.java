package io.quarkus.deployment.util;

import java.util.regex.Pattern;

/**
 * @deprecated in favor of {@link io.quarkus.util.GlobUtil}
 */
@Deprecated
public class GlobUtil {

    private GlobUtil() {
    }

    /**
     * @deprecated in favor of {@link io.quarkus.util.GlobUtil#toRegexPattern(String)}
     *
     *             Transforms the given {@code glob} to a regular expression suitable for passing to
     *             {@link Pattern#compile(String)}.
     *
     *             <h2>Glob syntax
     *             <h2>
     *
     *             <table>
     *             <tr>
     *             <th>Construct</th>
     *             <th>Description</th>
     *             </tr>
     *             <tr>
     *             <td><code>*</code></td>
     *             <td>Matches a (possibly empty) sequence of characters that does not contain slash ({@code /})</td>
     *             </tr>
     *             <tr>
     *             <td><code>**</code></td>
     *             <td>Matches a (possibly empty) sequence of characters that may contain slash ({@code /})</td>
     *             </tr>
     *             <tr>
     *             <td><code>?</code></td>
     *             <td>Matches one character, but not slash</td>
     *             </tr>
     *             <tr>
     *             <td><code>[abc]</code></td>
     *             <td>Matches one character given in the bracket, but not slash</td>
     *             </tr>
     *             <tr>
     *             <td><code>[a-z]</code></td>
     *             <td>Matches one character from the range given in the bracket, but not slash</td>
     *             </tr>
     *             <tr>
     *             <td><code>[!abc]</code></td>
     *             <td>Matches one character not named in the bracket; does not match slash</td>
     *             </tr>
     *             <tr>
     *             <td><code>[a-z]</code></td>
     *             <td>Matches one character outside the range given in the bracket; does not match slash</td>
     *             </tr>
     *             <tr>
     *             <td><code>{one,two,three}</code></td>
     *             <td>Matches any of the alternating tokens separated by comma; the tokens may contain wildcards, nested
     *             alternations and ranges</td>
     *             </tr>
     *             <tr>
     *             <td><code>\</code></td>
     *             <td>The escape character</td>
     *             </tr>
     *             </table>
     *
     * @param glob the glob expression to transform
     * @return a regular expression suitable for {@link Pattern}
     * @throws IllegalStateException in case the {@code glob} is syntactically invalid
     */
    @Deprecated
    public static String toRegexPattern(String glob) {
        return io.quarkus.util.GlobUtil.toRegexPattern(glob);
    }
}
