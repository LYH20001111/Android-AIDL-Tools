package com.hudou.tools.dialog;

import com.hudou.tools.dialog.INotifyDialogListener;
import com.hudou.tools.dialog.EditParameters;
import com.hudou.tools.dialog.IEditDialogListener;

interface IDialog {

    void notifyDialog(String title, in INotifyDialogListener callback);

    void editDialog(in EditParameters params, IEditDialogListener callback);


}