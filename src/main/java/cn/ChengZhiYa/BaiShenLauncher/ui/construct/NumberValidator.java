package cn.ChengZhiYa.BaiShenLauncher.ui.construct;

import cn.ChengZhiYa.BaiShenLauncher.util.Lang;
import cn.ChengZhiYa.BaiShenLauncher.util.StringUtils;
import com.jfoenix.validation.base.ValidatorBase;
import javafx.beans.NamedArg;
import javafx.scene.control.TextInputControl;

public class NumberValidator extends ValidatorBase {
    private final boolean nullable;

    public NumberValidator() {
        this(false);
    }

    public NumberValidator(@NamedArg("nullable") boolean nullable) {
        this.nullable = nullable;
    }

    public NumberValidator(@NamedArg("message") String message, @NamedArg("nullable") boolean nullable) {
        super(message);
        this.nullable = nullable;
    }

    @Override
    protected void eval() {
        if (srcControl.get() instanceof TextInputControl) {
            evalTextInputField();
        }
    }

    private void evalTextInputField() {
        TextInputControl textField = ((TextInputControl) srcControl.get());

        if (StringUtils.isBlank(textField.getText()))
            hasErrors.set(!nullable);
        else
            hasErrors.set(Lang.toIntOrNull(textField.getText()) == null);
    }
}
