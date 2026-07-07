package io.quarkus.gradle.application.dsl;

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.ExtensiblePolymorphicDomainObjectContainer;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.model.ObjectFactory;

/**
 * Registers deployment operations associated with one named application build.
 * <p>
 * Each method lazily registers a uniquely named deployment and its derived deploy task. Declaring a deployment does not
 * execute it. By default, selecting the deploy task also selects the named build's normal image-push task.
 */
public class QuarkusApplicationDeployments {

    private final ExtensiblePolymorphicDomainObjectContainer<QuarkusApplicationDeployment> container;

    /**
     * Creates an empty polymorphic deployment container.
     *
     * @param objects Gradle's object factory
     */
    @Inject
    public QuarkusApplicationDeployments(ObjectFactory objects) {
        this.container = objects.polymorphicDomainObjectContainer(QuarkusApplicationDeployment.class);
        registerFactories(objects);
    }

    private void registerFactories(ObjectFactory objects) {
        container.registerFactory(QuarkusKubernetesDeployment.class,
                name -> objects.newInstance(QuarkusKubernetesDeployment.class, name));
        container.registerFactory(QuarkusOpenShiftDeployment.class,
                name -> objects.newInstance(QuarkusOpenShiftDeployment.class, name));
        container.registerFactory(QuarkusKnativeDeployment.class,
                name -> objects.newInstance(QuarkusKnativeDeployment.class, name));
        container.registerFactory(QuarkusKindDeployment.class,
                name -> objects.newInstance(QuarkusKindDeployment.class, name));
        container.registerFactory(QuarkusMinikubeDeployment.class,
                name -> objects.newInstance(QuarkusMinikubeDeployment.class, name));
    }

    /**
     * Lazily registers a Kubernetes deployment.
     *
     * @param name the deployment name used in its generated task name
     * @return a provider for the deployment
     */
    public NamedDomainObjectProvider<QuarkusKubernetesDeployment> kubernetes(String name) {
        return container.register(name, QuarkusKubernetesDeployment.class);
    }

    /**
     * Lazily registers and configures a Kubernetes deployment.
     *
     * @param name the deployment name used in its generated task name
     * @param action the deployment configuration action
     * @return a provider for the deployment
     */
    public NamedDomainObjectProvider<QuarkusKubernetesDeployment> kubernetes(String name,
            Action<? super QuarkusKubernetesDeployment> action) {
        return container.register(name, QuarkusKubernetesDeployment.class, action);
    }

    /**
     * Lazily registers an OpenShift deployment.
     *
     * @param name the deployment name used in its generated task name
     * @return a provider for the deployment
     */
    public NamedDomainObjectProvider<QuarkusOpenShiftDeployment> openshift(String name) {
        return container.register(name, QuarkusOpenShiftDeployment.class);
    }

    /**
     * Lazily registers and configures an OpenShift deployment.
     *
     * @param name the deployment name used in its generated task name
     * @param action the deployment configuration action
     * @return a provider for the deployment
     */
    public NamedDomainObjectProvider<QuarkusOpenShiftDeployment> openshift(String name,
            Action<? super QuarkusOpenShiftDeployment> action) {
        return container.register(name, QuarkusOpenShiftDeployment.class, action);
    }

    /**
     * Lazily registers a Knative deployment.
     *
     * @param name the deployment name used in its generated task name
     * @return a provider for the deployment
     */
    public NamedDomainObjectProvider<QuarkusKnativeDeployment> knative(String name) {
        return container.register(name, QuarkusKnativeDeployment.class);
    }

    /**
     * Lazily registers and configures a Knative deployment.
     *
     * @param name the deployment name used in its generated task name
     * @param action the deployment configuration action
     * @return a provider for the deployment
     */
    public NamedDomainObjectProvider<QuarkusKnativeDeployment> knative(String name,
            Action<? super QuarkusKnativeDeployment> action) {
        return container.register(name, QuarkusKnativeDeployment.class, action);
    }

    /**
     * Lazily registers a Kind deployment.
     *
     * @param name the deployment name used in its generated task name
     * @return a provider for the deployment
     */
    public NamedDomainObjectProvider<QuarkusKindDeployment> kind(String name) {
        return container.register(name, QuarkusKindDeployment.class);
    }

    /**
     * Lazily registers and configures a Kind deployment.
     *
     * @param name the deployment name used in its generated task name
     * @param action the deployment configuration action
     * @return a provider for the deployment
     */
    public NamedDomainObjectProvider<QuarkusKindDeployment> kind(String name,
            Action<? super QuarkusKindDeployment> action) {
        return container.register(name, QuarkusKindDeployment.class, action);
    }

    /**
     * Lazily registers a Minikube deployment.
     *
     * @param name the deployment name used in its generated task name
     * @return a provider for the deployment
     */
    public NamedDomainObjectProvider<QuarkusMinikubeDeployment> minikube(String name) {
        return container.register(name, QuarkusMinikubeDeployment.class);
    }

    /**
     * Lazily registers and configures a Minikube deployment.
     *
     * @param name the deployment name used in its generated task name
     * @param action the deployment configuration action
     * @return a provider for the deployment
     */
    public NamedDomainObjectProvider<QuarkusMinikubeDeployment> minikube(String name,
            Action<? super QuarkusMinikubeDeployment> action) {
        return container.register(name, QuarkusMinikubeDeployment.class, action);
    }

    /**
     * Configures every present and future deployment for this build.
     *
     * @param action the deployment configuration action
     */
    public void all(Action<? super QuarkusApplicationDeployment> action) {
        container.all(action);
    }
}
