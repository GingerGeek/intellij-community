package com.siyeh.ig.classlayout;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocTag;
import com.intellij.util.IncorrectOperationException;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.ClassInspection;
import com.siyeh.ig.GroupNames;
import com.siyeh.ig.InspectionGadgetsFix;
import org.jetbrains.annotations.NotNull;

public class MissingDeprecatedAnnotationInspection extends ClassInspection{
    private final MissingDeprecatedAnnotationFix fix = new MissingDeprecatedAnnotationFix();

    public String getDisplayName(){
        return "Missing @Deprecated annotation";
    }

    public String getGroupDisplayName(){
        return GroupNames.CLASSLAYOUT_GROUP_NAME;
    }

    public String buildErrorString(PsiElement location){
        return "Missing @Deprecated annotation on '#ref' #loc";
    }

    protected InspectionGadgetsFix buildFix(PsiElement location){
        return fix;
    }

    private static class MissingDeprecatedAnnotationFix
            extends InspectionGadgetsFix{
        public String getName(){
            return "Add @Deprecated annotation";
        }

        public void doFix(Project project, ProblemDescriptor descriptor)
                                                                         throws IncorrectOperationException{

            final PsiElement identifier = descriptor.getPsiElement();
            final PsiModifierListOwner parent =
                    (PsiModifierListOwner) identifier.getParent();
                final PsiManager psiManager = parent.getManager();
                final PsiElementFactory factory = psiManager
                                .getElementFactory();
                final PsiAnnotation annotation = factory
                        .createAnnotationFromText("@java.lang.Deprecated",
                                                  parent);

                final PsiModifierList modifierList = parent.getModifierList();
                modifierList.addAfter(annotation, null);

        }
    }
    public BaseInspectionVisitor buildVisitor(){
        return new MissingDeprecatedAnnotationVisitor();
    }

    private static class MissingDeprecatedAnnotationVisitor
            extends BaseInspectionVisitor{
        private boolean inClass = false;

        public void visitClass(@NotNull PsiClass aClass){
            super.visitClass(aClass);
            final boolean wasInClass = inClass;
            if(!inClass){
                inClass = true;
                final PsiManager manager = aClass.getManager();
                final LanguageLevel languageLevel =
                        manager.getEffectiveLanguageLevel();
                if(languageLevel.equals(LanguageLevel.JDK_1_3) ||
                           languageLevel.equals(LanguageLevel.JDK_1_4)){
                    return;
                }
                if(!hasDeprecatedCommend(aClass)){
                    return;
                }
                if(hasDeprecatedAnnotation(aClass)){
                    return;
                }
                registerClassError(aClass);
            }
            inClass = wasInClass;
        }

        public void visitMethod(@NotNull PsiMethod method){
            final PsiManager manager = method.getManager();
            final LanguageLevel languageLevel =
                    manager.getEffectiveLanguageLevel();
            if(languageLevel.equals(LanguageLevel.JDK_1_3) ||
                       languageLevel.equals(LanguageLevel.JDK_1_4)){
                return;
            }
            if(!hasDeprecatedCommend(method)){
                return;
            }
            if(hasDeprecatedAnnotation(method)){
                return;
            }
            registerMethodError(method);
        }

        public void visitField(@NotNull PsiField field){
            final PsiManager manager = field.getManager();
            final LanguageLevel languageLevel =
                    manager.getEffectiveLanguageLevel();
            if(languageLevel.equals(LanguageLevel.JDK_1_3) ||
                       languageLevel.equals(LanguageLevel.JDK_1_4)){
                return;
            }
            if(!hasDeprecatedCommend(field)){
                return;
            }
            if(hasDeprecatedAnnotation(field)){
                return;
            }
            registerFieldError(field);
        }
    }

    private static boolean hasDeprecatedAnnotation(PsiModifierListOwner element){
        final PsiModifierList modifierList = element.getModifierList();
        if(modifierList == null){
            return false;
        }
        final PsiAnnotation[] annotations = modifierList.getAnnotations();
        if(annotations == null){
            return false;
        }
        for(final PsiAnnotation annotation : annotations){
            final PsiJavaCodeReferenceElement reference = annotation.getNameReferenceElement();
            final PsiClass annotationClass =
                    (PsiClass) reference.resolve();
            if(annotationClass == null){
                return false;
            }
            final String annotationClassName =
                    annotationClass.getQualifiedName();
            if("java.lang.Deprecated".equals(annotationClassName)){
                return true;
            }
        }
        return false;
    }

    private  static boolean hasDeprecatedCommend(PsiDocCommentOwner element){
        final PsiDocComment comment = element.getDocComment();
        if(comment == null){
            return false;
        }
        final PsiDocTag[] tags = comment.getTags();
        for(PsiDocTag tag : tags){
            final String tagName = tag.getName();
            if("deprecated".equals(tagName)){
                return true;
            }
        }
        return false;
    }
}