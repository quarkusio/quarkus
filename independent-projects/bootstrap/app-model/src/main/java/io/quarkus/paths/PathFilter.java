package io.quarkus.paths;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import io.quarkus.util.GlobUtil;

public class PathFilter implements Serializable {

    private static final long serialVersionUID = -5712472676677054175L;

    public static boolean isVisible(PathFilter filter, String path) {
        if (path == null) {
            return false;
        }
        if (filter == null) {
            return true;
        }
        return filter.isVisible(path.replace('\\', '/'));
    }

    public static PathFilter forIncludes(Collection<String> includes) {
        return new PathFilter(includes, null);
    }

    public static PathFilter forExcludes(Collection<String> excludes) {
        return new PathFilter(null, excludes);
    }

    private List<Pattern> includes;
    private List<Pattern> excludes;

    public PathFilter(Collection<String> includes, Collection<String> excludes) {
        this.includes = compile(includes);
        this.excludes = compile(excludes);
    }

    public boolean isVisible(String pathStr) {
        if (includes != null && !includes.isEmpty()) {
            int i = 0;
            while (i < includes.size()) {
                if (includes.get(i).matcher(pathStr).matches()) {
                    break;
                }
                ++i;
            }
            if (i == includes.size()) {
                return false;
            }
        }
        if (excludes != null) {
            for (Pattern pattern : excludes) {
                if (pattern.matcher(pathStr).matches()) {
                    return false;
                }
            }
            return true;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(excludes, includes);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PathFilter other = (PathFilter) obj;
        return Objects.equals(excludes, other.excludes) && Objects.equals(includes, other.includes);
    }

    @Override
    public String toString() {
        final StringBuilder s = new StringBuilder();
        if (includes != null && !includes.isEmpty()) {
            s.append("includes ").append(includes.get(0).pattern());
            for (int i = 1; i < includes.size(); ++i) {
                s.append(",").append(includes.get(i));
            }
        }
        if (excludes != null && !excludes.isEmpty()) {
            if (s.length() > 0) {
                s.append(" ");
            }
            s.append("excludes ").append(excludes.get(0).pattern());
            for (int i = 1; i < excludes.size(); ++i) {
                s.append(",").append(excludes.get(i));
            }
        }
        return s.toString();
    }

    private static List<Pattern> compile(Collection<String> expressions) {
        if (expressions == null) {
            return null;
        }
        final List<Pattern> compiled = new ArrayList<>(expressions.size());
        for (String expr : expressions) {
            compiled.add(Pattern.compile(GlobUtil.toRegexPattern(expr)));
        }
        return compiled;
    }
}
