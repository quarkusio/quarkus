package io.quarkus.cxf.runtime.graal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.common.util.ReflectionInvokationHandler;
import org.apache.cxf.common.util.ReflectionUtil;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.databinding.WrapperHelper;
import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.quarkus.cxf.runtime.CXFException;

@TargetClass(className = "org.apache.cxf.wsdl.JAXBExtensionHelper")
final class Target_org_apache_cxf_wsdl_JAXBExtensionHelper {
    @Alias
    private static Logger LOG = null;

    @Substitute()
    private static Class<?> createExtensionClass(Class<?> cls, QName qname, ClassLoader loader) {
        try {

            Class<?> clz = Class.forName("io.quarkus.cxf.runtime." + cls.getSimpleName() + "Extensibility");
            LOG.info("extensibility class substitute: " + cls.getName());
            return clz;
        } catch (ClassNotFoundException e) {
            LOG.warning("extensibility class to create: " + cls.getName());
            throw new UnsupportedOperationException(
                    cls.getName() + " extensibility not implemented yet for GraalVM native images", e);
            // TODO CORBA support : org.apache.cxf.wsdl.http.OperationType and org.apache.cxf.wsdl.http.BindingType
        }
    }
}

@TargetClass(className = "org.apache.cxf.jaxb.JAXBContextInitializer")
final class Target_org_aapche_cxf_jaxb_JAXBContextInitializer {
    @Alias
    private static Logger LOG = null;

    @Substitute()
    private Object createFactory(Class<?> cls, Constructor<?> contructor) {
        try {
            Class<?> factoryClass = Class.forName("io.quarkus.cxf.runtime." + cls.getSimpleName() + "Factory");
            LOG.info("load factory class for : " + cls.getSimpleName());
            try {
                return factoryClass.getConstructor().newInstance();
            } catch (Exception e) {
                LOG.warning("factory class not created for " + cls.getSimpleName());
            }
            return null;
        } catch (ClassNotFoundException e) {
            LOG.warning("factory class to create : " + cls.getSimpleName());
            throw new UnsupportedOperationException(cls.getName() + " factory not implemented yet for GraalVM native images",
                    e);
        }
    }
}

@TargetClass(className = "org.apache.cxf.jaxb.JAXBDataBinding")
final class Target_org_apache_cxf_jaxb_JAXBDataBinding {
    @Alias
    private static Logger LOG = null;

    @Substitute()
    private static WrapperHelper compileWrapperHelper(Class<?> wrapperType, Method[] setMethods,
            Method[] getMethods, Method[] jaxbMethods,
            Field[] fields, Object objectFactory) {
        LOG.info("compileWrapperHelper substitution");
        int count = 1;
        String newClassName = wrapperType.getName() + "_WrapperTypeHelper" + count;
        //todo handle signature
        Class<?> cls = null;
        try {
            cls = Class.forName(newClassName);
        } catch (ClassNotFoundException e) {
            LOG.warning("Wrapper helper class not found : " + e.toString());
        }

        WrapperHelper helper = null;
        try {
            helper = WrapperHelper.class.cast(cls.getConstructor().newInstance());
            return helper;
        } catch (Exception e) {
            LOG.warning("Wrapper helper class not created : " + e.toString());
        }
        throw new UnsupportedOperationException(cls.getName() + " wrapperHelper not implemented yet for GraalVM native images");
    }

}

@TargetClass(className = "org.apache.cxf.jaxws.WrapperClassGenerator")
final class Target_org_apache_cxf_jaxws_WrapperClassGenerator {
    @Alias
    private static Logger LOG = null;

    @Alias
    private InterfaceInfo interfaceInfo;

    @Alias
    private Set<Class<?>> wrapperBeans = null;

    @Alias
    private JaxWsServiceFactoryBean factory;

    @Alias
    private String getPackageName(Method method) {
        return null;
    }

    @Substitute()
    private void createWrapperClass(MessagePartInfo wrapperPart,
            MessageInfo messageInfo,
            OperationInfo op,
            Method method,
            boolean isRequest) {
        LOG.info("wrapper class substitution : " + op.getName());
        QName wrapperElement = messageInfo.getName();
        //TODO handle it when config for anonymous is handle
        //boolean anonymous = factory.getAnonymousWrapperTypes();
        boolean anonymous = false;

        String pkg = getPackageName(method) + ".jaxws_asm" + (anonymous ? "_an" : "");
        String className = pkg + "."
                + StringUtils.capitalize(op.getName().getLocalPart());
        if (!isRequest) {
            className = className + "Response";
        }
        // generation is done on quarkus side now
        Class<?> clz = null;
        try {
            clz = Class.forName(className);
            wrapperPart.setTypeClass(clz);
            wrapperBeans.add(clz);
        } catch (Exception e) {
            LOG.warning("wrapper class substitution failed : " + e.toString());
        }
    }
}

