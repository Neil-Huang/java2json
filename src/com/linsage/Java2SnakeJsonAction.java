package com.linsage;

import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.annotations.NonNls;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

public class Java2SnakeJsonAction extends AnAction {
    private static NotificationGroup notificationGroup;
    private static String pattern = "yyyy-MM-dd HH:mm:ss";
    private static DateFormat df = new SimpleDateFormat(pattern);

    @NonNls
    private static final Map<String, Object> normalTypes = new HashMap<>();

    static {
        notificationGroup = new NotificationGroup("Java2Json.NotificationGroup", NotificationDisplayType.BALLOON, true);

        normalTypes.put("Boolean", false);
        normalTypes.put("Byte", 0);
        normalTypes.put("Short", Short.valueOf((short) 0));
        normalTypes.put("Integer", 0);
        normalTypes.put("Long", 0L);
        normalTypes.put("Float", 0.0F);
        normalTypes.put("Double", 0.0D);
        normalTypes.put("String", "");
        normalTypes.put("BigDecimal", 0.0);
        normalTypes.put("Date", df.format(new Date()));
        normalTypes.put("Timestamp", System.currentTimeMillis());
        normalTypes.put("LocalDate", LocalDate.now().toString());
        normalTypes.put("LocalTime", LocalTime.now().toString());
        normalTypes.put("LocalDateTime", LocalDateTime.now().toString());

    }

    private static boolean isNormalType(String typeName) {
        return normalTypes.containsKey(typeName);
    }


    @Override
    public void actionPerformed(AnActionEvent e) {
        Editor editor = (Editor) e.getDataContext().getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = (PsiFile) e.getDataContext().getData(CommonDataKeys.PSI_FILE);
        Project project = editor.getProject();
        PsiElement referenceAt = psiFile.findElementAt(editor.getCaretModel().getOffset());
        PsiClass selectedClass = (PsiClass) PsiTreeUtil.getContextOfType(referenceAt, new Class[]{PsiClass.class});
        try {
            KV kv = getFields(selectedClass);
            String json = kv.toPrettySnakeJson();

            StringSelection selection = new StringSelection(json);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);
            String message = "Convert " + selectedClass.getName() + " to snake JSON success, copied to clipboard.";
            Notification success = notificationGroup.createNotification(message, NotificationType.INFORMATION);
            Notifications.Bus.notify(success, project);
        } catch (Exception ex) {
            Notification error = notificationGroup.createNotification("Convert to JSON failed.", NotificationType.ERROR);
            Notifications.Bus.notify(error, project);
        }
    }


    public static KV getFields(PsiClass psiClass) {
        KV kv = KV.create();
        KV commentKV = KV.create();

        if (psiClass != null) {
            for (PsiField field : psiClass.getAllFields()) {
                PsiType type = field.getType();
                String name = field.getName();
                name = translateName(name);


                //doc comment
                if (field.getDocComment() != null && field.getDocComment().getText() != null) {
                    commentKV.set(name, field.getDocComment().getText());
                }

                if (type instanceof PsiPrimitiveType) {       //primitive Type
                    kv.set(name, PsiTypesUtil.getDefaultValue(type));
                } else {    //reference Type
                    String fieldTypeName = type.getPresentableText();
                    if (isNormalType(fieldTypeName)) {    //normal Type
                        kv.set(name, normalTypes.get(fieldTypeName));
                    } else if (type instanceof PsiArrayType) {   //array type
                        PsiType deepType = type.getDeepComponentType();
                        ArrayList list = new ArrayList<>();
                        String deepTypeName = deepType.getPresentableText();
                        if (deepType instanceof PsiPrimitiveType) {
                            list.add(PsiTypesUtil.getDefaultValue(deepType));
                        } else if (isNormalType(deepTypeName)) {
                            list.add(normalTypes.get(deepTypeName));
                        } else {
                            list.add(getFields(PsiUtil.resolveClassInType(deepType)));
                        }
                        kv.set(name, list);
                    } else if (fieldTypeName.startsWith("List")) {   //list type
                        PsiType iterableType = PsiUtil.extractIterableTypeParameter(type, false);
                        PsiClass iterableClass = PsiUtil.resolveClassInClassTypeOnly(iterableType);
                        ArrayList list = new ArrayList<>();
                        String classTypeName = iterableClass.getName();
                        if (isNormalType(classTypeName)) {
                            list.add(normalTypes.get(classTypeName));
                        } else {
                            list.add(getFields(iterableClass));
                        }
                        kv.set(name, list);
                    } else if (PsiUtil.resolveClassInClassTypeOnly(type).isEnum()) { //enum
                        ArrayList namelist = new ArrayList<String>();
                        PsiField[] fieldList = PsiUtil.resolveClassInClassTypeOnly(type).getFields();
                        if (fieldList != null) {
                            for (PsiField f : fieldList) {
                                if (f instanceof PsiEnumConstant) {
                                    namelist.add(f.getName());
                                }
                            }
                        }
                        kv.set(name, namelist);
                    } else {    //class type
                        //System.out.println(name + ":" + type);
                        kv.set(name, getFields(PsiUtil.resolveClassInType(type)));
                    }
                }
            }

            if (commentKV.size() > 0) {
                kv.set("@comment", commentKV);
            }
        }

        return kv;
    }


    public static String translateName(String f) {
        return separateCamelCase(f, "_").toLowerCase(Locale.ENGLISH);
    }


    static String separateCamelCase(String name, String separator) {
        StringBuilder translation = new StringBuilder();
        int i = 0;

        for(int length = name.length(); i < length; ++i) {
            char character = name.charAt(i);
            if (Character.isUpperCase(character) && translation.length() != 0) {
                translation.append(separator);
            }

            translation.append(character);
        }

        return translation.toString();
    }

}