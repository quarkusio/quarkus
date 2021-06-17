package io.quarkus.deployment.util;

import java.util.List;

public class OuterParam<W> {
    public String aaa(String arg, W arg2, OuterParam<W> self) throws IllegalArgumentException {
        return null;
    }

    public <T extends Number & Comparable<T>, U extends Comparable<U>, V extends Exception> T bbb(
            U arg, W arg2, OuterParam<W> self) {
        return null;
    }

    public static class NestedRaw {
        public <T extends Number & Comparable<T>, U extends Comparable<U>, V extends Exception> T ccc(
                List<? extends U> arg, NestedRaw self) throws IllegalArgumentException, V {
            return null;
        }
    }

    public static class NestedParam<X> {
        public <T extends Number & Comparable<T>, U extends Comparable<U>, V extends Exception> T ddd(
                List<? super U> arg, X arg2, NestedParam<X> self) throws IllegalArgumentException {
            return null;
        }
    }

    public static class NestedParamBound<X extends Number & Comparable<X>> {
        public <T extends Number & Comparable<T>, U extends Comparable<U>, V extends Exception> T eee(
                List<?> arg, X arg2, NestedParamBound<X> self) throws V {
            return null;
        }
    }

    public class InnerRaw {
        public <T extends Number & Comparable<T>, U extends Comparable<U>, V extends Exception> T fff(
                List<? extends U> arg, W arg2, InnerRaw self) throws IllegalArgumentException, V {
            return null;
        }
    }

    public class InnerParam<X> {
        public <T extends Number & Comparable<T>, U extends Comparable<U>, V extends Exception> T ggg(
                List<? super U> arg, X arg2, W arg3, InnerParam<X> self) throws IllegalArgumentException {
            return null;
        }
    }

    public class InnerParamBound<X extends Number & Comparable<X>> {
        public <T extends Number & Comparable<T>, U extends Comparable<U>, V extends Exception> T hhh(
                List<?> arg, X arg2, W arg3, InnerParamBound<X> self) throws V {
            return null;
        }

        public class DoubleInner<Y extends CharSequence> {
            public <T extends Number & Comparable<T>, U extends Comparable<U>, V extends Exception> T iii(
                    List<?> arg, Y arg2, X arg3, W arg4, DoubleInner<Y> self) throws V {
                return null;
            }
        }
    }
}
