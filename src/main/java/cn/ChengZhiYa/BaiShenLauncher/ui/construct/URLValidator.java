package cn.ChengZhiYa.BaiShenLauncher.ui.construct;

import cn.ChengZhiYa.BaiShenLauncher.util.StringUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import com.jfoenix.validation.base.ValidatorBase;
import javafx.beans.NamedArg;
import javafx.scene.control.TextInputControl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

public class URLValidator extends ValidatorBase {
    private final boolean nullable;

    public URLValidator() {
        this(false);
    }

    public URLValidator(@NamedArg("nullable") boolean nullable) {
        this(I18n.i18n("input.url"), nullable);
    }

    public URLValidator(@NamedArg("message") String message, @NamedArg("nullable") boolean nullable) {
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
        else {
            try {
                new URL(textField.getText()).toURI();
                hasErrors.set(false);
            } catch (IOException | URISyntaxException e) {
                hasErrors.set(true);
            }
        }
    }
}
