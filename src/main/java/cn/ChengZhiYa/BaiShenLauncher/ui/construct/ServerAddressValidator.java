 package cn.ChengZhiYa.BaiShenLauncher.ui.construct;

import cn.ChengZhiYa.BaiShenLauncher.util.StringUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import com.jfoenix.validation.base.ValidatorBase;
import javafx.beans.NamedArg;
import javafx.scene.control.TextInputControl;

import java.util.regex.Pattern;

public class ServerAddressValidator extends ValidatorBase {
    private static final Pattern PATTERN = Pattern.compile("[-a-zA-Z0-9@:%._+~#=]{1,256}(:\\d+)?");
    private final boolean nullable;

    public ServerAddressValidator() {
        this(false);
    }

    public ServerAddressValidator(@NamedArg("nullable") boolean nullable) {
        this(I18n.i18n("input.url"), nullable);
    }

    public ServerAddressValidator(@NamedArg("message") String message, @NamedArg("nullable") boolean nullable) {
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
            hasErrors.set(!PATTERN.matcher(textField.getText()).matches());
    }
}
