package com.siyeh.ig.performance;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.search.PsiSearchHelper;
import com.intellij.util.IncorrectOperationException;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.GroupNames;
import com.siyeh.ig.InspectionGadgetsFix;
import com.siyeh.ig.MethodInspection;
import com.siyeh.ig.psiutils.ClassUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

public class MethodMayBeStaticInspection extends MethodInspection {
    /** @noinspection PublicField*/
    public boolean m_onlyPrivateOrFinal = false;
    /** @noinspection PublicField*/
    public boolean m_ignoreEmptyMethods = true;
    private final MethodMayBeStaticFix fix = new MethodMayBeStaticFix();

    public String getDisplayName() {
        return "Method may be 'static'";
    }

    public String getGroupDisplayName() {
        return GroupNames.PERFORMANCE_GROUP_NAME;
    }

    protected String buildErrorString(PsiElement location) {
        return "Method #ref may be 'static' #loc";
    }

    protected InspectionGadgetsFix buildFix(PsiElement location) {
        return fix;
    }

    private static class MethodMayBeStaticFix extends InspectionGadgetsFix {
        public String getName() {
            return "Make static";
        }

        public void doFix(Project project, ProblemDescriptor descriptor)
                                                                         throws IncorrectOperationException{
            final PsiJavaToken classNameToken = (PsiJavaToken) descriptor.getPsiElement();
                final PsiMethod innerClass = (PsiMethod) classNameToken.getParent();
                final PsiModifierList modifiers = innerClass.getModifierList();
                modifiers.setModifierProperty(PsiModifier.STATIC, true);
        }
    }

    public JComponent createOptionsPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final JCheckBox ignoreFieldAccessesCheckBox = new JCheckBox("Only check private or final methods",
                m_onlyPrivateOrFinal);
        final ButtonModel ignoreFieldAccessesModel = ignoreFieldAccessesCheckBox.getModel();
        ignoreFieldAccessesModel.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                m_onlyPrivateOrFinal = ignoreFieldAccessesModel.isSelected();
            }
        });
        final JCheckBox ignoreEmptyMethodsCheckBox = new JCheckBox("Ignore empty methods",
                m_ignoreEmptyMethods);
        final ButtonModel ignoreEmptyMethodsModel = ignoreEmptyMethodsCheckBox.getModel();
        ignoreEmptyMethodsModel.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                m_ignoreEmptyMethods = ignoreEmptyMethodsModel.isSelected();
            }
        });
        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1.0;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(ignoreFieldAccessesCheckBox, constraints);
        constraints.gridy = 1;
        panel.add(ignoreEmptyMethodsCheckBox, constraints);
        return panel;
    }

    public BaseInspectionVisitor buildVisitor() {
        return new MethodCanBeStaticVisitor();
    }

    private class MethodCanBeStaticVisitor extends BaseInspectionVisitor {

        public void visitMethod(@NotNull PsiMethod method) {
            super.visitMethod(method);
            if (method.hasModifierProperty(PsiModifier.STATIC)) {
                return;
            }
            if (method.isConstructor()) {
                return;
            }
            if (method.hasModifierProperty(PsiModifier.ABSTRACT)) {
                return;
            }
            if (m_ignoreEmptyMethods) {
                final PsiCodeBlock methodBody = method.getBody();
                if (methodBody == null) {
                    return;
                }
                final PsiStatement[] methodStatements = methodBody.getStatements();
                if (methodStatements.length == 0) {
                    return;
                }
            }
            final PsiClass containingClass = ClassUtils.getContainingClass(method);
            if(containingClass == null)
            {
                return;
            }
            final PsiElement scope = containingClass.getScope();
            if (!(scope instanceof PsiJavaFile) &&
                    !containingClass.hasModifierProperty(PsiModifier.STATIC)) {
                return;
            }
            if (m_onlyPrivateOrFinal &&
                    !method.hasModifierProperty(PsiModifier.FINAL) &&
                    !method.hasModifierProperty(PsiModifier.PRIVATE)) {
                return;
            }
            final String methodName = method.getName();
            if(methodName!=null && methodName.startsWith("test") &&
                    ClassUtils.isSubclass(containingClass,
                                          "junit.framework.TestCase")){
                return;
            }
            final PsiMethod[] superMethods = method.findSuperMethods();
            if (superMethods.length > 0) {
                return;
            }
            // ignore overridden methods
            final PsiElementProcessor.FindElement<PsiMethod> processor =
                    new PsiElementProcessor.FindElement<PsiMethod>();
            final PsiManager manager = method.getManager();
            final PsiSearchHelper helper = manager.getSearchHelper();
            helper.processOverridingMethods(processor, method,
                                            GlobalSearchScope.projectScope(manager.getProject()),
                                            true);
            if(processor.isFound()) return;

            final MethodReferenceVisitor visitor = new MethodReferenceVisitor(method);
            method.accept(visitor);
            if (!visitor.areReferencesStaticallyAccessible()) {
                return;
            }

            registerMethodError(method);
        }
    }

}