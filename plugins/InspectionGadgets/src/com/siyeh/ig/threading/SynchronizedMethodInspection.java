package com.siyeh.ig.threading;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.util.IncorrectOperationException;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.GroupNames;
import com.siyeh.ig.InspectionGadgetsFix;
import com.siyeh.ig.MethodInspection;
import com.siyeh.ig.ui.SingleCheckboxOptionsPanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class SynchronizedMethodInspection extends MethodInspection{
    /**
     * @noinspection PublicField
     */
    public boolean m_includeNativeMethods = true;
    private final SynchronizedMethodFix fix = new SynchronizedMethodFix();

    public String getDisplayName(){
        return "'synchronized' method";
    }

    public String getGroupDisplayName(){
        return GroupNames.THREADING_GROUP_NAME;
    }

    public String buildErrorString(PsiElement location){
        final PsiModifierList modifierList = (PsiModifierList) location.getParent();
        final PsiMethod method = (PsiMethod) modifierList.getParent();
        return "Method " + method.getName() + "() declared '#ref' #loc";
    }

    protected InspectionGadgetsFix buildFix(PsiElement location){
        final PsiMethod method = (PsiMethod) location.getParent().getParent();
        if(method.getBody() == null){
            return null;
        }

        return fix;
    }

    public BaseInspectionVisitor buildVisitor(){
        return new SynchronizedMethodVisitor();
    }

    public JComponent createOptionsPanel(){
        return new SingleCheckboxOptionsPanel("Include native methods",
                                              this, "m_includeNativeMethods");
    }

    private static class SynchronizedMethodFix extends InspectionGadgetsFix{
        public String getName(){
            return "Move synchronization into method";
        }

        public void doFix(Project project,
                          ProblemDescriptor descriptor)
                throws IncorrectOperationException{
            final PsiElement nameElement =
                    descriptor.getPsiElement();
            final PsiModifierList modiferList = (PsiModifierList) nameElement.getParent();
            final PsiMethod method =
                    (PsiMethod) modiferList.getParent();
            modiferList.setModifierProperty(PsiModifier.SYNCHRONIZED, false);
            final PsiCodeBlock body = method.getBody();
            final String text = body.getText();
            final String replacementText;
            if(method.hasModifierProperty(PsiModifier.STATIC)){
                final PsiClass containingClass = method.getContainingClass();
                final String className = containingClass.getName();
                replacementText = "{ synchronized(" + className + ".class){" +
                        text.substring(1) + '}';
            } else{
                replacementText = "{ synchronized(this){" + text.substring(1) +
                        '}';
            }
            final PsiManager psiManager = PsiManager.getInstance(project);
            final PsiElementFactory elementFactory =
                    psiManager.getElementFactory();
            final PsiCodeBlock block =
                    elementFactory.createCodeBlockFromText(replacementText,
                                                           null);
            body.replace(block);
            psiManager.getCodeStyleManager().reformat(method);
        }
    }

    private class SynchronizedMethodVisitor extends BaseInspectionVisitor{
        public void visitMethod(@NotNull PsiMethod method){
            //no call to super, so we don't drill into anonymous classes
            if(!method.hasModifierProperty(PsiModifier.SYNCHRONIZED)){
                return;
            }
            if(!m_includeNativeMethods &&
                    method.hasModifierProperty(PsiModifier.NATIVE)){
                return;
            }
            registerModifierError(PsiModifier.SYNCHRONIZED, method);
        }
    }
}
