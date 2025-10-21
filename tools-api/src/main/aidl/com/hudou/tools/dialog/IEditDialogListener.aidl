package com.hudou.tools.dialog;

interface IEditDialogListener {

    /**
     * 获取输入到EditText的文本信息
     *
     * @param message 输入信息
     */
    void onResult(String message);
}