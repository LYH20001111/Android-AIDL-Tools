package com.hudou.tools.dialog;

interface INotifyDialogListener {

    /**
     * 点击取消后执行的行为
     */
    void onCancel();

    /**
     * 点击确定后执行的行为
     */
    void onConfirm();

}