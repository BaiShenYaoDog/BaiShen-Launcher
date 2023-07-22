package cn.ChengZhiYa.BaiShenLauncher.ui.construct;

import cn.ChengZhiYa.BaiShenLauncher.util.StringUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import com.jfoenix.validation.base.ValidatorBase;
import javafx.beans.NamedArg;
import javafx.scene.control.TextInputControl;

public class RequiredValidator extends ValidatorBase {

    public RequiredValidator() {
        this(I18n.i18n("input.not_empty"));
    }

    public RequiredValidator(@NamedArg("message") String message) {
        super(message);
    }

    @Override
    protected void eval() {
        if (srcControl.get() instanceof TextInputControl) {
            evalTextInputField();
        }
    }

    private void evalTextInputField() {
        TextInputControl textField = ((TextInputControl) srcControl.get());

        hasErrors.set(StringUtils.isBlank(textField.getText()));
    }
}
