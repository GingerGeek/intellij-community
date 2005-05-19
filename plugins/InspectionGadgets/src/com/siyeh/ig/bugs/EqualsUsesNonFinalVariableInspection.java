package com.siyeh.ig.bugs;

import com.intellij.psi.*;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.ExpressionInspection;
import com.siyeh.ig.GroupNames;
import com.siyeh.ig.psiutils.MethodUtils;
import org.jetbrains.annotations.NotNull;

public class EqualsUsesNonFinalVariableInspection extends ExpressionInspection{
    public String getID(){
        return "NonFinalFieldReferenceInEquals";
    }

    public String getDisplayName(){
        return "Non-final field referenced in 'equals()'";
    }

    public String getGroupDisplayName(){
        return GroupNames.BUGS_GROUP_NAME;
    }

    public String buildErrorString(PsiElement location){
        return "Non-final field #ref accessed in equals()  #loc";
    }

    public BaseInspectionVisitor buildVisitor(){
        return new EqualsUsesNonFinalVariableVisitor();
    }

    private static class EqualsUsesNonFinalVariableVisitor
            extends BaseInspectionVisitor{
        private boolean m_inEquals = false;

        public void visitReferenceExpression(@NotNull PsiReferenceExpression expression){
            super.visitReferenceExpression(expression);
            if(!m_inEquals){
                return;
            }
            final PsiElement element = expression.resolve();
            if(!(element instanceof PsiField)){
                return;
            }
            final PsiField field = (PsiField) element;
            if(field.hasModifierProperty(PsiModifier.FINAL)){
                return;
            }
            registerError(expression);
        }

        public void visitMethod(@NotNull PsiMethod method){
            final boolean isEquals = MethodUtils.isEquals(method);
            if(isEquals){
                m_inEquals = true;
            }
            super.visitMethod(method);
            if(isEquals){
                m_inEquals = false;
            }
        }

    }
}
