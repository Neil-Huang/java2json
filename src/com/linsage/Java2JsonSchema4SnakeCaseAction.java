package com.linsage;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import org.codehaus.jettison.json.JSONException;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class Java2JsonSchema4SnakeCaseAction extends AnAction {

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
            KV kv = getJsonSchema(selectedClass,project);
            String json = kv.toPrettyJson();

            StringSelection selection = new StringSelection(json);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);
            String message = "Convert " + selectedClass.getName() + " to snake JSON success, copied to clipboard.";
            Notification success = notificationGroup.createNotification(message, NotificationType.INFORMATION);
            Notifications.Bus.notify(success, project);
        } catch (Exception ex) {
            Notification error = notificationGroup.createNotification("Convert to JSON failed.",
                    NotificationType.ERROR);
            Notifications.Bus.notify(error, project);
        }
    }

    public static KV getFields(PsiClass psiClass, Project project) throws JSONException {
        KV kv = KV.create();

        if (psiClass != null) {
            for (PsiField field : psiClass.getAllFields()) {
                PsiType type = field.getType();
                String name = field.getName();
                String remark = "";
                handleAnnotations(kv,type,name);
                //snake case
                name = toSnakeCase(name);

                //doc comment
                if (field.getDocComment() != null && field.getDocComment().getText() != null) {
                    remark = docHelper(field.getDocComment().getText());
                }

                // 如果是基本类型
                if (type instanceof PsiPrimitiveType) {
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("type", type.getPresentableText());
                    if (!Strings.isNullOrEmpty(remark)) {
                        jsonObject.addProperty("description", remark);
                    }
                    kv.set(name, jsonObject);


                } else {

                    //reference Type
                    String fieldTypeName = type.getPresentableText();

                    //normal Type
                    if (isNormalType(fieldTypeName)) {
                        JsonObject jsonObject = new JsonObject();
                        jsonObject.addProperty("type", fieldTypeName);
                        if (!Strings.isNullOrEmpty(remark)) {
                            jsonObject.addProperty("description", remark);
                        }
                        kv.set(name, jsonObject);

                    } else if (type instanceof PsiArrayType) {
                        //array type
                        PsiType deepType = type.getDeepComponentType();
                        KV kvlist = new KV();
                        String deepTypeName = deepType.getPresentableText();
                        if (deepType instanceof PsiPrimitiveType) {
                            kvlist.set("type", type.getPresentableText());
                            if (!Strings.isNullOrEmpty(remark)) {
                                kvlist.set("description", remark);
                            }
                        } else if (isNormalType(deepTypeName)) {
                            kvlist.set("type", deepTypeName);
                            if (!Strings.isNullOrEmpty(remark)) {
                                kvlist.set("description", remark);
                            }
                        } else {
                            kvlist.set(KV.by("type", "object"));
                            if (!Strings.isNullOrEmpty(remark)) {
                                kvlist.set(KV.by("description", remark));
                            }
                            kvlist.set("properties", getFields(PsiUtil.resolveClassInType(deepType), project));
                        }
                        KV kv1 = new KV();
                        kv1.set(KV.by("type", "array"));
                        if (!Strings.isNullOrEmpty(remark)) {
                            kv1.set(KV.by("description", remark));
                        }
                        kv1.set("items", kvlist);
                        kv.set(name, kv1);

                    } else if (fieldTypeName.startsWith("List") || fieldTypeName.startsWith("Set") || fieldTypeName.startsWith(
                            "HashSet")) {
                        //list type
                        PsiType iterableType = PsiUtil.extractIterableTypeParameter(type, false);
                        PsiClass iterableClass = PsiUtil.resolveClassInClassTypeOnly(iterableType);
                        KV kvlist = new KV();
                        String classTypeName = iterableClass.getName();
                        if (isNormalType(classTypeName)) {
                            kvlist.set("type", classTypeName);
                            if (!Strings.isNullOrEmpty(remark)) {
                                kvlist.set("description", remark);
                            }
                        } else {
                            kvlist.set(KV.by("type", "object"));
                            if (!Strings.isNullOrEmpty(remark)) {
                                kvlist.set(KV.by("description", remark));
                            }
                            kvlist.set("properties", getFields(iterableClass, project));
                        }
                        KV kv1 = new KV();
                        kv1.set(KV.by("type", "array"));
                        if (!Strings.isNullOrEmpty(remark)) {
                            kv1.set(KV.by("description", remark));
                        }
                        kv1.set("items", kvlist);
                        kv.set(name, kv1);
                    } else if (fieldTypeName.startsWith("HashMap") || fieldTypeName.startsWith("Map")) {
                        //HashMap or Map
                        CompletableFuture.runAsync(() -> {
                            try {
                                TimeUnit.MILLISECONDS.sleep(700);
                                Notification warning = notificationGroup.createNotification(
                                        "Map Type Can not Change,So pass",
                                        NotificationType.WARNING);
                                Notifications.Bus.notify(warning, project);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        });

                    } else if (PsiUtil.resolveClassInClassTypeOnly(type).isEnum()) {
                        //enum
                        ArrayList namelist = new ArrayList<String>();
                        PsiField[] fieldList = PsiUtil.resolveClassInClassTypeOnly(type).getFields();
                        if (fieldList != null) {
                            for (PsiField f : fieldList) {
                                if (f instanceof PsiEnumConstant) {
                                    namelist.add(f.getName());
                                }
                            }
                        }

                        KV kv1 = new KV();
                        kv1.set(KV.by("type", "string"));
                        if (!Strings.isNullOrEmpty(remark)) {
                            kv1.set(KV.by("description", remark));
                        }
                        kv1.set("enum", namelist);
                        kv1.set("enumDesc","");
                        kv.set(name, kv1);

                    } else {
                        //class type
                        KV kv1 = new KV();
                        kv1.set(KV.by("type", "object"));
                        if (!Strings.isNullOrEmpty(remark)) {
                            kv1.set(KV.by("description", remark));
                        }
                        kv1.set(KV.by("properties", getFields(PsiUtil.resolveClassInType(type), project)));
                        kv.set(name, kv1);
                    }
                }
            }
        }

        return kv;
    }

    public static KV getJsonSchema(PsiClass psiClass, Project project) throws JSONException {

        String name = psiClass.getName();
        String remark = psiClass.getDocComment() == null || psiClass.getDocComment().getText() == null ? "" : psiClass.getDocComment().getText();

        KV kv = new KV();
        kv.set("type", "object");
        kv.set("description", remark);
        kv.set("title",name);
        kv.set("properties", getFields(psiClass, project));
        return kv;
    }

    private static void handleAnnotations(KV kv, PsiType type, String name){
        //todo:处理验证参数的注解
        for (PsiAnnotation annotation : type.getAnnotations()) {
            if ("javax.validation.constraints.NotNull".equals(annotation.getQualifiedName())){
                System.out.println(name);
            }
        }

    }

    public static String docHelper(String doc){
        //todo:优化javadoc的读取
        String str = doc.replace("*", "").replace("/", "");
        return str;
    }



    // 蛇形
    public static String toSnakeCase(String f) {
        return separateCamelCase(f, "_").toLowerCase(Locale.ENGLISH);
    }


    static String separateCamelCase(String name, String separator) {
        StringBuilder translation = new StringBuilder();
        int i = 0;

        for (int length = name.length(); i < length; ++i) {
            char character = name.charAt(i);
            if (Character.isUpperCase(character) && translation.length() != 0) {
                translation.append(separator);
            }

            translation.append(character);
        }

        return translation.toString();
    }

}