@TargetClass(className = "org.apache.cxf.endpoint.dynamic.TypeClassInitializer$ExceptionCreator")
final class Target_org_apache_cxf_endpoint_dynamic_TypeClassInitializer$ExceptionCreator {

    @Substitute
    public Class<?> createExceptionClass(Class<?> bean) throws ClassNotFoundException {
        //TODO not sure if I use CXFException or generated one. I have both system in place. but I use CXFEx currently.
        String newClassName = CXFException.class.getSimpleName();

        try {
            Class<?> clz = Class.forName("io.quarkus.cxf.runtime." + newClassName);
            return clz;
        } catch (ClassNotFoundException e) {
            try {
                Class<?> clz = Class.forName("io.quarkus.cxf.runtime.CXFException");
                return clz;
            } catch (ClassNotFoundException ex) {
                throw new UnsupportedOperationException(
                        newClassName + " exception not implemented yet for GraalVM native images", ex);
            }
        }
    }
}

@TargetClass(className = "org.apache.cxf.common.util.ReflectionInvokationHandler")
final class Target_org_apache_cxf_common_util_ReflectionInvokationHandler {
    @Alias
    private Object target;

    @Alias
    private Class<?>[] getParameterTypes(Method method, Object[] args) {
        return null;
    }

    @Substitute
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //add this to handle args null bug
        if (args == null)
            args = new Object[0];
        ReflectionInvokationHandler.WrapReturn wr = (ReflectionInvokationHandler.WrapReturn) method
                .getAnnotation(ReflectionInvokationHandler.WrapReturn.class);
        Class<?> targetClass = this.target.getClass();
        Class[] parameterTypes = this.getParameterTypes(method, args);

        int i;
        int x;
        try {
            Method m;
            try {
                m = targetClass.getMethod(method.getName(), parameterTypes);
            } catch (NoSuchMethodException var20) {
                boolean[] optionals = new boolean[method.getParameterTypes().length];
                i = 0;
                int optionalNumber = 0;
                Annotation[][] var25 = method.getParameterAnnotations();
                x = var25.length;

                int argI;
                for (argI = 0; argI < x; ++argI) {
                    Annotation[] a = var25[argI];
                    optionals[i] = false;
                    Annotation[] var16 = a;
                    int var17 = a.length;

                    for (int var18 = 0; var18 < var17; ++var18) {
                        Annotation potential = var16[var18];
                        if (ReflectionInvokationHandler.Optional.class.equals(potential.annotationType())) {
                            optionals[i] = true;
                            ++optionalNumber;
                            break;
                        }
                    }

                    ++i;
                }

                Class<?>[] newParams = new Class[args.length - optionalNumber];
                Object[] newArgs = new Object[args.length - optionalNumber];
                argI = 0;

                for (int j = 0; j < parameterTypes.length; ++j) {
                    if (!optionals[j]) {
                        newArgs[argI] = args[j];
                        newParams[argI] = parameterTypes[j];
                        ++argI;
                    }
                }

                m = targetClass.getMethod(method.getName(), newParams);
                args = newArgs;
            }

            ReflectionUtil.setAccessible(m);
            return wrapReturn(wr, m.invoke(this.target, args));
        } catch (InvocationTargetException var21) {
            throw var21.getCause();
        } catch (NoSuchMethodException var22) {
            Method[] var8 = targetClass.getMethods();
            int var9 = var8.length;

            for (i = 0; i < var9; ++i) {
                Method m2 = var8[i];
                if (m2.getName().equals(method.getName())
                        && m2.getParameterTypes().length == method.getParameterTypes().length) {
                    boolean found = true;

                    for (x = 0; x < m2.getParameterTypes().length; ++x) {
                        if (args[x] != null && !m2.getParameterTypes()[x].isInstance(args[x])) {
                            found = false;
                        }
                    }

                    if (found) {
                        ReflectionUtil.setAccessible(m2);
                        return wrapReturn(wr, m2.invoke(this.target, args));
                    }
                }
            }

            throw var22;
        }
    }

    @Alias
    private static Object wrapReturn(ReflectionInvokationHandler.WrapReturn wr, Object t) {
        return null;
    }
}

public class CXFSubstitutions {
}
