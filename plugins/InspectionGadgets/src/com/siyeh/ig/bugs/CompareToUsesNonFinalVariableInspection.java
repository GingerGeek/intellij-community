package com.siyeh.ig.bugs;

import com.intellij.psi.*;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.ExpressionInspection;
import com.siyeh.ig.GroupNames;
import com.siyeh.ig.psiutils.MethodUtils;
import org.jetbrains.annotations.NotNull;

public class CompareToUsesNonFinalVariableInspection extends ExpressionInspection {

    public String getDisplayName() {
        return "Non-final field referenced in 'compareTo()'";
    }

    public String getGroupDisplayName() {
        return GroupNames.BUGS_GROUP_NAME;
    }


    public String buildErrorString(PsiElement location) {
        return "Non-final field #ref accessed in compareTo() #loc";
    }

    public BaseInspectionVisitor buildVisitor() {
        return new CompareToUsesNonFinalVariableVisitor();
    }

    private static class CompareToUsesNonFinalVariableVisitor extends BaseInspectionVisitor {
        private boolean m_inCompareTo = false;

        public void visitReferenceExpression(@NotNull PsiReferenceExpression expression) {
            super.visitReferenceExpression(expression);
            if (!m_inCompareTo) {
                return;
            }
            final PsiElement element = expression.resolve();
            if (!(element instanceof PsiField)) {
                return;
            }
            final PsiField field = (PsiField) element;
            if (field.hasModifierProperty(PsiModifier.FINAL)) {
                return;
            }
            registerError(expression);
        }

        public void visitMethod(@NotNull PsiMethod method) {
            final boolean isCompareTo = MethodUtils.isCompareTo(method);
            if (isCompareTo) {
                m_inCompareTo = true;
            }

            super.visitMethod(method);
            if (isCompareTo) {
                m_inCompareTo = false;
            }
        }

    }

}
