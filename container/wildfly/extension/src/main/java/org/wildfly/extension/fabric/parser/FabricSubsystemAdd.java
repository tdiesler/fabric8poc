/*
 * #%L
 * Fabric8 :: Container :: WildFly :: Extension
 * %%
 * Copyright (C) 2014 Red Hat
 * %%
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
 * #L%
 */

package org.wildfly.extension.fabric.parser;

import java.util.List;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.dmr.ModelNode;
import org.jboss.gravia.utils.IllegalStateAssertion;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.ServiceController;
import org.wildfly.extension.fabric.service.FabricBootstrapService;
import org.wildfly.extension.gravia.parser.GraviaSubsystemBootstrap;

/**
 * The fabric subsystem add update handler.
 *
 * @since 13-Nov-2013
 */
final class FabricSubsystemAdd extends AbstractBoottimeAddStepHandler {

    public FabricSubsystemAdd(SubsystemState subsystemState) {
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) {
        model.setEmptyObject();
    }

    @Override
    protected void performBoottime(final OperationContext context, final ModelNode operation, final ModelNode model, final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) {

        final FabricSubsystemBootstrap bootstrap = new FabricSubsystemBootstrap();

        // [TODO] Remove workaround for WFLY-3511
        // https://issues.jboss.org/browse/WFLY-3511
        try {
            ModuleClassLoader classLoader = (ModuleClassLoader) getClass().getClassLoader();
            Module graviaModule = classLoader.getModule().getModuleLoader().loadModule(ModuleIdentifier.create("org.jboss.gravia"));
            Module osgiModule = classLoader.getModule().getModuleLoader().loadModule(ModuleIdentifier.create("org.osgi.enterprise"));
            Class<?> interfClass = loadClass(null, osgiModule.getClassLoader(), "org.osgi.service.http.HttpService");
            Class<?> implClass = loadClass(null, graviaModule.getClassLoader(), "org.apache.felix.http.base.internal.service.HttpServiceImpl");
            IllegalStateAssertion.assertTrue(interfClass.isAssignableFrom(implClass), "Cannot assign impl to interf");
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }

        // Register subsystem services
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                newControllers.addAll(bootstrap.getSubsystemServices(context, verificationHandler));
                context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
            }
        }, OperationContext.Stage.RUNTIME);

        // Register deployment unit processors
        context.addStep(new AbstractDeploymentChainStep() {
            @Override
            public void execute(DeploymentProcessorTarget processorTarget) {
                bootstrap.addDeploymentUnitProcessors(processorTarget);
            }
        }, OperationContext.Stage.RUNTIME);
    }

    @Override
    protected boolean requiresRuntimeVerification() {
        return false;
    }

    private Class<?> loadClass(Class<?> loaderClass, ClassLoader classLoader, String className) throws ClassNotFoundException {
        if (classLoader == null) {
            classLoader = loaderClass.getClassLoader();
        }
        Class<?> clazz = classLoader.loadClass(className);
        System.out.println("LOADED: " + clazz + "\n   using " + loaderClass + " from " + classLoader + "\n   loaded from => " + clazz.getClassLoader());
        return clazz;
    }

   static class FabricSubsystemBootstrap extends GraviaSubsystemBootstrap {

        @Override
        protected ServiceController<?> getBoostrapService(OperationContext context, ServiceVerificationHandler verificationHandler) {
            return new FabricBootstrapService().install(context.getServiceTarget(), verificationHandler);
        }
    }
}
