package com.siyeh.ig.methodmetrics;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.siyeh.ig.psiutils.ClassUtils;
import com.siyeh.ig.psiutils.LibraryUtil;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

class CouplingVisitor extends PsiRecursiveElementVisitor {
    private boolean m_inClass = false;
    private final PsiMethod m_method;
    private final boolean m_includeJavaClasses;
    private final boolean m_includeLibraryClasses;
    private final Set<String> m_dependencies = new HashSet<String>(10);

    CouplingVisitor(PsiMethod method, boolean includeJavaClasses,
                    boolean includeLibraryClasses) {
        super();
        m_method = method;
        m_includeJavaClasses = includeJavaClasses;
        m_includeLibraryClasses = includeLibraryClasses;
    }

    public void visitVariable(@NotNull PsiVariable variable) {
        super.visitVariable(variable);
        final PsiType type = variable.getType();
        addDependency(type);
    }

    public void visitMethod(@NotNull PsiMethod method) {
        super.visitMethod(method);
        final PsiType returnType = method.getReturnType();
        addDependency(returnType);
        addDependenciesForThrowsList(method);
    }

    private void addDependenciesForThrowsList(PsiMethod method) {
        final PsiReferenceList throwsList = method.getThrowsList();
        if (throwsList == null) {
            return;
        }
        final PsiClassType[] throwsTypes = throwsList.getReferencedTypes();
        for(PsiClassType throwsType : throwsTypes){
            addDependency(throwsType);
        }
    }

    public void visitNewExpression(@NotNull PsiNewExpression exp) {
        super.visitNewExpression(exp);
        final PsiType classType = exp.getType();
        addDependency(classType);
    }

    public void visitClassObjectAccessExpression(PsiClassObjectAccessExpression exp) {
        super.visitClassObjectAccessExpression(exp);
        final PsiTypeElement operand = exp.getOperand();
        addDependency(operand);
    }

    public void visitClass(@NotNull PsiClass aClass) {
        final boolean wasInClass = m_inClass;
        if (!m_inClass) {

            m_inClass = true;
            super.visitClass(aClass);
        }
        m_inClass = wasInClass;
        final PsiType[] superTypes = aClass.getSuperTypes();
        for(PsiType superType : superTypes){
            addDependency(superType);
        }
    }

    public void visitTryStatement(@NotNull PsiTryStatement statement) {
        super.visitTryStatement(statement);
        final PsiParameter[] catchBlockParameters = statement.getCatchBlockParameters();
        for(PsiParameter catchBlockParameter : catchBlockParameters){
            final PsiType catchType = catchBlockParameter.getType();
            addDependency(catchType);
        }
    }

    public void visitInstanceOfExpression(@NotNull PsiInstanceOfExpression exp) {
        super.visitInstanceOfExpression(exp);
        final PsiTypeElement checkType = exp.getCheckType();
        addDependency(checkType);
    }

    public void visitTypeCastExpression(@NotNull PsiTypeCastExpression exp) {
        super.visitTypeCastExpression(exp);
        final PsiTypeElement castType = exp.getCastType();
        addDependency(castType);
    }

    private void addDependency(PsiTypeElement typeElement) {
        if (typeElement == null) {
            return;
        }
        final PsiType type = typeElement.getType();
        addDependency(type);
    }

    private void addDependency(PsiType type) {
        if (type == null) {
            return;
        }
        final PsiType baseType = type.getDeepComponentType();
        if (ClassUtils.isPrimitive(type)) {
            return;
        }
        final PsiClass containingClass = m_method.getContainingClass();
        final String qualifiedName = containingClass.getQualifiedName();
        if(qualifiedName == null)
        {
            return;
        }
        if (baseType.equalsToText(qualifiedName)) {
            return;
        }
        final String baseTypeName = baseType.getCanonicalText();
        if (!m_includeJavaClasses &&
                    (baseTypeName.startsWith("java.") ||
                    baseTypeName.startsWith("javax."))) {
            return;
        }
        if (baseTypeName.startsWith(qualifiedName + '.')) {
            return;
        }
        if (!m_includeLibraryClasses) {
            final PsiManager manager = m_method.getManager();
            final Project project = manager.getProject();
            final GlobalSearchScope searchScope = GlobalSearchScope.allScope(project);
            final PsiClass aClass = manager.findClass(baseTypeName,
                    searchScope);
            if (aClass == null) {
                return;
            }
            if (LibraryUtil.classIsInLibrary(aClass)) {
                return;
            }
        }
        m_dependencies.add(baseTypeName);
    }

    public int getNumDependencies() {
        return m_dependencies.size();
    }

}
