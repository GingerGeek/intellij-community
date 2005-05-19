package com.siyeh.ig.psiutils;

import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;

public class MethodUtils{
    private MethodUtils(){
        super();
    }

    public static boolean isCompareTo(PsiMethod method){
        return methodMatches(method, "compareTo", 1, PsiType.INT);
    }
    public static boolean isHashCode(PsiMethod method){
        return methodMatches(method, "hashCode", 0, PsiType.INT);
    }

    public static boolean isEquals(PsiMethod method){
        return methodMatches(method, "equals", 1, PsiType.BOOLEAN);
    }

    private static boolean methodMatches(PsiMethod method,
                                         String methodNameP,
                                         int parameterCount,
                                         PsiType returnTypeP){
        if(method == null){
            return false;
        }
        final String methodName = method.getName();
        if(!methodNameP.equals(methodName)){
            return false;
        }
        final PsiParameterList parameterList = method.getParameterList();
        if(parameterList == null){
            return false;
        }
        final PsiParameter[] parameters = parameterList.getParameters();
        if(parameters.length != parameterCount){
            return false;
        }
        final PsiType returnType = method.getReturnType();
        return returnTypeP.equals(returnType);
    }

}
