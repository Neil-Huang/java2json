package com.wawa;

import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.actionSystem.EditorWriteActionHandler;
import com.intellij.openapi.project.Project;
import com.wawa.utils.UnicodeUtil;

/**
 * 把选中的内容添加前缀和后缀
 *
 * @author kevin
 * @date 2020/06/16
 */
public class Unicode2Chinese extends EditorAction {

    protected Unicode2Chinese(){
        super(new EditHandler());
    }

    private static NotificationGroup notificationGroup;
    static {
        notificationGroup = new NotificationGroup("Java2Json.NotificationGroup", NotificationDisplayType.BALLOON, true);
    }

    public static class EditHandler extends EditorWriteActionHandler{
        @Override
        public void executeWriteAction(final Editor editor, DataContext var2) {
            Project project = editor.getProject();
            try {
                SelectionModel selectionModel = editor.getSelectionModel();
                String selectedText = selectionModel.getSelectedText();

                String decodeSelectTxt = UnicodeUtil.decodeUnicode(selectedText);
                System.out.println("select text :" + selectedText);

                String reTxt = decodeSelectTxt;
                //覆盖插入
                EditorModificationUtil.insertStringAtCaret(editor, reTxt, true, false);

            }catch (Exception e){
                Notification error = notificationGroup.createNotification("Unicode to 中文 failed.",NotificationType.ERROR);
                Notifications.Bus.notify(error, project);
            }
        }
    }







}
