package cn.bigcoder.plugin.objecthelper.generator.method;

import cn.bigcoder.plugin.objecthelper.common.util.PsiUtils;
import cn.bigcoder.plugin.objecthelper.common.util.StringUtils;
import cn.bigcoder.plugin.objecthelper.generator.AbstractMethodGenerator;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static cn.bigcoder.plugin.objecthelper.common.constant.JavaKeyWord.*;
import static cn.bigcoder.plugin.objecthelper.common.util.PsiUtils.getMethodReturnClassName;
import static cn.bigcoder.plugin.objecthelper.common.util.PsiUtils.getPsiClass;

/**
 * @author: Jindong.Tian
 * @date: 2021-01-09
 **/
public class ObjectPatchMethodGenerator extends AbstractMethodGenerator {



    /**
     * 方法第一个参数名称
     */
    private String firstParameterName;
    private String secondaryParameterName;

    private void init(PsiMethod psiMethod) {
        if (psiMethod == null) {
            return;
        }
        super.project = psiMethod.getProject();
        super.psiMethod = psiMethod;
        this.firstParameterName = getFirstParameter().getName();
        this.secondaryParameterName = getSecondaryParameter().getName();
    }

    public static ObjectPatchMethodGenerator getInstance(PsiMethod psiMethod) {
        ObjectPatchMethodGenerator objectPatchMethodGenerator = new ObjectPatchMethodGenerator();
        objectPatchMethodGenerator.init(psiMethod);
        return objectPatchMethodGenerator;
    }

    /**
     * 此方法中不应该存在判空的操作，依赖环境的建议重写父类的check方法，在生成早期拦截异常情况
     *
     * @return
     */
    @Override
    protected String generateMethodBody() {
        StringBuilder result = new StringBuilder();
        result.append(generateNullCheck());
        for (PsiField field : PsiUtils.getAllPsiFields(getFirstParameterClass())) {
            PsiDocComment docComment=field.getDocComment();
            if(docComment!=null && docComment.getText().indexOf("@skip")>0){
                continue;
            }
            PsiModifierList modifierList = field.getModifierList();
            if (modifierList == null ||
                    modifierList.hasModifierProperty(PsiModifier.STATIC) ||
                    modifierList.hasModifierProperty(PsiModifier.FINAL) ||
                    modifierList.hasModifierProperty(PsiModifier.SYNCHRONIZED)) {
                continue;
            }
            result.append(generateFieldCopyLine(field));
        }
        return result.toString();
    }

    @Override
    protected boolean check() {
        if(!Objects.equals(getSecondaryParameterClass().getName(), getFirstParameterClass().getName())){
            return false;
        }
        return true;
    }

    /**
     * 生成示例：{@code userDTO.setId(user.getId());}
     *
     * @param field
     * @return
     */
    @NotNull
    private String generateFieldCopyLine(PsiField field) {
        return "if (" + firstParameterName + ".get" + StringUtils.firstUpperCase(field.getName()) + "()!=null){" +
                secondaryParameterName + ".set" + StringUtils.firstUpperCase(field.getName()) + "(" + firstParameterName + ".get" + StringUtils.firstUpperCase(field.getName()) + "());" + LINE_SEPARATOR +
                "}";
    }

    /**
     * 生成示例：{@code if (user == null) {return null;}}
     *
     * @return
     */
    private String generateNullCheck() {
        return "if(" + getFirstParameter().getName() + "==null || "+getSecondaryParameter().getName()+"==null){return;}";
    }

    /**
     * 获取参数列表第一个参数的{@code PsiParameter}
     *
     * @return
     */
    private PsiParameter getFirstParameter() {
        return getParameters().get(0);
    }

    private PsiParameter getSecondaryParameter() {
        return getParameters().get(1);
    }

    /**
     * 获取参数列表第一个参数的{@code PsiClass}
     *
     * @return
     */
    private PsiClass getFirstParameterClass() {
        return getPsiClass(getFirstParameter().getType(), project);
    }

    private PsiClass getSecondaryParameterClass() {
        return getPsiClass(getSecondaryParameter().getType(), project);
    }
}