/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.plugins.groovy.runner;

import com.intellij.execution.JavaExecutionUtil;
import com.intellij.execution.Location;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.application.ApplicationConfigurationProducer;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunConfigurationModule;
import com.intellij.execution.junit.RuntimeConfigurationProducer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiMethodUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.extensions.GroovyScriptTypeDetector;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod;

/**
 * @author ilyas
 */
public class GroovyScriptRunConfigurationProducer extends RuntimeConfigurationProducer implements Cloneable {
  protected PsiElement mySourceElement;

  public GroovyScriptRunConfigurationProducer() {
    super(GroovyScriptRunConfigurationType.getInstance());
  }

  public static boolean canBeRunByGroovy(final PsiClass psiClass) {
    if (isRunnable(psiClass)) {
      return true;
    }

    if (PsiMethodUtil.hasMainMethod(psiClass) && psiClass instanceof GrTypeDefinition) {
      return true;
    }
    
    return false;
  }

  private static boolean isRunnable(final PsiClass psiClass) {
    if (!(psiClass instanceof GrTypeDefinition)) return false;
    if (psiClass instanceof PsiAnonymousClass) return false;
    if (psiClass.isInterface()) return false;
    final PsiClass runnable = JavaPsiFacade.getInstance(psiClass.getProject()).findClass("java.lang.Runnable", psiClass.getResolveScope());
    if (runnable == null) return false;
    final PsiMethod runMethod = runnable.getMethods()[0];
    final PsiMethod[] runImplementations = psiClass.findMethodsBySignature(runMethod, false);
    if (runImplementations.length == 1 &&
        runImplementations[0] instanceof GrMethod &&
        ((GrMethod)runImplementations[0]).getBlock() != null) {
      return psiClass.getContainingClass() == null || psiClass.hasModifierProperty(PsiModifier.STATIC);
    }
    return false;
  }

  public PsiElement getSourceElement() {
    return mySourceElement;
  }

  protected RunnerAndConfigurationSettings createConfigurationByElement(final Location location, final ConfigurationContext context) {
    final PsiElement element = location.getPsiElement();
    final PsiFile file = element.getContainingFile();
    if (!(file instanceof GroovyFile)) {
      return null;
    }

    GroovyFile groovyFile = (GroovyFile)file;
    if (groovyFile.isScript()) {
      mySourceElement = element;
      final PsiClass scriptClass = getScriptClass(location.getPsiElement());
      if (scriptClass == null) return null;
      final RunnerAndConfigurationSettings settings = createConfiguration(scriptClass);
      if (settings != null) {
        final GroovyScriptRunConfiguration configuration = (GroovyScriptRunConfiguration)settings.getConfiguration();
        GroovyScriptTypeDetector.getScriptType(groovyFile).tuneConfiguration(groovyFile, configuration, location);
        return settings;
      }
    }
    
    if (!file.getText().contains("@Grab")) return null;

    ApplicationConfigurationProducer producer = new ApplicationConfigurationProducer();
    RunnerAndConfigurationSettings settings = producer.createConfigurationByElement(location, context);
    if (settings != null) {
      PsiElement src = producer.getSourceElement();
      mySourceElement = src;
      return createConfiguration(src instanceof PsiMethod ? ((PsiMethod) src).getContainingClass() : (PsiClass)src);
    }

    return null;
  }

  @Override
  protected RunnerAndConfigurationSettings findExistingByElement(Location location,
                                                                 @NotNull RunnerAndConfigurationSettings[] existingConfigurations,
                                                                 ConfigurationContext context) {
    for (RunnerAndConfigurationSettings existingConfiguration : existingConfigurations) {
      final RunConfiguration configuration = existingConfiguration.getConfiguration();
      final GroovyScriptRunConfiguration existing = (GroovyScriptRunConfiguration)configuration;
      final String path = existing.getScriptPath();
      if (path != null) {
        final PsiFile file = location.getPsiElement().getContainingFile();
        if (file instanceof GroovyFile) {
          final VirtualFile vfile = file.getVirtualFile();
          if (vfile != null && FileUtil.toSystemIndependentName(path).equals(vfile.getPath())) {
            if (!((GroovyFile)file).isScript() ||
                GroovyScriptTypeDetector.getScriptType((GroovyFile)file).isConfigurationByLocation(existing, location)) {
              return existingConfiguration;
            }
          }
        }
      }
    }
    return null;
  }


  public int compareTo(final Object o) {
    return PREFERED;
  }

  @Nullable
  private RunnerAndConfigurationSettings createConfiguration(final PsiClass aClass) {
    final Project project = aClass.getProject();
    RunnerAndConfigurationSettings settings = RunManagerEx.getInstanceEx(project).createConfiguration("", getConfigurationFactory());
    final GroovyScriptRunConfiguration configuration = (GroovyScriptRunConfiguration) settings.getConfiguration();
    final PsiFile file = aClass.getContainingFile().getOriginalFile();
    final PsiDirectory dir = file.getContainingDirectory();
    if (dir == null) return null;
    configuration.setWorkDir(dir.getVirtualFile().getPath());
    final VirtualFile vFile = file.getVirtualFile();
    if (vFile == null) return null;
    configuration.setScriptPath(vFile.getPath());
    RunConfigurationModule module = configuration.getConfigurationModule();


    String name = getConfigurationName(aClass, module);
    configuration.setName(name);
    configuration.setModule(JavaExecutionUtil.findModule(aClass));
    return settings;
  }

  private static String getConfigurationName(PsiClass aClass, RunConfigurationModule module) {
    String qualifiedName = aClass.getQualifiedName();
    Project project = module.getProject();
    if (qualifiedName != null) {
      PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(qualifiedName.replace('$', '.'), GlobalSearchScope.projectScope(project));
      if (psiClass != null) {
        return psiClass.getName();
      } else {
        int lastDot = qualifiedName.lastIndexOf('.');
        if (lastDot == -1 || lastDot == qualifiedName.length() - 1) {
          return qualifiedName;
        }
        return qualifiedName.substring(lastDot + 1, qualifiedName.length());
      }
    }
    return module.getModuleName();
  }

  @Nullable
   private static PsiClass getScriptClass(PsiElement element) {
     final PsiFile file = element.getContainingFile();
     if (!(file instanceof GroovyFile)) return null;
     return ((GroovyFile) file).getScriptClass();
   }

}
