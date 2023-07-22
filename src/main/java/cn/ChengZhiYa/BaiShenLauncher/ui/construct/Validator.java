package cn.ChengZhiYa.BaiShenLauncher.ui.construct;

import cn.ChengZhiYa.BaiShenLauncher.util.javafx.SafeStringConverter;
import com.jfoenix.controls.JFXTextField;
import com.jfoenix.validation.base.ValidatorBase;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.scene.control.TextInputControl;

import java.util.function.Consumer;
import java.util.function.Predicate;

public final class Validator extends ValidatorBase {

    private final Predicate<String> validator;

    /**
     * @param validator return true if the input string is valid.
     */
    public Validator(Predicate<String> validator) {
        this.validator = validator;
    }

    public Validator(String message, Predicate<String> validator) {
        this(validator);

        setMessage(message);
    }

    public static Consumer<Predicate<String>> addTo(JFXTextField control) {
        return addTo(control, null);
    }

    /**
     * @see SafeStringConverter#asPredicate(Consumer)
     */
    public static Consumer<Predicate<String>> addTo(JFXTextField control, String message) {
        return predicate -> {
            Validator validator = new Validator(message, predicate);
            InvalidationListener listener = any -> control.validate();
            validator.getProperties().put(validator, listener);
            control.textProperty().addListener(new WeakInvalidationListener(listener));
            control.getValidators().add(validator);
        };
    }

    @Override
    protected void eval() {
        if (this.srcControl.get() instanceof TextInputControl) {
            String text = ((TextInputControl) srcControl.get()).getText();
            hasErrors.set(!validator.test(text));
        }
    }
}
