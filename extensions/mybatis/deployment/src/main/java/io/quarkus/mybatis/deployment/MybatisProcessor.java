package io.quarkus.mybatis.deployment;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.cache.decorators.LruCache;
import org.apache.ibatis.cache.impl.PerpetualCache;
import org.apache.ibatis.javassist.util.proxy.ProxyFactory;
import org.apache.ibatis.logging.log4j.Log4jImpl;
import org.apache.ibatis.scripting.defaults.RawLanguageDriver;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;

import io.quarkus.agroal.deployment.JdbcDataSourceBuildItem;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.configuration.ConfigurationError;
import io.quarkus.mybatis.runtime.MyBatisProducers;
import io.quarkus.mybatis.runtime.MyBatisRecorder;
import io.quarkus.mybatis.runtime.MyBatisRuntimeConfig;

class MybatisProcessor {

    private static final Logger LOG = Logger.getLogger(MybatisProcessor.class);
    private static final String FEATURE = "mybatis";
    private static final DotName MYBATIS_MAPPER = DotName.createSimple(Mapper.class.getName());

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void runtimeInitialzed(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInit) {
        runtimeInit.produce(new RuntimeInitializedClassBuildItem(Log4jImpl.class.getName()));
    }

    @BuildStep
    void reflectiveClasses(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false,
                ProxyFactory.class,
                XMLLanguageDriver.class,
                RawLanguageDriver.class));

        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true,
                PerpetualCache.class, LruCache.class));
    }

    @BuildStep
    void addMyBatisMappers(BuildProducer<MyBatisMapperBuildItem> mappers,
            BuildProducer<ReflectiveClassBuildItem> reflective,
            BuildProducer<NativeImageProxyDefinitionBuildItem> proxy,
            CombinedIndexBuildItem indexBuildItem) {
        for (AnnotationInstance i : indexBuildItem.getIndex().getAnnotations(MYBATIS_MAPPER)) {
            if (i.target().kind() == AnnotationTarget.Kind.CLASS) {
                DotName dotName = i.target().asClass().name();
                mappers.produce(new MyBatisMapperBuildItem(dotName));
                reflective.produce(new ReflectiveClassBuildItem(true, false, dotName.toString()));
                proxy.produce(new NativeImageProxyDefinitionBuildItem(dotName.toString()));
            }
        }
    }

    @BuildStep
    void unremovableBeans(BuildProducer<AdditionalBeanBuildItem> beanProducer) {
        beanProducer.produce(AdditionalBeanBuildItem.unremovableOf(MyBatisProducers.class));
    }

    @BuildStep
    void initalSql(BuildProducer<NativeImageResourceBuildItem> resource, MyBatisRuntimeConfig config) {
        if (config.initialSql.isPresent()) {
            resource.produce(new NativeImageResourceBuildItem(config.initialSql.get()));
        }
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    SqlSessionFactoryBuildItem generateSqlSessionFactory(MyBatisRuntimeConfig myBatisRuntimeConfig,
            List<MyBatisMapperBuildItem> myBatisMapperBuildItems,
            List<JdbcDataSourceBuildItem> jdbcDataSourcesBuildItem,
            MyBatisRecorder recorder) {
        List<String> mappers = myBatisMapperBuildItems
                .stream().map(m -> m.getMapperName().toString()).collect(Collectors.toList());

        String dataSourceName;
        if (myBatisRuntimeConfig.dataSource.isPresent()) {
            dataSourceName = myBatisRuntimeConfig.dataSource.get();
            Optional<JdbcDataSourceBuildItem> jdbcDataSourceBuildItem = jdbcDataSourcesBuildItem.stream()
                    .filter(i -> i.getName().equals(dataSourceName))
                    .findFirst();
            if (!jdbcDataSourceBuildItem.isPresent()) {
                throw new ConfigurationError("Can not find datasource " + dataSourceName);
            }
        } else {
            Optional<JdbcDataSourceBuildItem> defaultJdbcDataSourceBuildItem = jdbcDataSourcesBuildItem.stream()
                    .filter(i -> i.isDefault())
                    .findFirst();
            if (defaultJdbcDataSourceBuildItem.isPresent()) {
                dataSourceName = defaultJdbcDataSourceBuildItem.get().getName();
            } else {
                throw new ConfigurationError("No default datasource");
            }
        }

        return new SqlSessionFactoryBuildItem(recorder.createSqlSessionFactory(
                myBatisRuntimeConfig.environment,
                myBatisRuntimeConfig.transactionFactory,
                dataSourceName,
                mappers));
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    SqlSessionManagerBuildItem generateSqlSessionManager(SqlSessionFactoryBuildItem sqlSessionFactoryBuildItem,
            MyBatisRecorder recorder) {
        return new SqlSessionManagerBuildItem(recorder.createSqlSessionManager(
                sqlSessionFactoryBuildItem.getSqlSessionFactory()));
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void generateMapperBeans(MyBatisRecorder recorder,
            List<MyBatisMapperBuildItem> myBatisMapperBuildItems,
            SqlSessionManagerBuildItem sqlSessionManagerBuildItem,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer) {

        for (MyBatisMapperBuildItem i : myBatisMapperBuildItems) {
            SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                    .configure(i.getMapperName())
                    .scope(Singleton.class)
                    .setRuntimeInit()
                    .unremovable()
                    .supplier(recorder.MyBatisMapperSupplier(i.getMapperName().toString(),
                            sqlSessionManagerBuildItem.getSqlSessionManager()));
            syntheticBeanBuildItemBuildProducer.produce(configurator.done());
        }
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void register(SqlSessionFactoryBuildItem sqlSessionFactoryBuildItem,
            BeanContainerBuildItem beanContainerBuildItem,
            MyBatisRecorder recorder) {
        recorder.register(sqlSessionFactoryBuildItem.getSqlSessionFactory(), beanContainerBuildItem.getValue());
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void runInitialSql(SqlSessionFactoryBuildItem sqlSessionFactoryBuildItem,
            MyBatisRuntimeConfig myBatisRuntimeConfig,
            MyBatisRecorder recorder) {
        if (myBatisRuntimeConfig.initialSql.isPresent()) {
            recorder.runInitialSql(sqlSessionFactoryBuildItem.getSqlSessionFactory(),
                    myBatisRuntimeConfig.initialSql.get());
        }
    }
}
