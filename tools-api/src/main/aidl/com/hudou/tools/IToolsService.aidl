package com.hudou.tools;

import com.hudou.tools.dialog.IDialog;
import com.hudou.tools.keyboard.IKeyboard;

interface IToolsService {

    IDialog getDialog();

    IKeyboard getKeyboard();
}